import json
import mimetypes
import os
from pathlib import Path

import httpx
import pytest
from dotenv import load_dotenv

from app.ai_assessment import AssessmentService
from app.ai_assessment.enums import ApplicantRole, AssessmentStatus, DocumentType, LegalEntityType
from app.ai_assessment.providers.factory import build_document_extraction_provider, build_ocr_text_provider
from app.ai_assessment.providers.ocr import AzureDocumentIntelligenceOcrTextProvider, NaverClovaOcrTextProvider
from app.ai_assessment.providers.openai import OpenAiDocumentExtractionProvider
from app.ai_assessment.schemas import DocumentMetadata, KycApplication
from app.core.config import Settings, get_settings


load_dotenv()


@pytest.mark.integration
def test_openai_structured_extraction_smoke():
    if os.getenv("RUN_AI_INTEGRATION_TESTS") != "1":
        pytest.skip("Set RUN_AI_INTEGRATION_TESTS=1 to call external AI providers.")
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        pytest.skip("OPENAI_API_KEY is required for OpenAI integration smoke test.")

    model = os.getenv("OPENAI_MODEL") or os.getenv("LLM_MODEL") or "gpt-5.5"
    response = httpx.post(
        os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/") + "/responses",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        json={
            "model": model,
            "input": [
                {
                    "role": "system",
                    "content": (
                        "You are a KYvC KYC document extraction smoke-test provider. "
                        "Return only a compact JSON object."
                    ),
                },
                {
                    "role": "user",
                    "content": (
                        "Extract a business registration document from this text: "
                        "Company KYvC Labs, business registration number 123-45-67890, "
                        "representative Kim Representative. "
                        "Return JSON with documentType, legalName, businessRegistrationNumber, representativeName."
                    ),
                },
            ],
            "text": {
                "format": {
                    "type": "json_schema",
                    "name": "kyvc_business_registration_smoke",
                    "schema": {
                        "type": "object",
                        "properties": {
                            "documentType": {"type": "string", "enum": ["BUSINESS_REGISTRATION"]},
                            "legalName": {"type": "string"},
                            "businessRegistrationNumber": {"type": "string"},
                            "representativeName": {"type": "string"},
                        },
                        "required": [
                            "documentType",
                            "legalName",
                            "businessRegistrationNumber",
                            "representativeName",
                        ],
                        "additionalProperties": False,
                    },
                    "strict": True,
                }
            },
        },
        timeout=60,
    )
    response.raise_for_status()
    payload = response.json()
    output_text = _response_output_text(payload)
    extracted = json.loads(output_text)

    assert extracted["documentType"] == "BUSINESS_REGISTRATION"
    assert extracted["legalName"] == "KYvC Labs"
    assert extracted["businessRegistrationNumber"] == "123-45-67890"
    assert extracted["representativeName"] == "Kim Representative"


@pytest.mark.integration
def test_openai_document_extraction_provider_runs_assessment(tmp_path):
    if os.getenv("RUN_AI_INTEGRATION_TESTS") != "1":
        pytest.skip("Set RUN_AI_INTEGRATION_TESTS=1 to call external AI providers.")
    if not os.getenv("OPENAI_API_KEY"):
        pytest.skip("OPENAI_API_KEY is required for OpenAI integration smoke test.")

    business = tmp_path / "business.txt"
    business.write_text(
        "Business Registration Certificate\n"
        "Company: KYvC Labs\n"
        "Business registration number: 123-45-67890\n"
        "Representative: Kim Representative\n"
        "Business address: Seoul\n",
        encoding="utf-8",
    )
    registry = tmp_path / "registry.txt"
    registry.write_text(
        "Corporate Registry Full Certificate\n"
        "Company: KYvC Labs\n"
        "Corporate registration number: 110111-1234567\n"
        "Representative: Kim Representative\n"
        "Head office address: Seoul\n"
        "Purpose: software business\n",
        encoding="utf-8",
    )
    owners = tmp_path / "owners.txt"
    owners.write_text(
        "Shareholder Registry\n"
        "Company: KYvC Labs\n"
        "Total shares: 1000\n"
        "Shareholder Alice Park, individual, nationality KR, 600 shares, ownership 60 percent\n"
        "Shareholder Bob Lee, individual, nationality KR, 400 shares, ownership 40 percent\n",
        encoding="utf-8",
    )
    settings = get_settings().model_copy(update={"llm_provider": "openai", "ocr_provider": "structured_payload"})
    provider = build_document_extraction_provider(settings)
    assert provider is not None
    assessment = AssessmentService(extraction_provider=provider).assess(
        KycApplication(
            kycApplicationId="app-openai",
            legalEntityType=LegalEntityType.STOCK_COMPANY,
            applicantRole=ApplicantRole.REPRESENTATIVE,
            businessRegistrationNumber="1234567890",
            corporateRegistrationNumber="1101111234567",
        ),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, business),
            _document("registry", DocumentType.CORPORATE_REGISTRY, registry),
            _document("owners", DocumentType.SHAREHOLDER_REGISTRY, owners),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"Alice Park", "Bob Lee"}
    assert all(result.extracted for result in assessment.documentResults)


