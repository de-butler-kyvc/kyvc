import json
from pathlib import Path

import httpx
import pytest
from fastapi.testclient import TestClient

from app.ai_assessment.enums import ApplicantRole, AssessmentStatus, DocumentType, HolderType, LegalEntityType
from app.ai_assessment.providers.openai import OpenAiDocumentExtractionProvider
from app.ai_assessment.schemas import DocumentMetadata, KycApplication
from app.ai_assessment.service import AssessmentService
from app.core.config import Settings
from app.main import create_app


def _value(value, confidence=0.99):
    return {"raw": value, "normalized": value, "confidence": confidence}


def _person(name="Kim Representative"):
    return {
        "name": _value(name),
        "birthDate": _value("1980-01-01"),
        "nationality": _value("KR"),
    }


def _business():
    return {
        "legalName": _value("KYvC Labs"),
        "businessRegistrationNumber": _value("123-45-67890"),
        "representativeName": _value("Kim Representative"),
        "representative": _person(),
        "businessAddress": _value("Seoul"),
    }


def _registry():
    return {
        "legalName": _value("KYvC Labs"),
        "corporateRegistrationNumber": _value("110111-1234567"),
        "representativeName": _value("Kim Representative"),
        "representative": _person(),
        "headOfficeAddress": _value("Seoul"),
        "purpose": _value("software business"),
    }


def _owners():
    return {
        "legalName": _value("KYvC Labs"),
        "totalShares": _value(1000),
        "shareholders": [
            {
                "name": "Owner One",
                "holderType": HolderType.INDIVIDUAL,
                "birthDate": "1979-02-03",
                "nationality": "KR",
                "shares": 600,
                "ownershipPercent": 60.0,
            },
            {
                "name": "Owner Two",
                "holderType": HolderType.INDIVIDUAL,
                "shares": 400,
                "ownershipPercent": 40.0,
            },
        ],
    }


class FakeLlmPrimaryProvider:
    provider_name = "fake_llm_primary"

    def __init__(self):
        self.calls = []

    def extract(self, document):
        self.calls.append(document.documentId)
        payloads = {
            "business": (DocumentType.BUSINESS_REGISTRATION, _business()),
            "registry": (DocumentType.CORPORATE_REGISTRY, _registry()),
            "owners": (DocumentType.SHAREHOLDER_REGISTRY, _owners()),
        }
        document_type, extracted = payloads[document.documentId]
        assert document.storagePath
        assert Path(document.storagePath).exists()
        return document.model_copy(
            update={
                "predictedDocumentType": document_type,
                "classificationConfidence": 0.97,
                "extracted": extracted,
            }
        )


class RaisingLlmProvider:
    provider_name = "raising_llm"

    def extract(self, document):
        raise RuntimeError("LLM exploded")


class ExceptionLlmProvider:
    provider_name = "exception_llm"

    def __init__(self, exc):
        self.exc = exc

    def extract(self, document):
        raise self.exc


class InvalidSchemaProvider:
    provider_name = "invalid_schema_llm"

    def extract(self, document):
        invalid_by_type = {
            DocumentType.BUSINESS_REGISTRATION: {"representative": "not-an-object"},
            DocumentType.CORPORATE_REGISTRY: {"representative": "not-an-object"},
            DocumentType.SHAREHOLDER_REGISTRY: {"shareholders": "not-a-list"},
        }
        document_type = document.declaredDocumentType
        return document.model_copy(
            update={
                "predictedDocumentType": document_type,
                "classificationConfidence": 0.97,
                "extracted": invalid_by_type[document_type],
            }
        )


class MisleadingStatusProvider:
    provider_name = "misleading_status_llm"

    def extract(self, document):
        payloads = {
            "business": (DocumentType.BUSINESS_REGISTRATION, _business()),
            "registry": (DocumentType.CORPORATE_REGISTRY, _registry()),
            "owners": (
                DocumentType.SHAREHOLDER_REGISTRY,
                {
                    "legalName": _value("KYvC Labs"),
                    "totalShares": _value(1000),
                    "shareholders": [],
                    "assessmentStatus": "NORMAL",
                },
            ),
        }
        document_type, extracted = payloads[document.documentId]
        return document.model_copy(
            update={
                "predictedDocumentType": document_type,
                "classificationConfidence": 0.97,
                "extracted": extracted,
            }
        )


def _document(document_id, document_type, extracted, *, storage_path=None):
    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId="app-fallback",
        originalFileName=f"{document_id}.txt",
        mimeType="text/plain",
        declaredDocumentType=document_type,
        predictedDocumentType=document_type,
        classificationConfidence=0.98,
        storagePath=str(storage_path) if storage_path else None,
        extracted=extracted,
    )


