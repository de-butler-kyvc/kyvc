import base64
import binascii
import json
from typing import Any

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.asymmetric.ec import (
    EllipticCurvePrivateKey,
    EllipticCurvePublicKey,
)
from cryptography.hazmat.primitives.asymmetric.utils import (
    decode_dss_signature,
    encode_dss_signature,
)

from app.credentials.canonical import canonical_json

# ES256K is JOSE-standardized for secp256k1, but many JWT/KMS stacks support
# ES256/P-256 or EdDSA more broadly. See README for migration tradeoffs.
JWS_ALG = "ES256K"
JWS_SIGNATURE_BYTES = 64


def b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def b64url_decode(value: str) -> bytes:
    padding = "=" * (-len(value) % 4)
    return base64.urlsafe_b64decode((value + padding).encode("ascii"))


def generate_private_key() -> EllipticCurvePrivateKey:
    return ec.generate_private_key(ec.SECP256K1())


def private_key_to_pem(private_key: EllipticCurvePrivateKey) -> str:
    return private_key.private_bytes(
        serialization.Encoding.PEM,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    ).decode("ascii")


def private_key_from_pem(pem: str) -> EllipticCurvePrivateKey:
    normalized = pem.replace("\\n", "\n").strip().encode("ascii")
    key = serialization.load_pem_private_key(normalized, password=None)
    if not isinstance(key, EllipticCurvePrivateKey):
        raise ValueError("expected EC private key")
    return key


def public_key_to_jwk(public_key: EllipticCurvePublicKey) -> dict[str, str]:
    numbers = public_key.public_numbers()
    return {
        "kty": "EC",
        "crv": "secp256k1",
        "x": b64url_encode(numbers.x.to_bytes(32, "big")),
        "y": b64url_encode(numbers.y.to_bytes(32, "big")),
    }


def private_key_to_jwk(private_key: EllipticCurvePrivateKey) -> dict[str, str]:
    return public_key_to_jwk(private_key.public_key())


def public_key_from_jwk(jwk: dict[str, Any]) -> EllipticCurvePublicKey:
    if jwk.get("kty") != "EC" or jwk.get("crv") != "secp256k1":
        raise ValueError("expected EC secp256k1 JWK")
    x = int.from_bytes(b64url_decode(str(jwk["x"])), "big")
    y = int.from_bytes(b64url_decode(str(jwk["y"])), "big")
    return ec.EllipticCurvePublicNumbers(x, y, ec.SECP256K1()).public_key()


def _der_to_raw_signature(der_signature: bytes) -> bytes:
    r, s = decode_dss_signature(der_signature)
    return r.to_bytes(32, "big") + s.to_bytes(32, "big")


def _raw_to_der_signature(raw_signature: bytes) -> bytes:
    if len(raw_signature) != JWS_SIGNATURE_BYTES:
        raise ValueError("expected 64-byte ES256K JWS signature")
    r = int.from_bytes(raw_signature[:32], "big")
    s = int.from_bytes(raw_signature[32:], "big")
    return encode_dss_signature(r, s)


def compact_jws_signing_input(protected_header: dict[str, Any], payload: dict[str, Any]) -> bytes:
    protected = b64url_encode(canonical_json(protected_header))
    encoded_payload = b64url_encode(canonical_json(payload))
    return f"{protected}.{encoded_payload}".encode("ascii")


def sign_compact_jws(
    private_key: EllipticCurvePrivateKey,
    payload: dict[str, Any],
    protected_header: dict[str, Any],
) -> str:
    header = {"alg": JWS_ALG, **protected_header}
    signing_input = compact_jws_signing_input(header, payload)
    signature = _der_to_raw_signature(
        private_key.sign(signing_input, ec.ECDSA(hashes.SHA256()))
    )
    return f"{signing_input.decode('ascii')}.{b64url_encode(signature)}"


def decode_compact_jws(value: str) -> tuple[dict[str, Any], dict[str, Any], bytes, bytes]:
    parts = value.split(".")
    if len(parts) != 3:
        raise ValueError("expected compact JWS")
    protected_b64, payload_b64, signature_b64 = parts
    protected = json.loads(b64url_decode(protected_b64))
    payload = json.loads(b64url_decode(payload_b64))
    if not isinstance(protected, dict) or not isinstance(payload, dict):
        raise ValueError("expected JWS header and payload objects")
    signing_input = f"{protected_b64}.{payload_b64}".encode("ascii")
    return protected, payload, signing_input, b64url_decode(signature_b64)


def verify_compact_jws(
    public_key: EllipticCurvePublicKey,
    value: str,
    expected_payload: dict[str, Any] | None = None,
    expected_headers: dict[str, Any] | None = None,
) -> bool:
    try:
        protected, payload, signing_input, raw_signature = decode_compact_jws(value)
        if protected.get("alg") != JWS_ALG:
            return False
        if expected_headers is not None:
            for key, expected_value in expected_headers.items():
                if protected.get(key) != expected_value:
                    return False
        if expected_payload is not None and payload != expected_payload:
            return False
        public_key.verify(
            _raw_to_der_signature(raw_signature),
            signing_input,
            ec.ECDSA(hashes.SHA256()),
        )
        return True
    except (
        InvalidSignature,
        ValueError,
        KeyError,
        json.JSONDecodeError,
        UnicodeDecodeError,
        binascii.Error,
    ):
        return False
