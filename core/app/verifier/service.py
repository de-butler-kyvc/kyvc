import base64
import hashlib
import json
import mimetypes
import tempfile
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from app.ai_assessment.enums import DocumentType
from app.ai_assessment.extractors.ocr_layout import OcrLayoutDeterministicExtractor
from app.ai_assessment.extractors.structured_payload import StructuredPayloadExtractor
from app.ai_assessment.providers import DocumentExtractionProvider
from app.ai_assessment.schemas import DocumentMetadata, PowerOfAttorneyExtraction
from app.credentials.delegate_identity import (
    DELEGATE_IDENTITY_DIGEST_ALGORITHM,
    DELEGATE_IDENTITY_DIGEST_VERSION,
    delegate_identity_digest,
)
from app.credentials.did import account_from_did
from app.credentials.resolver import DidResolver, find_verification_method
from app.credentials.crypto import decode_compact_jws
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
from app.policy.sdjwt_policy import SdJwtVerificationPolicy
from app.sdjwt.kb import decode_kb_jwt, sd_hash_for_presentation, verify_kb_jwt
from app.sdjwt.verifier import parse_sd_jwt, verify_issuer_sd_jwt
from app.status.sdjwt_status import credential_type_hex_from_payload
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


def did_resolution_details(resolution: dict[str, Any]) -> dict[str, Any]:
    metadata = resolution.get("didResolutionMetadata") or {}
    document_metadata = resolution.get("didDocumentMetadata") or {}
    details = {
        "resolver": metadata.get("resolver"),
        "cacheResolver": metadata.get("cacheResolver"),
        "ledger": metadata.get("ledger"),
        "account": metadata.get("account"),
        "uri": document_metadata.get("uri"),
        "dataHash": document_metadata.get("dataHash"),
        "cached": document_metadata.get("cached"),
        "cacheVerified": document_metadata.get("cacheVerified"),
    }
    return {key: value for key, value in details.items() if value is not None}


