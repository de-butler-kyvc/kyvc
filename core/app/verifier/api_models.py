from typing import Any, Literal

from pydantic import BaseModel, Field


class PolicyModel(BaseModel):
    trustedIssuers: list[str] | None = None
    acceptedKycLevels: list[str] | None = None
    acceptedJurisdictions: list[str] | None = None
    minimumAssuranceLevel: str | None = None
    requiredClaims: list[str] = Field(default_factory=list)


class VerifyCredentialRequest(BaseModel):
    credential: dict[str, Any]
    did_documents: dict[str, dict[str, Any]] = Field(default_factory=dict)
    policy: PolicyModel | None = None
    require_status: bool = True
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False
    status_mode: Literal["xrpl", "local"] = "xrpl"


class VerifyPresentationRequest(BaseModel):
    presentation: dict[str, Any]
    did_documents: dict[str, dict[str, Any]] = Field(default_factory=dict)
    policy: PolicyModel | None = None
    require_status: bool = True
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False
    status_mode: Literal["xrpl", "local"] = "xrpl"


class IssuePresentationChallengeRequest(BaseModel):
    domain: str = Field(min_length=1)


class IssuePresentationChallengeResponse(BaseModel):
    challenge: str
    domain: str
    expires_at: str


class VerificationResponse(BaseModel):
    ok: bool
    errors: list[str]
    details: dict[str, Any]
