import os
from functools import lru_cache

from pydantic import BaseModel, Field


class Settings(BaseModel):
    app_name: str = Field(default="kyvc-core")
    app_env: str = Field(default="dev")
    app_port: int = Field(default=8090)
    log_level: str = Field(default="INFO")


@lru_cache
def get_settings() -> Settings:
    return Settings(
        app_name=os.getenv("APP_NAME", "kyvc-core"),
        app_env=os.getenv("APP_ENV", "dev"),
        app_port=int(os.getenv("APP_PORT", "8090")),
        log_level=os.getenv("LOG_LEVEL", "INFO"),
    )
