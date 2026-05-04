from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any

from app.credentials.did import account_from_did
from app.credentials.resolver import DidResolver, find_verification_method
from app.credentials.vc import (
    ENVELOPED_VC_TYPE,
    credential_type_hex_for_vc,
    decode_vc_jwt,
    parse_datetime,
    vc_jwt_from_enveloped,
    verify_vc_jwt,
    verify_vc_signature,
)
from app.credentials.vp import (
    ENVELOPED_VP_TYPE,
    decode_vp_jwt,
    vc_jwts_from_vp,
    verify_vp_jwt,
    verify_vp_signature,
    vp_jwt_from_enveloped,
)
from app.storage.interfaces import VerificationChallengeEntry
from app.verifier.policy import VerificationPolicy
from app.xrpl.ledger import is_credential_active


@dataclass
class VerificationResult:
    ok: bool
    errors: list[str] = field(default_factory=list)
    details: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {"ok": self.ok, "errors": self.errors, "details": self.details}


StatusLookup = Callable[[str, str, str], dict[str, Any] | None]
VerificationLogger = Callable[[str | None, VerificationResult, datetime], None]
ChallengeLookup = Callable[[str], VerificationChallengeEntry | None]
ChallengeMarker = Callable[[str, datetime], bool]


class VerifierService:
    def __init__(
        self,
        resolver: DidResolver,
        *,
        status_lookup: StatusLookup | None = None,
        policy: VerificationPolicy | None = None,
        verification_logger: VerificationLogger | None = None,
        challenge_lookup: ChallengeLookup | None = None,
        challenge_marker: ChallengeMarker | None = None,
    ):
        self.resolver = resolver
        self.status_lookup = status_lookup
        self.policy = policy or VerificationPolicy()
        self.verification_logger = verification_logger
        self.challenge_lookup = challenge_lookup
        self.challenge_marker = challenge_marker
        if self.challenge_lookup is None and hasattr(resolver, "get_verification_challenge"):
            self.challenge_lookup = getattr(resolver, "get_verification_challenge")
        if self.challenge_marker is None and hasattr(resolver, "mark_verification_challenge_used"):
            self.challenge_marker = getattr(resolver, "mark_verification_challenge_used")

    def verify_vc(
        self,
        vc: dict[str, Any] | str,
        *,
        now: datetime | None = None,
        policy: VerificationPolicy | None = None,
        require_status: bool = True,
    ) -> VerificationResult:
        checked_at = (now or datetime.now(tz=UTC)).astimezone(UTC)
        errors: list[str] = []
        details: dict[str, Any] = {}
        subject_id: str | None = None

        try:
            vc_jwt: str | None = None
            protected: dict[str, Any] | None = None
            if isinstance(vc, str):
                vc_jwt = vc
                protected, vc_document = decode_vc_jwt(vc_jwt)
                details["mediaType"] = "vc+jwt"
            elif vc.get("type") == ENVELOPED_VC_TYPE:
                vc_jwt = vc_jwt_from_enveloped(vc)
                protected, vc_document = decode_vc_jwt(vc_jwt)
                details["mediaType"] = "vc+jwt"
            else:
                vc_document = vc
                details["mediaType"] = "legacy-embedded-jws"

            issuer_did = str(vc_document["issuer"])
            issuer_account = account_from_did(issuer_did)
            resolution = self.resolver.resolve(issuer_did)
            diddoc = resolution["didDocument"]
            proof = vc_document.get("proof") or {}
            vm_id = str(protected.get("kid", "") if protected is not None else proof.get("verificationMethod", ""))
            method = find_verification_method(diddoc, vm_id)
            if protected is not None and protected.get("iss") != issuer_did:
                errors.append("VC JOSE iss does not match issuer")
            if protected is None and proof.get("proofPurpose") != "assertionMethod":
                errors.append("VC proofPurpose is not assertionMethod")
            if method is None:
                errors.append("VC verificationMethod not found in issuer DID Document")
            elif vm_id not in diddoc.get("assertionMethod", []):
                errors.append("VC verificationMethod is not authorized for assertionMethod")
            elif protected is not None and vc_jwt is not None and not verify_vc_jwt(
                vc_jwt,
                method["publicKeyJwk"],
                vm_id,
            ):
                errors.append("VC signature verification failed")
            elif protected is None and not verify_vc_signature(vc_document, method["publicKeyJwk"]):
                errors.append("VC signature verification failed")

            valid_from = parse_datetime(str(vc_document["validFrom"]))
            valid_until = parse_datetime(str(vc_document["validUntil"]))
            if not (valid_from <= checked_at <= valid_until):
                errors.append("VC is outside validFrom/validUntil")

            status = vc_document.get("credentialStatus") or {}
            expected_type = credential_type_hex_for_vc(vc_document)
            details["expectedCredentialType"] = expected_type
            if status.get("credentialType") != expected_type:
                errors.append("VC credentialStatus credentialType mismatch")

            credential_subject = vc_document.get("credentialSubject") or {}
            subject_id = str(credential_subject["id"])
            subject_account = account_from_did(subject_id)
            if status.get("issuer") != issuer_account:
                errors.append("VC credentialStatus issuer does not match issuer DID account")
            if status.get("subject") != subject_account:
                errors.append("VC credentialStatus subject does not match credentialSubject DID account")

            if require_status:
                if self.status_lookup is None:
                    errors.append("credential status lookup is not configured")
                    entry = None
                else:
                    entry = self.status_lookup(issuer_account, subject_account, expected_type)
                details["credentialEntryFound"] = entry is not None
                details["credentialAccepted"] = is_credential_active(entry, checked_at)
                if not is_credential_active(entry, checked_at):
                    errors.append("XRPL Credential status is not active")

            policy_errors = (policy or self.policy).validate_vc(vc_document)
            errors.extend(policy_errors)
            details["policyErrors"] = policy_errors
        except Exception as exc:
            errors.append(str(exc))

        result = VerificationResult(ok=not errors, errors=errors, details=details)
        if self.verification_logger is not None:
            self.verification_logger(subject_id, result, checked_at)
        return result

    def verify_vp(
        self,
        vp: dict[str, Any] | str,
        *,
        now: datetime | None = None,
        policy: VerificationPolicy | None = None,
        require_status: bool = True,
    ) -> VerificationResult:
        errors: list[str] = []
        details: dict[str, Any] = {}
        checked_at = (now or datetime.now(tz=UTC)).astimezone(UTC)
        holder_did: str | None = None
        proof_challenge: str | None = None

        try:
            vp_jwt: str | None = None
            protected: dict[str, Any] | None = None
            if isinstance(vp, str):
                vp_jwt = vp
                protected, vp_document = decode_vp_jwt(vp_jwt)
                details["mediaType"] = "vp+jwt"
            elif vp.get("type") == ENVELOPED_VP_TYPE:
                vp_jwt = vp_jwt_from_enveloped(vp)
                protected, vp_document = decode_vp_jwt(vp_jwt)
                details["mediaType"] = "vp+jwt"
            else:
                vp_document = vp
                details["mediaType"] = "legacy-embedded-jws"

            holder_did = str(vp_document["holder"])
            resolution = self.resolver.resolve(holder_did)
            diddoc = resolution["didDocument"]
            proof = vp_document.get("proof") or {}
            proof_challenge_value = protected.get("challenge") if protected is not None else proof.get("challenge")
            proof_domain = protected.get("domain") if protected is not None else proof.get("domain")
            proof_challenge = proof_challenge_value if isinstance(proof_challenge_value, str) else None
            vm_id = str(protected.get("kid", "") if protected is not None else proof.get("verificationMethod", ""))
            method = find_verification_method(diddoc, vm_id)
            if protected is None and proof.get("proofPurpose") != "authentication":
                errors.append("VP proofPurpose is not authentication")
            challenge_entry: VerificationChallengeEntry | None = None
            if not isinstance(proof_challenge_value, str) or not proof_challenge_value:
                errors.append("VP proof challenge is required")
            elif not isinstance(proof_domain, str) or not proof_domain:
                errors.append("VP proof domain is required")
            elif self.challenge_lookup is None:
                errors.append("verifier challenge lookup is not configured")
            else:
                challenge_entry = self.challenge_lookup(proof_challenge_value)
                details["challengeFound"] = challenge_entry is not None
                if challenge_entry is None:
                    errors.append("VP challenge was not issued by verifier")
                else:
                    details["challengeExpiresAt"] = challenge_entry.expires_at.isoformat().replace("+00:00", "Z")
                    details["challengeUsed"] = challenge_entry.used_at is not None
                    if challenge_entry.used_at is not None:
                        errors.append("VP challenge was already used")
                    if checked_at > challenge_entry.expires_at:
                        errors.append("VP challenge is expired")
                    if challenge_entry.domain != proof_domain:
                        errors.append("VP domain mismatch")
            if method is None:
                errors.append("VP verificationMethod not found in holder DID Document")
            elif vm_id not in diddoc.get("authentication", []):
                errors.append("VP verificationMethod is not authorized for authentication")
            elif protected is not None and vp_jwt is not None and not verify_vp_jwt(
                vp_jwt,
                method["publicKeyJwk"],
                vm_id,
            ):
                errors.append("VP signature verification failed")
            elif protected is None and not verify_vp_signature(vp_document, method["publicKeyJwk"]):
                errors.append("VP signature verification failed")

            if protected is not None:
                verifiable_credentials: list[dict[str, Any] | str] = vc_jwts_from_vp(vp_document)
            else:
                verifiable_credentials = vp_document.get("verifiableCredential", [])

            for index, vc in enumerate(verifiable_credentials):
                vc_result = self.verify_vc(
                    vc,
                    now=checked_at,
                    policy=policy,
                    require_status=require_status,
                )
                details[f"vc_{index}"] = vc_result.to_dict()
                if not vc_result.ok:
                    errors.append(f"embedded VC {index} failed verification")
                if isinstance(vc, str):
                    _, vc_document = decode_vc_jwt(vc)
                elif isinstance(vc, dict) and vc.get("type") == ENVELOPED_VC_TYPE:
                    _, vc_document = decode_vc_jwt(vc_jwt_from_enveloped(vc))
                else:
                    vc_document = vc
                if vc_document.get("credentialSubject", {}).get("id") != holder_did:
                    errors.append(f"embedded VC {index} subject does not match VP holder")
        except Exception as exc:
            errors.append(str(exc))

        if not errors:
            if not isinstance(proof_challenge, str) or self.challenge_marker is None:
                errors.append("verifier challenge marker is not configured")
            elif not self.challenge_marker(proof_challenge, checked_at):
                errors.append("VP challenge was already used")

        result = VerificationResult(ok=not errors, errors=errors, details=details)
        if self.verification_logger is not None:
            self.verification_logger(holder_did, result, checked_at)
        return result
