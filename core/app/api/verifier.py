import json
import secrets
from datetime import UTC, datetime, timedelta
from typing import Any

from fastapi import APIRouter, HTTPException, Request

from app.credentials.resolver import (
    CachingDidResolver,
    CompositeDidResolver,
    StaticDidResolver,
    VerifiedCachedDidResolver,
    VerifiedStaticDidResolver,
    XrplDidResolver,
)
from app.verifier.api_models import (
    IssuePresentationChallengeRequest,
    IssuePresentationChallengeResponse,
    VerificationResponse,
    VerifyCredentialRequest,
    VerifyPresentationRequest,
)
from app.verifier.policy import VerificationPolicy
from app.policy.sdjwt_policy import SdJwtVerificationPolicy
from app.sdjwt.issuer import DEFAULT_LEGAL_ENTITY_KYC_VCT
from app.verifier.service import VerificationResult, VerifierService
from app.xrpl.client import enforce_mainnet_policy, make_client
from app.xrpl.ledger import get_credential_entry as get_xrpl_credential_entry

router = APIRouter(prefix="/verifier", tags=["verifier"])


def _policy_dict(policy: Any) -> dict[str, Any] | None:
    if policy is None:
        return None
    model_dump = getattr(policy, "model_dump", None)
    if callable(model_dump):
        return model_dump(exclude_none=True)
    return policy.dict(exclude_none=True)


def _sdjwt_policy(policy: Any) -> SdJwtVerificationPolicy | None:
    policy_data = _policy_dict(policy)
    return SdJwtVerificationPolicy.from_dict(policy_data) if policy_data is not None else None


def _default_presentation_definition(payload: IssuePresentationChallengeRequest) -> dict[str, Any]:
    return {
        "id": payload.definitionId,
        "acceptedFormat": "dc+sd-jwt",
        "acceptedVct": [DEFAULT_LEGAL_ENTITY_KYC_VCT],
        "trustedIssuers": [],
        "requiredDisclosures": [],
        "documentRules": [],
    }


def _status_lookup(request: Request, status_mode: str, rpc_url: str | None, allow_mainnet: bool):
    repository = request.app.state.repository
    if status_mode == "local":
        return repository.get_credential_entry

    settings = request.app.state.settings
    selected_rpc_url = rpc_url or settings.xrpl_json_rpc_url
    try:
        enforce_mainnet_policy(selected_rpc_url, settings.allow_mainnet, allow_mainnet)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    client = make_client(selected_rpc_url)

    def lookup(issuer_account: str, holder_account: str, credential_type: str) -> dict[str, Any] | None:
        return get_xrpl_credential_entry(client, issuer_account, holder_account, credential_type)

    return lookup


def _xrpl_client_for_verifier(
    request: Request,
    status_mode: str,
    rpc_url: str | None,
    allow_mainnet: bool,
):
    if status_mode != "xrpl" and rpc_url is None:
        return None
    settings = request.app.state.settings
    selected_rpc_url = rpc_url or settings.xrpl_json_rpc_url
    try:
        enforce_mainnet_policy(selected_rpc_url, settings.allow_mainnet, allow_mainnet)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return make_client(selected_rpc_url)


def _resolver(
    request: Request,
    did_documents: dict[str, dict[str, Any]],
    *,
    status_mode: str,
    xrpl_json_rpc_url: str | None,
    allow_mainnet: bool,
):
    repository = request.app.state.repository
    client = _xrpl_client_for_verifier(request, status_mode, xrpl_json_rpc_url, allow_mainnet)
    resolvers = []
    if did_documents:
        if client is None:
            resolvers.append(StaticDidResolver(did_documents))
        else:
            resolvers.append(VerifiedStaticDidResolver(did_documents, client, cache=repository))
    if client is not None:
        resolvers.append(VerifiedCachedDidResolver(repository, client))
        resolvers.append(CachingDidResolver(XrplDidResolver(client), repository))
    else:
        resolvers.append(repository)
    return CompositeDidResolver(*resolvers)


def _service(
    request: Request,
    did_documents: dict[str, dict[str, Any]],
    policy_data: dict[str, Any] | None,
    *,
    status_mode: str,
    xrpl_json_rpc_url: str | None,
    allow_mainnet: bool,
) -> VerifierService:
    repository = request.app.state.repository
    resolver = _resolver(
        request,
        did_documents,
        status_mode=status_mode,
        xrpl_json_rpc_url=xrpl_json_rpc_url,
        allow_mainnet=allow_mainnet,
    )

    def logger(subject_id: str | None, result: VerificationResult, verified_at: datetime) -> None:
        repository.save_verification_result(
            subject_id=subject_id,
            ok=result.ok,
            errors=result.errors,
            details=result.details,
            verified_at=verified_at,
        )

    return VerifierService(
        resolver,
        status_lookup=_status_lookup(request, status_mode, xrpl_json_rpc_url, allow_mainnet),
        policy=VerificationPolicy.from_dict(policy_data),
        verification_logger=logger,
        challenge_lookup=repository.get_verification_challenge,
        challenge_marker=repository.mark_verification_challenge_used,
    )


