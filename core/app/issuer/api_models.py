from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field


class IssueKycCredentialRequest(BaseModel):
    issuer_account: str | None = None
    issuer_seed: str | None = None
    issuer_private_key_pem: str | None = None
    issuer_did: str | None = None
    key_id: str = Field(default="issuer-key-1")
    holder_account: str
    holder_did: str | None = None
    claims: dict[str, Any] = Field(default_factory=dict)
    valid_from: datetime
    valid_until: datetime
    persist: bool = True
    persist_status: bool = True
    mark_status_accepted: bool = False
    store_issuer_did_document: bool = True
    status_uri: str | None = None
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False
    status_mode: Literal["xrpl", "local"] = "xrpl"
    credential_format: Literal["jwt", "embedded_jws"] = "jwt"


class IssueKycCredentialResponse(BaseModel):
    credential: dict[str, Any] | str
    issuer_did_document: dict[str, Any] | None = None
    credential_type: str
    vc_core_hash: str
    credential_create_transaction: dict[str, Any] | None = None
    ledger_entry: dict[str, Any] | None = None
    status_mode: str


class RevokeCredentialRequest(BaseModel):
    issuer_account: str | None = None
    issuer_seed: str | None = None
    holder_account: str
    credential_type: str
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False
    status_mode: Literal["xrpl", "local"] = "xrpl"


class RevokeCredentialResponse(BaseModel):
    revoked: bool
    credential_delete_transaction: dict[str, Any] | None = None
    ledger_entry: dict[str, Any] | None = None
    status_mode: str


class GenerateIssuerWalletRequest(BaseModel):
    xrpl_json_rpc_url: str | None = None
    faucet_host: str | None = None
    allow_mainnet: bool = False


class GenerateIssuerWalletResponse(BaseModel):
    account: str
    seed: str
    network_rpc_url: str
    warning: str


class RegisterIssuerDidRequest(BaseModel):
    issuer_private_key_pem: str | None = None
    issuer_seed: str | None = None
    key_id: str = Field(default="issuer-key-1")
    did_doc_base_url: str | None = None
    xrpl_json_rpc_url: str | None = None
    allow_mainnet: bool = False


class RegisterIssuerDidResponse(BaseModel):
    issuer_account: str
    issuer_did: str
    diddoc_url: str
    did_document: dict[str, Any]
    did_set_transaction: dict[str, Any]
