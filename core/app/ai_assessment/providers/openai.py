import json
import time
from pathlib import Path
from typing import Any

import httpx

from app.ai_assessment.enums import DocumentType, HolderType
from app.ai_assessment.schemas import DocumentMetadata


class OpenAiDocumentExtractionProvider:
    provider_name = "openai"

    def __init__(
        self,
        *,
        api_key: str,
        model: str = "gpt-5.5",
        base_url: str = "https://api.openai.com/v1",
        timeout: float = 120.0,
    ) -> None:
        self.api_key = api_key
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def extract(self, document: DocumentMetadata) -> DocumentMetadata:
        document_type = document.predictedDocumentType or document.declaredDocumentType or DocumentType.UNKNOWN
        source_text = self._document_text(document)
        if not source_text.strip():
            return document
        started = time.perf_counter()
        extracted = self._extract_json(document_type, source_text)
        elapsed_ms = (time.perf_counter() - started) * 1000
        normalized = self._normalize_extraction(document_type, extracted)
        return document.model_copy(
            update={
                "predictedDocumentType": document_type,
                "classificationConfidence": document.classificationConfidence or 0.9,
                "extracted": normalized,
            }
        )

    def _extract_json(self, document_type: DocumentType, source_text: str) -> dict[str, Any]:
        response = httpx.post(
            f"{self.base_url}/responses",
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": self.model,
                "input": [
                    {
                        "role": "system",
                        "content": (
                            "You are a KYvC KYC document extraction provider. "
                            "Extract only values present in the document text. "
                            "Do not decide final KYC status."
                        ),
                    },
                    {
                        "role": "user",
                        "content": (
                            f"Expected document type: {document_type.value}.\n"
                            "Return compact JSON matching the schema. "
                            "Use null for unknown values.\n\n"
                            f"Document text:\n{source_text[:12000]}"
                        ),
                    },
                ],
                "text": {"format": self._response_format(document_type)},
            },
            timeout=self.timeout,
        )
        response.raise_for_status()
        return json.loads(self._response_output_text(response.json()))

    def _response_format(self, document_type: DocumentType) -> dict[str, Any]:
        if document_type == DocumentType.BUSINESS_REGISTRATION:
            properties = {
                "documentType": {"type": "string", "enum": [document_type.value]},
                "legalName": {"type": ["string", "null"]},
                "businessRegistrationNumber": {"type": ["string", "null"]},
                "representativeName": {"type": ["string", "null"]},
                "representativeBirthDate": {"type": ["string", "null"]},
                "representativeNationality": {"type": ["string", "null"]},
                "businessAddress": {"type": ["string", "null"]},
            }
        elif document_type == DocumentType.CORPORATE_REGISTRY:
            properties = {
                "documentType": {"type": "string", "enum": [document_type.value]},
                "legalName": {"type": ["string", "null"]},
                "corporateRegistrationNumber": {"type": ["string", "null"]},
                "representativeName": {"type": ["string", "null"]},
                "representativeBirthDate": {"type": ["string", "null"]},
                "representativeNationality": {"type": ["string", "null"]},
                "headOfficeAddress": {"type": ["string", "null"]},
                "purpose": {"type": ["string", "null"]},
            }
        elif document_type in {
            DocumentType.SHAREHOLDER_REGISTRY,
            DocumentType.STOCK_CHANGE_STATEMENT,
            DocumentType.INVESTOR_REGISTRY,
            DocumentType.MEMBER_REGISTRY,
            DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT,
        }:
            properties = {
                "documentType": {"type": "string", "enum": [document_type.value]},
                "legalName": {"type": ["string", "null"]},
                "totalShares": {"type": ["number", "null"]},
                "shareholders": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "name": {"type": ["string", "null"]},
                            "holderType": {"type": "string", "enum": [item.value for item in HolderType]},
                            "birthDate": {"type": ["string", "null"]},
                            "nationality": {"type": ["string", "null"]},
                            "englishName": {"type": ["string", "null"]},
                            "shares": {"type": ["number", "null"]},
                            "ownershipPercent": {"type": ["number", "null"]},
                        },
                        "required": [
                            "name",
                            "holderType",
                            "birthDate",
                            "nationality",
                            "englishName",
                            "shares",
                            "ownershipPercent",
                        ],
                        "additionalProperties": False,
                    },
                },
            }
        else:
            properties = {
                "documentType": {"type": "string"},
                "legalName": {"type": ["string", "null"]},
                "representativeName": {"type": ["string", "null"]},
                "establishmentPurpose": {"type": ["string", "null"]},
                "purposeVerificationSatisfied": {"type": "boolean"},
            }
        return {
            "type": "json_schema",
            "name": "kyvc_ai_document_extraction",
            "schema": {
                "type": "object",
                "properties": properties,
                "required": list(properties),
                "additionalProperties": False,
            },
            "strict": True,
        }

    def _normalize_extraction(self, document_type: DocumentType, payload: dict[str, Any]) -> dict[str, Any]:
        if document_type == DocumentType.BUSINESS_REGISTRATION:
            return {
                "legalName": self._value(payload.get("legalName")),
                "businessRegistrationNumber": self._value(payload.get("businessRegistrationNumber")),
                "representativeName": self._value(payload.get("representativeName")),
                "representative": {
                    "name": self._value(payload.get("representativeName")),
                    "birthDate": self._value(payload.get("representativeBirthDate")),
                    "nationality": self._value(payload.get("representativeNationality")),
                },
                "businessAddress": self._value(payload.get("businessAddress")),
            }
        if document_type == DocumentType.CORPORATE_REGISTRY:
            return {
                "legalName": self._value(payload.get("legalName")),
                "corporateRegistrationNumber": self._value(payload.get("corporateRegistrationNumber")),
                "representativeName": self._value(payload.get("representativeName")),
                "representative": {
                    "name": self._value(payload.get("representativeName")),
                    "birthDate": self._value(payload.get("representativeBirthDate")),
                    "nationality": self._value(payload.get("representativeNationality")),
                },
                "headOfficeAddress": self._value(payload.get("headOfficeAddress")),
                "purpose": self._value(payload.get("purpose")),
            }
        if document_type in {
            DocumentType.SHAREHOLDER_REGISTRY,
            DocumentType.STOCK_CHANGE_STATEMENT,
            DocumentType.INVESTOR_REGISTRY,
            DocumentType.MEMBER_REGISTRY,
            DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT,
        }:
            return {
                "legalName": self._value(payload.get("legalName")),
                "totalShares": self._value(payload.get("totalShares")),
                "shareholders": [
                    {
                        "name": item.get("name"),
                        "holderType": item.get("holderType") or HolderType.UNKNOWN.value,
                        "birthDate": item.get("birthDate"),
                        "nationality": item.get("nationality"),
                        "englishName": item.get("englishName"),
                        "shares": self._int_or_none(item.get("shares")),
                        "ownershipPercent": item.get("ownershipPercent"),
                    }
                    for item in payload.get("shareholders", [])
                    if isinstance(item, dict)
                ],
            }
        purpose = payload.get("establishmentPurpose")
        return {
            "legalName": self._value(payload.get("legalName")),
            "representative": {"name": self._value(payload.get("representativeName"))},
            "purposeVerification": {
                "establishmentPurpose": self._value(purpose),
                "acceptableForPurposeVerification": bool(purpose),
                "purposeVerificationSatisfied": bool(payload.get("purposeVerificationSatisfied")),
            },
        }

    def _document_text(self, document: DocumentMetadata) -> str:
        if document.storagePath:
            path = Path(document.storagePath)
            if path.exists():
                if path.suffix.lower() == ".json":
                    return json.dumps(json.loads(path.read_text(encoding="utf-8")), ensure_ascii=False)
                return path.read_text(encoding="utf-8", errors="ignore")
        if isinstance(document.extracted, str):
            return document.extracted
        if isinstance(document.extracted, dict):
            return json.dumps(document.extracted, ensure_ascii=False)
        return ""

    def _value(self, value: Any) -> dict[str, Any]:
        return {"raw": value, "normalized": value, "confidence": 0.85 if value not in (None, "") else 0.0}

    def _int_or_none(self, value: Any) -> int | None:
        if value is None:
            return None
        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    def _response_output_text(self, payload: dict[str, Any]) -> str:
        for item in payload.get("output", []):
            for content in item.get("content", []):
                if content.get("type") == "output_text" and content.get("text"):
                    return content["text"]
        text = payload.get("output_text")
        if text:
            return text
        raise RuntimeError("OpenAI response did not include output text.")
