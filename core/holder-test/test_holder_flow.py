import argparse
import base64
import hashlib
import json
import os
import sqlite3
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any

from dotenv import load_dotenv

from app.credentials.crypto import b64url_decode, decode_compact_jws, generate_private_key, private_key_to_jwk
from app.credentials.did import account_from_did, did_from_account, holder_diddoc
from app.credentials.resolver import did_resolution_result
from app.credentials.vc import decode_vc_jwt, issuer_account_from_vc
from app.issuer.service import IssuerService
from app.policy.sdjwt_policy import SdJwtVerificationPolicy
from app.sdjwt.kb import create_kb_jwt
from app.sdjwt.verifier import parse_sd_jwt
from app.storage.interfaces import VerificationChallengeEntry
from app.verifier.service import VerifierService
from app.xrpl.client import enforce_mainnet_policy, make_client, tx_hash
from app.xrpl.ledger import (
    get_credential_entry as get_xrpl_credential_entry,
    is_credential_active,
    submit_credential_accept,
    submit_credential_create,
    submit_credential_delete,
)
from app.xrpl.status import remove_status_mirror, sync_status_mirror
from app.xrpl.wallets import generate_funded_wallet, wallet_from_seed, wallet_seed


class HolderTestRepository:
    def __init__(self):
        self.did_documents: dict[str, dict[str, Any]] = {}
        self.issued_credentials: dict[str, dict[str, Any]] = {}
        self.credential_status: dict[tuple[str, str, str], dict[str, Any]] = {}
        self.verification_challenges: dict[str, VerificationChallengeEntry] = {}
        self.verification_logs: list[dict[str, Any]] = []

    def save_did_document(self, did: str, did_document: dict[str, Any]) -> None:
        self.did_documents[did] = did_document

    def get_did_document(self, did: str) -> dict[str, Any] | None:
        return self.did_documents.get(did)

    def resolve(self, did: str) -> dict[str, Any]:
        did_document = self.get_did_document(did)
        if did_document is None:
            raise ValueError(f"DID Document not found for {did}")
        return did_resolution_result(did, did_document, {"resolver": "holder-test-memory"})

    def save_issued_credential(
        self,
        *,
        vc: dict[str, Any],
        issuer_did: str,
        issuer_account: str,
        holder_did: str,
        holder_account: str,
        credential_type: str,
        vc_core_hash: str,
    ) -> None:
        self.issued_credentials[str(vc["id"])] = {
            "vc": vc,
            "issuer_did": issuer_did,
            "issuer_account": issuer_account,
            "holder_did": holder_did,
            "holder_account": holder_account,
            "credential_type": credential_type,
            "vc_core_hash": vc_core_hash,
            "revoked_at": None,
        }

    def save_credential_status(
        self,
        *,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
        flags: int,
        expiration: int | None = None,
        uri: str | None = None,
    ) -> None:
        entry: dict[str, Any] = {
            "Issuer": issuer_account,
            "Subject": holder_account,
            "CredentialType": credential_type,
            "Flags": flags,
        }
        if expiration is not None:
            entry["Expiration"] = expiration
        if uri is not None:
            entry["URI"] = uri
        self.credential_status[(issuer_account, holder_account, credential_type)] = entry

    def get_credential_entry(
        self,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
    ) -> dict[str, Any] | None:
        return self.credential_status.get((issuer_account, holder_account, credential_type))

    def revoke_credential_status(
        self,
        *,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
    ) -> None:
        self.credential_status.pop((issuer_account, holder_account, credential_type), None)
        for issued in self.issued_credentials.values():
            if (
                issued["issuer_account"] == issuer_account
                and issued["holder_account"] == holder_account
                and issued["credential_type"] == credential_type
            ):
                issued["revoked_at"] = datetime.now(tz=UTC)

    def save_verification_result(
        self,
        *,
        subject_id: str | None,
        ok: bool,
        errors: list[str],
        details: dict[str, Any],
        verified_at: datetime,
    ) -> None:
        self.verification_logs.append(
            {
                "subject_id": subject_id,
                "ok": ok,
                "errors": errors,
                "details": details,
                "verified_at": verified_at.astimezone(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
            }
        )

    def save_verification_challenge(
        self,
        *,
        challenge: str,
        domain: str,
        expires_at: datetime,
        created_at: datetime,
        presentation_definition: dict[str, Any] | None = None,
    ) -> None:
        self.verification_challenges[challenge] = VerificationChallengeEntry(
            challenge=challenge,
            domain=domain,
            expires_at=expires_at.astimezone(UTC).replace(microsecond=0),
            created_at=created_at.astimezone(UTC).replace(microsecond=0),
            presentation_definition=presentation_definition,
        )

    def get_verification_challenge(self, challenge: str) -> VerificationChallengeEntry | None:
        return self.verification_challenges.get(challenge)

    def mark_verification_challenge_used(self, challenge: str, used_at: datetime) -> bool:
        entry = self.verification_challenges.get(challenge)
        if entry is None or entry.used_at is not None:
            return False
        self.verification_challenges[challenge] = VerificationChallengeEntry(
            challenge=entry.challenge,
            domain=entry.domain,
            expires_at=entry.expires_at,
            used_at=used_at.astimezone(UTC).replace(microsecond=0),
            created_at=entry.created_at,
            presentation_definition=entry.presentation_definition,
        )
        return True


def _now() -> str:
    return datetime.now(tz=UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def _init_holder_wallet(db_path: Path) -> None:
    with sqlite3.connect(db_path) as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS holder_test_credentials (
                credential_id TEXT PRIMARY KEY,
                vc_json TEXT NOT NULL,
                stored_at TEXT NOT NULL,
                submitted_at TEXT,
                credential_accept_hash TEXT
            )
            """
        )
        columns = {
            str(row[1])
            for row in connection.execute("PRAGMA table_info(holder_test_credentials)").fetchall()
        }
        if "credential_accept_hash" not in columns:
            connection.execute("ALTER TABLE holder_test_credentials ADD COLUMN credential_accept_hash TEXT")


def _credential_document(credential: dict[str, Any] | str) -> dict[str, Any]:
    if isinstance(credential, str) and "~" in credential:
        return decode_compact_jws(credential.split("~", 1)[0])[1]
    return decode_vc_jwt(credential)[1] if isinstance(credential, str) else credential


def _credential_id(credential_document: dict[str, Any]) -> str:
    return str(credential_document.get("id") or credential_document["jti"])


def holder_store_credential(db_path: Path, credential: dict[str, Any] | str) -> None:
    credential_document = _credential_document(credential)
    _init_holder_wallet(db_path)
    with sqlite3.connect(db_path) as connection:
        connection.execute(
            """
            INSERT INTO holder_test_credentials (credential_id, vc_json, stored_at)
            VALUES (?, ?, ?)
            ON CONFLICT(credential_id) DO UPDATE SET
                vc_json = excluded.vc_json,
                stored_at = excluded.stored_at
            """,
            (
                _credential_id(credential_document),
                json.dumps(credential, ensure_ascii=False, sort_keys=True, separators=(",", ":")),
                _now(),
            ),
        )


def holder_submit_credential_accept(
    *,
    client: Any,
    db_path: Path,
    holder_seed: str,
    credential: dict[str, Any] | str,
) -> tuple[dict[str, Any], dict[str, Any] | None]:
    credential_document = _credential_document(credential)
    status = credential_document["credentialStatus"]
    holder_wallet = wallet_from_seed(holder_seed)
    issuer_account = (
        account_from_did(str(credential_document["iss"]))
        if "iss" in credential_document
        else issuer_account_from_vc(credential_document)
    )
    credential_type = str(status["credentialType"])
    expected_holder_account = (
        account_from_did(str(credential_document["sub"]))
        if "sub" in credential_document
        else str(status["subject"])
    )
    if holder_wallet.address != expected_holder_account:
        raise RuntimeError("holder seed does not match credential subject account")

    tx = submit_credential_accept(client, holder_wallet, issuer_account, credential_type)
    entry = get_xrpl_credential_entry(client, issuer_account, holder_wallet.address, credential_type)
    with sqlite3.connect(db_path) as connection:
        connection.execute(
            """
            UPDATE holder_test_credentials
            SET submitted_at = ?, credential_accept_hash = ?
            WHERE credential_id = ?
            """,
            (_now(), tx_hash(tx), _credential_id(credential_document)),
        )
    return tx, entry


@dataclass(frozen=True)
class HolderFlowResult:
    issuer_account: str
    holder_account: str
    credential_type: str
    credential_create_hash: str
    credential_accept_hash: str
    credential_delete_hash: str
    verify_before_accept_ok: bool
    active_before_accept: bool
    verify_after_accept_ok: bool
    active_after_accept: bool
    verify_after_delete_ok: bool
    active_after_delete: bool
    sd_presentation_after_accept_ok: bool | None = None
    sd_selective_presentation_after_accept_ok: bool | None = None
    sd_presentation_after_delete_ok: bool | None = None
    selected_disclosure_count: int | None = None
    total_disclosure_count: int | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "issuer_account": self.issuer_account,
            "holder_account": self.holder_account,
            "credential_type": self.credential_type,
            "credential_create_hash": self.credential_create_hash,
            "credential_accept_hash": self.credential_accept_hash,
            "credential_delete_hash": self.credential_delete_hash,
            "verify_before_accept_ok": self.verify_before_accept_ok,
            "active_before_accept": self.active_before_accept,
            "verify_after_accept_ok": self.verify_after_accept_ok,
            "active_after_accept": self.active_after_accept,
            "verify_after_delete_ok": self.verify_after_delete_ok,
            "active_after_delete": self.active_after_delete,
            "sd_presentation_after_accept_ok": self.sd_presentation_after_accept_ok,
            "sd_selective_presentation_after_accept_ok": self.sd_selective_presentation_after_accept_ok,
            "sd_presentation_after_delete_ok": self.sd_presentation_after_delete_ok,
            "selected_disclosure_count": self.selected_disclosure_count,
            "total_disclosure_count": self.total_disclosure_count,
        }


def _digest_sri(data: bytes) -> str:
    return "sha384-" + base64.b64encode(hashlib.sha384(data).digest()).decode("ascii")


def _sdjwt_legal_entity_claims() -> dict[str, Any]:
    return {
        "kyc": {"jurisdiction": "KR", "assuranceLevel": "STANDARD"},
        "legalEntity": {
            "type": "STOCK_COMPANY",
            "name": "KYvC Holder Test Co.",
            "registrationNumber": "110111-7654321",
        },
        "representative": {"name": "Holder Tester", "birthDate": "1980-01-01", "nationality": "KR"},
        "beneficialOwners": [{"name": "Beneficial Owner", "birthDate": "1975-02-03", "nationality": "KR"}],
        "documentEvidence": [
            {
                "documentId": "urn:kyvc:doc:business-registration",
                "documentType": "KR_BUSINESS_REGISTRATION_CERTIFICATE",
                "digestSRI": _digest_sri(b"holder-test-business-registration"),
                "mediaType": "application/pdf",
                "byteSize": len(b"holder-test-business-registration"),
                "evidenceFor": ["legalEntity.name"],
            },
            {
                "documentId": "urn:kyvc:doc:corporate-register",
                "documentType": "KR_CORPORATE_REGISTER_FULL_CERTIFICATE",
                "digestSRI": _digest_sri(b"holder-test-corporate-register"),
                "mediaType": "application/pdf",
                "byteSize": len(b"holder-test-corporate-register"),
                "evidenceFor": ["legalEntity.registrationNumber"],
            },
            {
                "documentId": "urn:kyvc:doc:shareholder-register",
                "documentType": "KR_SHAREHOLDER_REGISTER",
                "digestSRI": _digest_sri(b"holder-test-shareholder-register"),
                "mediaType": "application/pdf",
                "byteSize": len(b"holder-test-shareholder-register"),
                "evidenceFor": ["beneficialOwners"],
            },
        ],
    }


def _save_sdjwt_challenge(repository: HolderTestRepository, *, nonce: str, aud: str) -> None:
    issued_at = datetime.now(tz=UTC)
    repository.save_verification_challenge(
        challenge=nonce,
        domain=aud,
        expires_at=issued_at + timedelta(minutes=5),
        created_at=issued_at,
    )


def _selected_holder_disclosures(disclosures: list[str]) -> list[str]:
    selected: list[str] = []
    for disclosure in disclosures:
        decoded = json.loads(b64url_decode(disclosure))
        if len(decoded) == 3:
            _, claim_name, claim_value = decoded
            if claim_name in {"type", "name", "birthDate", "nationality", "jurisdiction", "assuranceLevel"}:
                selected.append(disclosure)
            elif claim_name == "verifiedAt":
                continue
            elif claim_name == "registrationNumber":
                continue
        elif len(decoded) == 2:
            _, claim_value = decoded
            if isinstance(claim_value, dict) and claim_value.get("documentId"):
                continue
            selected.append(disclosure)
    return selected


def run_xrpl_holder_flow(
    *,
    xrpl_json_rpc_url: str,
    faucet_host: str | None,
    db_path: Path,
    allow_mainnet: bool = False,
) -> HolderFlowResult:
    enforce_mainnet_policy(xrpl_json_rpc_url, False, allow_mainnet)
    client = make_client(xrpl_json_rpc_url)
    repository = HolderTestRepository()

    issuer_wallet = generate_funded_wallet(client, faucet_host)
    holder_wallet = generate_funded_wallet(client, faucet_host)
    issuer_key = generate_private_key()
    holder_key = generate_private_key()
    holder_did = did_from_account(holder_wallet.address)

    issuer = IssuerService(
        issuer_wallet.address,
        issuer_key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    issuer.register_did_document()
    repository.save_did_document(
        holder_did,
        holder_diddoc(holder_did, "holder-test-key-1", private_key_to_jwk(holder_key)),
    )

    valid_until = datetime.now(tz=UTC) + timedelta(days=1)
    credential = issuer.issue_kyc_vc(
        holder_account=holder_wallet.address,
        holder_did=holder_did,
        claims={"kycLevel": "BASIC", "jurisdiction": "KR", "amlScreened": True},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=valid_until,
        persist_status=False,
    )
    credential_document = _credential_document(credential)
    credential_type = str(credential_document["credentialStatus"]["credentialType"])

    create_tx = submit_credential_create(
        client,
        issuer_wallet,
        holder_wallet.address,
        credential_type,
        valid_until,
    )
    entry_before_accept = get_xrpl_credential_entry(
        client,
        issuer_wallet.address,
        holder_wallet.address,
        credential_type,
    )
    sync_status_mirror(
        repository,
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
        entry=entry_before_accept,
    )

    verifier = VerifierService(
        repository,
        status_lookup=lambda issuer_account, holder_account, credential_type_hex: get_xrpl_credential_entry(
            client,
            issuer_account,
            holder_account,
            credential_type_hex,
        ),
        verification_logger=lambda subject_id, result, verified_at: repository.save_verification_result(
            subject_id=subject_id,
            ok=result.ok,
            errors=result.errors,
            details=result.details,
            verified_at=verified_at,
        ),
    )
    before_accept = verifier.verify_vc(credential)

    holder_store_credential(db_path, credential)
    accept_tx, entry_after_accept = holder_submit_credential_accept(
        client=client,
        db_path=db_path,
        holder_seed=wallet_seed(holder_wallet),
        credential=credential,
    )
    sync_status_mirror(
        repository,
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
        entry=entry_after_accept,
    )
    after_accept = verifier.verify_vc(credential)

    delete_tx = submit_credential_delete(
        client,
        issuer_wallet,
        holder_wallet.address,
        credential_type,
    )
    remove_status_mirror(
        repository,
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
    )
    entry_after_delete = get_xrpl_credential_entry(
        client,
        issuer_wallet.address,
        holder_wallet.address,
        credential_type,
    )
    after_delete = verifier.verify_vc(credential)

    return HolderFlowResult(
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
        credential_create_hash=tx_hash(create_tx),
        credential_accept_hash=tx_hash(accept_tx),
        credential_delete_hash=tx_hash(delete_tx),
        verify_before_accept_ok=before_accept.ok,
        active_before_accept=is_credential_active(entry_before_accept, datetime.now(tz=UTC)),
        verify_after_accept_ok=after_accept.ok,
        active_after_accept=is_credential_active(entry_after_accept, datetime.now(tz=UTC)),
        verify_after_delete_ok=after_delete.ok,
        active_after_delete=is_credential_active(entry_after_delete, datetime.now(tz=UTC)),
    )


def run_xrpl_sdjwt_holder_flow(
    *,
    xrpl_json_rpc_url: str,
    faucet_host: str | None,
    db_path: Path,
    allow_mainnet: bool = False,
) -> HolderFlowResult:
    enforce_mainnet_policy(xrpl_json_rpc_url, False, allow_mainnet)
    client = make_client(xrpl_json_rpc_url)
    repository = HolderTestRepository()

    issuer_wallet = generate_funded_wallet(client, faucet_host)
    holder_wallet = generate_funded_wallet(client, faucet_host)
    issuer_key = generate_private_key()
    holder_key = generate_private_key()
    holder_did = did_from_account(holder_wallet.address)
    holder_key_id = "holder-test-key-1"

    issuer = IssuerService(
        issuer_wallet.address,
        issuer_key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    issuer.register_did_document()
    repository.save_did_document(
        holder_did,
        holder_diddoc(holder_did, holder_key_id, private_key_to_jwk(holder_key)),
    )

    valid_until = datetime.now(tz=UTC) + timedelta(days=1)
    credential, status, _ = issuer.issue_kyc_sd_jwt(
        holder_account=holder_wallet.address,
        holder_did=holder_did,
        holder_key_id=holder_key_id,
        claims=_sdjwt_legal_entity_claims(),
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=valid_until,
        persist_status=False,
    )
    credential_document = _credential_document(credential)
    credential_type = str(status["credentialType"])

    create_tx = submit_credential_create(
        client,
        issuer_wallet,
        holder_wallet.address,
        credential_type,
        valid_until,
    )
    entry_before_accept = get_xrpl_credential_entry(
        client,
        issuer_wallet.address,
        holder_wallet.address,
        credential_type,
    )
    sync_status_mirror(
        repository,
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
        entry=entry_before_accept,
    )

    verifier = VerifierService(
        repository,
        status_lookup=lambda issuer_account, holder_account, credential_type_hex: get_xrpl_credential_entry(
            client,
            issuer_account,
            holder_account,
            credential_type_hex,
        ),
        verification_logger=lambda subject_id, result, verified_at: repository.save_verification_result(
            subject_id=subject_id,
            ok=result.ok,
            errors=result.errors,
            details=result.details,
            verified_at=verified_at,
        ),
    )
    before_accept = verifier.verify_sd_jwt_credential(credential)

    holder_store_credential(db_path, credential)
    accept_tx, entry_after_accept = holder_submit_credential_accept(
        client=client,
        db_path=db_path,
        holder_seed=wallet_seed(holder_wallet),
        credential=credential,
    )
    sync_status_mirror(
        repository,
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
        entry=entry_after_accept,
    )
    after_accept = verifier.verify_sd_jwt_credential(credential)

    parsed = parse_sd_jwt(credential)
    presented_without_kb = f"{parsed.issuer_jwt}~{'~'.join(parsed.disclosures)}"
    aud = "https://holder-test.verifier.example"
    nonce_after_accept = "holder-test-sdjwt-nonce-after-accept"
    _save_sdjwt_challenge(repository, nonce=nonce_after_accept, aud=aud)
    kb_after_accept = create_kb_jwt(
        private_key=holder_key,
        verification_method=f"{holder_did}#{holder_key_id}",
        aud=aud,
        nonce=nonce_after_accept,
        presented_sd_jwt_without_kb=presented_without_kb,
    )
    sd_presentation_after_accept = verifier.verify_sd_jwt_presentation(
        {
            "format": "kyvc-sd-jwt-presentation-v1",
            "aud": aud,
            "nonce": nonce_after_accept,
            "sdJwtKb": f"{presented_without_kb}~{kb_after_accept}",
        }
    )

    selected_disclosures = _selected_holder_disclosures(parsed.disclosures)
    selective_presented_without_kb = f"{parsed.issuer_jwt}~{'~'.join(selected_disclosures)}"
    selective_nonce = "holder-test-sdjwt-selective-nonce-after-accept"
    _save_sdjwt_challenge(repository, nonce=selective_nonce, aud=aud)
    selective_kb = create_kb_jwt(
        private_key=holder_key,
        verification_method=f"{holder_did}#{holder_key_id}",
        aud=aud,
        nonce=selective_nonce,
        presented_sd_jwt_without_kb=selective_presented_without_kb,
    )
    sd_selective_presentation_after_accept = verifier.verify_sd_jwt_presentation(
        {
            "format": "kyvc-sd-jwt-presentation-v1",
            "aud": aud,
            "nonce": selective_nonce,
            "sdJwtKb": f"{selective_presented_without_kb}~{selective_kb}",
        },
        policy=SdJwtVerificationPolicy.from_dict(
            {
                "acceptedFormat": "dc+sd-jwt",
                "acceptedVct": ["https://kyvc.example/vct/legal-entity-kyc-v1"],
                "acceptedJurisdictions": ["KR"],
                "minimumAssuranceLevel": "STANDARD",
                "requiredDisclosures": [
                    "legalEntity.type",
                    "representative.name",
                    "representative.birthDate",
                    "representative.nationality",
                    "beneficialOwners[].name",
                    "beneficialOwners[].birthDate",
                    "beneficialOwners[].nationality",
                ],
                "documentRules": [
                    {
                        "id": "no-original-needed-for-selective-holder-test",
                        "required": False,
                        "oneOf": ["KR_OFFICIAL_LETTER"],
                    }
                ],
            }
        ),
    )

    delete_tx = submit_credential_delete(
        client,
        issuer_wallet,
        holder_wallet.address,
        credential_type,
    )
    remove_status_mirror(
        repository,
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
    )
    entry_after_delete = get_xrpl_credential_entry(
        client,
        issuer_wallet.address,
        holder_wallet.address,
        credential_type,
    )
    after_delete = verifier.verify_sd_jwt_credential(credential)

    nonce_after_delete = "holder-test-sdjwt-nonce-after-delete"
    _save_sdjwt_challenge(repository, nonce=nonce_after_delete, aud=aud)
    kb_after_delete = create_kb_jwt(
        private_key=holder_key,
        verification_method=f"{holder_did}#{holder_key_id}",
        aud=aud,
        nonce=nonce_after_delete,
        presented_sd_jwt_without_kb=presented_without_kb,
    )
    sd_presentation_after_delete = verifier.verify_sd_jwt_presentation(
        {
            "format": "kyvc-sd-jwt-presentation-v1",
            "aud": aud,
            "nonce": nonce_after_delete,
            "sdJwtKb": f"{presented_without_kb}~{kb_after_delete}",
        }
    )

    return HolderFlowResult(
        issuer_account=issuer_wallet.address,
        holder_account=holder_wallet.address,
        credential_type=credential_type,
        credential_create_hash=tx_hash(create_tx),
        credential_accept_hash=tx_hash(accept_tx),
        credential_delete_hash=tx_hash(delete_tx),
        verify_before_accept_ok=before_accept.ok,
        active_before_accept=is_credential_active(entry_before_accept, datetime.now(tz=UTC)),
        verify_after_accept_ok=after_accept.ok,
        active_after_accept=is_credential_active(entry_after_accept, datetime.now(tz=UTC)),
        verify_after_delete_ok=after_delete.ok,
        active_after_delete=is_credential_active(entry_after_delete, datetime.now(tz=UTC)),
        sd_presentation_after_accept_ok=sd_presentation_after_accept.ok,
        sd_selective_presentation_after_accept_ok=sd_selective_presentation_after_accept.ok,
        sd_presentation_after_delete_ok=sd_presentation_after_delete.ok,
        selected_disclosure_count=len(selected_disclosures),
        total_disclosure_count=len(parsed.disclosures),
    )


def main() -> None:
    load_dotenv()
    parser = argparse.ArgumentParser(description="Run the holder-side XRPL Credential flow on the configured network.")
    parser.add_argument("--format", choices=["sd-jwt", "legacy-vc-jwt"], default="sd-jwt")
    parser.add_argument(
        "--xrpl-json-rpc-url",
        default=os.environ.get("XRPL_JSON_RPC_URL", "https://s.devnet.rippletest.net:51234"),
    )
    parser.add_argument("--faucet-host", default=os.environ.get("XRPL_FAUCET_HOST") or None)
    parser.add_argument(
        "--db-path",
        default=str(Path(__file__).resolve().parent / "holder-flow.sqlite3"),
    )
    parser.add_argument("--allow-mainnet", action="store_true", default=os.environ.get("ALLOW_MAINNET") == "1")
    args = parser.parse_args()

    if args.format == "sd-jwt":
        result = run_xrpl_sdjwt_holder_flow(
            xrpl_json_rpc_url=args.xrpl_json_rpc_url,
            faucet_host=args.faucet_host,
            db_path=Path(args.db_path),
            allow_mainnet=args.allow_mainnet,
        )
    else:
        result = run_xrpl_holder_flow(
            xrpl_json_rpc_url=args.xrpl_json_rpc_url,
            faucet_host=args.faucet_host,
            db_path=Path(args.db_path),
            allow_mainnet=args.allow_mainnet,
        )
    print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
