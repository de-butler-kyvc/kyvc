import logging
from typing import Any

from fastapi import FastAPI, Request, status
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException

from app.api.ai_assessment import router as ai_assessment_router
from app.api.did import router as did_router
from app.api.health import router as health_router
from app.api.internal_status import router as internal_status_router
from app.api.issuer import router as issuer_router
from app.api.provider_selection import router as provider_selection_router
from app.api.credential_status import router as credential_status_router
from app.api.verifier import router as verifier_router
from app.core.config import Settings, get_settings
from app.logging_config import configure_logging
from app.storage.mysql import MySQLRepository

logger = logging.getLogger(__name__)


def create_repository(settings: Settings) -> MySQLRepository:
    return MySQLRepository(
        host=settings.db_host,
        port=settings.db_port,
        database=settings.db_name,
        user=settings.db_user,
        password=settings.db_password,
        charset=settings.db_charset,
        connect_timeout=settings.db_connect_timeout,
    )


def create_app(
    settings: Settings | None = None,
    repository: Any | None = None,
) -> FastAPI:
    selected_settings = settings or get_settings()
    configure_logging(selected_settings.log_level)
    selected_repository = repository or create_repository(selected_settings)

    application = FastAPI(
        title=selected_settings.app_name,
        version="0.1.0",
        description="KYvC Core API",
    )
    application.state.settings = selected_settings
    application.state.repository = selected_repository

    _register_exception_handlers(application)

    application.include_router(health_router)
    application.include_router(internal_status_router)
    application.include_router(provider_selection_router)
    application.include_router(ai_assessment_router)
    application.include_router(did_router)
    application.include_router(issuer_router)
    application.include_router(credential_status_router)
    application.include_router(verifier_router)
    return application


def _error_response(status_code: int, detail: Any) -> JSONResponse:
    return JSONResponse(status_code=status_code, content={"detail": jsonable_encoder(detail)})


def _exception_detail(exc: Exception, fallback: str) -> str:
    detail = str(exc).strip()
    return detail or fallback


def _is_request_runtime_error(exc: RuntimeError) -> bool:
    detail = str(exc)
    return (
        detail.startswith("Refusing to connect to XRPL Mainnet.")
        or detail.startswith("Mainnet URL configured.")
        or " failed because the selected XRPL network has the amendment disabled" in detail
        or " failed with result=" in detail
        or detail == "could not determine JsonRpcClient URL for raw ledger_entry"
    )


def _register_exception_handlers(application: FastAPI) -> None:
    @application.exception_handler(HTTPException)
    async def http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
        return _error_response(exc.status_code, exc.detail)

    @application.exception_handler(RequestValidationError)
    async def request_validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        return _error_response(422, exc.errors())

    @application.exception_handler(ValueError)
    async def value_error_exception_handler(request: Request, exc: ValueError) -> JSONResponse:
        return _error_response(status.HTTP_400_BAD_REQUEST, _exception_detail(exc, "Invalid request"))

    @application.exception_handler(FileNotFoundError)
    async def file_not_found_exception_handler(request: Request, exc: FileNotFoundError) -> JSONResponse:
        detail = "document file not found"
        if exc.filename:
            detail = f"{detail}: {exc.filename}"
        return _error_response(status.HTTP_400_BAD_REQUEST, detail)

    @application.exception_handler(RuntimeError)
    async def runtime_error_exception_handler(request: Request, exc: RuntimeError) -> JSONResponse:
        if _is_request_runtime_error(exc):
            return _error_response(
                status.HTTP_400_BAD_REQUEST,
                _exception_detail(exc, "Request could not be processed"),
            )
        logger.exception("Unhandled runtime error while processing %s %s", request.method, request.url.path)
        return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, "Internal server error")

    @application.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled exception while processing %s %s", request.method, request.url.path)
        return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, "Internal server error")


app = create_app()
