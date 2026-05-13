from pathlib import Path

from fastapi import APIRouter, HTTPException, Request

from app.credentials.resolver import did_document_hash
from app.credentials.crypto import decode_compact_jws, private_key_from_pem
from app.credentials.vc import decode_vc_jwt
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
from app.issuer.bootstrap import create_funded_issuer_wallet, diddoc_url, register_issuer_did_with_wallet
from app.issuer.service import IssuerService
from app.sdjwt.issuer import DEFAULT_LEGAL_ENTITY_KYC_VCT
from app.status.sdjwt_status import credential_type_hex_for_sdjwt
from app.xrpl.client import enforce_mainnet_policy, make_client
from app.xrpl.ledger import (
    datetime_to_ripple_epoch,
    get_account_info as get_xrpl_account_info,
    get_credential_entry as get_xrpl_credential_entry,
    get_did_entry,
    submit_credential_create,
    submit_credential_delete,
    submit_did_set,
)
from app.xrpl.status import remove_status_mirror, sync_status_mirror
from app.xrpl.wallets import wallet_from_seed

router = APIRouter(prefix="/issuer", tags=["issuer"])


def _diddoc_url(base_url: str, issuer_account: str) -> str:
    return diddoc_url(base_url, issuer_account)


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


def _issuer_seed(payload_value: str | None, settings, *, detail: str, runtime_seed: str | None = None) -> str:
    seed = payload_value or settings.xrpl_issuer_seed or runtime_seed
    if not seed:
        raise HTTPException(status_code=400, detail=detail)
    return seed


def _runtime_issuer_seed(request: Request) -> str | None:
    return getattr(request.app.state, "runtime_issuer_seed", None)


def _default_credential_format(claims: dict) -> str:
    legal_entity_keys = {
        "kyc",
        "legalEntity",
        "representative",
        "beneficialOwners",
        "establishmentPurpose",
        "documentEvidence",
    }
    return "dc+sd-jwt" if any(key in claims for key in legal_entity_keys) else "vc+jwt"


def _issuer_did_document_for_issue(
    *,
    service: IssuerService,
    repository,
    status_mode: str,
    client,
    issuer_account: str,
    store: bool,
) -> dict | None:
    if not store:
        return None
    did_document = service.build_did_document()
    if status_mode == "xrpl":
        if client is None:
            raise HTTPException(status_code=400, detail="XRPL client is required to validate issuer DID Document")
        entry = get_did_entry(client, issuer_account)
        if not entry:
            raise HTTPException(
                status_code=400,
                detail="issuer DID is not registered on XRPL. Call /issuer/did/register before issuing credentials.",
            )
        ledger_hash = str(entry.get("Data") or entry.get("data") or "").upper()
        generated_hash = did_document_hash(did_document)
        if ledger_hash != generated_hash:
            raise HTTPException(
                status_code=400,
                detail=(
                    "issuer DID Document hash mismatch with XRPL DIDSet: "
                    f"ledger={ledger_hash} generated={generated_hash}. "
                    "Re-register the issuer DID with the current issuer key, or issue with the key that matches "
                    "the DID Document currently registered on XRPL."
                ),
            )
    repository.save_did_document(service.issuer_did, did_document)
    return did_document


