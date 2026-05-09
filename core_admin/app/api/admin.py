from fastapi import APIRouter, HTTPException, Query, Request

from app.admin.api_models import ProviderCategory, ProviderSelectionUpdateRequest
from app.services.core_client import CoreClient, CoreClientError

router = APIRouter(prefix="/admin", tags=["admin"])


def _core_client(request: Request) -> CoreClient:
    return CoreClient(request.app.state.settings)


def _model_data(model) -> dict:
    model_dump = getattr(model, "model_dump", None)
    if callable(model_dump):
        return model_dump()
    return model.dict()


def _call_core(action):
    try:
        return action()
    except CoreClientError as exc:
        raise HTTPException(status_code=exc.status_code, detail=exc.detail) from exc


@router.get("/core/status")
def core_internal_status(request: Request) -> dict:
    return _call_core(lambda: _core_client(request).get_internal_status())


@router.get("/provider-selections/options")
def provider_selection_options(request: Request) -> dict:
    return _call_core(lambda: _core_client(request).get_provider_options())


@router.get("/provider-selections")
def provider_selections(request: Request) -> dict:
    return _call_core(lambda: _core_client(request).get_provider_selections())


@router.put("/provider-selections/{category}")
def update_provider_selection(
    category: ProviderCategory,
    payload: ProviderSelectionUpdateRequest,
    request: Request,
) -> dict:
    data = _model_data(payload)
    changed_by = data["changed_by"] or request.app.state.settings.default_operator_id
    return _call_core(
        lambda: _core_client(request).update_provider_selection(
            category,
            provider=data["provider"],
            profile=data["profile"],
            changed_by=changed_by,
        )
    )


@router.get("/provider-selections/history")
def provider_selection_history(
    request: Request,
    limit: int = Query(default=20, ge=1, le=100),
) -> dict:
    return _call_core(lambda: _core_client(request).get_provider_history(limit=limit))
