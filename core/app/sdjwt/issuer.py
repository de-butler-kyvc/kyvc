import uuid
from datetime import datetime
from typing import Any

from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey

from app.credential_schema.kyc import XRPL_CREDENTIAL_STATUS_TYPE
from app.credentials.crypto import sign_compact_jws
from app.credentials.vc import isoformat_z
from app.sdjwt.disclosure import DisclosureBuildResult, build_sd_payload
from app.status.sdjwt_status import credential_type_hex_for_sdjwt

DEFAULT_SD_JWT_TYP = "dc+sd-jwt"
DEFAULT_LEGAL_ENTITY_KYC_VCT = "https://kyvc.example/vct/legal-entity-kyc-v1"
SD_JWT_PRESENTATION_FORMAT = "kyvc-sd-jwt-presentation-v1"


def datetime_to_numeric_date(value: datetime) -> int:
    if value.tzinfo is None:
        return int(value.timestamp())
    return int(value.timestamp())


def build_legal_entity_kyc_payload(
    *,
    issuer_did: str,
    holder_did: str,
    holder_key_id: str,
    claims: dict[str, Any],
    valid_from: datetime,
    valid_until: datetime,
    vct: str = DEFAULT_LEGAL_ENTITY_KYC_VCT,
    jti: str | None = None,
) -> dict[str, Any]:
    selected_jti = jti or f"urn:uuid:{uuid.uuid4()}"
    status_id = f"urn:kyvc:status:{selected_jti.removeprefix('urn:uuid:')}"
    credential_type = credential_type_hex_for_sdjwt(
        issuer_did=issuer_did,
        holder_did=holder_did,
        vct=vct,
        jti=selected_jti,
    )
    payload: dict[str, Any] = {
        "iss": issuer_did,
        "sub": holder_did,
        "vct": vct,
        "jti": selected_jti,
        "iat": datetime_to_numeric_date(valid_from),
        "exp": datetime_to_numeric_date(valid_until),
        "cnf": {"kid": f"{holder_did}#{holder_key_id}"},
        "credentialStatus": {
            "type": XRPL_CREDENTIAL_STATUS_TYPE,
            "statusId": status_id,
            "credentialType": credential_type,
        },
    }
    payload.update(claims)
    kyc = payload.setdefault("kyc", {})
    if isinstance(kyc, dict):
        kyc.setdefault("verifiedAt", isoformat_z(valid_from))
    return payload


def issue_sd_jwt(
    *,
    payload: dict[str, Any],
    private_key: EllipticCurvePrivateKey,
    verification_method: str,
    typ: str = DEFAULT_SD_JWT_TYP,
) -> tuple[str, DisclosureBuildResult]:
    disclosure_result = build_sd_payload(payload)
    issuer_jwt = sign_compact_jws(
        private_key,
        disclosure_result.payload,
        {
            "typ": typ,
            "kid": verification_method,
            "iss": payload["iss"],
        },
    )
    return f"{issuer_jwt}~{'~'.join(disclosure_result.disclosures)}", disclosure_result


def presentation_from_credential(credential: str, selected_disclosures: list[str]) -> str:
    issuer_jwt = credential.split("~", 1)[0]
    return f"{issuer_jwt}~{'~'.join(selected_disclosures)}"
