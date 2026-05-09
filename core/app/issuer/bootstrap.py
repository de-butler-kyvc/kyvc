import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.credentials.crypto import private_key_from_pem
from app.credentials.resolver import did_document_hash
from app.issuer.service import IssuerService
from app.xrpl.client import enforce_mainnet_policy, make_client
from app.xrpl.ledger import get_did_entry, submit_did_set
from app.xrpl.wallets import fund_wallet, generate_funded_wallet, wallet_from_seed, wallet_seed

logger = logging.getLogger(__name__)

DEV_LIKE_ENVS = {"dev", "development", "local", "staging", "stage"}
PROD_LIKE_ENVS = {"prod", "production"}


@dataclass
class IssuerDidRegistration:
    issuer_account: str
    issuer_did: str
    diddoc_url: str
    did_document: dict[str, Any]
    did_set_transaction: dict[str, Any]


@dataclass
class IssuerBootstrapResult:
    status: str = "DEGRADED"
    configured: bool = False
    detail: str = "issuer bootstrap has not run"
    issuer_account: str | None = None
    issuer_did: str | None = None
    runtime_issuer_seed: str | None = None
    events: list[str] = field(default_factory=list)

    def add(self, message: str) -> None:
        self.events.append(message)
        self.detail = "; ".join(self.events)

    def up(self, message: str) -> "IssuerBootstrapResult":
        self.status = "UP"
        self.configured = True
        self.add(message)
        return self

    def degraded(self, message: str, *, configured: bool | None = None) -> "IssuerBootstrapResult":
        self.status = "DEGRADED"
        if configured is not None:
            self.configured = configured
        self.add(message)
        return self


def default_enabled_for_env(app_env: str) -> bool:
    normalized = app_env.strip().lower()
    if normalized in PROD_LIKE_ENVS:
        return False
    if normalized in DEV_LIKE_ENVS:
        return True
    return False


def effective_flag(value: bool | None, app_env: str) -> bool:
    return value if value is not None else default_enabled_for_env(app_env)


def diddoc_url(base_url: str, issuer_account: str) -> str:
    return f"{base_url.rstrip('/')}/dids/{issuer_account}/diddoc.json"


def read_issuer_private_key_pem(settings: Settings) -> str:
    path = settings.issuer_private_key_pem_path
    if not path:
        raise RuntimeError("issuer private key PEM is required. Set ISSUER_PRIVATE_KEY_PEM_PATH.")
    try:
        return Path(path).expanduser().read_text(encoding="ascii")
    except OSError as exc:
        raise RuntimeError(f"issuer private key PEM file could not be read: {path}") from exc


def create_funded_issuer_wallet(client: Any, faucet_host: str | None) -> tuple[str, str]:
    wallet = generate_funded_wallet(client, faucet_host)
    return wallet.address, wallet_seed(wallet)


def register_issuer_did_with_wallet(
    *,
    client: Any,
    wallet: Any,
    private_key_pem: str,
    key_id: str,
    did_doc_base_url: str,
    repository: Any | None,
    submit_func: Any = submit_did_set,
) -> IssuerDidRegistration:
    service = IssuerService(
        issuer_account=wallet.address,
        private_key=private_key_from_pem(private_key_pem),
        key_id=key_id,
        did_document_repository=repository,
    )
    did_document = service.register_did_document()
    url = diddoc_url(did_doc_base_url, wallet.address)
    tx = submit_func(client, wallet, url, did_document)
    return IssuerDidRegistration(
        issuer_account=wallet.address,
        issuer_did=service.issuer_did,
        diddoc_url=url,
        did_document=did_document,
        did_set_transaction=tx,
    )