def _extracted_value(value: Any) -> Any:
    if hasattr(value, "model_dump"):
        value = value.model_dump(mode="json")
    if isinstance(value, dict):
        normalized = value.get("normalized")
        return normalized if normalized not in (None, "") else value.get("raw")
    return value


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
        document_extraction_provider: DocumentExtractionProvider | None = None,
    ):
        self.resolver = resolver
        self.status_lookup = status_lookup
        self.policy = policy or VerificationPolicy()
        self.verification_logger = verification_logger
        self.challenge_lookup = challenge_lookup
        self.challenge_marker = challenge_marker
        self.document_extraction_provider = document_extraction_provider
        self.structured_extractor = StructuredPayloadExtractor()
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
            details["issuerDidResolution"] = did_resolution_details(resolution)
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

    def verify_sd_jwt_credential(
        self,
        credential: str,
        *,
        now: datetime | None = None,
        policy: SdJwtVerificationPolicy | None = None,
        require_status: bool = True,
    ) -> VerificationResult:
        checked_at = (now or datetime.now(tz=UTC)).astimezone(UTC)
        errors: list[str] = []
        details: dict[str, Any] = {"mediaType": "dc+sd-jwt"}
        subject_id: str | None = None
        try:
            issuer_header, issuer_payload, _, _ = decode_compact_jws(credential.split("~", 1)[0])
            issuer_did = str(issuer_payload["iss"])
            holder_did = str(issuer_payload["sub"])
            subject_id = holder_did
            issuer_account = account_from_did(issuer_did)
            holder_account = account_from_did(holder_did)
            resolution = self.resolver.resolve(issuer_did)
            details["issuerDidResolution"] = did_resolution_details(resolution)
            diddoc = resolution["didDocument"]
            vm_id = str(issuer_header.get("kid", ""))
            method = find_verification_method(diddoc, vm_id)
            if method is None:
                errors.append("SD-JWT verificationMethod not found in issuer DID Document")
            elif vm_id not in diddoc.get("assertionMethod", []):
                errors.append("SD-JWT verificationMethod is not authorized for assertionMethod")
            else:
                try:
                    verified = verify_issuer_sd_jwt(
                        credential,
                        public_jwk=method["publicKeyJwk"],
                        verification_method=vm_id,
                        accepted_typ=(policy.accepted_format if policy else "dc+sd-jwt"),
                        now=checked_at,
                    )
                    disclosed_payload = verified.disclosed_payload
                    details["disclosedPayload"] = disclosed_payload
                    details["disclosedPaths"] = sorted(verified.disclosed_paths)
                    details["credentialVerified"] = True
                except Exception as exc:
                    errors.append(str(exc))
                    disclosed_payload = {}
                    details["credentialVerified"] = False

            status = issuer_payload.get("credentialStatus") or {}
            expected_type = credential_type_hex_from_payload(issuer_payload)
            details["expectedCredentialType"] = expected_type
            if status.get("credentialType") != expected_type:
                errors.append("SD-JWT credentialStatus credentialType mismatch")
            if status.get("type") != "XRPLCredentialStatus":
                errors.append("SD-JWT credentialStatus type is not XRPLCredentialStatus")
            if require_status:
                if self.status_lookup is None:
                    errors.append("credential status lookup is not configured")
                    entry = None
                else:
                    entry = self.status_lookup(issuer_account, holder_account, expected_type)
                details["credentialEntryFound"] = entry is not None
                details["credentialAccepted"] = is_credential_active(entry, checked_at)
                details["statusVerified"] = is_credential_active(entry, checked_at)
                if entry is not None:
                    if entry.get("Issuer", entry.get("issuer")) != issuer_account:
                        errors.append("XRPL Credential issuer does not match issuer DID account")
                    if entry.get("Subject", entry.get("subject")) != holder_account:
                        errors.append("XRPL Credential subject does not match holder DID account")
                if not is_credential_active(entry, checked_at):
                    errors.append("XRPL Credential status is not active")
            else:
                details["statusVerified"] = True

            selected_policy = policy or SdJwtVerificationPolicy()
            if "disclosed_payload" in locals():
                policy_details = selected_policy.validate(disclosed_payload, set(details.get("disclosedPaths", [])))
            else:
                policy_details = selected_policy.validate({}, set())
            details.update(
                {
                    "policyVerified": not policy_details["errors"],
                    "satisfiedRequirements": policy_details["satisfiedRequirements"],
                    "missingRequirements": policy_details["missingRequirements"],
                    "submittedDocumentTypes": policy_details["submittedDocumentTypes"],
                    "policyErrors": policy_details["errors"],
                }
            )
            errors.extend(policy_details["errors"])
        except Exception as exc:
            errors.append(str(exc))

        result = VerificationResult(ok=not errors, errors=errors, details=details)
        if self.verification_logger is not None:
            self.verification_logger(subject_id, result, checked_at)
        return result

    def verify_sd_jwt_presentation(
        self,
        presentation: dict[str, Any] | str,
        *,
        now: datetime | None = None,
        policy: SdJwtVerificationPolicy | None = None,
        require_status: bool = True,
        attachments: dict[str, tuple[bytes, str | None]] | None = None,
    ) -> VerificationResult:
        checked_at = (now or datetime.now(tz=UTC)).astimezone(UTC)
        errors: list[str] = []
        details: dict[str, Any] = {
            "mediaType": "kyvc-sd-jwt-presentation-v1",
            "credentialVerified": False,
            "holderBindingVerified": False,
            "statusVerified": False,
            "policyVerified": False,
            "originalDocumentHashMatches": [],
            "delegateIdentityMatches": [],
        }
        holder_did: str | None = None
        nonce: str | None = None

        try:
            if isinstance(presentation, str):
                presentation_data = {"sdJwtKb": presentation}
            else:
                presentation_data = presentation
            sd_jwt_kb = str(presentation_data["sdJwtKb"])
            parsed = parse_sd_jwt(sd_jwt_kb, expect_kb=True)
            presented_without_kb = f"{parsed.issuer_jwt}~{'~'.join(parsed.disclosures)}"
            _, kb_payload = decode_kb_jwt(parsed.kb_jwt or "")
            nonce_value = kb_payload.get("nonce")
            aud_value = kb_payload.get("aud")
            nonce = nonce_value if isinstance(nonce_value, str) else None
            expected_aud = presentation_data.get("aud")
            expected_nonce = presentation_data.get("nonce")
            challenge_entry: VerificationChallengeEntry | None = None
            challenge_policy: SdJwtVerificationPolicy | None = None
            if kb_payload.get("sd_hash") != sd_hash_for_presentation(presented_without_kb):
                errors.append("KB-JWT sd_hash mismatch")
            if expected_nonce is not None and nonce_value != expected_nonce:
                errors.append("KB-JWT nonce does not match presentation nonce")
            if expected_aud is not None and aud_value != expected_aud:
                errors.append("KB-JWT aud does not match presentation aud")
            if not isinstance(nonce_value, str) or not nonce_value:
                errors.append("KB-JWT nonce is required")
            elif not isinstance(aud_value, str) or not aud_value:
                errors.append("KB-JWT aud is required")
            elif self.challenge_lookup is None:
                errors.append("verifier challenge lookup is not configured")
            else:
                challenge_entry = self.challenge_lookup(nonce_value)
                details["challengeFound"] = challenge_entry is not None
                if challenge_entry is None:
                    errors.append("KB-JWT nonce was not issued by verifier")
                else:
                    details["challengeExpiresAt"] = challenge_entry.expires_at.isoformat().replace("+00:00", "Z")
                    details["challengeUsed"] = challenge_entry.used_at is not None
                    if challenge_entry.presentation_definition is not None:
                        details["presentationDefinition"] = challenge_entry.presentation_definition
                        challenge_policy = SdJwtVerificationPolicy.from_dict(challenge_entry.presentation_definition)
                    if challenge_entry.used_at is not None:
                        errors.append("KB-JWT nonce was already used")
                    if checked_at > challenge_entry.expires_at:
                        errors.append("KB-JWT nonce is expired")
                    if challenge_entry.domain != aud_value:
                        errors.append("KB-JWT aud mismatch")
            if isinstance(kb_payload.get("iat"), int) and kb_payload["iat"] > checked_at.timestamp() + 300:
                errors.append("KB-JWT iat is in the future")
            if policy is not None and challenge_policy is not None and policy != challenge_policy:
                errors.append("presentation policy does not match verifier challenge")
            effective_policy = challenge_policy or policy or SdJwtVerificationPolicy()

            credential_result = self.verify_sd_jwt_credential(
                presented_without_kb,
                now=checked_at,
                policy=effective_policy,
                require_status=require_status,
            )
            details["credential"] = credential_result.to_dict()
            details["credentialVerified"] = bool(credential_result.details.get("credentialVerified"))
            details["statusVerified"] = credential_result.details.get("statusVerified", False)
            issuer_payload = credential_result.details.get("disclosedPayload") or {}
            issuer_signed = decode_compact_jws(parsed.issuer_jwt)[1]
            holder_did = str(issuer_signed["sub"])
            cnf = issuer_signed.get("cnf") if isinstance(issuer_signed.get("cnf"), dict) else {}
            holder_kid = cnf.get("kid")
            if not isinstance(holder_kid, str) or not holder_kid.startswith(f"{holder_did}#"):
                errors.append("SD-JWT cnf.kid does not bind to holder DID")
            holder_resolution = self.resolver.resolve(holder_did)
            details["holderDidResolution"] = did_resolution_details(holder_resolution)
            holder_doc = holder_resolution["didDocument"]
            holder_method = find_verification_method(holder_doc, str(holder_kid))
            if holder_method is None:
                errors.append("KB-JWT verificationMethod not found in holder DID Document")
            elif holder_kid not in holder_doc.get("authentication", []):
                errors.append("KB-JWT verificationMethod is not authorized for authentication")
            elif not verify_kb_jwt(parsed.kb_jwt or "", holder_method["publicKeyJwk"], holder_kid):
                errors.append("KB-JWT signature verification failed")

            attachment_errors, hash_matches = self._verify_attachments(
                credential_result.details.get("disclosedPayload") or issuer_payload,
                presentation_data.get("attachmentManifest") or [],
                attachments or {},
                effective_policy,
            )
            errors.extend(attachment_errors)
            details["originalDocumentHashMatches"] = hash_matches
            delegate_errors, delegate_matches = self._verify_delegate_identity(
                credential_result.details.get("disclosedPayload") or issuer_payload,
                presentation_data.get("attachmentManifest") or [],
                attachments or {},
            )
            errors.extend(delegate_errors)
            details["delegateIdentityMatches"] = delegate_matches

            errors.extend(credential_result.errors)
            details["holderBindingVerified"] = not errors
        except Exception as exc:
            errors.append(str(exc))

        if not errors:
            if not isinstance(nonce, str) or self.challenge_marker is None:
                errors.append("verifier challenge marker is not configured")
            elif not self.challenge_marker(nonce, checked_at):
                errors.append("KB-JWT nonce was already used")
        details["verified"] = not errors
        details["holderBindingVerified"] = details["holderBindingVerified"] and not errors
        result = VerificationResult(ok=not errors, errors=errors, details=details)
        if self.verification_logger is not None:
            self.verification_logger(holder_did, result, checked_at)
        return result

    def _verify_attachments(
        self,
        disclosed_payload: dict[str, Any],
        manifest: list[dict[str, Any]],
        attachments: dict[str, tuple[bytes, str | None]],
        policy: SdJwtVerificationPolicy,
    ) -> tuple[list[str], list[dict[str, Any]]]:
        errors: list[str] = []
        matches: list[dict[str, Any]] = []
        evidence_items = disclosed_payload.get("documentEvidence")
        if evidence_items is None:
            evidence_items = []
        if not isinstance(evidence_items, list):
            return ["documentEvidence must be disclosed to verify attachments"], matches
        evidence_by_id = {
            item.get("documentId"): item for item in evidence_items if isinstance(item, dict) and item.get("documentId")
        }
        rules_by_id = {rule.id: rule for rule in policy.document_rules}
        if attachments and not manifest:
            errors.append("attachments require an attachmentManifest")
        manifest_by_rule = {
            str(item.get("requirementId")): item for item in manifest if isinstance(item, dict) and item.get("requirementId")
        }
        for rule in policy.document_rules:
            if rule.original_policy == "REQUIRED":
                manifest_item = manifest_by_rule.get(rule.id)
                attachment_ref = manifest_item.get("attachmentRef") if isinstance(manifest_item, dict) else None
                if not isinstance(attachment_ref, str) or attachment_ref not in attachments:
                    errors.append(f"document rule {rule.id} requires an original attachment")
        for item in manifest:
            if not isinstance(item, dict):
                errors.append("attachmentManifest entries must be objects")
                continue
            submission_mode = item.get("submissionMode")
            if submission_mode == "EXTERNAL_ORIGINAL":
                errors.append("EXTERNAL_ORIGINAL document submission is not supported")
            elif submission_mode not in {None, "EVIDENCE_ONLY", "HASH_ONLY", "ATTACHED_ORIGINAL"}:
                errors.append("unsupported document submission mode")
            evidence = evidence_by_id.get(item.get("documentId"))
            if evidence is None:
                errors.append("attachmentManifest document is not disclosed in SD-JWT documentEvidence")
                continue
            if item.get("digestSRI") != evidence.get("digestSRI"):
                errors.append("attachmentManifest digestSRI does not match disclosed documentEvidence")
            if item.get("documentType") != evidence.get("documentType"):
                errors.append("attachmentManifest documentType does not match disclosed documentEvidence")
            rule = rules_by_id.get(str(item.get("requirementId")))
            attachment_ref = item.get("attachmentRef")
            has_attachment = isinstance(attachment_ref, str) and attachment_ref in attachments
            if rule is not None and rule.original_policy == "NOT_ALLOWED" and has_attachment:
                errors.append(f"document rule {rule.id} does not allow original attachments")
            if has_attachment:
                file_bytes, media_type = attachments[str(attachment_ref)]
                digest = "sha384-" + base64.b64encode(hashlib.sha384(file_bytes).digest()).decode("ascii")
                matched = digest == evidence.get("digestSRI") == item.get("digestSRI")
                if not matched:
                    errors.append("attached original digest does not match disclosed documentEvidence")
                if item.get("byteSize") is not None and int(item["byteSize"]) != len(file_bytes):
                    errors.append("attached original byteSize does not match manifest")
                if item.get("mediaType") is not None and media_type is not None and item["mediaType"] != media_type:
                    errors.append("attached original mediaType does not match manifest")
                matches.append({"documentId": item.get("documentId"), "matched": matched})
        return errors, matches

    def _verify_delegate_identity(
        self,
        disclosed_payload: dict[str, Any],
        manifest: list[dict[str, Any]],
        attachments: dict[str, tuple[bytes, str | None]],
    ) -> tuple[list[str], list[dict[str, Any]]]:
        errors: list[str] = []
        matches: list[dict[str, Any]] = []
        delegate = disclosed_payload.get("delegate")
        if not isinstance(delegate, dict) or not delegate.get("identityDigest"):
            return errors, matches
        expected_digest = delegate.get("identityDigest")
        if delegate.get("identityDigestAlgorithm") != DELEGATE_IDENTITY_DIGEST_ALGORITHM:
            errors.append("delegate identity digest algorithm is not supported")
            return errors, matches
        if delegate.get("identityDigestVersion") != DELEGATE_IDENTITY_DIGEST_VERSION:
            errors.append("delegate identity digest version is not supported")
            return errors, matches

        for item in manifest:
            if not isinstance(item, dict) or item.get("documentType") != "KR_POWER_OF_ATTORNEY":
                continue
            attachment_ref = item.get("attachmentRef")
            if not isinstance(attachment_ref, str) or attachment_ref not in attachments:
                continue
            file_bytes, _ = attachments[attachment_ref]
            extracted = self._extract_delegate_from_power_of_attorney(
                file_bytes,
                document_id=str(item.get("documentId") or "power-of-attorney"),
                media_type=item.get("mediaType") if isinstance(item.get("mediaType"), str) else None,
            )
            if extracted is None:
                errors.append("delegate identity fields could not be extracted from power of attorney attachment")
                matches.append({"documentId": item.get("documentId"), "matched": False})
                continue
            actual_digest = delegate_identity_digest(
                name=_extracted_value(extracted.delegateName),
                rrn=_extracted_value(extracted.delegateRrn),
                address=_extracted_value(extracted.delegateAddress),
                contact=_extracted_value(extracted.delegateContact),
            )
            matched = actual_digest == expected_digest
            if not matched:
                errors.append("delegate identity digest does not match power of attorney attachment")
            matches.append({"documentId": item.get("documentId"), "matched": matched})
        return errors, matches

    def _extract_delegate_from_power_of_attorney(
        self,
        file_bytes: bytes,
        *,
        document_id: str = "power-of-attorney",
        media_type: str | None = None,
    ) -> PowerOfAttorneyExtraction | None:
        llm_extracted = self._extract_delegate_with_provider(file_bytes, document_id=document_id, media_type=media_type)
        if llm_extracted is not None:
            return llm_extracted
        source = self._attachment_source(file_bytes)
        extracted = OcrLayoutDeterministicExtractor().extract(DocumentType.POWER_OF_ATTORNEY, source)
        if not isinstance(extracted, PowerOfAttorneyExtraction):
            return None
        required = [
            _extracted_value(extracted.delegateName),
            _extracted_value(extracted.delegateRrn),
        ]
        if any(value in (None, "") for value in required):
            return None
        return extracted

    def _extract_delegate_with_provider(
        self,
        file_bytes: bytes,
        *,
        document_id: str,
        media_type: str | None,
    ) -> PowerOfAttorneyExtraction | None:
        if self.document_extraction_provider is None:
            return None
        suffix = mimetypes.guess_extension(media_type or "") or ".bin"
        with tempfile.NamedTemporaryFile(prefix="kyvc-poa-", suffix=suffix) as handle:
            handle.write(file_bytes)
            handle.flush()
            document = DocumentMetadata(
                documentId=document_id,
                kycApplicationId="verifier-delegate-identity",
                originalFileName=f"{document_id}{suffix}",
                mimeType=media_type or mimetypes.guess_type(f"document{suffix}")[0] or "application/octet-stream",
                sizeBytes=len(file_bytes),
                sha256=hashlib.sha256(file_bytes).hexdigest(),
                declaredDocumentType=DocumentType.POWER_OF_ATTORNEY,
                predictedDocumentType=DocumentType.POWER_OF_ATTORNEY,
                storagePath=str(Path(handle.name)),
            )
            try:
                extracted_document = self.document_extraction_provider.extract(document)
                extracted = self.structured_extractor.extract(extracted_document)
            except Exception:
                return None
        if not isinstance(extracted, PowerOfAttorneyExtraction):
            return None
        required = [
            _extracted_value(extracted.delegateName),
            _extracted_value(extracted.delegateRrn),
        ]
        if any(value in (None, "") for value in required):
            return None
        return extracted

    def _attachment_source(self, file_bytes: bytes) -> Any:
        text = file_bytes.decode("utf-8", errors="ignore")
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text

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
            details["holderDidResolution"] = did_resolution_details(resolution)
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
