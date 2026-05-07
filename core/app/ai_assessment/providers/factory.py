from app.ai_assessment.providers.base import DocumentExtractionProvider
from app.ai_assessment.providers.openai import OpenAiDocumentExtractionProvider
from app.core.config import Settings


def build_document_extraction_provider(settings: Settings) -> DocumentExtractionProvider | None:
    provider = settings.llm_provider.lower()
    if provider in {"none", "structured_payload", "mock"}:
        return None
    if provider == "openai":
        if not settings.openai_api_key:
            raise ValueError("OPENAI_API_KEY is required when LLM_PROVIDER=openai.")
        return OpenAiDocumentExtractionProvider(
            api_key=settings.openai_api_key,
            model=settings.openai_model or settings.llm_model or "gpt-5.5",
            base_url=settings.openai_base_url,
        )
    raise ValueError(f"Unsupported LLM_PROVIDER for document extraction: {settings.llm_provider}")
