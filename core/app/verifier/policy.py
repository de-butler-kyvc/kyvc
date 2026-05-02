from dataclasses import dataclass, field
from typing import Any


ASSURANCE_ORDER = {
    "LOW": 1,
    "BASIC": 1,
    "MEDIUM": 2,
    "STANDARD": 2,
    "HIGH": 3,
    "ADVANCED": 3,
}


@dataclass(frozen=True)
class VerificationPolicy:
    trusted_issuers: set[str] | None = None
    accepted_kyc_levels: set[str] | None = None
    accepted_jurisdictions: set[str] | None = None
    minimum_assurance_level: str | None = None
    required_claims: set[str] = field(default_factory=set)

    @classmethod
    def from_dict(cls, data: dict[str, Any] | None) -> "VerificationPolicy":
        data = data or {}
        return cls(
            trusted_issuers=set(data["trustedIssuers"]) if data.get("trustedIssuers") else None,
            accepted_kyc_levels=set(data["acceptedKycLevels"]) if data.get("acceptedKycLevels") else None,
            accepted_jurisdictions=set(data["acceptedJurisdictions"]) if data.get("acceptedJurisdictions") else None,
            minimum_assurance_level=data.get("minimumAssuranceLevel"),
            required_claims=set(data.get("requiredClaims") or []),
        )

    def validate_vc(self, vc: dict[str, Any]) -> list[str]:
        errors: list[str] = []
        issuer = str(vc.get("issuer", ""))
        subject = vc.get("credentialSubject") or {}
        if self.trusted_issuers is not None and issuer not in self.trusted_issuers:
            errors.append("issuer is not trusted by verifier policy")

        kyc_level = subject.get("kycLevel")
        if self.accepted_kyc_levels is not None and kyc_level not in self.accepted_kyc_levels:
            errors.append("kycLevel is not accepted by verifier policy")

        jurisdiction = subject.get("jurisdiction")
        if self.accepted_jurisdictions is not None and jurisdiction not in self.accepted_jurisdictions:
            errors.append("jurisdiction is not accepted by verifier policy")

        missing_claims = [claim for claim in self.required_claims if claim not in subject]
        if missing_claims:
            errors.append(f"required claims missing: {', '.join(sorted(missing_claims))}")

        if self.minimum_assurance_level is not None:
            actual = str(subject.get("assuranceLevel") or subject.get("kycLevel") or "").upper()
            minimum = self.minimum_assurance_level.upper()
            if ASSURANCE_ORDER.get(actual, 0) < ASSURANCE_ORDER.get(minimum, 0):
                errors.append("assurance level is lower than verifier policy minimum")

        return errors

