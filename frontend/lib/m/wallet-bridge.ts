import {
  bridge,
  isBridgeAvailable,
  type WalletAssetsResult,
  type WalletInfo,
} from "@/lib/m/android-bridge";

export type MobileWalletState = {
  wallet: WalletInfo | null;
  assets: WalletAssetsResult | null;
  created: boolean;
};

function normalizeWalletInfo(wallet: WalletInfo): WalletInfo {
  const holderAccount = wallet.holderAccount ?? wallet.account;
  const holderDid = wallet.holderDid ?? wallet.did;
  return {
    ...wallet,
    ...(holderAccount ? { account: holderAccount, holderAccount } : {}),
    ...(holderDid ? { did: holderDid, holderDid } : {}),
  };
}

export function formatXrp(value?: string | number | null) {
  if (value == null || value === "") return "0 XRP";
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return `${value} XRP`;
  return `${numeric.toLocaleString("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 6,
  })} XRP`;
}

export const DROPS_PER_XRP = BigInt(1_000_000);

export function dropsToXrp(drops?: string | number | null) {
  if (drops == null || drops === "") return null;
  const raw = String(drops);
  if (!/^\d+$/.test(raw)) return null;
  const padded = raw.padStart(7, "0");
  const whole = padded.slice(0, -6).replace(/^0+(?=\d)/, "") || "0";
  const fraction = padded.slice(-6).replace(/0+$/, "");
  return fraction ? `${whole}.${fraction}` : whole;
}

export function parseXrpToDrops(value?: string | number | null) {
  if (value == null || value === "") return null;
  const [wholeRaw, fractionRaw = ""] = String(value).split(".");
  const whole = wholeRaw && /^\d+$/.test(wholeRaw) ? BigInt(wholeRaw) : null;
  if (whole == null) return null;
  const fraction = fractionRaw.replace(/\D/g, "").slice(0, 6).padEnd(6, "0");
  return whole * DROPS_PER_XRP + BigInt(fraction || "0");
}

export function xrpBalanceDropsFromAssets(assets?: WalletAssetsResult | null) {
  if (!assets?.ok || assets.depositRequired) return null;
  if (
    (typeof assets.xrpBalanceDrops === "string" ||
      typeof assets.xrpBalanceDrops === "number") &&
    /^\d+$/.test(String(assets.xrpBalanceDrops))
  ) {
    return BigInt(String(assets.xrpBalanceDrops));
  }
  return parseXrpToDrops(readXrpBalance(assets));
}

export function formatXrpDrops(value?: bigint | null) {
  if (value == null) return "-";
  const safe = value < BigInt(0) ? BigInt(0) : value;
  const whole = safe / DROPS_PER_XRP;
  const fraction = (safe % DROPS_PER_XRP).toString().padStart(6, "0");
  const trimmed = fraction.replace(/0+$/, "");
  return `${whole.toLocaleString("en-US")}${trimmed ? `.${trimmed}` : ""} XRP`;
}

export function readXrpBalance(assets?: WalletAssetsResult | null) {
  if (!assets?.ok) return null;
  if (assets.depositRequired) return "0";
  return (
    assets.xrpBalanceXrp ??
    dropsToXrp(assets.xrpBalanceDrops) ??
    (assets.xrpBalance as string | number | undefined) ??
    (assets.balanceXrp as string | number | undefined) ??
    (assets.availableXrp as string | number | undefined) ??
    null
  );
}

export async function ensureMobileWallet(): Promise<MobileWalletState> {
  if (!isBridgeAvailable()) {
    return { wallet: null, assets: null, created: false };
  }

  let wallet: WalletInfo | null = null;
  let created = false;
  const authStatus = await bridge.getAuthStatus().catch(() => null);

  try {
    const info = await bridge.getWalletInfo();
    const account = info.holderAccount ?? info.account;
    if (info.ok && account) wallet = normalizeWalletInfo(info);
  } catch {
    wallet = null;
  }

  if (!wallet && authStatus?.walletReady === true) {
    return { wallet: null, assets: null, created: false };
  }

  if (!wallet) {
    const createdWallet = await bridge.createWallet(false);
    const account = createdWallet.holderAccount ?? createdWallet.account;
    if (createdWallet.ok && account) {
      wallet = normalizeWalletInfo(createdWallet);
      created = true;
    } else {
      throw new Error(createdWallet.error ?? "지갑 생성에 실패했습니다.");
    }
  }

  let assets: WalletAssetsResult | null = null;
  try {
    assets = await bridge.getWalletAssets();
  } catch {
    assets = null;
  }

  return { wallet, assets, created };
}
