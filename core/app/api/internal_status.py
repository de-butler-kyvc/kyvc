from fastapi import APIRouter, Request

from app.internal_status.service import InternalStatusResponse, build_internal_status

router = APIRouter(prefix="/internal", tags=["internal"])


@router.get("/status", response_model=InternalStatusResponse)
def internal_status(request: Request) -> InternalStatusResponse:
    return build_internal_status(request.app.state.settings, request.app.state.repository)
