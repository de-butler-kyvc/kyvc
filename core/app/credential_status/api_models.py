from typing import Any

from pydantic import BaseModel


class CredentialStatusResponse(BaseModel):
    issuer_account: str
    holder_account: str
    credential_type: str
    found: bool
    active: bool
    entry: dict[str, Any] | None
    checked_at: str
