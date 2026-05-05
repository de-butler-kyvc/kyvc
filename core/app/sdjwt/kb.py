import time
from typing import Any

from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey

from app.credentials.canonical import sha256_bytes
from app.credentials.crypto import (
    b64url_encode,
    decode_compact_jws,
    public_key_from_jwk,
    sign_compact_jws,
    verify_compact_jws,
)

KB_JWT_TYP = "kb+jwt"


def sd_hash_for_presentation(presented_sd_jwt_without_kb: str) -> str:
    return b64url_encode(sha256_bytes(presented_sd_jwt_without_kb.encode("ascii")))


def create_kb_jwt(
    *,
    private_key: EllipticCurvePrivateKey,
    verification_method: str,
    aud: str,
    nonce: str,
    presented_sd_jwt_without_kb: str,
    iat: int | None = None,
) -> str:
    return sign_compact_jws(
        private_key,
        {
            "iat": int(iat if iat is not None else time.time()),
            "aud": aud,
            "nonce": nonce,
            "sd_hash": sd_hash_for_presentation(presented_sd_jwt_without_kb),
        },
        {
            "typ": KB_JWT_TYP,
            "kid": verification_method,
        },
    )


def decode_kb_jwt(kb_jwt: str) -> tuple[dict[str, Any], dict[str, Any]]:
    try:
        protected, payload, _, _ = decode_compact_jws(kb_jwt)
        return protected, payload
    except Exception as exc:
        raise ValueError("invalid KB-JWT") from exc


def verify_kb_jwt(kb_jwt: str, public_jwk: dict[str, Any], verification_method: str) -> bool:
    return verify_compact_jws(
        public_key_from_jwk(public_jwk),
        kb_jwt,
        expected_headers={"typ": KB_JWT_TYP, "kid": verification_method},
    )
