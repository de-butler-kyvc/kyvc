from datetime import UTC, datetime
from typing import Any

import httpx
from xrpl.models.transactions import CredentialAccept, CredentialCreate, CredentialDelete, DIDSet
from xrpl.wallet import Wallet

from app.credentials.canonical import canonical_json, multihash_sha2_256
from app.credentials.hexutil import bytes_to_hex, utf8_to_hex
from app.xrpl.client import submit_tx

RIPPLE_EPOCH_OFFSET = 946684800
LSF_ACCEPTED = 0x00010000


def datetime_to_ripple_epoch(dt: datetime) -> int:
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=UTC)
    return int(dt.timestamp()) - RIPPLE_EPOCH_OFFSET


def ripple_epoch_to_datetime(seconds: int) -> datetime:
    return datetime.fromtimestamp(seconds + RIPPLE_EPOCH_OFFSET, tz=UTC)


def submit_did_set(client: Any, wallet: Wallet, diddoc_url: str, did_document: dict) -> dict[str, Any]:
    tx = DIDSet(
        account=wallet.address,
        uri=utf8_to_hex(diddoc_url),
        data=bytes_to_hex(multihash_sha2_256(canonical_json(did_document))),
    )
    return submit_tx(client, tx, wallet, "DIDSet")


def submit_credential_create(
    client: Any,
    issuer_wallet: Wallet,
    subject_account: str,
    credential_type_hex: str,
    expiration: datetime | None = None,
    uri: str | None = None,
) -> dict[str, Any]:
    kwargs: dict[str, Any] = {
        "account": issuer_wallet.address,
        "subject": subject_account,
        "credential_type": credential_type_hex,
    }
    if expiration is not None:
        kwargs["expiration"] = datetime_to_ripple_epoch(expiration)
    if uri is not None:
        kwargs["uri"] = utf8_to_hex(uri)
    return submit_tx(client, CredentialCreate(**kwargs), issuer_wallet, "CredentialCreate")


def submit_credential_accept(
    client: Any,
    holder_wallet: Wallet,
    issuer_account: str,
    credential_type_hex: str,
) -> dict[str, Any]:
    tx = CredentialAccept(
        account=holder_wallet.address,
        issuer=issuer_account,
        credential_type=credential_type_hex,
    )
    return submit_tx(client, tx, holder_wallet, "CredentialAccept")


def submit_credential_delete(
    client: Any,
    issuer_wallet: Wallet,
    subject_account: str,
    credential_type_hex: str,
) -> dict[str, Any]:
    tx = CredentialDelete(
        account=issuer_wallet.address,
        subject=subject_account,
        credential_type=credential_type_hex,
    )
    return submit_tx(client, tx, issuer_wallet, "CredentialDelete")


def _client_url(client: Any) -> str:
    url = getattr(client, "_kyvc_json_rpc_url", None) or getattr(client, "url", None) or getattr(client, "_url", None)
    if not url:
        raise RuntimeError("could not determine JsonRpcClient URL for raw ledger_entry")
    return str(url)


def raw_rpc(client: Any, payload: dict[str, Any]) -> dict[str, Any]:
    if "command" in payload:
        request_payload = {"method": payload["command"], "params": [payload]}
    else:
        request_payload = payload
    response = httpx.post(_client_url(client), json=request_payload, timeout=30)
    response.raise_for_status()
    data = response.json()
    if "result" in data:
        return data["result"]
    return data


def get_did_entry(client: Any, account: str) -> dict[str, Any] | None:
    payload = {"command": "ledger_entry", "did": account, "ledger_index": "validated"}
    result = raw_rpc(client, payload)
    if result.get("error") in {"entryNotFound", "objectNotFound"}:
        return None
    node = result.get("node") or result.get("node_binary") or result
    return node if isinstance(node, dict) else None


def get_credential_entry(
    client: Any,
    issuer_account: str,
    subject_account: str,
    credential_type_hex: str,
) -> dict[str, Any] | None:
    payload = {
        "command": "ledger_entry",
        "credential": {
            "subject": subject_account,
            "issuer": issuer_account,
            "credential_type": credential_type_hex,
        },
        "ledger_index": "validated",
    }
    result = raw_rpc(client, payload)
    if result.get("error") in {"entryNotFound", "objectNotFound"}:
        return None
    node = result.get("node") or result
    return node if isinstance(node, dict) else None


def is_credential_active(entry: dict | None, now: datetime) -> bool:
    if not entry:
        return False
    flags = int(entry.get("Flags", entry.get("flags", 0)))
    if flags & LSF_ACCEPTED != LSF_ACCEPTED:
        return False
    expiration = entry.get("Expiration")
    if expiration is not None and int(expiration) <= datetime_to_ripple_epoch(now):
        return False
    return True