@router.post("/wallets", response_model=GenerateIssuerWalletResponse)
def generate_issuer_wallet(
    payload: GenerateIssuerWalletRequest,
    request: Request,
) -> GenerateIssuerWalletResponse:
    settings = request.app.state.settings
    rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
    enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
    client = make_client(rpc_url)
    account, seed = create_funded_issuer_wallet(client, payload.faucet_host or settings.xrpl_faucet_host)
    return GenerateIssuerWalletResponse(
        account=account,
        seed=seed,
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
        runtime_seed=_runtime_issuer_seed(request),
    )
    private_key_pem = _issuer_private_key_pem(payload.issuer_private_key_pem, settings)

    rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
    enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
    client = make_client(rpc_url)
    wallet = wallet_from_seed(seed)

    registration = register_issuer_did_with_wallet(
        client=client,
        wallet=wallet,
        private_key_pem=private_key_pem,
        key_id=payload.key_id,
        did_doc_base_url=payload.did_doc_base_url or settings.did_doc_base_url,
        repository=request.app.state.repository,
        submit_func=submit_did_set,
    )
    return RegisterIssuerDidResponse(
        issuer_account=registration.issuer_account,
        issuer_did=registration.issuer_did,
        diddoc_url=registration.diddoc_url,
        did_document=registration.did_document,
        did_set_transaction=registration.did_set_transaction,
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
            runtime_seed=_runtime_issuer_seed(request),
        )
        rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
        enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
        client = make_client(rpc_url)
        issuer_wallet = wallet_from_seed(seed)
        if issuer_account is not None and issuer_account != issuer_wallet.address:
            raise HTTPException(status_code=400, detail="issuer_account does not match issuer_seed wallet address")
        issuer_account = issuer_wallet.address
    elif issuer_account is None and (payload.issuer_seed or settings.xrpl_issuer_seed or _runtime_issuer_seed(request)):
        issuer_account = wallet_from_seed(
            payload.issuer_seed or settings.xrpl_issuer_seed or _runtime_issuer_seed(request),
        ).address
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
    issuer_did_document = _issuer_did_document_for_issue(
        service=service,
        repository=repository,
        status_mode=payload.status_mode,
        client=client,
        issuer_account=issuer_account,
        store=payload.store_issuer_did_document,
    )
    selected_format = payload.format or _default_credential_format(payload.claims)
    if selected_format == "dc+sd-jwt":
        credential, status, disclosable_paths = service.issue_kyc_sd_jwt(
            holder_account=payload.holder_account,
            holder_did=payload.holder_did,
            holder_key_id=payload.holder_key_id,
            claims=payload.claims,
            valid_from=payload.valid_from,
            valid_until=payload.valid_until,
            vct=payload.vct or DEFAULT_LEGAL_ENTITY_KYC_VCT,
            persist=payload.persist,
            persist_status=payload.persist_status and payload.status_mode == "local",
            mark_status_accepted=payload.mark_status_accepted,
            status_uri=payload.status_uri,
        )
        credential_id = str(decode_compact_jws(credential.split("~", 1)[0])[1]["jti"])
        vc_core_hash = None
    else:
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
            credential_format=payload.credential_format,
        )
        credential_document = decode_vc_jwt(credential)[1] if isinstance(credential, str) else credential
        status = credential_document["credentialStatus"]
        disclosable_paths = []
        credential_id = str(credential_document.get("id", ""))
        vc_core_hash = str(status["vcCoreHash"])
    credential_type = str(status["credentialType"])
    create_tx = None
    ledger_entry = None
    if payload.status_mode == "xrpl":
        assert client is not None and issuer_wallet is not None
        if get_xrpl_account_info(client, payload.holder_account) is None:
            raise HTTPException(
                status_code=400,
                detail=(
                    "holder_account was not found on the configured XRPL network. "
                    "Fund or create the holder account before issuing XRPL-backed credentials."
                ),
            )
        create_tx = submit_credential_create(
            client,
            issuer_wallet,
            payload.holder_account,
            credential_type,
            payload.valid_until,
            payload.status_uri,
        )
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
        format=selected_format,
        credential=credential,
        credentialId=credential_id,
        issuer_did_document=issuer_did_document,
        credential_type=credential_type,
        vc_core_hash=vc_core_hash,
        status=status,
        selectiveDisclosure={"disclosablePaths": disclosable_paths} if selected_format == "dc+sd-jwt" else None,
        credential_create_transaction=create_tx,
        ledger_entry=ledger_entry,
        status_mode=payload.status_mode,
    )


@router.post("/credentials/revoke", response_model=RevokeCredentialResponse)
def revoke_credential(payload: RevokeCredentialRequest, request: Request) -> RevokeCredentialResponse:
    settings = request.app.state.settings
    repository = request.app.state.repository
    issuer_account = payload.issuer_account
    credential_type = payload.credential_type
    if credential_type is None and (payload.jti or payload.status_id):
        selected_issuer_did = payload.issuer_did
        if selected_issuer_did is None and issuer_account is not None:
            selected_issuer_did = f"did:xrpl:1:{issuer_account}"
        selected_holder_did = payload.holder_did
        if selected_holder_did is None:
            selected_holder_did = f"did:xrpl:1:{payload.holder_account}"
        if selected_issuer_did is None:
            raise HTTPException(status_code=400, detail="issuer_account or issuer_did is required to revoke by jti/status_id")
        jti = payload.jti or str(payload.status_id).removeprefix("urn:kyvc:status:")
        if not str(jti).startswith("urn:uuid:"):
            jti = f"urn:uuid:{jti}"
        credential_type = credential_type_hex_for_sdjwt(
            issuer_did=selected_issuer_did,
            holder_did=selected_holder_did,
            vct=payload.vct or DEFAULT_LEGAL_ENTITY_KYC_VCT,
            jti=str(jti),
        )
    if credential_type is None:
        raise HTTPException(status_code=400, detail="credential_type or jti/status_id is required")
    delete_tx = None
    ledger_entry = None
    if payload.status_mode == "xrpl":
        seed = _issuer_seed(
            payload.issuer_seed,
            settings,
            detail="issuer seed is required for XRPL mode. Set XRPL_ISSUER_SEED or pass issuer_seed.",
            runtime_seed=_runtime_issuer_seed(request),
        )
        rpc_url = payload.xrpl_json_rpc_url or settings.xrpl_json_rpc_url
        enforce_mainnet_policy(rpc_url, settings.allow_mainnet, payload.allow_mainnet)
        client = make_client(rpc_url)
        issuer_wallet = wallet_from_seed(seed)
        if issuer_account is not None and issuer_account != issuer_wallet.address:
            raise HTTPException(status_code=400, detail="issuer_account does not match issuer_seed wallet address")
        issuer_account = issuer_wallet.address
        delete_tx = submit_credential_delete(
            client,
            issuer_wallet,
            payload.holder_account,
            credential_type,
        )
        ledger_entry = get_xrpl_credential_entry(client, issuer_account, payload.holder_account, credential_type)
    elif issuer_account is None:
        raise HTTPException(status_code=400, detail="issuer_account is required for local status mode")

    remove_status_mirror(
        repository,
        issuer_account=issuer_account,
        holder_account=payload.holder_account,
        credential_type=credential_type,
    )
    return RevokeCredentialResponse(
        revoked=ledger_entry is None,
        credential_delete_transaction=delete_tx,
        ledger_entry=ledger_entry,
        status_mode=payload.status_mode,
    )
