from typing import Any

from app.ai_assessment.enums import DocumentType
from app.ai_assessment.schemas import (
    BusinessRegistrationExtraction,
    CorporateRegistryExtraction,
    DocumentMetadata,
    ExtractionModel,
    OrganizationDocumentExtraction,
    PowerOfAttorneyExtraction,
    SealCertificateExtraction,
    ShareholderRegistryExtraction,
)


class StructuredPayloadExtractor:
    """Normalizes provider output into assessment extraction schemas."""

    MODEL_BY_TYPE = {
        DocumentType.BUSINESS_REGISTRATION: BusinessRegistrationExtraction,
        DocumentType.CORPORATE_REGISTRY: CorporateRegistryExtraction,
        DocumentType.SHAREHOLDER_REGISTRY: ShareholderRegistryExtraction,
        DocumentType.STOCK_CHANGE_STATEMENT: ShareholderRegistryExtraction,
        DocumentType.INVESTOR_REGISTRY: ShareholderRegistryExtraction,
        DocumentType.MEMBER_REGISTRY: ShareholderRegistryExtraction,
        DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT: ShareholderRegistryExtraction,
        DocumentType.POWER_OF_ATTORNEY: PowerOfAttorneyExtraction,
        DocumentType.SEAL_CERTIFICATE: SealCertificateExtraction,
        DocumentType.ARTICLES_OF_ASSOCIATION: OrganizationDocumentExtraction,
        DocumentType.OPERATING_RULES: OrganizationDocumentExtraction,
        DocumentType.REGULATIONS: OrganizationDocumentExtraction,
        DocumentType.MEETING_MINUTES: OrganizationDocumentExtraction,
        DocumentType.OFFICIAL_LETTER: OrganizationDocumentExtraction,
        DocumentType.PURPOSE_PROOF_DOCUMENT: OrganizationDocumentExtraction,
        DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE: OrganizationDocumentExtraction,
        DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE: OrganizationDocumentExtraction,
        DocumentType.BOARD_REGISTRY: OrganizationDocumentExtraction,
    }

    def extract(self, document: DocumentMetadata) -> ExtractionModel | None:
        document_type = document.predictedDocumentType or document.declaredDocumentType or DocumentType.UNKNOWN
        model = self.MODEL_BY_TYPE.get(document_type)
        if model is None or document.extracted is None:
            return None
        if isinstance(document.extracted, model):
            return document.extracted
        if hasattr(document.extracted, "model_dump"):
            payload: Any = document.extracted.model_dump()
        else:
            payload = document.extracted
        return model.model_validate(payload)
