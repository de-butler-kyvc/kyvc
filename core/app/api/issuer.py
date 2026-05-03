from pathlib import Path

from fastapi import APIRouter, HTTPException, Request

from app.credentials.crypto import private_key_from_pem
from app.issuer.api_models import (
    GenerateIssuerWalletRequest,
    GenerateIssuerWalletResponse,
    IssueKycCredentialRequest,
    IssueKycCredentialResponse,
    RegisterIssuerDidRequest,
    RegisterIssuerDidResponse,
    RevokeCredentialRequest,
    RevokeCredentialResponse,
)
from app.issuer.service import IssuerService
from app.xrpl.client import enforce_mainnet_policy, make_client
from app.xrpl.ledger import (
    datetime_to_ripple_epoch,
    get_credential_entry as get_xrpl_credential_entry,
    submit_credential_create,
    submit_credential_delete,
    submit_did_set,
)
from app.xrpl.status import remove_status_mirror, sync_status_mirror
from app.xrpl.wallets import generate_funded_wallet, wallet_from_seed, wallet_seed

router = APIRouter(prefix="/issuer", tags=["issuer"])


def _diddoc_url(base_url: str, issuer_account: str) -> str:
    return f"{base_url.rstrip('/')}/dids/{issuer_account}/diddoc.json"


def _issuer_private_key_pem(payload_value: str | None, settings) -> str:
    if payload_value:
        return payload_value

    path = settings.issuer_private_key_pem_path
    if not path:
        raise HTTPException(
            status_code=400,
            detail="issuer private key PEM is required. Set ISSUER_PRIVATE_KEY_PEM_PATH or pass issuer_private_key_pem.",
        )

    try:
        return Path(path).expanduser().read_text(encoding="ascii")
    except OSError as exc:
        raise HTTPException(
            status_code=400,
            detail=f"issuer private key PEM file could not be read: {path}",
        ) from exc


def _issuer_seed(payload_value: str | None, settings, *, detail: str) -> str:
    seed = payload_value or settings.xrpl_issuer_seed
    if not seed:
        raise HTTPException(status_code=400, detail=detail)
    return seed


@router.post("/wallets", response_model=GenerateIssuerWalletResponse)
def generate_issuer_wallet(
    payload: GenerateIssuerWalletRequest,
    request: Request,
) -> GenerateIssuerWalletResponse:
    settings = request.app.state.settings
    rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
    enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
    client = make_client(rpc_url)
    wallet = generate_funded_wallet(client, payload.faucet_host or settings.xrpl_faucet_host)
    return GenerateIssuerWalletResponse(
        account=wallet.address,
        seed=wallet_seed(wallet),
        network_rpc_url=rpc_url,
        warning="Store this seed in core/.env as XRPL_ISSUER_SEED. Do not commit it.",
    )


@router.post("/did/register", response_model=RegisterIssuerDidResponse)
def register_issuer_did(
    payload: RegisterIssuerDidRequest,
    request: Request,
) -> RegisterIssuerDidResponse:
    settings = request.app.state.settings
    seed = _issuer_seed(
        payload.issuer_seed,
        settings,
        detail="issuer seed is required. Set XRPL_ISSUER_SEED or pass issuer_seed.",
    )
    private_key_pem = _issuer_private_key_pem(payload.issuer_private_key_pem, settings)

    rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
    enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
    client = make_client(rpc_url)
    wallet = wallet_from_seed(seed)

    service = IssuerService(
        issuer_account=wallet.address,
        private_key=private_key_from_pem(private_key_pem),
        key_id=payload.key_id,
        did_document_repository=request.app.state.repository,
    )
    did_document = service.register_did_document()
    diddoc_url = _diddoc_url(payload.did_doc_base_url or settings.did_doc_base_url, wallet.address)
    tx = submit_did_set(client, wallet, diddoc_url, did_document)
    return RegisterIssuerDidResponse(
        issuer_account=wallet.address,
        issuer_did=service.issuer_did,
        diddoc_url=diddoc_url,
        did_document=did_document,
        did_set_transaction=tx,
    )


