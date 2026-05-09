from typing import Any

from xrpl.wallet import Wallet, generate_faucet_wallet


def wallet_from_seed(seed: str) -> Wallet:
    return Wallet.from_seed(seed)


def wallet_seed(wallet: Wallet) -> str:
    seed = getattr(wallet, "seed", None) or getattr(wallet, "private_key", None)
    if not seed:
        raise RuntimeError("xrpl-py Wallet did not expose a seed")
    return str(seed)


def generate_funded_wallet(client: Any, faucet_host: str | None = None) -> Wallet:
    kwargs: dict[str, Any] = {}
    if faucet_host:
        kwargs["faucet_host"] = faucet_host
    return generate_faucet_wallet(client, **kwargs)


def fund_wallet(client: Any, wallet: Wallet, faucet_host: str | None = None) -> Wallet:
    kwargs: dict[str, Any] = {"wallet": wallet}
    if faucet_host:
        kwargs["faucet_host"] = faucet_host
    return generate_faucet_wallet(client, **kwargs)
