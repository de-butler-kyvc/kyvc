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
from app.credentials.crypto import public_key_from_jwk, sign_der_base64url, verify_der_base64url
from app.credentials.did import account_from_did
from app.credentials.hexutil import bytes_to_hex

PROOF_DOMAIN = b"POC-DATA-INTEGRITY-v1"
CRYPTOSUITE = "ecdsa-secp256k1-jcs-poc-2026"
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


def proof_options_without_value(proof: dict[str, Any]) -> dict[str, Any]:
    cleaned = copy.deepcopy(proof)
    cleaned.pop("proofValue", None)
    return cleaned


def signing_input(document_without_proof: dict[str, Any], proof_without_value: dict[str, Any]) -> bytes:
    return PROOF_DOMAIN + canonical_json(document_without_proof) + b"." + canonical_json(proof_without_value)


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


def add_vc_proof(vc: dict[str, Any], private_key: EllipticCurvePrivateKey, verification_method: str) -> dict[str, Any]:
    signed = copy.deepcopy(vc)
    proof = {
        "type": "DataIntegrityProof",
        "cryptosuite": CRYPTOSUITE,
        "created": now_iso(),
        "verificationMethod": verification_method,
        "proofPurpose": "assertionMethod",
    }
    proof["proofValue"] = sign_der_base64url(
        private_key,
        signing_input(without_keys(signed, "proof"), proof),
    )
    signed["proof"] = proof
    return signed


def verify_vc_signature(vc: dict[str, Any], public_jwk: dict[str, Any]) -> bool:
    proof = vc.get("proof") or {}
    signature = proof.get("proofValue")
    if not isinstance(signature, str):
        return False
    public_key = public_key_from_jwk(public_jwk)
    return verify_der_base64url(
        public_key,
        signature,
        signing_input(without_keys(vc, "proof"), proof_options_without_value(proof)),
    )


def issuer_account_from_vc(vc: dict[str, Any]) -> str:
    return account_from_did(str(vc["issuer"]))


def subject_account_from_vc(vc: dict[str, Any]) -> str:
    return account_from_did(str(vc["credentialSubject"]["id"]))
