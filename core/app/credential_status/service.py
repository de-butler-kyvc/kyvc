from datetime import UTC, datetime
from typing import Any, Protocol
from collections.abc import Callable

from app.xrpl.ledger import is_credential_active


class CredentialStatusLookup(Protocol):
    def get_credential_entry(
        self,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
    ) -> dict[str, Any] | None:
        """Return a ledger-shaped credential entry, or None."""


class CredentialStatusService:
    def __init__(self, status_lookup: CredentialStatusLookup | Callable[[str, str, str], dict[str, Any] | None]):
        self.status_lookup = status_lookup

    def get_status(
        self,
        *,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
        now: datetime | None = None,
    ) -> dict[str, Any]:
        checked_at = (now or datetime.now(tz=UTC)).astimezone(UTC)
        if callable(self.status_lookup):
            entry = self.status_lookup(issuer_account, holder_account, credential_type)
        else:
            entry = self.status_lookup.get_credential_entry(issuer_account, holder_account, credential_type)
        return {
            "issuer_account": issuer_account,
            "holder_account": holder_account,
            "credential_type": credential_type,
            "found": entry is not None,
            "active": is_credential_active(entry, checked_at),
            "entry": entry,
            "checked_at": checked_at.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        }
