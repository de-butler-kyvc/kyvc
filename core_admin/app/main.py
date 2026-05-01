from fastapi import FastAPI

from app.api.health import router as health_router
from app.core.config import get_settings
from app.logging_config import configure_logging

settings = get_settings()
configure_logging(settings.log_level)

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="KYvC Core Admin API",
)
app.state.settings = settings

app.include_router(health_router)
