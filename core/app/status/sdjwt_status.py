from app.credentials.canonical import sha256_bytes
from app.credentials.hexutil import bytes_to_hex

SDJWT_STATUS_PREFIX = "SDJWT_STATUS_V1:"


def credential_type_hex_for_sdjwt(
    *,
    issuer_did: str,
    holder_did: str,
    vct: str,
    jti: str,
) -> str:
    """Return the XRPL CredentialType for an SD-JWT credential.

    KYvC's XRPL adapter already uses uppercase hex for CredentialType values, so
    this function keeps that representation while hashing deterministic UTF-8
    input material.
    """

    material = f"{SDJWT_STATUS_PREFIX}{issuer_did}\x1f{holder_did}\x1f{vct}\x1f{jti}"
    return bytes_to_hex(sha256_bytes(material.encode("utf-8")))


def credential_type_hex_from_payload(payload: dict) -> str:
    return credential_type_hex_for_sdjwt(
        issuer_did=str(payload["iss"]),
        holder_did=str(payload["sub"]),
        vct=str(payload["vct"]),
        jti=str(payload["jti"]),
    )
