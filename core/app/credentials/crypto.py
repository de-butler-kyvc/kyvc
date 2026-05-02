import base64
from typing import Any

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.asymmetric.ec import (
    EllipticCurvePrivateKey,
    EllipticCurvePublicKey,
)


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


def sign_der_base64url(private_key: EllipticCurvePrivateKey, data: bytes) -> str:
    return b64url_encode(private_key.sign(data, ec.ECDSA(hashes.SHA256())))


def verify_der_base64url(public_key: EllipticCurvePublicKey, signature: str, data: bytes) -> bool:
    try:
        public_key.verify(b64url_decode(signature), data, ec.ECDSA(hashes.SHA256()))
        return True
    except InvalidSignature:
        return False

