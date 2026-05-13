import base64
import hashlib
import mimetypes
from pathlib import Path
from typing import Any

from app.ai_assessment.enums import DocumentType, LegalEntityType
from app.ai_assessment.schemas import DocumentMetadata, KycAssessment


CORE_DOCUMENT_TYPE_MAP: dict[DocumentType, str] = {
    DocumentType.BUSINESS_REGISTRATION: "KR_BUSINESS_REGISTRATION_CERTIFICATE",
    DocumentType.CORPORATE_REGISTRY: "KR_CORPORATE_REGISTER_FULL_CERTIFICATE",
    DocumentType.SHAREHOLDER_REGISTRY: "KR_SHAREHOLDER_REGISTER",
    DocumentType.STOCK_CHANGE_STATEMENT: "KR_STOCK_CHANGE_STATEMENT",
    DocumentType.INVESTOR_REGISTRY: "KR_CONTRIBUTOR_REGISTER",
    DocumentType.MEMBER_REGISTRY: "KR_MEMBER_REGISTER",
    DocumentType.BOARD_REGISTRY: "KR_BOARD_MEMBER_LIST",
    DocumentType.ARTICLES_OF_ASSOCIATION: "KR_ARTICLES_OF_ASSOCIATION",
    DocumentType.OPERATING_RULES: "KR_OPERATING_RULES",
    DocumentType.REGULATIONS: "KR_BYLAWS",
    DocumentType.MEETING_MINUTES: "KR_MEETING_MINUTES",
    DocumentType.OFFICIAL_LETTER: "KR_OFFICIAL_LETTER",
    DocumentType.PURPOSE_PROOF_DOCUMENT: "KR_ESTABLISHMENT_PERMIT",
    DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE: "KR_UNIQUE_NUMBER_CERTIFICATE",
    DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE: "FOREIGN_INVESTMENT_REGISTRATION_CERTIFICATE",
    DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT: "FOREIGN_BENEFICIAL_OWNER_DOCUMENT",
    DocumentType.POWER_OF_ATTORNEY: "KR_POWER_OF_ATTORNEY",
    DocumentType.SEAL_CERTIFICATE: "KR_SEAL_CERTIFICATE",
    DocumentType.UNKNOWN: "UNKNOWN",
}


CORE_LEGAL_ENTITY_TYPE_MAP: dict[LegalEntityType, str] = {
    LegalEntityType.STOCK_COMPANY: "STOCK_COMPANY",
    LegalEntityType.LIMITED_COMPANY: "LIMITED_COMPANY",
    LegalEntityType.LIMITED_PARTNERSHIP: "LIMITED_PARTNERSHIP",
    LegalEntityType.GENERAL_PARTNERSHIP: "GENERAL_PARTNERSHIP",
    LegalEntityType.INCORPORATED_ASSOCIATION: "INCORPORATED_ASSOCIATION",
    LegalEntityType.FOUNDATION: "FOUNDATION",
    LegalEntityType.COOPERATIVE: "COOPERATIVE",
    LegalEntityType.UNIQUE_NUMBER_ORGANIZATION: "UNIQUE_NUMBER_ORGANIZATION",
    LegalEntityType.FOREIGN_COMPANY: "FOREIGN_COMPANY",
}


def build_core_kyc_claims(
    assessment: KycAssessment,
    documents: list[DocumentMetadata] | None = None,
    *,
    assurance_level: str,
    jurisdiction: str = "KR",
    include_assessment_status: bool = False,
) -> dict[str, Any]:
    """Build claims for core's legal-entity SD-JWT issuer.

    This adapter intentionally returns claims only. Callers decide whether a
    given assessment is approved for issuance before calling core issuer APIs.
    """

    corporate_profile = assessment.extractedFields.get("corporateProfile") or {}
    purpose_payload = assessment.extractedFields.get("purposeVerification") or {}
    representative = corporate_profile.get("representative") or {}
    delegate = assessment.extractedFields.get("delegate") or {}
    delegation = assessment.extractedFields.get("delegation") or {}
    claims: dict[str, Any] = {
        "kyc": {
            "jurisdiction": jurisdiction,
            "assuranceLevel": assurance_level,
        },
        "legalEntity": {
            "type": CORE_LEGAL_ENTITY_TYPE_MAP.get(assessment.legalEntityType, assessment.legalEntityType.value),
            "name": _field_value(corporate_profile.get("legalName")),
            "registrationNumber": _field_value(corporate_profile.get("corporateRegistrationNumber"))
            or _field_value(corporate_profile.get("businessRegistrationNumber")),
            "nonProfit": corporate_profile.get("nonProfit"),
            "purposeCheckRequired": bool(purpose_payload.get("documents")),
        },
        "representative": _compact(
            {
                "name": _field_value(representative.get("name")),
                "birthDate": _field_value(representative.get("birthDate")),
                "nationality": _field_value(representative.get("nationality")),
                "englishName": _field_value(representative.get("englishName")),
            }
        ),
        "beneficialOwners": [
            _compact(
                {
                    "name": owner.name,
                    "birthDate": owner.birthDate,
                    "nationality": owner.nationality,
                    "englishName": owner.englishName,
                    "ownershipPercentage": owner.ownershipPercent,
                }
            )
            for owner in assessment.beneficialOwnership.owners
        ],
        "delegate": _compact(
            {
                "name": delegate.get("name"),
                "address": delegate.get("address"),
                "contact": delegate.get("contact"),
                "identityDigest": delegate.get("identityDigest"),
                "identityDigestAlgorithm": delegate.get("identityDigestAlgorithm"),
                "identityDigestVersion": delegate.get("identityDigestVersion"),
            }
        ),
        "delegation": _compact(
            {
                "kycApplication": delegation.get("kycApplication"),
                "documentSubmission": delegation.get("documentSubmission"),
                "vcReceipt": delegation.get("vcReceipt"),
                "validFrom": delegation.get("validFrom"),
                "validUntil": delegation.get("validUntil"),
                "targetCorporateName": delegation.get("targetCorporateName"),
            }
        ),
        "establishmentPurpose": _establishment_purpose(purpose_payload),
        "extra": {"aiAssessmentRef": {"assessmentId": assessment.assessmentId, "applicationId": assessment.kycApplicationId}},
    }
    if include_assessment_status:
        claims["extra"]["aiAssessmentRef"]["status"] = assessment.status.value
    if documents:
        claims["documentEvidence"] = [_document_evidence(document) for document in documents]
    return _compact(claims)


