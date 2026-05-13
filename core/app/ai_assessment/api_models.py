from typing import Any

from pydantic import BaseModel, Field

from app.ai_assessment.enums import ApplicantRole, DocumentType, LegalEntityType
from app.ai_assessment.schemas import DeclaredBeneficialOwner, DeclaredPerson, DocumentMetadata, KycAssessment


class LlmPrimaryDocumentInput(BaseModel):
    documentId: str | None = None
    originalFileName: str = "document.pdf"
    mimeType: str | None = None
    declaredDocumentType: DocumentType | None = None
    storagePath: str | None = None
    contentBase64: str | None = None
    textContent: str | None = None
    sizeBytes: int | None = None
    sha256: str | None = None
    extracted: dict[str, Any] | None = None


class LlmPrimaryAssessmentRequest(BaseModel):
    kycApplicationId: str
    legalEntityType: LegalEntityType
    applicantRole: ApplicantRole = ApplicantRole.REPRESENTATIVE
    applicantName: str | None = None
    isNonprofit: bool = False
    businessRegistrationNumber: str | None = None
    corporateRegistrationNumber: str | None = None
    declaredRepresentative: DeclaredPerson | None = None
    declaredBeneficialOwners: list[DeclaredBeneficialOwner] = Field(default_factory=list)
    documents: list[LlmPrimaryDocumentInput] = Field(default_factory=list)


class LlmPrimaryAssessmentResponse(BaseModel):
    strategy: str = "llm_primary"
    extractionProvider: str
    assessment: KycAssessment
    documents: list[DocumentMetadata]