@router.post("/credentials/kyc", response_model=IssueKycCredentialResponse)
def issue_kyc_credential(payload: IssueKycCredentialRequest, request: Request) -> IssueKycCredentialResponse:
    settings = request.app.state.settings
    repository = request.app.state.repository
    issuer_account = payload.issuer_account
    issuer_wallet = None
    client = None
    private_key_pem = _issuer_private_key_pem(payload.issuer_private_key_pem, settings)
    if payload.status_mode == "xrpl":
        seed = _issuer_seed(
            payload.issuer_seed,
            settings,
            detail="issuer seed is required for XRPL mode. Set XRPL_ISSUER_SEED or pass issuer_seed.",
        )
        rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
        try:
            enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
        except RuntimeError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        client = make_client(rpc_url)
        issuer_wallet = wallet_from_seed(seed)
        if issuer_account is not None and issuer_account != issuer_wallet.address:
            raise HTTPException(status_code=400, detail="issuer_account does not match issuer_seed wallet address")
        issuer_account = issuer_wallet.address
    elif issuer_account is None and (payload.issuer_seed or settings.xrpl_issuer_seed):
        issuer_account = wallet_from_seed(payload.issuer_seed or settings.xrpl_issuer_seed).address
    elif issuer_account is None:
        raise HTTPException(status_code=400, detail="issuer_account is required for local status mode")

    service = IssuerService(
        issuer_account=issuer_account,
        issuer_did=payload.issuer_did,
        private_key=private_key_from_pem(private_key_pem),
        key_id=payload.key_id,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    issuer_did_document = service.register_did_document() if payload.store_issuer_did_document else None
    credential = service.issue_kyc_vc(
        holder_account=payload.holder_account,
        holder_did=payload.holder_did,
        claims=payload.claims,
        valid_from=payload.valid_from,
        valid_until=payload.valid_until,
        persist=payload.persist,
        persist_status=payload.persist_status and payload.status_mode == "local",
        mark_status_accepted=payload.mark_status_accepted,
        status_uri=payload.status_uri,
    )
    status = credential["credentialStatus"]
    credential_type = str(status["credentialType"])
    create_tx = None
    ledger_entry = None
    if payload.status_mode == "xrpl":
        assert client is not None and issuer_wallet is not None
        try:
            create_tx = submit_credential_create(
                client,
                issuer_wallet,
                payload.holder_account,
                credential_type,
                payload.valid_until,
                payload.status_uri,
            )
        except RuntimeError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        ledger_entry = get_xrpl_credential_entry(client, issuer_account, payload.holder_account, credential_type)
        if payload.persist_status:
            sync_status_mirror(
                repository,
                issuer_account=issuer_account,
                holder_account=payload.holder_account,
                credential_type=credential_type,
                entry=ledger_entry,
                fallback_flags=0,
                fallback_expiration=datetime_to_ripple_epoch(payload.valid_until),
                fallback_uri=payload.status_uri,
            )
    return IssueKycCredentialResponse(
        credential=credential,
        issuer_did_document=issuer_did_document,
        credential_type=credential_type,
        vc_core_hash=str(status["vcCoreHash"]),
        credential_create_transaction=create_tx,
        ledger_entry=ledger_entry,
        status_mode=payload.status_mode,
    )


@router.post("/credentials/revoke", response_model=RevokeCredentialResponse)
def revoke_credential(payload: RevokeCredentialRequest, request: Request) -> RevokeCredentialResponse:
    settings = request.app.state.settings
    repository = request.app.state.repository
    issuer_account = payload.issuer_account
    delete_tx = None
    ledger_entry = None
    if payload.status_mode == "xrpl":
        seed = _issuer_seed(
            payload.issuer_seed,
            settings,
            detail="issuer seed is required for XRPL mode. Set XRPL_ISSUER_SEED or pass issuer_seed.",
        )
        rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
        try:
            enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
        except RuntimeError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        client = make_client(rpc_url)
        issuer_wallet = wallet_from_seed(seed)
        if issuer_account is not None and issuer_account != issuer_wallet.address:
            raise HTTPException(status_code=400, detail="issuer_account does not match issuer_seed wallet address")
        issuer_account = issuer_wallet.address
        try:
            delete_tx = submit_credential_delete(
                client,
                issuer_wallet,
                payload.holder_account,
                payload.credential_type,
            )
        except RuntimeError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc
        ledger_entry = get_xrpl_credential_entry(client, issuer_account, payload.holder_account, payload.credential_type)
    elif issuer_account is None:
        raise HTTPException(status_code=400, detail="issuer_account is required for local status mode")

    remove_status_mirror(
        repository,
        issuer_account=issuer_account,
        holder_account=payload.holder_account,
        credential_type=payload.credential_type,
    )
    return RevokeCredentialResponse(
        revoked=ledger_entry is None,
        credential_delete_transaction=delete_tx,
        ledger_entry=ledger_entry,
        status_mode=payload.status_mode,
    )
