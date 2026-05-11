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

export function formatXrp(value?: string | number | null) {
  if (value == null || value === "") return "0 XRP";
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return `${value} XRP`;
  return `${numeric.toLocaleString("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 6,
  })} XRP`;
}

export function readXrpBalance(assets?: WalletAssetsResult | null) {
  if (!assets?.ok || assets.depositRequired) return null;
  return (
    assets.xrpBalanceXrp ??
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
    if (info.ok && info.account) wallet = info;
  } catch {
    wallet = null;
  }

  if (!wallet && authStatus?.walletReady === true) {
    return { wallet: null, assets: null, created: false };
  }

  if (!wallet) {
    const createdWallet = await bridge.createWallet(false);
    if (createdWallet.ok && createdWallet.account) {
      wallet = createdWallet;
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
