from dataclasses import dataclass
from datetime import datetime
from typing import Any, Protocol


@dataclass(frozen=True)
class CredentialStatusEntry:
    issuer: str
    subject: str
    credential_type: str
    flags: int
    expiration: int | None = None
    uri: str | None = None

    def to_ledger_dict(self) -> dict[str, Any]:
        data: dict[str, Any] = {
            "Issuer": self.issuer,
            "Subject": self.subject,
            "CredentialType": self.credential_type,
            "Flags": self.flags,
        }
        if self.expiration is not None:
            data["Expiration"] = self.expiration
        if self.uri is not None:
            data["URI"] = self.uri
        return data


@dataclass(frozen=True)
class VerificationChallengeEntry:
    challenge: str
    domain: str
    expires_at: datetime
    used_at: datetime | None = None
    created_at: datetime | None = None


class CredentialRepository(Protocol):
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
        """Persist an issued VC."""


class StatusRepository(Protocol):
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
        """Persist a local status mirror."""

    def get_credential_entry(
        self,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
    ) -> dict[str, Any] | None:
        """Return a ledger-shaped credential entry, or None."""


class DidDocumentRepository(Protocol):
    def save_did_document(self, did: str, did_document: dict[str, Any]) -> None:
        """Persist a DID Document."""

    def get_did_document(self, did: str) -> dict[str, Any] | None:
        """Return a DID Document, or None."""


class VerificationLogRepository(Protocol):
    def save_verification_result(
        self,
        *,
        subject_id: str | None,
        ok: bool,
        errors: list[str],
        details: dict[str, Any],
        verified_at: datetime,
    ) -> None:
        """Persist a verifier result."""


class VerificationChallengeRepository(Protocol):
    def save_verification_challenge(
        self,
        *,
        challenge: str,
        domain: str,
        expires_at: datetime,
        created_at: datetime,
    ) -> None:
        """Persist a verifier-issued presentation challenge."""

    def get_verification_challenge(self, challenge: str) -> VerificationChallengeEntry | None:
        """Return a verifier-issued presentation challenge, or None."""

    def mark_verification_challenge_used(self, challenge: str, used_at: datetime) -> bool:
        """Mark a challenge as used if it has not already been used."""