def bootstrap_issuer_did(settings: Settings, repository: Any | None = None) -> IssuerBootstrapResult:
    result = IssuerBootstrapResult()
    auto_create = effective_flag(settings.auto_create_issuer_wallet_on_boot, settings.app_env)
    auto_register = effective_flag(settings.auto_register_issuer_did, settings.app_env)
    auto_fund = effective_flag(settings.auto_fund_issuer_on_boot, settings.app_env)
    seed = settings.xrpl_issuer_seed
    seed_was_configured = bool(seed)

    try:
        rpc_url = settings.xrpl_json_rpc_url.strip()
        if not rpc_url:
            return result.degraded("XRPL not ready: XRPL JSON-RPC URL is not configured", configured=bool(seed))
        enforce_mainnet_policy(rpc_url, settings.allow_mainnet, False)
        client = make_client(rpc_url)
    except Exception as exc:
        logger.warning("issuer DID bootstrap read-only setup failed: %s", exc)
        return result.degraded(f"XRPL not ready: {exc}", configured=bool(seed))

    if seed:
        result.configured = True
        result.add("issuer seed configured")
    else:
        result.add("issuer seed is not configured")
        if not auto_create:
            logger.info("issuer wallet auto-create disabled on boot")
            return result.degraded("issuer wallet auto-create disabled", configured=False)
        try:
            logger.info("issuer wallet auto-create write step starting")
            account, seed = create_funded_issuer_wallet(client, settings.xrpl_faucet_host)
            result.runtime_issuer_seed = seed
            result.issuer_account = account
            result.configured = True
            result.add("issuer wallet auto-create succeeded")
            logger.info("issuer wallet auto-create succeeded account=%s", account)
        except Exception as exc:
            logger.warning("issuer wallet auto-create failed: %s", exc)
            return result.degraded(f"issuer wallet auto-create failed: {type(exc).__name__}", configured=False)

    try:
        assert seed is not None
        wallet = wallet_from_seed(seed)
        result.issuer_account = wallet.address
        private_key_pem = read_issuer_private_key_pem(settings)
        service = IssuerService(
            issuer_account=wallet.address,
            private_key=private_key_from_pem(private_key_pem),
            did_document_repository=repository,
        )
        did_document = service.build_did_document()
        expected_hash = did_document_hash(did_document)
        result.issuer_did = service.issuer_did
    except Exception as exc:
        logger.warning("issuer DID bootstrap prerequisite failed: %s", exc)
        return result.degraded(f"issuer DID bootstrap prerequisite failed: {exc}", configured=True)

    try:
        logger.info("issuer DID bootstrap read-only ledger check starting account=%s", wallet.address)
        entry = get_did_entry(client, wallet.address)
    except Exception as exc:
        logger.warning("issuer DID bootstrap read-only ledger check failed: %s", exc)
        return result.degraded(f"XRPL not ready: {exc}", configured=True)

    if entry:
        ledger_hash = str(entry.get("Data") or entry.get("data") or "").upper()
        if ledger_hash == expected_hash:
            if repository is not None:
                repository.save_did_document(service.issuer_did, did_document)
            return result.up("DID registered and hash matched")
        logger.warning(
            "issuer DID hash mismatch; auto-register and auto-funding are forbidden account=%s ledger=%s generated=%s",
            wallet.address,
            ledger_hash,
            expected_hash,
        )
        return result.degraded(
            f"DID hash mismatch: ledger={ledger_hash} generated={expected_hash}",
            configured=True,
        )

    result.add("DID missing")
    if not auto_register:
        logger.info("issuer DID missing and auto-register disabled")
        return result.degraded("DID missing and auto-register disabled", configured=True)

    try:
        logger.info("issuer DID auto-register write step starting account=%s", wallet.address)
        registration = register_issuer_did_with_wallet(
            client=client,
            wallet=wallet,
            private_key_pem=private_key_pem,
            key_id="issuer-key-1",
            did_doc_base_url=settings.did_doc_base_url,
            repository=repository,
        )
        result.issuer_did = registration.issuer_did
        return result.up("DID auto-register succeeded")
    except Exception as exc:
        if not _is_insufficient_funds_error(exc):
            logger.warning("issuer DID auto-register attempted but failed: %s", exc)
            return result.degraded(
                f"DID auto-register attempted but failed: {type(exc).__name__}",
                configured=True,
            )
        result.add("issuer funding required for DIDSet")

    if not seed_was_configured:
        logger.info("issuer DIDSet needed funding after auto-created wallet; not attempting separate auto-funding")
        return result.degraded(
            "DID auto-register attempted but failed: insufficient XRP after wallet auto-create",
            configured=True,
        )

    if not auto_fund:
        logger.info("issuer funding required but auto-funding disabled")
        return result.degraded("issuer funding required but auto-funding disabled", configured=True)

    try:
        logger.info("issuer funding write step starting account=%s", wallet.address)
        fund_wallet(client, wallet, settings.xrpl_faucet_host)
        result.add("issuer funding attempted and succeeded")
        logger.info("issuer funding succeeded account=%s", wallet.address)
    except Exception as exc:
        logger.warning("issuer funding attempted but failed: %s", exc)
        return result.degraded(f"issuer funding attempted but failed: {type(exc).__name__}", configured=True)

    try:
        logger.info("issuer DID auto-register retry write step starting account=%s", wallet.address)
        registration = register_issuer_did_with_wallet(
            client=client,
            wallet=wallet,
            private_key_pem=private_key_pem,
            key_id="issuer-key-1",
            did_doc_base_url=settings.did_doc_base_url,
            repository=repository,
        )
        result.issuer_did = registration.issuer_did
        return result.up("DID auto-register succeeded")
    except Exception as exc:
        logger.warning("issuer DID auto-register retry failed: %s", exc)
        return result.degraded(f"DID auto-register attempted but failed: {type(exc).__name__}", configured=True)


def _is_insufficient_funds_error(exc: Exception) -> bool:
    detail = str(exc).lower()
    markers = (
        "tecinsuf",
        "insufficient",
        "insuf",
        "reserve",
        "tecno_dst_insuf_xrp",
        "tecinsufficientreserve",
    )
    return any(marker in detail for marker in markers)
