from dataclasses import dataclass, field
from typing import Any

from app.policy.document_rules import DocumentRule, condition_matches, document_rules_for_legal_entity_type
from app.verifier.policy import ASSURANCE_ORDER


@dataclass(frozen=True)
class SdJwtVerificationPolicy:
    id: str | None = None
    accepted_format: str = "dc+sd-jwt"
    accepted_vct: set[str] | None = None
    trusted_issuers: set[str] | None = None
    accepted_jurisdictions: set[str] | None = None
    minimum_assurance_level: str | None = None
    required_disclosures: set[str] = field(default_factory=set)
    document_rules: list[DocumentRule] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict[str, Any] | None) -> "SdJwtVerificationPolicy":
        data = data or {}
        return cls(
            id=data.get("id"),
            accepted_format=str(data.get("acceptedFormat") or "dc+sd-jwt"),
            accepted_vct=set(data["acceptedVct"]) if data.get("acceptedVct") else None,
            trusted_issuers=set(data["trustedIssuers"]) if data.get("trustedIssuers") else None,
            accepted_jurisdictions=set(data["acceptedJurisdictions"]) if data.get("acceptedJurisdictions") else None,
            minimum_assurance_level=data.get("minimumAssuranceLevel"),
            required_disclosures=set(data.get("requiredDisclosures") or []),
            document_rules=[DocumentRule.from_dict(item) for item in data.get("documentRules") or []],
        )

    def validate(self, disclosed_payload: dict[str, Any], disclosed_paths: set[str] | None = None) -> dict[str, Any]:
        errors: list[str] = []
        satisfied: list[str] = []
        missing: list[str] = []
        disclosed_paths = disclosed_paths or set()

        issuer = str(disclosed_payload.get("iss", ""))
        if self.trusted_issuers is not None and issuer not in self.trusted_issuers:
            errors.append("issuer is not trusted by verifier policy")

        vct = str(disclosed_payload.get("vct", ""))
        if self.accepted_vct is not None and vct not in self.accepted_vct:
            errors.append("vct is not accepted by verifier policy")

        kyc = disclosed_payload.get("kyc") if isinstance(disclosed_payload.get("kyc"), dict) else {}
        jurisdiction = kyc.get("jurisdiction")
        if self.accepted_jurisdictions is not None and jurisdiction not in self.accepted_jurisdictions:
            errors.append("jurisdiction is not accepted by verifier policy")

        if self.minimum_assurance_level is not None:
            actual = str(kyc.get("assuranceLevel") or "").upper()
            minimum = self.minimum_assurance_level.upper()
            if ASSURANCE_ORDER.get(actual, 0) < ASSURANCE_ORDER.get(minimum, 0):
                errors.append("assurance level is lower than verifier policy minimum")

        for path in sorted(self.required_disclosures):
            if _path_present(disclosed_payload, path):
                satisfied.append(path)
            else:
                missing.append(path)
                errors.append(f"required disclosure missing: {path}")

        rules = list(self.document_rules)
        if not rules:
            legal_entity = disclosed_payload.get("legalEntity") if isinstance(disclosed_payload.get("legalEntity"), dict) else {}
            rules = document_rules_for_legal_entity_type(legal_entity.get("type"))
        doc_details = validate_document_rules(disclosed_payload, rules)
        errors.extend(doc_details["errors"])
        satisfied.extend(doc_details["satisfiedRequirements"])
        missing.extend(doc_details["missingRequirements"])
        return {
            "errors": errors,
            "satisfiedRequirements": satisfied,
            "missingRequirements": missing,
            "submittedDocumentTypes": doc_details["submittedDocumentTypes"],
        }


def _path_present(value: Any, path: str) -> bool:
    if "[]" in path:
        prefix, _, suffix = path.partition("[].")
        items = value.get(prefix) if isinstance(value, dict) else None
        if not isinstance(items, list) or not items:
            return False
        return any(_path_present(item, suffix) for item in items)
    current = value
    for part in path.split("."):
        if not isinstance(current, dict) or part not in current:
            return False
        current = current[part]
    return current is not None


def validate_document_rules(disclosed_payload: dict[str, Any], rules: list[DocumentRule]) -> dict[str, Any]:
    errors: list[str] = []
    satisfied: list[str] = []
    missing: list[str] = []
    documents = disclosed_payload.get("documentEvidence")
    if documents is None:
        documents = []
    if not isinstance(documents, list):
        errors.append("documentEvidence must be an array")
        documents = []
    submitted_types = [
        str(item.get("documentType"))
        for item in documents
        if isinstance(item, dict) and item.get("documentType") is not None
    ]
    for rule in rules:
        required = rule.required and condition_matches(rule.required_when, disclosed_payload)
        matching = [
            item
            for item in documents
            if isinstance(item, dict)
            and isinstance(item.get("documentType"), str)
            and item["documentType"] in rule.one_of
            and item["documentType"] not in rule.not_allowed
        ]
        disallowed = [
            item["documentType"]
            for item in documents
            if isinstance(item, dict)
            and isinstance(item.get("documentType"), str)
            and item["documentType"] in rule.not_allowed
        ]
        if disallowed:
            errors.append(f"document rule {rule.id} has notAllowed documentType: {', '.join(sorted(set(disallowed)))}")
        if required and not matching:
            missing.append(rule.id)
            errors.append(f"document rule missing: {rule.id}")
        elif matching:
            satisfied.append(rule.id)
    return {
        "errors": errors,
        "satisfiedRequirements": satisfied,
        "missingRequirements": missing,
        "submittedDocumentTypes": submitted_types,
    }
