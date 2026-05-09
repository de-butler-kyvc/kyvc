from app.ai_assessment.providers.base import DocumentExtractionProvider
from app.ai_assessment.providers.ocr import (
    AzureDocumentIntelligenceOcrTextProvider,
    NaverClovaOcrTextProvider,
    OcrTextProvider,
)
from app.ai_assessment.providers.openai import OpenAiDocumentExtractionProvider
from app.core.config import Settings
from app.provider_selection.service import effective_provider
from app.resilience.outbound import outbound_timeout


def build_document_extraction_provider(
    settings: Settings,
    repository=None,
) -> DocumentExtractionProvider | None:
    provider, _profile = effective_provider(settings, repository, "llm")
    provider = provider.lower()
    if provider in {"none", "structured_payload", "mock"}:
        return None
    if provider == "openai":
        if not settings.openai_api_key:
            raise ValueError("OPENAI_API_KEY is required when LLM_PROVIDER=openai.")
        return OpenAiDocumentExtractionProvider(
            api_key=settings.openai_api_key,
            model=settings.openai_model or settings.llm_model or "gpt-5.5",
            base_url=settings.openai_base_url,
            ocr_provider=build_ocr_text_provider(settings, repository),
            timeout=outbound_timeout("llm", settings),
        )
    if provider == "azure_openai":
        raise ValueError("LLM_PROVIDER=azure_openai is selectable but not implemented by the current core provider.")
    raise ValueError(f"Unsupported LLM_PROVIDER for document extraction: {provider}")


def build_ocr_text_provider(settings: Settings, repository=None) -> OcrTextProvider | None:
    provider, _profile = effective_provider(settings, repository, "ocr")
    provider = provider.lower()
    if provider in {"none", "structured_payload", "mock"}:
        return None
    if provider == "azure_document_intelligence":
        if not settings.azure_document_intelligence_endpoint or not settings.azure_document_intelligence_key:
            raise ValueError(
                "AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT and AZURE_DOCUMENT_INTELLIGENCE_KEY are required "
                "when OCR_PROVIDER=azure_document_intelligence."
            )
        return AzureDocumentIntelligenceOcrTextProvider(
            endpoint=settings.azure_document_intelligence_endpoint,
            key=settings.azure_document_intelligence_key,
            model_id=settings.azure_document_intelligence_model_id,
            api_version=settings.azure_document_intelligence_api_version,
            timeout=outbound_timeout("ocr", settings),
        )
    if provider in {"naver_clova_ocr", "naver_clova"}:
        if not settings.naver_clova_ocr_endpoint or not settings.naver_clova_ocr_secret:
            raise ValueError(
                "NAVER_CLOVA_OCR_ENDPOINT and NAVER_CLOVA_OCR_SECRET are required when OCR_PROVIDER=naver_clova_ocr."
            )
        return NaverClovaOcrTextProvider(
            endpoint=settings.naver_clova_ocr_endpoint,
            secret=settings.naver_clova_ocr_secret,
            template_id=settings.naver_clova_ocr_template_id,
            timeout=outbound_timeout("ocr", settings),
        )
    raise ValueError(f"Unsupported OCR_PROVIDER for document extraction: {provider}")
