import copy
import secrets
import uuid
from datetime import UTC, datetime
from typing import Any

from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey

from app.credential_schema.kyc import (
    KYC_CREDENTIAL_TYPE,
    REVOCATION_STATUS_PURPOSE,
    VC_CONTEXTS,
    VERIFIABLE_CREDENTIAL_TYPE,
    XRPL_CREDENTIAL_STATUS_TYPE,
    kyc_credential_subject,
)
from app.credentials.canonical import canonical_json, sha256_bytes
from app.credentials.crypto import (
    decode_compact_jws,
    public_key_from_jwk,
    sign_compact_jws,
    verify_compact_jws,
)
from app.credentials.did import account_from_did
from app.credentials.hexutil import bytes_to_hex

JWS_PROOF_TYPE = "JsonWebSignature"
VC_JWS_TYP = "vc+jwt"
VC_JWS_CTY = "vc"
VC_JWT_MEDIA_TYPE = "application/vc+jwt"
ENVELOPED_VC_TYPE = "EnvelopedVerifiableCredential"
STATUS_PREFIX = b"VC_STATUS_V1:"
SALT_BYTES = 32


def generate_credential_salt() -> str:
    return secrets.token_urlsafe(SALT_BYTES)


def now_iso() -> str:
    return datetime.now(tz=UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def isoformat_z(dt: datetime) -> str:
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=UTC)
    return dt.astimezone(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def parse_datetime(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(UTC)


def without_keys(document: dict[str, Any], *keys: str) -> dict[str, Any]:
    doc = copy.deepcopy(document)
    for key in keys:
        doc.pop(key, None)
    return doc


def vc_core_for_status(vc: dict[str, Any]) -> dict[str, Any]:
    return without_keys(vc, "credentialStatus", "proof")


def vc_core_hash(vc: dict[str, Any]) -> bytes:
    return sha256_bytes(canonical_json(vc_core_for_status(vc)))


def credential_type_bytes_for_vc(vc: dict[str, Any]) -> bytes:
    credential_type = STATUS_PREFIX + vc_core_hash(vc)
    if len(credential_type) > 64:
        raise ValueError("XRPL CredentialType exceeds 64 bytes")
    return credential_type


def credential_type_hex_for_vc(vc: dict[str, Any]) -> str:
    return bytes_to_hex(credential_type_bytes_for_vc(vc))


def make_credential_status(vc: dict[str, Any], issuer_account: str, holder_account: str) -> dict[str, Any]:
    core_hash = vc_core_hash(vc)
    credential_type_hex = bytes_to_hex(STATUS_PREFIX + core_hash)
    return {
        "id": f"xrpl:credential:{issuer_account}:{holder_account}:{credential_type_hex}",
        "type": XRPL_CREDENTIAL_STATUS_TYPE,
        "statusPurpose": REVOCATION_STATUS_PURPOSE,
        "issuer": issuer_account,
        "subject": holder_account,
        "credentialType": credential_type_hex,
        "vcCoreHash": bytes_to_hex(core_hash),
    }


def build_kyc_vc(
    issuer_did: str,
    holder_did: str,
    claims: dict[str, Any],
    valid_from: datetime,
    valid_until: datetime,
) -> dict[str, Any]:
    return {
        "@context": VC_CONTEXTS,
        "id": f"urn:uuid:{uuid.uuid4()}",
        "type": [VERIFIABLE_CREDENTIAL_TYPE, KYC_CREDENTIAL_TYPE],
        "issuer": issuer_did,
        "validFrom": isoformat_z(valid_from),
        "validUntil": isoformat_z(valid_until),
        "credentialSalt": generate_credential_salt(),
        "credentialSubject": kyc_credential_subject(holder_did, claims),
    }


def secure_vc_jwt(vc: dict[str, Any], private_key: EllipticCurvePrivateKey, verification_method: str) -> str:
    return sign_compact_jws(
        private_key,
        without_keys(vc, "proof"),
        {
            "typ": VC_JWS_TYP,
            "cty": VC_JWS_CTY,
            "kid": verification_method,
            "iss": vc["issuer"],
        },
    )


def decode_vc_jwt(vc_jwt: str) -> tuple[dict[str, Any], dict[str, Any]]:
    try:
        protected, payload, _, _ = decode_compact_jws(vc_jwt)
        return protected, payload
    except Exception as exc:
        raise ValueError("invalid vc+jwt") from exc


def enveloped_vc(vc_jwt: str) -> dict[str, Any]:
    return {
        "@context": "https://www.w3.org/ns/credentials/v2",
        "id": f"data:{VC_JWT_MEDIA_TYPE},{vc_jwt}",
        "type": ENVELOPED_VC_TYPE,
    }


def vc_jwt_from_enveloped(enveloped: dict[str, Any]) -> str:
    if enveloped.get("type") != ENVELOPED_VC_TYPE:
        raise ValueError("expected EnvelopedVerifiableCredential")
    credential_id = str(enveloped["id"])
    prefix = f"data:{VC_JWT_MEDIA_TYPE},"
    if not credential_id.startswith(prefix):
        raise ValueError("expected application/vc+jwt data URL")
    return credential_id[len(prefix) :]


def add_vc_compatibility_proof(
    vc: dict[str, Any],
    private_key: EllipticCurvePrivateKey,
    verification_method: str,
) -> dict[str, Any]:
    signed = copy.deepcopy(vc)
    created = now_iso()
    proof = {
        "type": JWS_PROOF_TYPE,
        "created": created,
        "verificationMethod": verification_method,
        "proofPurpose": "assertionMethod",
    }
    proof["jws"] = sign_compact_jws(
        private_key,
        without_keys(signed, "proof"),
        {
            "typ": VC_JWS_TYP,
            "cty": VC_JWS_CTY,
            "kid": verification_method,
            "created": created,
            "proofPurpose": "assertionMethod",
        },
    )
    signed["proof"] = proof
    return signed


def add_vc_proof(vc: dict[str, Any], private_key: EllipticCurvePrivateKey, verification_method: str) -> dict[str, Any]:
    return add_vc_compatibility_proof(vc, private_key, verification_method)


def verify_vc_jwt(vc_jwt: str, public_jwk: dict[str, Any], verification_method: str) -> bool:
    public_key = public_key_from_jwk(public_jwk)
    try:
        protected, payload = decode_vc_jwt(vc_jwt)
    except ValueError:
        return False
    issuer = payload.get("issuer")
    return verify_compact_jws(
        public_key,
        vc_jwt,
        expected_headers={
            "typ": VC_JWS_TYP,
            "cty": VC_JWS_CTY,
            "kid": verification_method,
            "iss": issuer,
        },
    )


def verify_vc_compatibility_proof(vc: dict[str, Any], public_jwk: dict[str, Any]) -> bool:
    proof = vc.get("proof") or {}
    jws = proof.get("jws")
    verification_method = proof.get("verificationMethod")
    created = proof.get("created")
    if (
        proof.get("type") != JWS_PROOF_TYPE
        or not isinstance(jws, str)
        or not isinstance(verification_method, str)
        or not isinstance(created, str)
    ):
        return False
    public_key = public_key_from_jwk(public_jwk)
    return verify_compact_jws(
        public_key,
        jws,
        without_keys(vc, "proof"),
        {
            "typ": VC_JWS_TYP,
            "cty": VC_JWS_CTY,
            "kid": verification_method,
            "created": created,
            "proofPurpose": "assertionMethod",
        },
    )


def verify_vc_signature(vc: dict[str, Any] | str, public_jwk: dict[str, Any]) -> bool:
    if isinstance(vc, str):
        try:
            protected, _ = decode_vc_jwt(vc)
        except ValueError:
            return False
        kid = protected.get("kid")
        return isinstance(kid, str) and verify_vc_jwt(vc, public_jwk, kid)
    return verify_vc_compatibility_proof(vc, public_jwk)


def issuer_account_from_vc(vc: dict[str, Any]) -> str:
    return account_from_did(str(vc["issuer"]))


def subject_account_from_vc(vc: dict[str, Any]) -> str:
    return account_from_did(str(vc["credentialSubject"]["id"]))
