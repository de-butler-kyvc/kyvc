from typing import Any, Literal

from pydantic import BaseModel, Field


class PolicyModel(BaseModel):
    id: str | None = None
    acceptedFormat: str | None = None
    acceptedVct: list[str] | None = None
    trustedIssuers: list[str] | None = None
    acceptedKycLevels: list[str] | None = None
    acceptedJurisdictions: list[str] | None = None
    minimumAssuranceLevel: str | None = None
    requiredClaims: list[str] = Field(default_factory=list)
    requiredDisclosures: list[str] = Field(default_factory=list)
    documentRules: list[dict[str, Any]] = Field(default_factory=list)


class VerifyCredentialRequest(BaseModel):
    format: Literal["dc+sd-jwt", "vc+jwt"] | None = None
    credential: dict[str, Any] | str
    did_documents: dict[str, dict[str, Any]] = Field(default_factory=dict)
    policy: PolicyModel | None = None
    require_status: bool = True
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False
    status_mode: Literal["xrpl", "local"] = "xrpl"


class VerifyPresentationRequest(BaseModel):
    presentation: dict[str, Any] | str
    format: Literal["kyvc-sd-jwt-presentation-v1", "vp+jwt"] | None = None
    did_documents: dict[str, dict[str, Any]] = Field(default_factory=dict)
    policy: PolicyModel | None = None
    require_status: bool = True
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False
    status_mode: Literal["xrpl", "local"] = "xrpl"


class IssuePresentationChallengeRequest(BaseModel):
    domain: str | None = Field(default=None)
    aud: str | None = Field(default=None)
    definitionId: str = Field(default="kr-stock-company-kyc-v1")
    format: Literal["dc+sd-jwt", "vp+jwt"] = "dc+sd-jwt"
    presentationDefinition: dict[str, Any] | None = None


class IssuePresentationChallengeResponse(BaseModel):
    challenge: str | None = None
    domain: str | None = None
    expires_at: str | None = None
    nonce: str | None = None
    aud: str | None = None
    expiresAt: str | None = None
    presentationDefinition: dict[str, Any] | None = None


class VerificationResponse(BaseModel):
    ok: bool
    errors: list[str]
    details: dict[str, Any]