@pytest.mark.integration
def test_openai_provider_uses_ocr_text_for_assessment(tmp_path):
    if os.getenv("RUN_AI_INTEGRATION_TESTS") != "1":
        pytest.skip("Set RUN_AI_INTEGRATION_TESTS=1 to call external AI providers.")
    if not os.getenv("OPENAI_API_KEY"):
        pytest.skip("OPENAI_API_KEY is required for OpenAI integration smoke test.")

    class StaticOcrProvider:
        provider_name = "static_ocr"

        def extract_text(self, document):
            texts = {
                "business": (
                    "Business Registration Certificate\n"
                    "Company: KYvC Labs\n"
                    "Business registration number: 123-45-67890\n"
                    "Representative: Kim Representative\n"
                    "Business address: Seoul\n"
                ),
                "registry": (
                    "Corporate Registry Full Certificate\n"
                    "Company: KYvC Labs\n"
                    "Corporate registration number: 110111-1234567\n"
                    "Representative: Kim Representative\n"
                    "Head office address: Seoul\n"
                    "Purpose: software business\n"
                ),
                "owners": (
                    "Shareholder Registry\n"
                    "Company: KYvC Labs\n"
                    "Total shares: 1000\n"
                    "Shareholder Alice Park, individual, nationality KR, 600 shares, ownership 60 percent\n"
                    "Shareholder Bob Lee, individual, nationality KR, 400 shares, ownership 40 percent\n"
                ),
            }
            return texts[document.documentId]

    blank = tmp_path / "scan.pdf"
    blank.write_bytes(b"%PDF-1.4\n% blank smoke fixture\n")
    provider = OpenAiDocumentExtractionProvider(
        api_key=os.environ["OPENAI_API_KEY"],
        model=os.getenv("OPENAI_MODEL") or os.getenv("LLM_MODEL") or "gpt-5.5",
        base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        ocr_provider=StaticOcrProvider(),
    )
    assessment = AssessmentService(extraction_provider=provider).assess(
        KycApplication(
            kycApplicationId="app-openai-ocr",
            legalEntityType=LegalEntityType.STOCK_COMPANY,
            applicantRole=ApplicantRole.REPRESENTATIVE,
            businessRegistrationNumber="1234567890",
            corporateRegistrationNumber="1101111234567",
        ),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, blank),
            _document("registry", DocumentType.CORPORATE_REGISTRY, blank),
            _document("owners", DocumentType.SHAREHOLDER_REGISTRY, blank),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"Alice Park", "Bob Lee"}


def test_azure_document_intelligence_ocr_text_provider_parses_content(tmp_path, monkeypatch):
    scanned = tmp_path / "scan.pdf"
    scanned.write_bytes(b"pdf")

    def fake_post(*args, **kwargs):
        assert "documentModels/prebuilt-layout:analyze" in str(args[0])
        return httpx.Response(
            202,
            headers={"operation-location": "https://azure.example/operations/1"},
            request=httpx.Request("POST", str(args[0])),
        )

    def fake_get(*args, **kwargs):
        assert args[0] == "https://azure.example/operations/1"
        return httpx.Response(
            200,
            json={
                "status": "succeeded",
                "analyzeResult": {
                    "content": "Company: KYvC Labs\nBusiness registration number: 123-45-67890",
                },
            },
            request=httpx.Request("GET", str(args[0])),
        )

    monkeypatch.setattr("app.ai_assessment.providers.ocr.httpx.post", fake_post)
    monkeypatch.setattr("app.ai_assessment.providers.ocr.httpx.get", fake_get)

    text = AzureDocumentIntelligenceOcrTextProvider(
        endpoint="https://azure.example",
        key="secret",
        poll_interval_seconds=0,
    ).extract_text(_document("business", DocumentType.BUSINESS_REGISTRATION, scanned))

    assert "KYvC Labs" in text
    assert "123-45-67890" in text


def test_naver_clova_ocr_text_provider_parses_fields(tmp_path, monkeypatch):
    scanned = tmp_path / "scan.jpg"
    scanned.write_bytes(b"jpg")

    def fake_post(*args, **kwargs):
        assert args[0] == "https://clova.example/ocr"
        assert kwargs["headers"]["X-OCR-SECRET"] == "secret"
        return httpx.Response(
            200,
            json={
                "images": [
                    {
                        "fields": [
                            {"inferText": "Company:", "lineBreak": False},
                            {"inferText": "KYvC Labs", "lineBreak": True},
                            {"inferText": "Representative:", "lineBreak": False},
                            {"inferText": "Kim Representative", "lineBreak": True},
                        ]
                    }
                ]
            },
            request=httpx.Request("POST", str(args[0])),
        )

    monkeypatch.setattr("app.ai_assessment.providers.ocr.httpx.post", fake_post)

    text = NaverClovaOcrTextProvider(
        endpoint="https://clova.example/ocr",
        secret="secret",
    ).extract_text(_document("business", DocumentType.BUSINESS_REGISTRATION, scanned))

    assert "Company: KYvC Labs" in text
    assert "Representative: Kim Representative" in text


