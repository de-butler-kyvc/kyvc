from typing import Any

from app.credentials.canonical import canonical_json, sha256_bytes
from app.credentials.crypto import b64url_encode


DELEGATE_IDENTITY_DIGEST_ALGORITHM = "sha-256"
DELEGATE_IDENTITY_DIGEST_VERSION = "delegate-identity-v1"


def normalize_delegate_identity_input(
    *,
    name: Any,
    rrn: Any,
    address: Any = None,
    contact: Any = None,
) -> dict[str, str]:
    """Normalize POA delegate fields for deterministic public digest comparison.

    Missing optional fields are included as empty strings, so every digest input
    has the same key set. The raw RRN must only be passed through this function
    transiently and must not be emitted in claims or API responses.
    """

    return {
        "address": _normalized_text(address),
        "contact": _digits(contact),
        "name": _compact_upper(name),
        "rrn": _digits(rrn),
    }


def delegate_identity_digest(
    *,
    name: Any,
    rrn: Any,
    address: Any = None,
    contact: Any = None,
) -> str:
    payload = normalize_delegate_identity_input(name=name, rrn=rrn, address=address, contact=contact)
    return "sha256-" + b64url_encode(sha256_bytes(canonical_json(payload)))


def _compact_upper(value: Any) -> str:
    if value is None:
        return ""
    return "".join(str(value).upper().split())


def _normalized_text(value: Any) -> str:
    if value is None:
        return ""
    return " ".join(str(value).upper().split())


def _digits(value: Any) -> str:
    if value is None:
        return ""
    return "".join(ch for ch in str(value) if ch.isdigit())
