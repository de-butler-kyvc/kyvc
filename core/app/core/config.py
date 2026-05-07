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
    ocr_provider: str = Field(default="structured_payload")
    llm_provider: str = Field(default="none")
    llm_model: str | None = Field(default=None)
    llm_multimodal_enabled: bool = Field(default=True)
    llm_multimodal_max_pages: int = Field(default=3)
    ownership_threshold_percent: float = Field(default=25.0)
    ownership_total_tolerance_percent: float = Field(default=1.0)
    assessment_schema_version: str = Field(default="kyvc-ai-assessment-schema-v1")
    prompt_version: str = Field(default="kyvc-ai-assessment-prompt-v1")
    azure_document_intelligence_endpoint: str | None = Field(default=None)
    azure_document_intelligence_key: str | None = Field(default=None)
    azure_document_intelligence_model_id: str = Field(default="prebuilt-layout")
    azure_document_intelligence_api_version: str | None = Field(default=None)
    openai_api_key: str | None = Field(default=None)
    openai_model: str | None = Field(default=None)
    openai_base_url: str = Field(default="https://api.openai.com/v1")
    azure_openai_endpoint: str | None = Field(default=None)
    azure_openai_api_key: str | None = Field(default=None)
    azure_openai_deployment: str | None = Field(default=None)
    azure_openai_api_version: str = Field(default="2024-10-21")
    naver_clova_ocr_endpoint: str | None = Field(default=None)
    naver_clova_ocr_secret: str | None = Field(default=None)
    naver_clova_ocr_template_id: str | None = Field(default=None)


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
        ocr_provider=os.getenv("OCR_PROVIDER", "structured_payload"),
        llm_provider=os.getenv("LLM_PROVIDER", "none"),
        llm_model=os.getenv("LLM_MODEL"),
        llm_multimodal_enabled=os.getenv("LLM_MULTIMODAL_ENABLED", "1").lower() in {"1", "true", "yes"},
        llm_multimodal_max_pages=int(os.getenv("LLM_MULTIMODAL_MAX_PAGES", "3")),
        ownership_threshold_percent=float(os.getenv("OWNERSHIP_THRESHOLD_PERCENT", "25.0")),
        ownership_total_tolerance_percent=float(os.getenv("OWNERSHIP_TOTAL_TOLERANCE_PERCENT", "1.0")),
        assessment_schema_version=os.getenv("ASSESSMENT_SCHEMA_VERSION", "kyvc-ai-assessment-schema-v1"),
        prompt_version=os.getenv("PROMPT_VERSION", "kyvc-ai-assessment-prompt-v1"),
        azure_document_intelligence_endpoint=os.getenv("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT"),
        azure_document_intelligence_key=os.getenv("AZURE_DOCUMENT_INTELLIGENCE_KEY"),
        azure_document_intelligence_model_id=os.getenv("AZURE_DOCUMENT_INTELLIGENCE_MODEL_ID", "prebuilt-layout"),
        azure_document_intelligence_api_version=os.getenv("AZURE_DOCUMENT_INTELLIGENCE_API_VERSION"),
        openai_api_key=os.getenv("OPENAI_API_KEY"),
        openai_model=os.getenv("OPENAI_MODEL"),
        openai_base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        azure_openai_endpoint=os.getenv("AZURE_OPENAI_ENDPOINT"),
        azure_openai_api_key=os.getenv("AZURE_OPENAI_API_KEY"),
        azure_openai_deployment=os.getenv("AZURE_OPENAI_DEPLOYMENT"),
        azure_openai_api_version=os.getenv("AZURE_OPENAI_API_VERSION", "2024-10-21"),
        naver_clova_ocr_endpoint=os.getenv("NAVER_CLOVA_OCR_ENDPOINT"),
        naver_clova_ocr_secret=os.getenv("NAVER_CLOVA_OCR_SECRET"),
        naver_clova_ocr_template_id=os.getenv("NAVER_CLOVA_OCR_TEMPLATE_ID"),
    )
