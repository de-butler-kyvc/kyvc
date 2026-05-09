import os
from functools import lru_cache

from dotenv import load_dotenv
from pydantic import BaseModel, Field


class Settings(BaseModel):
    app_name: str = Field(default="kyvc-core-admin")
    app_env: str = Field(default="dev")
    app_port: int = Field(default=8091)
    log_level: str = Field(default="INFO")
    core_base_url: str = Field(default="http://127.0.0.1:8090")
    core_request_timeout_seconds: float = Field(default=10.0)
    default_operator_id: str = Field(default="core-admin")


@lru_cache
def get_settings() -> Settings:
    load_dotenv()
    return Settings(
        app_name=os.getenv("APP_NAME", "kyvc-core-admin"),
        app_env=os.getenv("APP_ENV", os.getenv("ENV", "dev")),
        app_port=int(os.getenv("APP_PORT", "8091")),
        log_level=os.getenv("LOG_LEVEL", "INFO"),
        core_base_url=os.getenv("CORE_BASE_URL", "http://127.0.0.1:8090"),
        core_request_timeout_seconds=float(os.getenv("CORE_REQUEST_TIMEOUT_SECONDS", "10")),
        default_operator_id=os.getenv("DEFAULT_OPERATOR_ID", "core-admin"),
    )
