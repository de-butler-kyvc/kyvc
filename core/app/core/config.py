import os
from functools import lru_cache

from dotenv import load_dotenv
from pydantic import BaseModel, Field


class Settings(BaseModel):
    app_name: str = Field(default="kyvc-core")
    app_env: str = Field(default="dev")
    app_port: int = Field(default=8090)
    log_level: str = Field(default="INFO")
    db_host: str = Field(default="127.0.0.1")
    db_port: int = Field(default=3306)
    db_name: str = Field(default="kyvc_core")
    db_user: str = Field(default="kyvc_core")
    db_password: str = Field(default="")
    db_charset: str = Field(default="utf8mb4")
    db_connect_timeout: int = Field(default=10)
    xrpl_json_rpc_url: str = Field(default="https://s.devnet.rippletest.net:51234")
    xrpl_network_name: str = Field(default="devnet")
    allow_mainnet: bool = Field(default=False)
    xrpl_issuer_seed: str | None = Field(default=None)
    issuer_private_key_pem_path: str | None = Field(default=None)
    xrpl_faucet_host: str | None = Field(default=None)
    did_doc_base_url: str = Field(default="http://127.0.0.1:8090")
    verifier_challenge_ttl_seconds: int = Field(default=300)


@lru_cache
def get_settings() -> Settings:
    load_dotenv()
    return Settings(
        app_name=os.getenv("APP_NAME", "kyvc-core"),
        app_env=os.getenv("APP_ENV", os.getenv("ENV", "dev")),
        app_port=int(os.getenv("APP_PORT", "8090")),
        log_level=os.getenv("LOG_LEVEL", "INFO"),
        db_host=os.getenv("DB_HOST", "127.0.0.1"),
        db_port=int(os.getenv("DB_PORT", "3306")),
        db_name=os.getenv("DB_NAME", "kyvc_core"),
        db_user=os.getenv("DB_USER", "kyvc_core"),
        db_password=os.getenv("DB_PASSWORD", ""),
        db_charset=os.getenv("DB_CHARSET", "utf8mb4"),
        db_connect_timeout=int(os.getenv("DB_CONNECT_TIMEOUT", "10")),
        xrpl_json_rpc_url=os.getenv("XRPL_JSON_RPC_URL", "https://s.devnet.rippletest.net:51234"),
        xrpl_network_name=os.getenv("XRPL_NETWORK_NAME", "devnet"),
        allow_mainnet=os.getenv("ALLOW_MAINNET") == "1",
        xrpl_issuer_seed=os.getenv("XRPL_ISSUER_SEED"),
        issuer_private_key_pem_path=os.getenv("ISSUER_PRIVATE_KEY_PEM_PATH") or os.getenv("ISSUER_KEY_PEM_PATH"),
        xrpl_faucet_host=os.getenv("XRPL_FAUCET_HOST"),
        did_doc_base_url=os.getenv("DID_DOC_BASE_URL", "http://127.0.0.1:8090"),
        verifier_challenge_ttl_seconds=int(os.getenv("VERIFIER_CHALLENGE_TTL_SECONDS", "300")),
    )
