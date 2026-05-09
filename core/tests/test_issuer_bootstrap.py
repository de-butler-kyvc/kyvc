from types import SimpleNamespace

from app.core.config import Settings
from app.credentials.crypto import generate_private_key, private_key_to_pem
from app.issuer import bootstrap
from app.issuer.bootstrap import bootstrap_issuer_did


class Repository:
    def __init__(self):
        self.saved = []

    def save_did_document(self, did, did_document):
        self.saved.append((did, did_document))


def _settings(**kwargs):
    return Settings(xrpl_json_rpc_url="https://s.devnet.rippletest.net:51234", **kwargs)


def _patch_common(monkeypatch, *, account="rIssuer", did_entry=None):
    monkeypatch.setattr(bootstrap, "make_client", lambda rpc_url: object())
    monkeypatch.setattr(bootstrap, "wallet_from_seed", lambda seed: SimpleNamespace(address=account))
    monkeypatch.setattr(bootstrap, "read_issuer_private_key_pem", lambda settings: private_key_to_pem(generate_private_key()))
    monkeypatch.setattr(bootstrap, "get_did_entry", lambda client, issuer_account: did_entry)


def test_dev_without_issuer_seed_auto_create_unset_creates_wallet(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    monkeypatch.setattr(bootstrap, "create_funded_issuer_wallet", lambda client, faucet_host: ("rCreated", "sCreated"))
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: SimpleNamespace(issuer_did="did:xrpl:1:rCreated"),
    )

    result = bootstrap_issuer_did(_settings(app_env="dev", xrpl_issuer_seed=None), Repository())

    assert result.runtime_issuer_seed == "sCreated"
    assert "issuer wallet auto-create succeeded" in result.detail


def test_prod_without_issuer_seed_auto_create_unset_is_degraded(monkeypatch):
    _patch_common(monkeypatch)

    result = bootstrap_issuer_did(_settings(app_env="prod", xrpl_issuer_seed=None), Repository())

    assert result.status == "DEGRADED"
    assert result.configured is False
    assert "issuer wallet auto-create disabled" in result.detail


def test_explicit_auto_create_false_in_dev_does_not_create(monkeypatch):
    _patch_common(monkeypatch)

    result = bootstrap_issuer_did(
        _settings(app_env="dev", xrpl_issuer_seed=None, auto_create_issuer_wallet_on_boot=False),
        Repository(),
    )

    assert result.status == "DEGRADED"
    assert "issuer wallet auto-create disabled" in result.detail


def test_explicit_auto_create_true_in_prod_creates(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    monkeypatch.setattr(bootstrap, "create_funded_issuer_wallet", lambda client, faucet_host: ("rCreated", "sCreated"))
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: SimpleNamespace(issuer_did="did:xrpl:1:rCreated"),
    )

    result = bootstrap_issuer_did(
        _settings(app_env="prod", xrpl_issuer_seed=None, auto_create_issuer_wallet_on_boot=True),
        Repository(),
    )

    assert result.runtime_issuer_seed == "sCreated"
    assert "issuer wallet auto-create succeeded" in result.detail


def test_dev_did_missing_auto_register_unset_registers(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    calls = []
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: calls.append(kwargs) or SimpleNamespace(issuer_did="did:xrpl:1:rIssuer"),
    )

    result = bootstrap_issuer_did(_settings(app_env="dev", xrpl_issuer_seed="sIssuer"), Repository())

    assert result.status == "UP"
    assert calls
    assert "DID auto-register succeeded" in result.detail


def test_prod_did_missing_auto_register_unset_does_not_register(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)

    result = bootstrap_issuer_did(_settings(app_env="prod", xrpl_issuer_seed="sIssuer"), Repository())

    assert result.status == "DEGRADED"
    assert "DID missing and auto-register disabled" in result.detail


def test_explicit_auto_register_false_in_dev_does_not_register(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)

    result = bootstrap_issuer_did(
        _settings(app_env="dev", xrpl_issuer_seed="sIssuer", auto_register_issuer_did=False),
        Repository(),
    )

    assert result.status == "DEGRADED"
    assert "DID missing and auto-register disabled" in result.detail