def test_provider_factory_builds_openai_with_ocr_provider():
    settings = Settings(
        llm_provider="openai",
        openai_api_key="openai-secret",
        openai_model="gpt-test",
        ocr_provider="azure_document_intelligence",
        azure_document_intelligence_endpoint="https://azure.example",
        azure_document_intelligence_key="azure-secret",
    )

    provider = build_document_extraction_provider(settings)
    ocr_provider = build_ocr_text_provider(settings)

    assert isinstance(provider, OpenAiDocumentExtractionProvider)
    assert isinstance(provider.ocr_provider, AzureDocumentIntelligenceOcrTextProvider)
    assert isinstance(ocr_provider, AzureDocumentIntelligenceOcrTextProvider)


@pytest.mark.integration
def test_azure_document_intelligence_real_ocr_smoke():
    if os.getenv("RUN_AI_INTEGRATION_TESTS") != "1":
        pytest.skip("Set RUN_AI_INTEGRATION_TESTS=1 to call external OCR providers.")
    if not os.getenv("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT") or not os.getenv("AZURE_DOCUMENT_INTELLIGENCE_KEY"):
        pytest.skip("Azure Document Intelligence env is required for real OCR smoke test.")
    fixture = os.getenv("AZURE_DOCUMENT_INTELLIGENCE_TEST_FILE") or os.getenv("AI_ASSESSMENT_OCR_TEST_FILE")
    if not fixture:
        pytest.skip("AZURE_DOCUMENT_INTELLIGENCE_TEST_FILE or AI_ASSESSMENT_OCR_TEST_FILE is required.")
    path = Path(fixture)
    provider = AzureDocumentIntelligenceOcrTextProvider(
        endpoint=os.environ["AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT"],
        key=os.environ["AZURE_DOCUMENT_INTELLIGENCE_KEY"],
        model_id=os.getenv("AZURE_DOCUMENT_INTELLIGENCE_MODEL_ID", "prebuilt-layout"),
        api_version=os.getenv("AZURE_DOCUMENT_INTELLIGENCE_API_VERSION") or None,
    )
    text = provider.extract_text(_document("azure-real", DocumentType.BUSINESS_REGISTRATION, path))

    assert text.strip()


@pytest.mark.integration
def test_naver_clova_real_ocr_smoke():
    if os.getenv("RUN_AI_INTEGRATION_TESTS") != "1":
        pytest.skip("Set RUN_AI_INTEGRATION_TESTS=1 to call external OCR providers.")
    if not os.getenv("NAVER_CLOVA_OCR_ENDPOINT") or not os.getenv("NAVER_CLOVA_OCR_SECRET"):
        pytest.skip("NAVER CLOVA OCR env is required for real OCR smoke test.")
    fixture = os.getenv("NAVER_CLOVA_OCR_TEST_FILE") or os.getenv("AI_ASSESSMENT_OCR_TEST_FILE")
    if not fixture:
        pytest.skip("NAVER_CLOVA_OCR_TEST_FILE or AI_ASSESSMENT_OCR_TEST_FILE is required.")
    path = Path(fixture)
    provider = NaverClovaOcrTextProvider(
        endpoint=os.environ["NAVER_CLOVA_OCR_ENDPOINT"],
        secret=os.environ["NAVER_CLOVA_OCR_SECRET"],
        template_id=os.getenv("NAVER_CLOVA_OCR_TEMPLATE_ID") or None,
    )
    text = provider.extract_text(_document("clova-real", DocumentType.BUSINESS_REGISTRATION, path))

    assert text.strip()


def _document(document_id: str, document_type: DocumentType, path) -> DocumentMetadata:
    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId="app-openai",
        originalFileName=path.name,
        mimeType=mimetypes.guess_type(path.name)[0] or "application/octet-stream",
        sizeBytes=path.stat().st_size,
        sha256=f"sha-{document_id}",
        declaredDocumentType=document_type,
        predictedDocumentType=document_type,
        classificationConfidence=0.98,
        storagePath=str(path),
    )


def _response_output_text(payload: dict) -> str:
    for item in payload.get("output", []):
        for content in item.get("content", []):
            if content.get("type") == "output_text" and content.get("text"):
                return content["text"]
    text = payload.get("output_text")
    if text:
        return text
    raise AssertionError("OpenAI response did not include output text.")
