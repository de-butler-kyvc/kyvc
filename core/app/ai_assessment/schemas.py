from datetime import datetime
from pathlib import Path
from typing import Any

from pydantic import BaseModel, Field

from app.ai_assessment.enums import (
    ApplicantRole,
    AssessmentStatus,
    BeneficialOwnershipMethod,
    BeneficialOwnershipStatus,
    CheckStatus,
    DelegationResult,
    DocumentType,
    HolderType,
    LegalEntityType,
    Severity,
)


class ExtractedValue(BaseModel):
    raw: Any = None
    normalized: Any = None
    confidence: float = 0.0
    evidenceRefs: list[str] = Field(default_factory=list)


class DeclaredPerson(BaseModel):
    name: str | None = None
    birthDate: str | None = None
    nationality: str | None = None
    englishName: str | None = None


class DeclaredBeneficialOwner(DeclaredPerson):
    ownershipPercent: float | None = None


class KycApplication(BaseModel):
    kycApplicationId: str
    legalEntityType: LegalEntityType
    applicantRole: ApplicantRole = ApplicantRole.REPRESENTATIVE
    applicantName: str | None = None
    isNonprofit: bool = False
    businessRegistrationNumber: str | None = None
    corporateRegistrationNumber: str | None = None
    declaredRepresentative: DeclaredPerson | None = None
    declaredBeneficialOwners: list[DeclaredBeneficialOwner] = Field(default_factory=list)
    createdAt: datetime = Field(default_factory=datetime.utcnow)


class PersonExtraction(BaseModel):
    name: ExtractedValue = Field(default_factory=ExtractedValue)
    birthDate: ExtractedValue = Field(default_factory=ExtractedValue)
    nationality: ExtractedValue = Field(default_factory=ExtractedValue)
    englishName: ExtractedValue = Field(default_factory=ExtractedValue)


class PurposeVerificationExtraction(BaseModel):
    establishmentPurpose: ExtractedValue = Field(default_factory=ExtractedValue)
    acceptableForPurposeVerification: bool = False
    purposeVerificationSatisfied: bool = False
    evidenceRefs: list[str] = Field(default_factory=list)


class Shareholder(BaseModel):
    name: str | None = None
    holderType: HolderType = HolderType.UNKNOWN
    birthDate: str | None = None
    nationality: str | None = None
    englishName: str | None = None
    shares: int | None = None
    ownershipPercent: float | None = None
    evidenceRefs: list[str] = Field(default_factory=list)


class BusinessRegistrationExtraction(BaseModel):
    legalName: ExtractedValue = Field(default_factory=ExtractedValue)
    businessRegistrationNumber: ExtractedValue = Field(default_factory=ExtractedValue)
    representativeName: ExtractedValue = Field(default_factory=ExtractedValue)
    representative: PersonExtraction = Field(default_factory=PersonExtraction)
    businessAddress: ExtractedValue = Field(default_factory=ExtractedValue)
    businessType: ExtractedValue = Field(default_factory=ExtractedValue)
    businessItem: ExtractedValue = Field(default_factory=ExtractedValue)
    openingDate: ExtractedValue = Field(default_factory=ExtractedValue)
    issueDate: ExtractedValue = Field(default_factory=ExtractedValue)
    sealImpressionId: ExtractedValue = Field(default_factory=ExtractedValue)


class CorporateRegistryExtraction(BaseModel):
    legalName: ExtractedValue = Field(default_factory=ExtractedValue)
    corporateRegistrationNumber: ExtractedValue = Field(default_factory=ExtractedValue)
    headOfficeAddress: ExtractedValue = Field(default_factory=ExtractedValue)
    representativeName: ExtractedValue = Field(default_factory=ExtractedValue)
    representative: PersonExtraction = Field(default_factory=PersonExtraction)
    directors: ExtractedValue = Field(default_factory=ExtractedValue)
    purpose: ExtractedValue = Field(default_factory=ExtractedValue)
    issueDate: ExtractedValue = Field(default_factory=ExtractedValue)
    sealImpressionId: ExtractedValue = Field(default_factory=ExtractedValue)


class ShareholderRegistryExtraction(BaseModel):
    legalName: ExtractedValue = Field(default_factory=ExtractedValue)
    baseDate: ExtractedValue = Field(default_factory=ExtractedValue)
    totalShares: ExtractedValue = Field(default_factory=ExtractedValue)
    shareholders: list[Shareholder] = Field(default_factory=list)
    sealImpressionId: ExtractedValue = Field(default_factory=ExtractedValue)


class OrganizationDocumentExtraction(BaseModel):
    legalName: ExtractedValue = Field(default_factory=ExtractedValue)
    representative: PersonExtraction = Field(default_factory=PersonExtraction)
    beneficialOwners: list[Shareholder] = Field(default_factory=list)
    purposeVerification: PurposeVerificationExtraction = Field(default_factory=PurposeVerificationExtraction)
    issueDate: ExtractedValue = Field(default_factory=ExtractedValue)
    documentSummary: ExtractedValue = Field(default_factory=ExtractedValue)
    sealImpressionId: ExtractedValue = Field(default_factory=ExtractedValue)


