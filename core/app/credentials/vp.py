import copy
from typing import Any

from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey

from app.credential_schema.kyc import VERIFIABLE_PRESENTATION_TYPE, VP_CONTEXTS
from app.credentials.crypto import (
    decode_compact_jws,
    public_key_from_jwk,
    sign_compact_jws,
    verify_compact_jws,
)
from app.credentials.vc import (
    ENVELOPED_VC_TYPE,
    JWS_PROOF_TYPE,
    VC_JWT_MEDIA_TYPE,
    enveloped_vc,
    now_iso,
    vc_jwt_from_enveloped,
    without_keys,
)

VP_JWS_TYP = "vp+jwt"
VP_JWS_CTY = "vp"
VP_JWT_MEDIA_TYPE = "application/vp+jwt"
ENVELOPED_VP_TYPE = "EnvelopedVerifiablePresentation"


def create_vp(
    holder_did: str,
    vc: str | dict[str, Any],
    challenge: str,
    domain: str,
    private_key: EllipticCurvePrivateKey,
    verification_method: str,
) -> str:
    if not isinstance(vc, str):
        raise ValueError("standard VP creation requires a secured VC JWT")
    vp = {
        "@context": VP_CONTEXTS,
        "type": [VERIFIABLE_PRESENTATION_TYPE],
        "holder": holder_did,
        "verifiableCredential": [enveloped_vc(vc)],
    }
    return secure_vp_jwt(vp, challenge, domain, private_key, verification_method)


def secure_vp_jwt(
    vp: dict[str, Any],
    challenge: str,
    domain: str,
    private_key: EllipticCurvePrivateKey,
    verification_method: str,
) -> str:
    return sign_compact_jws(
        private_key,
        vp,
        {
            "typ": VP_JWS_TYP,
            "cty": VP_JWS_CTY,
            "kid": verification_method,
            "challenge": challenge,
            "domain": domain,
        },
    )


def decode_vp_jwt(vp_jwt: str) -> tuple[dict[str, Any], dict[str, Any]]:
    try:
        protected, payload, _, _ = decode_compact_jws(vp_jwt)
        return protected, payload
    except Exception as exc:
        raise ValueError("invalid vp+jwt") from exc


def enveloped_vp(vp_jwt: str) -> dict[str, Any]:
    return {
        "@context": "https://www.w3.org/ns/credentials/v2",
        "id": f"data:{VP_JWT_MEDIA_TYPE},{vp_jwt}",
        "type": ENVELOPED_VP_TYPE,
    }


def vp_jwt_from_enveloped(enveloped: dict[str, Any]) -> str:
    if enveloped.get("type") != ENVELOPED_VP_TYPE:
        raise ValueError("expected EnvelopedVerifiablePresentation")
    presentation_id = str(enveloped["id"])
    prefix = f"data:{VP_JWT_MEDIA_TYPE},"
    if not presentation_id.startswith(prefix):
        raise ValueError("expected application/vp+jwt data URL")
    return presentation_id[len(prefix) :]


def create_compatibility_vp(
    holder_did: str,
    vc: dict[str, Any],
    challenge: str,
    domain: str,
    private_key: EllipticCurvePrivateKey,
    verification_method: str,
) -> dict[str, Any]:
    vp = {
        "@context": VP_CONTEXTS,
        "type": [VERIFIABLE_PRESENTATION_TYPE],
        "holder": holder_did,
        "verifiableCredential": [copy.deepcopy(vc)],
    }
    created = now_iso()
    proof = {
        "type": JWS_PROOF_TYPE,
        "created": created,
        "verificationMethod": verification_method,
        "proofPurpose": "authentication",
        "challenge": challenge,
        "domain": domain,
    }
    proof["jws"] = sign_compact_jws(
        private_key,
        vp,
        {
            "typ": VP_JWS_TYP,
            "cty": VP_JWS_CTY,
            "kid": verification_method,
            "created": created,
            "proofPurpose": "authentication",
            "challenge": challenge,
            "domain": domain,
        },
    )
    vp["proof"] = proof
    return vp


def verify_vp_jwt(vp_jwt: str, public_jwk: dict[str, Any], verification_method: str) -> bool:
    return verify_compact_jws(
        public_key_from_jwk(public_jwk),
        vp_jwt,
        expected_headers={
            "typ": VP_JWS_TYP,
            "cty": VP_JWS_CTY,
            "kid": verification_method,
        },
    )


def verify_vp_compatibility_proof(vp: dict[str, Any], public_jwk: dict[str, Any]) -> bool:
    proof = vp.get("proof") or {}
    jws = proof.get("jws")
    verification_method = proof.get("verificationMethod")
    created = proof.get("created")
    challenge = proof.get("challenge")
    domain = proof.get("domain")
    if (
        proof.get("type") != JWS_PROOF_TYPE
        or not isinstance(jws, str)
        or not isinstance(verification_method, str)
        or not isinstance(created, str)
        or not isinstance(challenge, str)
        or not isinstance(domain, str)
    ):
        return False
    return verify_compact_jws(
        public_key_from_jwk(public_jwk),
        jws,
        without_keys(vp, "proof"),
        {
            "typ": VP_JWS_TYP,
            "cty": VP_JWS_CTY,
            "kid": verification_method,
            "created": created,
            "proofPurpose": "authentication",
            "challenge": challenge,
            "domain": domain,
        },
    )


def verify_vp_signature(vp: dict[str, Any] | str, public_jwk: dict[str, Any]) -> bool:
    if isinstance(vp, str):
        try:
            protected, _ = decode_vp_jwt(vp)
        except ValueError:
            return False
        kid = protected.get("kid")
        return isinstance(kid, str) and verify_vp_jwt(vp, public_jwk, kid)
    if vp.get("type") == ENVELOPED_VP_TYPE:
        try:
            return verify_vp_signature(vp_jwt_from_enveloped(vp), public_jwk)
        except ValueError:
            return False
    return verify_vp_compatibility_proof(vp, public_jwk)


def vc_jwts_from_vp(vp: dict[str, Any]) -> list[str]:
    vc_jwts: list[str] = []
    for credential in vp.get("verifiableCredential", []):
        if not isinstance(credential, dict):
            raise ValueError("VP verifiableCredential entries must be objects")
        if credential.get("type") != ENVELOPED_VC_TYPE:
            raise ValueError("VP verifiableCredential entries must be EnvelopedVerifiableCredential")
        credential_id = str(credential.get("id", ""))
        if not credential_id.startswith(f"data:{VC_JWT_MEDIA_TYPE},"):
            raise ValueError("VP credential envelope must contain application/vc+jwt")
        vc_jwts.append(vc_jwt_from_enveloped(credential))
    return vc_jwts
