import copy
from typing import Any

from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey

from app.credential_schema.kyc import VERIFIABLE_PRESENTATION_TYPE, VP_CONTEXTS
from app.credentials.crypto import public_key_from_jwk, sign_der_base64url, verify_der_base64url
from app.credentials.vc import CRYPTOSUITE, now_iso, proof_options_without_value, signing_input, without_keys


def create_vp(
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
    proof = {
        "type": "DataIntegrityProof",
        "cryptosuite": CRYPTOSUITE,
        "created": now_iso(),
        "verificationMethod": verification_method,
        "proofPurpose": "authentication",
        "challenge": challenge,
        "domain": domain,
    }
    proof["proofValue"] = sign_der_base64url(private_key, signing_input(vp, proof))
    vp["proof"] = proof
    return vp


def verify_vp_signature(vp: dict[str, Any], public_jwk: dict[str, Any]) -> bool:
    proof = vp.get("proof") or {}
    signature = proof.get("proofValue")
    if not isinstance(signature, str):
        return False
    return verify_der_base64url(
        public_key_from_jwk(public_jwk),
        signature,
        signing_input(without_keys(vp, "proof"), proof_options_without_value(proof)),
    )
