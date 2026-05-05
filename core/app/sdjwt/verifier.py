from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any

from app.credentials.crypto import decode_compact_jws, public_key_from_jwk, verify_compact_jws
from app.sdjwt.disclosure import DisclosureError, reconstruct_disclosed_payload
from app.sdjwt.issuer import DEFAULT_SD_JWT_TYP


@dataclass
class SdJwtParseResult:
    issuer_jwt: str
    disclosures: list[str]
    kb_jwt: str | None = None


@dataclass
class SdJwtVerificationResult:
    issuer_payload: dict[str, Any]
    disclosed_payload: dict[str, Any]
    issuer_header: dict[str, Any]
    disclosed_paths: set[str] = field(default_factory=set)


def parse_sd_jwt(value: str, *, expect_kb: bool = False) -> SdJwtParseResult:
    parts = value.split("~")
    if len(parts) < 1 or not parts[0]:
        raise ValueError("invalid SD-JWT serialization")
    if expect_kb:
        if len(parts) < 2 or not parts[-1]:
            raise ValueError("SD-JWT+KB presentation is missing KB-JWT")
        return SdJwtParseResult(issuer_jwt=parts[0], disclosures=parts[1:-1], kb_jwt=parts[-1])
    return SdJwtParseResult(issuer_jwt=parts[0], disclosures=parts[1:])


def presented_sd_jwt_without_kb(value: str) -> str:
    parsed = parse_sd_jwt(value, expect_kb=True)
    return f"{parsed.issuer_jwt}~{'~'.join(parsed.disclosures)}"


def verify_issuer_sd_jwt(
    credential: str,
    *,
    public_jwk: dict[str, Any],
    verification_method: str,
    accepted_typ: str = DEFAULT_SD_JWT_TYP,
    now: datetime | None = None,
) -> SdJwtVerificationResult:
    parsed = parse_sd_jwt(credential)
    protected, issuer_payload, _, _ = decode_compact_jws(parsed.issuer_jwt)
    if protected.get("typ") != accepted_typ:
        raise ValueError("SD-JWT typ is not accepted")
    if protected.get("iss") != issuer_payload.get("iss"):
        raise ValueError("SD-JWT JOSE iss does not match payload iss")
    if not verify_compact_jws(
        public_key_from_jwk(public_jwk),
        parsed.issuer_jwt,
        expected_headers={"typ": accepted_typ, "kid": verification_method, "iss": issuer_payload.get("iss")},
    ):
        raise ValueError("SD-JWT issuer signature verification failed")

    checked_at = (now or datetime.now(tz=UTC)).astimezone(UTC)
    exp = issuer_payload.get("exp")
    iat = issuer_payload.get("iat")
    if not isinstance(exp, int) or checked_at.timestamp() > exp:
        raise ValueError("SD-JWT credential is expired")
    if not isinstance(iat, int) or iat > checked_at.timestamp() + 300:
        raise ValueError("SD-JWT iat is in the future")

    try:
        disclosure_result = reconstruct_disclosed_payload(issuer_payload, parsed.disclosures)
    except DisclosureError as exc:
        raise ValueError(str(exc)) from exc

    return SdJwtVerificationResult(
        issuer_payload=issuer_payload,
        disclosed_payload=disclosure_result.disclosed_payload,
        issuer_header=protected,
        disclosed_paths=disclosure_result.paths,
    )
