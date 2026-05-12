"use client";

export type WalletOwnerDialogDetail = {
  title: string;
  hint: string;
};

export const WALLET_OWNER_DIALOG_EVENT = "kyvc-wallet-owner-dialog";
const WALLET_OWNER_DIALOG_KEY = "kyvc.m.walletOwnerDialog";

export function showWalletOwnerDialog(detail: WalletOwnerDialogDetail) {
  if (typeof window === "undefined") return;
  window.sessionStorage.setItem(WALLET_OWNER_DIALOG_KEY, JSON.stringify(detail));
  window.dispatchEvent(
    new CustomEvent<WalletOwnerDialogDetail>(WALLET_OWNER_DIALOG_EVENT, {
      detail,
    }),
  );
}

export function readWalletOwnerDialog() {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.sessionStorage.getItem(WALLET_OWNER_DIALOG_KEY);
    return raw ? (JSON.parse(raw) as WalletOwnerDialogDetail) : null;
  } catch {
    return null;
  }
}

export function clearWalletOwnerDialog() {
  if (typeof window === "undefined") return;
  window.sessionStorage.removeItem(WALLET_OWNER_DIALOG_KEY);
}