def _document_evidence(document: DocumentMetadata) -> dict[str, Any]:
    data = _document_bytes(document)
    document_type = document.predictedDocumentType or document.declaredDocumentType or DocumentType.UNKNOWN
    return _compact(
        {
            "documentId": f"urn:kyvc:doc:{document.documentId}",
            "documentType": CORE_DOCUMENT_TYPE_MAP.get(document_type, document_type.value),
            "documentClass": document_type.value,
            "digestSRI": _digest_sri(data),
            "mediaType": document.mimeType or mimetypes.guess_type(document.originalFileName)[0],
            "byteSize": document.sizeBytes or len(data),
            "hashInput": "original-file-bytes",
            "evidenceFor": _evidence_for(document_type),
        }
    )


def _document_bytes(document: DocumentMetadata) -> bytes:
    if document.storagePath and Path(document.storagePath).exists():
        return Path(document.storagePath).read_bytes()
    seed = document.sha256 or document.documentId
    return str(seed).encode("utf-8")


def _establishment_purpose(payload: dict[str, Any]) -> dict[str, Any]:
    documents = payload.get("documents") if isinstance(payload, dict) else []
    purpose_text = None
    if isinstance(documents, list):
        for document in documents:
            if not isinstance(document, dict):
                continue
            purpose_text = _field_value(document.get("establishmentPurpose"))
            if purpose_text:
                break
    return _compact({"checked": bool(payload.get("satisfied")), "purposeText": purpose_text})


def _field_value(value: Any) -> Any:
    if isinstance(value, dict):
        normalized = value.get("normalized")
        return normalized if _present(normalized) else value.get("raw")
    return value


def _evidence_for(document_type: DocumentType) -> list[str]:
    if document_type == DocumentType.BUSINESS_REGISTRATION:
        return ["legalEntity.name", "legalEntity.registrationNumber"]
    if document_type == DocumentType.CORPORATE_REGISTRY:
        return ["legalEntity.registrationNumber", "representative"]
    if document_type in {
        DocumentType.SHAREHOLDER_REGISTRY,
        DocumentType.STOCK_CHANGE_STATEMENT,
        DocumentType.INVESTOR_REGISTRY,
        DocumentType.MEMBER_REGISTRY,
        DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT,
    }:
        return ["beneficialOwners"]
    if document_type in {
        DocumentType.ARTICLES_OF_ASSOCIATION,
        DocumentType.OPERATING_RULES,
        DocumentType.REGULATIONS,
        DocumentType.PURPOSE_PROOF_DOCUMENT,
        DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE,
    }:
        return ["establishmentPurpose"]
    if document_type == DocumentType.POWER_OF_ATTORNEY:
        return ["delegate", "delegation"]
    if document_type == DocumentType.SEAL_CERTIFICATE:
        return ["extra.aiAssessmentRef.sealConsistency"]
    return []


def _digest_sri(data: bytes) -> str:
    return "sha384-" + base64.b64encode(hashlib.sha384(data).digest()).decode("ascii")


def _compact(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: _compact(item) for key, item in value.items() if _present(item)}
    if isinstance(value, list):
        return [_compact(item) for item in value if _present(item)]
    return value


def _present(value: Any) -> bool:
    return value is not None and value != "" and value != [] and value != {}
