from typing import Literal

from fastapi import APIRouter, Query, Request

from app.credential_status.api_models import CredentialStatusResponse
from app.credential_status.service import CredentialStatusService
from app.xrpl.client import enforce_mainnet_policy, make_client
from app.xrpl.ledger import get_credential_entry as get_xrpl_credential_entry

router = APIRouter(prefix="/credential-status", tags=["credential-status"])


@router.get(
    "/credentials/{issuer_account}/{holder_account}/{credential_type}",
    response_model=CredentialStatusResponse,
)
def get_credential_status(
    issuer_account: str,
    holder_account: str,
    credential_type: str,
    request: Request,
    xrpl_json_rpc_url: str | None = None,
    allow_mainnet: bool = False,
    status_mode: Literal["xrpl", "local"] = Query(default="xrpl"),
) -> CredentialStatusResponse:
    repository = request.app.state.repository
    if status_mode == "local":
        status_lookup = repository
    else:
        settings = request.app.state.settings
        rpc_url = xrpl_json_rpc_url or settings.xrpl_json_rpc_url
        enforce_mainnet_policy(rpc_url, settings.allow_mainnet, allow_mainnet)
        client = make_client(rpc_url)

        def status_lookup(issuer: str, holder: str, credential_type_hex: str):
            return get_xrpl_credential_entry(client, issuer, holder, credential_type_hex)

    service = CredentialStatusService(status_lookup)
    return CredentialStatusResponse(
        **service.get_status(
            issuer_account=issuer_account,
            holder_account=holder_account,
            credential_type=credential_type,
        )
    )
