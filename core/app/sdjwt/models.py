from typing import Any, Literal

from pydantic import BaseModel, Field

from app.policy.document_rules import DOCUMENT_TYPES


class ConfirmationKey(BaseModel):
    kid: str


class XrplCredentialStatus(BaseModel):
    type: Literal["XRPLCredentialStatus"] = "XRPLCredentialStatus"
    statusId: str
    credentialType: str


class KycMetadata(BaseModel):
    jurisdiction: str
    assuranceLevel: str
    verifiedAt: str | None = None


class LegalEntity(BaseModel):
    type: str
    name: str | None = None
    registrationNumber: str | None = None
    nonProfit: bool | None = None
    purposeCheckRequired: bool | None = None


class Representative(BaseModel):
    name: str | None = None
    birthDate: str | None = None
    nationality: str | None = None


class BeneficialOwner(BaseModel):
    name: str | None = None
    birthDate: str | None = None
    nationality: str | None = None
    englishName: str | None = None
    ownershipPercentage: float | None = None


class Delegate(BaseModel):
    name: str | None = None
    address: str | None = None
    contact: str | None = None
    identityDigest: str | None = None
    identityDigestAlgorithm: str | None = None
    identityDigestVersion: str | None = None


class Delegation(BaseModel):
    kycApplication: bool | None = None
    documentSubmission: bool | None = None
    vcReceipt: bool | None = None
    validFrom: str | None = None
    validUntil: str | None = None
    targetCorporateName: str | None = None


class EstablishmentPurpose(BaseModel):
    checked: bool | None = None
    purposeText: str | None = None


class DocumentEvidence(BaseModel):
    documentId: str
    documentType: str = Field(description=f"One of: {', '.join(sorted(DOCUMENT_TYPES))}")
    documentClass: str | None = None
    digestSRI: str
    mediaType: str | None = None
    byteSize: int | None = None
    hashInput: str | None = "original-file-bytes"
    verifiedAt: str | None = None
    evidenceFor: list[str] = Field(default_factory=list)


class LegalEntityKycCredential(BaseModel):
    iss: str
    sub: str
    vct: str
    jti: str
    iat: int
    exp: int
    cnf: ConfirmationKey
    credentialStatus: XrplCredentialStatus
    kyc: KycMetadata
    legalEntity: LegalEntity | None = None
    representative: Representative | None = None
    beneficialOwners: list[BeneficialOwner] = Field(default_factory=list)
    delegate: Delegate | None = None
    delegation: Delegation | None = None
    establishmentPurpose: EstablishmentPurpose | None = None
    documentEvidence: list[DocumentEvidence] = Field(default_factory=list)
    extra: dict[str, Any] = Field(default_factory=dict)
