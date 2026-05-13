import json
from pathlib import Path
from typing import Any

import httpx

from app.ai_assessment.enums import DocumentType, HolderType
from app.ai_assessment.providers.base import LlmExtractionError
from app.ai_assessment.providers.ocr import OcrTextProvider
from app.ai_assessment.schemas import DocumentMetadata
from app.resilience.outbound import execute_outbound


class OpenAiDocumentExtractionProvider:
    provider_name = "openai"

    def __init__(
        self,
        *,
        api_key: str,
        model: str = "gpt-5.5",
        base_url: str = "https://api.openai.com/v1",
        ocr_provider: OcrTextProvider | None = None,
        timeout: float = 120.0,
    ) -> None:
        self.api_key = api_key
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.ocr_provider = ocr_provider
        self.timeout = timeout

    def extract(self, document: DocumentMetadata) -> DocumentMetadata:
        declared_document_type = document.predictedDocumentType or document.declaredDocumentType or DocumentType.UNKNOWN
        source_text = self._document_text(document)
        if not source_text.strip():
            return document
        try:
            extracted = self._extract_json(declared_document_type, source_text)
            document_type = self._document_type(extracted.get("documentType"), declared_document_type)
            normalized = self._normalize_extraction(document_type, extracted)
        except Exception as exc:
            raise LlmExtractionError(
                f"OpenAI document extraction failed: {exc.__class__.__name__}",
                document=document,
                source_text=source_text,
                original_error=exc,
            ) from exc
        return document.model_copy(
            update={
                "predictedDocumentType": document_type,
                "classificationConfidence": extracted.get("classificationConfidence") or document.classificationConfidence or 0.9,
                "extracted": normalized,
            }
        )

    def _extract_json(self, document_type: DocumentType, source_text: str) -> dict[str, Any]:
        def call_openai() -> httpx.Response:
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
                                "Classify the document and extract all visible KYvC KYC fields into the schema. "
                                "Use the expected document type when it is consistent with the text. "
                                "For shareholder names, return the person's or company's name only; omit labels such as shareholder, owner, or representative. "
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
            return response

        response = execute_outbound("llm", "openai_responses", call_openai)
        return json.loads(self._response_output_text(response.json()))

    def _response_format(self, document_type: DocumentType) -> dict[str, Any]:
        properties = {
            "documentType": {"type": "string", "enum": [item.value for item in DocumentType]},
            "classificationConfidence": {"type": ["number", "null"]},
            "legalName": {"type": ["string", "null"]},
            "businessRegistrationNumber": {"type": ["string", "null"]},
            "corporateRegistrationNumber": {"type": ["string", "null"]},
            "representativeName": {"type": ["string", "null"]},
            "representativeBirthDate": {"type": ["string", "null"]},
            "representativeNationality": {"type": ["string", "null"]},
            "representativeEnglishName": {"type": ["string", "null"]},
            "businessAddress": {"type": ["string", "null"]},
            "headOfficeAddress": {"type": ["string", "null"]},
            "purpose": {"type": ["string", "null"]},
            "establishmentPurpose": {"type": ["string", "null"]},
            "acceptableForPurposeVerification": {"type": ["boolean", "null"]},
            "purposeVerificationSatisfied": {"type": ["boolean", "null"]},
            "issueDate": {"type": ["string", "null"]},
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
            "delegatorName": {"type": ["string", "null"]},
            "delegateName": {"type": ["string", "null"]},
            "delegateAddress": {"type": ["string", "null"]},
            "delegateContact": {"type": ["string", "null"]},
            "delegateRrn": {"type": ["string", "null"]},
            "targetCorporateName": {"type": ["string", "null"]},
            "authorityText": {"type": ["string", "null"]},
            "canApplyKyc": {"type": ["boolean", "null"]},
            "canSubmitDocuments": {"type": ["boolean", "null"]},
            "canReceiveVc": {"type": ["boolean", "null"]},
            "validFrom": {"type": ["string", "null"]},
            "validUntil": {"type": ["string", "null"]},
            "hasSignatureOrSeal": {"type": ["boolean", "null"]},
            "sealImpressionId": {"type": ["string", "null"]},
            "sealCertificateSubjectName": {"type": ["string", "null"]},
            "sealCertificateCorporateName": {"type": ["string", "null"]},
            "certificateNumber": {"type": ["string", "null"]},
        }
        return {
            "type": "json_schema",
            "name": "kyvc_ai_llm_primary_document_extraction",
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
                "issueDate": self._value(payload.get("issueDate")),
                "sealImpressionId": self._value(payload.get("sealImpressionId")),
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
                "issueDate": self._value(payload.get("issueDate")),
                "sealImpressionId": self._value(payload.get("sealImpressionId")),
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
                        "name": self._name_without_role_label(item.get("name")),
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
                "sealImpressionId": self._value(payload.get("sealImpressionId")),
            }
        if document_type == DocumentType.POWER_OF_ATTORNEY:
            return {
                "delegatorName": self._value(payload.get("delegatorName")),
                "delegateName": self._value(payload.get("delegateName")),
                "delegateAddress": self._value(payload.get("delegateAddress")),
                "delegateContact": self._value(payload.get("delegateContact")),
                "delegateRrn": self._value(payload.get("delegateRrn")),
                "targetCorporateName": self._value(payload.get("targetCorporateName")),
                "authorityText": self._value(payload.get("authorityText")),
                "canApplyKyc": self._value(payload.get("canApplyKyc")),
                "canSubmitDocuments": self._value(payload.get("canSubmitDocuments")),
                "canReceiveVc": self._value(payload.get("canReceiveVc")),
                "validFrom": self._value(payload.get("validFrom")),
                "validUntil": self._value(payload.get("validUntil")),
                "issueDate": self._value(payload.get("issueDate")),
                "hasSignatureOrSeal": self._value(payload.get("hasSignatureOrSeal")),
                "sealImpressionId": self._value(payload.get("sealImpressionId")),
            }
        if document_type == DocumentType.SEAL_CERTIFICATE:
            return {
                "subjectName": self._value(payload.get("sealCertificateSubjectName") or payload.get("representativeName")),
                "corporateName": self._value(payload.get("sealCertificateCorporateName") or payload.get("legalName")),
                "certificateNumber": self._value(payload.get("certificateNumber")),
                "sealImpressionId": self._value(payload.get("sealImpressionId")),
                "issueDate": self._value(payload.get("issueDate")),
            }
        purpose = payload.get("establishmentPurpose")
        return {
            "legalName": self._value(payload.get("legalName")),
            "representative": {
                "name": self._value(payload.get("representativeName")),
                "birthDate": self._value(payload.get("representativeBirthDate")),
                "nationality": self._value(payload.get("representativeNationality")),
                "englishName": self._value(payload.get("representativeEnglishName")),
            },
            "beneficialOwners": [
                {
                    "name": self._name_without_role_label(item.get("name")),
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
            "purposeVerification": {
                "establishmentPurpose": self._value(purpose),
                "acceptableForPurposeVerification": bool(payload.get("acceptableForPurposeVerification")),
                "purposeVerificationSatisfied": bool(payload.get("purposeVerificationSatisfied")),
            },
            "issueDate": self._value(payload.get("issueDate")),
            "sealImpressionId": self._value(payload.get("sealImpressionId")),
        }

    def _document_type(self, value: Any, fallback: DocumentType) -> DocumentType:
        try:
            parsed = DocumentType(str(value))
        except (TypeError, ValueError):
            return fallback
        return parsed if parsed != DocumentType.UNKNOWN else fallback

    def _document_text(self, document: DocumentMetadata) -> str:
        if self.ocr_provider is not None and document.storagePath:
            text = self.ocr_provider.extract_text(document)
            if text.strip():
                return text
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

    def _name_without_role_label(self, value: Any) -> Any:
        if not isinstance(value, str):
            return value
        text = value.strip()
        for prefix in ("shareholder ", "representative "):
            if text.lower().startswith(prefix):
                return text[len(prefix) :].strip() or text
        return text

    def _response_output_text(self, payload: dict[str, Any]) -> str:
        for item in payload.get("output", []):
            for content in item.get("content", []):
                if content.get("type") == "output_text" and content.get("text"):
                    return content["text"]
        text = payload.get("output_text")
        if text:
            return text
        raise RuntimeError("OpenAI response did not include output text.")
