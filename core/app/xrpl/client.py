import json
from typing import Any

from xrpl.clients import JsonRpcClient
from xrpl.transaction import submit_and_wait
from xrpl.wallet import Wallet

MAINNET_HOST_MARKERS = ("xrplcluster.com", "s1.ripple.com", "s2.ripple.com")


def make_client(url: str) -> JsonRpcClient:
    client = JsonRpcClient(url)
    setattr(client, "_kyvc_json_rpc_url", url)
    return client


def is_mainnet_url(url: str) -> bool:
    lowered = url.lower()
    return any(marker in lowered for marker in MAINNET_HOST_MARKERS)


def enforce_mainnet_policy(url: str, allow_mainnet_env: bool, allow_mainnet_flag: bool) -> None:
    if not is_mainnet_url(url):
        return
    if not allow_mainnet_env:
        raise RuntimeError("Refusing to connect to XRPL Mainnet. Set ALLOW_MAINNET=1 only if you understand the risk.")
    if not allow_mainnet_flag:
        raise RuntimeError("Mainnet URL configured. Pass allow_mainnet=True to acknowledge the risk.")


def _response_to_dict(response: Any) -> dict[str, Any]:
    if isinstance(response, dict):
        return response
    result = getattr(response, "result", None)
    if isinstance(result, dict):
        return result
    to_dict = getattr(response, "to_dict", None)
    if callable(to_dict):
        data = to_dict()
        if isinstance(data, dict):
            return data.get("result", data)
    return {"raw_response": repr(response)}


def get_tx_result(response: Any) -> str:
    data = _response_to_dict(response)
    meta = data.get("meta") or data.get("metaData") or {}
    return str(
        meta.get("TransactionResult")
        or data.get("engine_result")
        or data.get("engineResult")
        or data.get("transaction_result")
        or ""
    )


def detect_tem_disabled(response: Any) -> bool:
    return "temDISABLED" in json.dumps(_response_to_dict(response), default=str)


def assert_tes_success(response: Any, label: str) -> dict[str, Any]:
    data = _response_to_dict(response)
    result = get_tx_result(data)
    if result == "tesSUCCESS":
        return data
    if detect_tem_disabled(data):
        raise RuntimeError(f"{label} failed because the selected XRPL network has the amendment disabled")
    raise RuntimeError(f"{label} failed with result={result or 'unknown'} response={json.dumps(data, default=str)}")


def submit_tx(client: JsonRpcClient, tx: Any, wallet: Wallet, label: str) -> dict[str, Any]:
    try:
        response = submit_and_wait(tx, client, wallet)
    except Exception as exc:
        if "temDISABLED" in str(exc):
            raise RuntimeError(f"{label} failed because the selected XRPL network has the amendment disabled") from exc
        raise
    return assert_tes_success(response, label)


def tx_hash(response: dict[str, Any]) -> str:
    return str(
        response.get("hash")
        or response.get("tx_json", {}).get("hash")
        or response.get("transaction", {}).get("hash")
        or ""
    )
