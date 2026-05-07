import json
from pathlib import Path

import httpx
from fastapi.testclient import TestClient

from app.ai_assessment.enums import ApplicantRole, AssessmentStatus, DocumentType, HolderType, LegalEntityType
from app.ai_assessment.providers.openai import OpenAiDocumentExtractionProvider
from app.ai_assessment.schemas import DocumentMetadata
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
