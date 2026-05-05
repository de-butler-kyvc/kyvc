from datetime import datetime
from typing import Any, Literal

from cryptography.hazmat.primitives.asymmetric.ec import EllipticCurvePrivateKey

from app.credentials.crypto import private_key_to_jwk
from app.credentials.did import did_from_account, issuer_diddoc
from app.credentials.vc import (
    add_vc_compatibility_proof,
    build_kyc_vc,
    make_credential_status,
    parse_datetime,
    secure_vc_jwt,
)
from app.sdjwt.issuer import (
    DEFAULT_LEGAL_ENTITY_KYC_VCT,
    build_legal_entity_kyc_payload,
    issue_sd_jwt,
)
from app.status.sdjwt_status import credential_type_hex_for_sdjwt
from app.storage.interfaces import CredentialRepository, DidDocumentRepository, StatusRepository
from app.xrpl.ledger import LSF_ACCEPTED, datetime_to_ripple_epoch


class IssuerService:
    def __init__(
        self,
        issuer_account: str,
        private_key: EllipticCurvePrivateKey,
        *,
        issuer_did: str | None = None,
        key_id: str = "issuer-key-1",
        credential_repository: CredentialRepository | None = None,
        status_repository: StatusRepository | None = None,
        did_document_repository: DidDocumentRepository | None = None,
    ):
        self.issuer_account = issuer_account
        self.issuer_did = issuer_did or did_from_account(issuer_account)
        self.private_key = private_key
        self.key_id = key_id
        self.credential_repository = credential_repository
        self.status_repository = status_repository
        self.did_document_repository = did_document_repository

    @property
    def verification_method(self) -> str:
        return f"{self.issuer_did}#{self.key_id}"

    def build_did_document(self) -> dict[str, Any]:
        return issuer_diddoc(self.issuer_did, self.key_id, private_key_to_jwk(self.private_key))

    def register_did_document(self) -> dict[str, Any]:
        did_document = self.build_did_document()
        if self.did_document_repository is not None:
            self.did_document_repository.save_did_document(self.issuer_did, did_document)
        return did_document

    def issue_kyc_vc(
        self,
        *,
        holder_account: str,
        claims: dict[str, Any],
        valid_from: datetime,
        valid_until: datetime,
        holder_did: str | None = None,
        persist: bool = True,
        persist_status: bool = True,
        mark_status_accepted: bool = False,
        status_uri: str | None = None,
        credential_format: Literal["jwt", "embedded_jws"] = "jwt",
    ) -> str | dict[str, Any]:
        selected_holder_did = holder_did or did_from_account(holder_account)
        vc = build_kyc_vc(self.issuer_did, selected_holder_did, claims, valid_from, valid_until)
        vc["credentialStatus"] = make_credential_status(vc, self.issuer_account, holder_account)
        if credential_format == "jwt":
            signed: str | dict[str, Any] = secure_vc_jwt(vc, self.private_key, self.verification_method)
        else:
            signed = add_vc_compatibility_proof(vc, self.private_key, self.verification_method)

        credential_status = vc["credentialStatus"]
        credential_type = str(credential_status["credentialType"])
        if persist and self.credential_repository is not None:
            self.credential_repository.save_issued_credential(
                vc=vc,
                issuer_did=self.issuer_did,
                issuer_account=self.issuer_account,
                holder_did=selected_holder_did,
                holder_account=holder_account,
                credential_type=credential_type,
                vc_core_hash=str(credential_status["vcCoreHash"]),
            )
        if persist_status and self.status_repository is not None:
            flags = LSF_ACCEPTED if mark_status_accepted else 0
            self.status_repository.save_credential_status(
                issuer_account=self.issuer_account,
                holder_account=holder_account,
                credential_type=credential_type,
                flags=flags,
                expiration=datetime_to_ripple_epoch(parse_datetime(str(vc["validUntil"]))),
                uri=status_uri,
            )
        return signed

    def issue_kyc_sd_jwt(
        self,
        *,
        holder_account: str,
        claims: dict[str, Any],
        valid_from: datetime,
        valid_until: datetime,
        holder_did: str | None = None,
        holder_key_id: str = "holder-key-1",
        vct: str = DEFAULT_LEGAL_ENTITY_KYC_VCT,
        persist: bool = True,
        persist_status: bool = True,
        mark_status_accepted: bool = False,
        status_uri: str | None = None,
        typ: str = "dc+sd-jwt",
    ) -> tuple[str, dict[str, Any], list[str]]:
        selected_holder_did = holder_did or did_from_account(holder_account)
        payload = build_legal_entity_kyc_payload(
            issuer_did=self.issuer_did,
            holder_did=selected_holder_did,
            holder_key_id=holder_key_id,
            claims=claims,
            valid_from=valid_from,
            valid_until=valid_until,
            vct=vct,
        )
        credential, disclosure_result = issue_sd_jwt(
            payload=payload,
            private_key=self.private_key,
            verification_method=self.verification_method,
            typ=typ,
        )
        status = payload["credentialStatus"]
        credential_type = str(status["credentialType"])
        if persist and self.credential_repository is not None:
            self.credential_repository.save_issued_credential(
                vc={"id": payload["jti"], **payload},
                issuer_did=self.issuer_did,
                issuer_account=self.issuer_account,
                holder_did=selected_holder_did,
                holder_account=holder_account,
                credential_type=credential_type,
                vc_core_hash="SDJWT_STATUS_V1",
            )
        if persist_status and self.status_repository is not None:
            flags = LSF_ACCEPTED if mark_status_accepted else 0
            self.status_repository.save_credential_status(
                issuer_account=self.issuer_account,
                holder_account=holder_account,
                credential_type=credential_type,
                flags=flags,
                expiration=datetime_to_ripple_epoch(valid_until),
                uri=status_uri,
            )
        return credential, status, disclosure_result.disclosable_paths

    def credential_type_for_sd_jwt(
        self,
        *,
        holder_did: str,
        vct: str,
        jti: str,
    ) -> str:
        return credential_type_hex_for_sdjwt(
            issuer_did=self.issuer_did,
            holder_did=holder_did,
            vct=vct,
            jti=jti,
        )

    def revoke_local_status(self, *, holder_account: str, credential_type: str) -> None:
        revoke = getattr(self.status_repository, "revoke_credential_status", None)
        if not callable(revoke):
            raise RuntimeError("configured status repository does not support local revocation")
        revoke(
            issuer_account=self.issuer_account,
            holder_account=holder_account,
            credential_type=credential_type,
        )