def test_explicit_auto_register_true_in_prod_registers(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    calls = []
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: calls.append(kwargs) or SimpleNamespace(issuer_did="did:xrpl:1:rIssuer"),
    )

    result = bootstrap_issuer_did(
        _settings(app_env="prod", xrpl_issuer_seed="sIssuer", auto_register_issuer_did=True),
        Repository(),
    )

    assert result.status == "UP"
    assert calls


def test_did_mismatch_never_registers_or_funds(monkeypatch):
    _patch_common(monkeypatch, did_entry={"Data": "LEDGER_HASH"})
    monkeypatch.setattr(bootstrap, "did_document_hash", lambda did_document: "GENERATED_HASH")
    monkeypatch.setattr(bootstrap, "register_issuer_did_with_wallet", lambda **kwargs: (_ for _ in ()).throw(AssertionError()))
    monkeypatch.setattr(bootstrap, "fund_wallet", lambda *args, **kwargs: (_ for _ in ()).throw(AssertionError()))

    result = bootstrap_issuer_did(_settings(app_env="dev", xrpl_issuer_seed="sIssuer"), Repository())

    assert result.status == "DEGRADED"
    assert "DID hash mismatch" in result.detail


def test_did_exists_and_hash_matches_is_up(monkeypatch):
    _patch_common(monkeypatch, did_entry={"Data": "MATCH"})
    monkeypatch.setattr(bootstrap, "did_document_hash", lambda did_document: "MATCH")

    result = bootstrap_issuer_did(_settings(app_env="dev", xrpl_issuer_seed="sIssuer"), Repository())

    assert result.status == "UP"
    assert "DID registered and hash matched" in result.detail


def test_dev_missing_did_insufficient_xrp_auto_funds_and_retries(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    calls = []

    def register(**kwargs):
        calls.append(kwargs)
        if len(calls) == 1:
            raise RuntimeError("DIDSet failed with result=tecINSUFFICIENT_RESERVE")
        return SimpleNamespace(issuer_did="did:xrpl:1:rIssuer")

    monkeypatch.setattr(bootstrap, "register_issuer_did_with_wallet", register)
    monkeypatch.setattr(bootstrap, "fund_wallet", lambda client, wallet, faucet_host: wallet)

    result = bootstrap_issuer_did(_settings(app_env="dev", xrpl_issuer_seed="sIssuer"), Repository())

    assert result.status == "UP"
    assert len(calls) == 2
    assert "issuer funding attempted and succeeded" in result.detail


def test_prod_missing_did_insufficient_xrp_auto_funding_unset_is_degraded(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: (_ for _ in ()).throw(RuntimeError("DIDSet failed with result=tecINSUFFICIENT_RESERVE")),
    )

    result = bootstrap_issuer_did(
        _settings(app_env="prod", xrpl_issuer_seed="sIssuer", auto_register_issuer_did=True),
        Repository(),
    )

    assert result.status == "DEGRADED"
    assert "issuer funding required but auto-funding disabled" in result.detail


def test_explicit_auto_fund_false_in_dev_does_not_fund(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: (_ for _ in ()).throw(RuntimeError("DIDSet failed with result=tecINSUFFICIENT_RESERVE")),
    )

    result = bootstrap_issuer_did(
        _settings(app_env="dev", xrpl_issuer_seed="sIssuer", auto_fund_issuer_on_boot=False),
        Repository(),
    )

    assert result.status == "DEGRADED"
    assert "issuer funding required but auto-funding disabled" in result.detail


def test_funding_failure_is_degraded(monkeypatch):
    _patch_common(monkeypatch, did_entry=None)
    monkeypatch.setattr(
        bootstrap,
        "register_issuer_did_with_wallet",
        lambda **kwargs: (_ for _ in ()).throw(RuntimeError("DIDSet failed with result=tecINSUFFICIENT_RESERVE")),
    )
    monkeypatch.setattr(
        bootstrap,
        "fund_wallet",
        lambda client, wallet, faucet_host: (_ for _ in ()).throw(RuntimeError("faucet down")),
    )

    result = bootstrap_issuer_did(_settings(app_env="dev", xrpl_issuer_seed="sIssuer"), Repository())

    assert result.status == "DEGRADED"
    assert "issuer funding attempted but failed" in result.detail
