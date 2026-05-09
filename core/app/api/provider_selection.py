from fastapi import APIRouter, Request

from app.provider_selection.models import (
    ProviderCategory,
    ProviderSelectionHistoryResponse,
    ProviderSelectionOptionsResponse,
    ProviderSelectionsResponse,
    ProviderSelectionUpdateRequest,
)
from app.provider_selection.service import (
    active_selections,
    list_options,
    selection_history,
    set_active_selection,
)

router = APIRouter(prefix="/internal/provider-selections", tags=["internal-provider-selections"])


@router.get("/options", response_model=ProviderSelectionOptionsResponse)
def provider_selection_options(request: Request) -> ProviderSelectionOptionsResponse:
    return ProviderSelectionOptionsResponse(options=list_options(request.app.state.settings))


@router.get("", response_model=ProviderSelectionsResponse)
def provider_selections(request: Request) -> ProviderSelectionsResponse:
    return ProviderSelectionsResponse(selections=active_selections(request.app.state.settings, request.app.state.repository))


@router.put("/{category}", response_model=ProviderSelectionsResponse)
def update_provider_selection(
    category: ProviderCategory,
    payload: ProviderSelectionUpdateRequest,
    request: Request,
) -> ProviderSelectionsResponse:
    set_active_selection(
        request.app.state.settings,
        request.app.state.repository,
        category,
        payload.provider,
        payload.profile,
        changed_by=payload.changed_by,
    )
    return ProviderSelectionsResponse(selections=active_selections(request.app.state.settings, request.app.state.repository))


@router.get("/history", response_model=ProviderSelectionHistoryResponse)
def provider_selection_history(request: Request, limit: int = 20) -> ProviderSelectionHistoryResponse:
    return ProviderSelectionHistoryResponse(history=selection_history(request.app.state.repository, limit=limit))