def _fallback_documents(*, storage_dir=None):
    payloads = [
        ("business", DocumentType.BUSINESS_REGISTRATION, _business()),
        ("registry", DocumentType.CORPORATE_REGISTRY, _registry()),
        ("owners", DocumentType.SHAREHOLDER_REGISTRY, _owners()),
    ]
    documents = []
    for document_id, document_type, extracted in payloads:
        path = None
        if storage_dir:
            path = storage_dir / f"{document_id}.txt"
            path.write_text(f"{document_id} source text", encoding="utf-8")
        documents.append(_document(document_id, document_type, extracted, storage_path=path))
    return documents


def _application(app_id="app-fallback"):
    return KycApplication(
        kycApplicationId=app_id,
        legalEntityType=LegalEntityType.STOCK_COMPANY,
        applicantRole=ApplicantRole.REPRESENTATIVE,
        businessRegistrationNumber="1234567890",
        corporateRegistrationNumber="1101111234567",
    )


def _log_operations(assessment):
    return {(log.providerCategory, log.providerName, log.operation, log.error) for log in assessment.providerUsageLogs}


def test_llm_primary_assessment_api_accepts_documents_and_returns_assessment(tmp_path, monkeypatch):
    provider = FakeLlmPrimaryProvider()
    monkeypatch.setattr("app.ai_assessment.llm_primary.build_document_extraction_provider", lambda settings: provider)
    app = create_app(
        settings=Settings(app_storage_path=str(tmp_path), llm_provider="openai"),
        repository=object(),
    )
    client = TestClient(app)

    response = client.post(
        "/ai-assessment/assessments/llm-primary",
        json={
            "kycApplicationId": "app-llm-primary",
            "legalEntityType": LegalEntityType.STOCK_COMPANY,
            "applicantRole": ApplicantRole.REPRESENTATIVE,
            "businessRegistrationNumber": "1234567890",
            "corporateRegistrationNumber": "1101111234567",
            "documents": [
                {
                    "documentId": "business",
                    "originalFileName": "business.txt",
                    "declaredDocumentType": DocumentType.BUSINESS_REGISTRATION,
                    "textContent": "business registration scan text",
                },
                {
                    "documentId": "registry",
                    "originalFileName": "registry.txt",
                    "declaredDocumentType": DocumentType.CORPORATE_REGISTRY,
                    "textContent": "corporate registry scan text",
                },
                {
                    "documentId": "owners",
                    "originalFileName": "owners.txt",
                    "declaredDocumentType": DocumentType.SHAREHOLDER_REGISTRY,
                    "textContent": "shareholder registry scan text",
                },
            ],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["strategy"] == "llm_primary"
    assert payload["extractionProvider"] == "fake_llm_primary"
    assert payload["assessment"]["status"] == AssessmentStatus.NORMAL
    assert {owner["name"] for owner in payload["assessment"]["beneficialOwnership"]["owners"]} == {"Owner One", "Owner Two"}
    assert provider.calls == ["business", "registry", "owners"]
    assert all(document["storagePath"] for document in payload["documents"])


def test_llm_primary_assessment_api_requires_llm_provider(tmp_path):
    app = create_app(
        settings=Settings(app_storage_path=str(tmp_path), llm_provider="none"),
        repository=object(),
    )
    client = TestClient(app)

    response = client.post(
        "/ai-assessment/assessments/llm-primary",
        json={
            "kycApplicationId": "app-no-llm",
            "legalEntityType": LegalEntityType.STOCK_COMPANY,
            "documents": [{"documentId": "business", "textContent": "scan text"}],
        },
    )

    assert response.status_code == 400
    assert "LLM_PROVIDER" in response.json()["detail"]


def test_openai_llm_primary_provider_classifies_and_normalizes_poa(tmp_path, monkeypatch):
    source = tmp_path / "poa.txt"
    source.write_text("Power of attorney for KYvC Labs", encoding="utf-8")

    response_payload = {
        "documentType": "POWER_OF_ATTORNEY",
        "classificationConfidence": 0.91,
        "legalName": None,
        "businessRegistrationNumber": None,
        "corporateRegistrationNumber": None,
        "representativeName": None,
        "representativeBirthDate": None,
        "representativeNationality": None,
        "representativeEnglishName": None,
        "businessAddress": None,
        "headOfficeAddress": None,
        "purpose": None,
        "establishmentPurpose": None,
        "acceptableForPurposeVerification": None,
        "purposeVerificationSatisfied": None,
        "issueDate": "2026-01-01",
        "totalShares": None,
        "shareholders": [],
        "delegatorName": "Kim Representative",
        "delegateName": "Lee Delegate",
        "targetCorporateName": "KYvC Labs",
        "authorityText": "KYC application, document submission, VC receipt",
        "canApplyKyc": True,
        "canSubmitDocuments": True,
        "canReceiveVc": True,
        "validFrom": "2026-01-01",
        "validUntil": "2999-12-31",
        "hasSignatureOrSeal": True,
        "sealImpressionId": "seal-a",
        "sealCertificateSubjectName": None,
        "sealCertificateCorporateName": None,
        "certificateNumber": None,
    }

    def fake_post(*args, **kwargs):
        return httpx.Response(
            200,
            json={"output": [{"content": [{"type": "output_text", "text": json.dumps(response_payload)}]}]},
            request=httpx.Request("POST", str(args[0])),
        )

    monkeypatch.setattr("app.ai_assessment.providers.openai.httpx.post", fake_post)

    document = OpenAiDocumentExtractionProvider(api_key="test", model="test-model").extract(
        DocumentMetadata(
            documentId="poa",
            kycApplicationId="app-poa",
            originalFileName="poa.txt",
            mimeType="text/plain",
            storagePath=str(source),
        )
    )

    assert document.predictedDocumentType == DocumentType.POWER_OF_ATTORNEY
    assert document.classificationConfidence == 0.91
    assert document.extracted["delegateName"]["normalized"] == "Lee Delegate"
    assert document.extracted["canReceiveVc"]["normalized"] is True


def test_llm_provider_exception_falls_back_to_deterministic_extraction():
    assessment = AssessmentService(extraction_provider=RaisingLlmProvider()).assess(
        _application(),
        _fallback_documents(),
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"Owner One", "Owner Two"}
    assert all(result.classificationReason.startswith("deterministic fallback") for result in assessment.documentResults)
    assert any(
        category == "EXTRACTOR" and provider == "structured_payload" and operation == "deterministic_fallback"
        for category, provider, operation, error in _log_operations(assessment)
    )
    assert any(error == "RuntimeError" for category, provider, operation, error in _log_operations(assessment))


def test_malformed_openai_json_falls_back_to_deterministic_extraction(tmp_path, monkeypatch):
    def fake_post(*args, **kwargs):
        return httpx.Response(
            200,
            json={"output": [{"content": [{"type": "output_text", "text": "{not-json"}]}]},
            request=httpx.Request("POST", str(args[0])),
        )

    monkeypatch.setattr("app.ai_assessment.providers.openai.httpx.post", fake_post)

    assessment = AssessmentService(
        extraction_provider=OpenAiDocumentExtractionProvider(api_key="test", model="test-model")
    ).assess(
        _application(),
        _fallback_documents(storage_dir=tmp_path),
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"Owner One", "Owner Two"}
    assert any(error == "JSONDecodeError" for category, provider, operation, error in _log_operations(assessment))
    assert any(operation == "deterministic_fallback" for category, provider, operation, error in _log_operations(assessment))


@pytest.mark.parametrize(
    ("exc", "expected_error"),
    [
        (
            httpx.HTTPStatusError(
                "rate limited",
                request=httpx.Request("POST", "https://api.example/responses"),
                response=httpx.Response(429, request=httpx.Request("POST", "https://api.example/responses")),
            ),
            "RateLimitError",
        ),
        (
            httpx.HTTPStatusError(
                "server error",
                request=httpx.Request("POST", "https://api.example/responses"),
                response=httpx.Response(500, request=httpx.Request("POST", "https://api.example/responses")),
            ),
            "HTTPStatusError",
        ),
        (httpx.TimeoutException("timeout"), "TimeoutError"),
    ],
)
def test_http_timeout_and_rate_limit_failures_fall_back(exc, expected_error):
    assessment = AssessmentService(extraction_provider=ExceptionLlmProvider(exc)).assess(
        _application(),
        _fallback_documents(),
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert any(error == expected_error for category, provider, operation, error in _log_operations(assessment))
    assert any(operation == "deterministic_fallback" for category, provider, operation, error in _log_operations(assessment))


def test_llm_schema_parse_failure_falls_back_to_deterministic_extraction():
    assessment = AssessmentService(extraction_provider=InvalidSchemaProvider()).assess(
        _application(),
        _fallback_documents(),
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"Owner One", "Owner Two"}
    assert any(error == "ValidationError" for category, provider, operation, error in _log_operations(assessment))
    assert any(operation == "deterministic_fallback" for category, provider, operation, error in _log_operations(assessment))


def test_final_status_is_decision_engine_output_not_provider_status():
    assessment = AssessmentService(extraction_provider=MisleadingStatusProvider()).assess(
        _application("app-provider-status"),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, _business()),
            _document("registry", DocumentType.CORPORATE_REGISTRY, _registry()),
            _document("owners", DocumentType.SHAREHOLDER_REGISTRY, _owners()),
        ],
    )

    assert assessment.status == AssessmentStatus.MANUAL_REVIEW_REQUIRED
    assert "BENEFICIAL_OWNER_REVIEW_REQUIRED" in {issue.code for issue in assessment.manualReviewReasons}
    assert assessment.documentResults[2].extracted.get("assessmentStatus") is None