@router.post("/credentials/verify", response_model=VerificationResponse)
def verify_credential(payload: VerifyCredentialRequest, request: Request) -> VerificationResponse:
    service = _service(
        request,
        payload.did_documents,
        _policy_dict(payload.policy),
        status_mode=payload.status_mode,
        xrpl_json_rpc_url=payload.xrpl_json_rpc_url,
        allow_mainnet=payload.allow_mainnet,
    )
    if payload.format == "dc+sd-jwt" or (isinstance(payload.credential, str) and "~" in payload.credential):
        result = service.verify_sd_jwt_credential(
            str(payload.credential),
            require_status=payload.require_status,
            policy=_sdjwt_policy(payload.policy),
        )
    else:
        result = service.verify_vc(payload.credential, require_status=payload.require_status)
    return VerificationResponse(**result.to_dict())


@router.post("/presentations/challenges", response_model=IssuePresentationChallengeResponse)
def issue_presentation_challenge(
    payload: IssuePresentationChallengeRequest,
    request: Request,
) -> IssuePresentationChallengeResponse:
    settings = request.app.state.settings
    issued_at = datetime.now(tz=UTC)
    expires_at = issued_at + timedelta(seconds=settings.verifier_challenge_ttl_seconds)
    challenge = secrets.token_urlsafe(32)
    audience = payload.aud or payload.domain
    if not audience:
        raise HTTPException(status_code=400, detail="aud or domain is required")
    presentation_definition = (
        payload.presentationDefinition
        if payload.presentationDefinition is not None
        else _default_presentation_definition(payload)
    )
    request.app.state.repository.save_verification_challenge(
        challenge=challenge,
        domain=audience,
        expires_at=expires_at,
        created_at=issued_at,
        presentation_definition=payload.presentationDefinition if payload.format == "dc+sd-jwt" else None,
    )
    expires = expires_at.replace(microsecond=0).isoformat().replace("+00:00", "Z")
    if payload.format == "vp+jwt":
        return IssuePresentationChallengeResponse(
            challenge=challenge,
            domain=audience,
            expires_at=expires,
        )
    return IssuePresentationChallengeResponse(
        challenge=challenge,
        domain=audience,
        expires_at=expires,
        nonce=challenge,
        aud=audience,
        expiresAt=expires,
        presentationDefinition=presentation_definition,
    )


@router.post("/presentations/verify", response_model=VerificationResponse)
async def verify_presentation(request: Request) -> VerificationResponse:
    content_type = request.headers.get("content-type", "")
    attachments: dict[str, tuple[bytes, str | None]] = {}
    if content_type.startswith("multipart/form-data"):
        form = await request.form()
        raw_presentation = form.get("presentation")
        if not isinstance(raw_presentation, str):
            raise HTTPException(status_code=400, detail="multipart presentation field is required")
        try:
            payload = VerifyPresentationRequest(presentation=json.loads(raw_presentation))
        except Exception as exc:
            raise HTTPException(status_code=400, detail="multipart presentation field must be JSON") from exc
        for key, value in form.multi_items():
            if key == "presentation":
                continue
            filename = getattr(value, "filename", None)
            read = getattr(value, "read", None)
            if filename is not None and callable(read):
                file_bytes = await read()
                attachments[key] = (file_bytes, getattr(value, "content_type", None))
                attachments[str(filename)] = (file_bytes, getattr(value, "content_type", None))
    else:
        payload = VerifyPresentationRequest(**(await request.json()))
    service = _service(
        request,
        payload.did_documents,
        _policy_dict(payload.policy),
        status_mode=payload.status_mode,
        xrpl_json_rpc_url=payload.xrpl_json_rpc_url,
        allow_mainnet=payload.allow_mainnet,
    )
    is_sd_jwt = payload.format == "kyvc-sd-jwt-presentation-v1" or (
        isinstance(payload.presentation, dict) and payload.presentation.get("sdJwtKb")
    )
    if is_sd_jwt:
        result = service.verify_sd_jwt_presentation(
            payload.presentation,
            require_status=payload.require_status,
            policy=_sdjwt_policy(payload.policy),
            attachments=attachments,
        )
    else:
        result = service.verify_vp(
            payload.presentation,
            require_status=payload.require_status,
        )
    return VerificationResponse(**result.to_dict())
