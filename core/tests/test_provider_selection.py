from fastapi.testclient import TestClient

from app.ai_assessment.providers.factory import build_document_extraction_provider, build_ocr_text_provider
from app.core.config import Settings
from app.main import create_app
from app.provider_selection.models import ProviderSelection
from app.provider_selection.service import active_selection, set_active_selection


class SelectionRepository:
    def __init__(self):
        self.selections = {}
        self.history = []

    def get_provider_selection(self, category):
        return self.selections.get(category)

    def save_provider_selection(self, selection, *, previous=None):
        self.selections[selection.category] = selection
        self.history.append(
            {
                "category": selection.category,
                "previous_provider": previous.provider if previous else None,
                "previous_profile": previous.profile if previous else None,
                "new_provider": selection.provider,
                "new_profile": selection.profile,
                "changed_by": selection.updated_by,
                "changed_at": selection.updated_at,
            }
        )

    def list_provider_selection_history(self, *, limit=20):
        return self.history[:limit]


def test_env_fallback_works_when_no_runtime_selection_exists():
    repository = SelectionRepository()
    settings = Settings(ocr_provider="structured_payload", llm_provider="none")

    assert active_selection(settings, repository, "ocr").provider == "structured_payload"
    assert active_selection(settings, repository, "llm").provider == "none"


def test_valid_selection_overrides_env_default():
    repository = SelectionRepository()
    settings = Settings(llm_provider="none", openai_api_key="test-key")

    selection = set_active_selection(settings, repository, "llm", "openai", "balanced", changed_by="admin")

    assert selection.provider == "openai"
    assert active_selection(settings, repository, "llm").provider == "openai"
    assert active_selection(settings, repository, "llm").profile == "balanced"


def test_unsupported_provider_is_rejected():
    repository = SelectionRepository()

    try:
        set_active_selection(Settings(), repository, "llm", "custom_provider", "default")
    except ValueError as exc:
        assert "Unsupported llm provider" in str(exc)
    else:
        raise AssertionError("expected unsupported provider to be rejected")


def test_selection_requiring_missing_secret_is_rejected():
    repository = SelectionRepository()

    try:
        set_active_selection(Settings(openai_api_key=None), repository, "llm", "openai", "default")
    except ValueError as exc:
        assert "OPENAI_API_KEY" in str(exc)
    else:
        raise AssertionError("expected missing OpenAI key to be rejected")


def test_core_resolves_active_ocr_and_llm_selection():
    repository = SelectionRepository()
    settings = Settings(
        ocr_provider="azure_document_intelligence",
        llm_provider="none",
        openai_api_key="test-key",
    )
    repository.selections["ocr"] = ProviderSelection(category="ocr", provider="structured_payload", profile="default")
    repository.selections["llm"] = ProviderSelection(category="llm", provider="openai", profile="fast")

    assert build_ocr_text_provider(settings, repository) is None
    assert build_document_extraction_provider(settings, repository).provider_name == "openai"


def test_core_internal_provider_selection_api_updates_active_selection():
    repository = SelectionRepository()
    app = create_app(settings=Settings(openai_api_key="test-key"), repository=repository)
    client = TestClient(app)

    response = client.put(
        "/internal/provider-selections/llm",
        json={"provider": "openai", "profile": "balanced", "changed_by": "operator"},
    )

    assert response.status_code == 200
    assert response.json()["selections"]["llm"]["provider"] == "openai"
    assert repository.selections["llm"].updated_by == "operator"
    assert repository.history[0]["new_provider"] == "openai"


def test_core_internal_provider_selection_api_rejects_missing_config():
    repository = SelectionRepository()
    app = create_app(settings=Settings(openai_api_key=None), repository=repository)
    client = TestClient(app)

    response = client.put(
        "/internal/provider-selections/llm",
        json={"provider": "openai", "profile": "default"},
    )

    assert response.status_code == 400
    assert "OPENAI_API_KEY" in response.json()["detail"]