class PowerOfAttorneyExtraction(BaseModel):
    delegatorName: ExtractedValue = Field(default_factory=ExtractedValue)
    delegateName: ExtractedValue = Field(default_factory=ExtractedValue)
    delegateAddress: ExtractedValue = Field(default_factory=ExtractedValue)
    delegateContact: ExtractedValue = Field(default_factory=ExtractedValue)
    delegateRrn: ExtractedValue = Field(default_factory=ExtractedValue, exclude=True)
    targetCorporateName: ExtractedValue = Field(default_factory=ExtractedValue)
    authorityText: ExtractedValue = Field(default_factory=ExtractedValue)
    canApplyKyc: ExtractedValue = Field(default_factory=ExtractedValue)
    canSubmitDocuments: ExtractedValue = Field(default_factory=ExtractedValue)
    canReceiveVc: ExtractedValue = Field(default_factory=ExtractedValue)
    validFrom: ExtractedValue = Field(default_factory=ExtractedValue)
    validUntil: ExtractedValue = Field(default_factory=ExtractedValue)
    issueDate: ExtractedValue = Field(default_factory=ExtractedValue)
    hasSignatureOrSeal: ExtractedValue = Field(default_factory=ExtractedValue)
    sealImpressionId: ExtractedValue = Field(default_factory=ExtractedValue)


class SealCertificateExtraction(BaseModel):
    subjectName: ExtractedValue = Field(default_factory=ExtractedValue)
    corporateName: ExtractedValue = Field(default_factory=ExtractedValue)
    certificateNumber: ExtractedValue = Field(default_factory=ExtractedValue)
    sealImpressionId: ExtractedValue = Field(default_factory=ExtractedValue)
    issueDate: ExtractedValue = Field(default_factory=ExtractedValue)


ExtractionModel = (
    BusinessRegistrationExtraction
    | CorporateRegistryExtraction
    | ShareholderRegistryExtraction
    | OrganizationDocumentExtraction
    | PowerOfAttorneyExtraction
    | SealCertificateExtraction
)


class DocumentMetadata(BaseModel):
    documentId: str
    kycApplicationId: str
    originalFileName: str = "document.pdf"
    mimeType: str | None = "application/pdf"
    sizeBytes: int = 0
    sha256: str | None = None
    declaredDocumentType: DocumentType | None = None
    predictedDocumentType: DocumentType | None = None
    classificationConfidence: float | None = None
    storagePath: str | None = None
    extracted: dict[str, Any] | ExtractionModel | None = None
    createdAt: datetime = Field(default_factory=datetime.utcnow)


class DocumentResult(BaseModel):
    documentId: str
    declaredDocumentType: DocumentType | None = None
    predictedDocumentType: DocumentType
    classificationConfidence: float
    classificationReason: str
    evidenceRefs: list[str] = Field(default_factory=list)
    extracted: dict[str, Any] = Field(default_factory=dict)


class CrossDocumentCheck(BaseModel):
    checkCode: str
    status: CheckStatus
    severity: Severity
    message: str
    values: list[dict[str, Any]] = Field(default_factory=list)
    confidence: float = 1.0
    evidenceRefs: list[str] = Field(default_factory=list)


class EngineIssue(BaseModel):
    code: str
    message: str
    evidenceRefs: list[str] = Field(default_factory=list)


class BeneficialOwner(BaseModel):
    name: str
    holderType: HolderType
    birthDate: str | None = None
    nationality: str | None = None
    englishName: str | None = None
    ownershipPercent: float | None = None
    basis: str
    evidenceRefs: list[str] = Field(default_factory=list)
    confidence: float = 1.0


class BeneficialOwnershipResult(BaseModel):
    status: BeneficialOwnershipStatus
    method: BeneficialOwnershipMethod
    thresholdPercent: float = 25.0
    owners: list[BeneficialOwner] = Field(default_factory=list)
    issues: list[EngineIssue] = Field(default_factory=list)


class DelegationAuthority(BaseModel):
    kycApplication: bool | None = None
    documentSubmission: bool | None = None
    vcReceipt: bool | None = None


class DelegationResultModel(BaseModel):
    result: DelegationResult
    manualReviewRequired: bool = False
    reasons: list[EngineIssue] = Field(default_factory=list)
    authority: DelegationAuthority = Field(default_factory=DelegationAuthority)
    evidenceRefs: list[str] = Field(default_factory=list)


class ProviderUsageLog(BaseModel):
    providerCategory: str
    providerName: str
    operation: str
    model: str | None = None
    latencyMs: float | None = None
    inputTokens: int | None = None
    outputTokens: int | None = None
    totalTokens: int | None = None
    pages: int | None = None
    estimatedCost: float | None = None
    error: str | None = None
    createdAt: datetime = Field(default_factory=datetime.utcnow)


class ModelMetadata(BaseModel):
    promptVersion: str = "core-ai-assessment-v1"
    schemaVersion: str = "ai-assessment-v1"
    providerArchitecture: str = "pluggable-provider-v1"


class KycAssessment(BaseModel):
    assessmentId: str
    kycApplicationId: str
    legalEntityType: LegalEntityType
    applicantRole: ApplicantRole
    status: AssessmentStatus
    overallConfidence: float
    summary: str
    documentResults: list[DocumentResult]
    extractedFields: dict[str, Any]
    crossDocumentChecks: list[CrossDocumentCheck]
    beneficialOwnership: BeneficialOwnershipResult
    delegation: DelegationResultModel | None = None
    supplementRequests: list[EngineIssue]
    manualReviewReasons: list[EngineIssue]
    evidence: list[Any] = Field(default_factory=list)
    providerUsageLogs: list[ProviderUsageLog] = Field(default_factory=list)
    modelMetadata: ModelMetadata = Field(default_factory=ModelMetadata)
    createdAt: datetime = Field(default_factory=datetime.utcnow)


JsonDict = dict[str, Any]
PathLike = str | Path
