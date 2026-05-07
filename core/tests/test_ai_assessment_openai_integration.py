import json
import os

import httpx
import pytest
from dotenv import load_dotenv

from app.ai_assessment import AssessmentService
from app.ai_assessment.enums import ApplicantRole, AssessmentStatus, DocumentType, LegalEntityType
from app.ai_assessment.providers.factory import build_document_extraction_provider
from app.ai_assessment.schemas import DocumentMetadata, KycApplication
from app.core.config import get_settings


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
        "Shareholder Owner One, individual, nationality KR, 600 shares, ownership 60 percent\n"
        "Shareholder Owner Two, individual, nationality KR, 400 shares, ownership 40 percent\n",
        encoding="utf-8",
    )
    settings = get_settings().model_copy(update={"llm_provider": "openai"})
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
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"Owner One", "Owner Two"}
    assert all(result.extracted for result in assessment.documentResults)


def _document(document_id: str, document_type: DocumentType, path) -> DocumentMetadata:
    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId="app-openai",
        originalFileName=path.name,
        mimeType="text/plain",
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
