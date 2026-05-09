from datetime import UTC, datetime
from typing import Any

from app.core.config import Settings
from app.provider_selection.models import (
    ProviderCategory,
    ProviderSelection,
    ProviderSelectionHistoryEntry,
    ProviderSelectionOption,
)

PROFILES = ["default", "fast", "balanced", "high_accuracy"]
OPTIONS: dict[ProviderCategory, tuple[str, ...]] = {
    "ocr": ("structured_payload", "azure_document_intelligence", "naver_clova_ocr"),
    "llm": ("none", "openai", "azure_openai"),
}


def list_options(settings: Settings) -> dict[ProviderCategory, list[ProviderSelectionOption]]:
    return {
        category: [_option(settings, category, provider) for provider in providers]
        for category, providers in OPTIONS.items()
    }


def active_selection(settings: Settings, repository: Any, category: ProviderCategory) -> ProviderSelection:
    stored = _repository_selection(repository, category)
    if stored is not None:
        return stored
    return _env_selection(settings, category)


def active_selections(settings: Settings, repository: Any) -> dict[ProviderCategory, ProviderSelection]:
    return {
        "ocr": active_selection(settings, repository, "ocr"),
        "llm": active_selection(settings, repository, "llm"),
    }


def set_active_selection(
    settings: Settings,
    repository: Any,
    category: ProviderCategory,
    provider: str,
    profile: str,
    *,
    changed_by: str | None = None,
) -> ProviderSelection:
    _validate_selection(settings, category, provider, profile)
    current = _repository_selection(repository, category)
    updated_at = _now()
    selection = ProviderSelection(
        category=category,
        provider=provider,
        profile=profile,
        active=True,
        updated_by=changed_by,
        updated_at=updated_at,
    )
    save = getattr(repository, "save_provider_selection", None)
    if not callable(save):
        raise RuntimeError("repository does not support provider selection storage")
    save(selection, previous=current)
    return selection


def selection_history(repository: Any, *, limit: int = 20) -> list[ProviderSelectionHistoryEntry]:
    list_history = getattr(repository, "list_provider_selection_history", None)
    if not callable(list_history):
        return []
    return list_history(limit=limit)


def effective_provider(settings: Settings, repository: Any | None, category: ProviderCategory) -> tuple[str, str]:
    if repository is not None:
        stored = _repository_selection(repository, category)
        if stored is not None:
            return stored.provider, stored.profile
    fallback = _env_selection(settings, category)
    return fallback.provider, fallback.profile


def _repository_selection(repository: Any, category: ProviderCategory) -> ProviderSelection | None:
    get_selection = getattr(repository, "get_provider_selection", None)
    if not callable(get_selection):
        return None
    return get_selection(category)


def _env_selection(settings: Settings, category: ProviderCategory) -> ProviderSelection:
    provider = settings.ocr_provider if category == "ocr" else settings.llm_provider
    return ProviderSelection(category=category, provider=provider, profile="default", active=True)


def _validate_selection(settings: Settings, category: ProviderCategory, provider: str, profile: str) -> None:
    if provider not in OPTIONS[category]:
        raise ValueError(f"Unsupported {category} provider: {provider}")
    if profile not in PROFILES:
        raise ValueError(f"Unsupported {category} profile: {profile}")
    option = _option(settings, category, provider)
    if not option.configured:
        raise ValueError(option.detail)


def _option(settings: Settings, category: ProviderCategory, provider: str) -> ProviderSelectionOption:
    detail = "configured"
    configured = True
    if category == "ocr" and provider == "azure_document_intelligence":
        configured = bool(settings.azure_document_intelligence_endpoint and settings.azure_document_intelligence_key)
        detail = "configured" if configured else "AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT and key are required"
    elif category == "ocr" and provider == "naver_clova_ocr":
        configured = bool(settings.naver_clova_ocr_endpoint and settings.naver_clova_ocr_secret)
        detail = "configured" if configured else "NAVER_CLOVA_OCR_ENDPOINT and secret are required"
    elif category == "llm" and provider == "openai":
        configured = bool(settings.openai_api_key)
        detail = "configured" if configured else "OPENAI_API_KEY is required"
    elif category == "llm" and provider == "azure_openai":
        configured = False
        detail = "Azure OpenAI selection is not implemented by the current core provider"
    return ProviderSelectionOption(
        category=category,
        provider=provider,
        profiles=list(PROFILES),
        configured=configured,
        detail=detail,
    )


def _now() -> str:
    return datetime.now(tz=UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")
