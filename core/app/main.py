from typing import Any

from fastapi import FastAPI

from app.api.ai_assessment import router as ai_assessment_router
from app.api.did import router as did_router
from app.api.health import router as health_router
from app.api.issuer import router as issuer_router
from app.api.credential_status import router as credential_status_router
from app.api.verifier import router as verifier_router
from app.core.config import Settings, get_settings
from app.logging_config import configure_logging
from app.storage.mysql import MySQLRepository


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

    application.include_router(health_router)
    application.include_router(ai_assessment_router)
    application.include_router(did_router)
    application.include_router(issuer_router)
    application.include_router(credential_status_router)
    application.include_router(verifier_router)
    return application


app = create_app()
