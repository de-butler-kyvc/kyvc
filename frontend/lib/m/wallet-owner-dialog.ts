"use client";

export type WalletOwnerDialogDetail = {
  title: string;
  hint: string;
};

export const WALLET_OWNER_DIALOG_EVENT = "kyvc-wallet-owner-dialog";

export function showWalletOwnerDialog(detail: WalletOwnerDialogDetail) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(
    new CustomEvent<WalletOwnerDialogDetail>(WALLET_OWNER_DIALOG_EVENT, {
      detail,
    }),
  );
}
