"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";

import { auth } from "@/lib/api";
import { isBridgeAvailable, type BridgeResult } from "@/lib/m/android-bridge";
import {
  bindSessionWalletOwner,
  clearWalletUiState,
  logoutForWalletOwnerMismatch,
  WalletOwnerMismatchError,
} from "@/lib/m/wallet-owner";
import {
  clearWalletOwnerDialog,
  readWalletOwnerDialog,
  showWalletOwnerDialog,
  WALLET_OWNER_DIALOG_EVENT,
  type WalletOwnerDialogDetail,
} from "@/lib/m/wallet-owner-dialog";

export default function WalletOwnerGate({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [blockingDialog, setBlockingDialog] =
    useState<WalletOwnerDialogDetail | null>(() => readWalletOwnerDialog());

  useEffect(() => {
    let cancelled = false;

    const blockAndLogout = async (error: WalletOwnerMismatchError) => {
      showWalletOwnerDialog({ title: error.title, hint: error.hint });
      if (!cancelled) {
        setReady(true);
        setBlockingDialog({ title: error.title, hint: error.hint });
      }
      await logoutForWalletOwnerMismatch();
    };

    const onMismatch = (event: Event) => {
      const detail = (event as CustomEvent<BridgeResult>).detail;
      void blockAndLogout(new WalletOwnerMismatchError(detail));
    };

    window.addEventListener("kyvc-wallet-owner-mismatch", onMismatch);
    const onDialog = (event: Event) => {
      setBlockingDialog((event as CustomEvent<WalletOwnerDialogDetail>).detail);
    };
    window.addEventListener(WALLET_OWNER_DIALOG_EVENT, onDialog);

    (async () => {
      try {
        if (!isBridgeAvailable()) {
          if (!cancelled) setReady(true);
          return;
        }

        const session = await auth.session().catch(() => null);
        if (!session?.authenticated) {
          if (!cancelled) setReady(true);
          return;
        }

        const result = await bindSessionWalletOwner(session);
        if (result?.walletAccess === "binding_required" && !cancelled) {
          setMessage("기존 지갑 연결 확인이 필요합니다.");
        }
        if (!cancelled) setReady(true);
      } catch (error) {
        if (error instanceof WalletOwnerMismatchError) {
          await blockAndLogout(error);
          return;
        }
        if (!cancelled) {
          setMessage(error instanceof Error ? error.message : "지갑 사용자 확인에 실패했습니다.");
          setReady(true);
        }
      }
    })();

    return () => {
      cancelled = true;
      window.removeEventListener("kyvc-wallet-owner-mismatch", onMismatch);
      window.removeEventListener(WALLET_OWNER_DIALOG_EVENT, onDialog);
    };
  }, [router]);

  if (!ready) {
    return (
      <div className="m-shell">
        <div className="m-loading">지갑 사용자 확인 중...</div>
      </div>
    );
  }

  return (
    <>
      {message ? <div className="m-error" style={{ margin: 16 }}>{message}</div> : null}
      {blockingDialog ? (
        <WalletOwnerBlockingDialog
          title={blockingDialog.title}
          hint={blockingDialog.hint}
          onConfirm={() => {
            clearWalletOwnerDialog();
            setBlockingDialog(null);
            router.replace("/m/login");
          }}
        />
      ) : null}
      {children}
    </>
  );
}

export function showWalletOwnerMismatchDialog(error: WalletOwnerMismatchError) {
  showWalletOwnerDialog({ title: error.title, hint: error.hint });
}

function WalletOwnerBlockingDialog({
  title,
  hint,
  onConfirm,
}: {
  title: string;
  hint: string;
  onConfirm: () => void;
}) {
  return (
    <div className="wallet-owner-dialog-layer" role="dialog" aria-modal="true">
      <div className="wallet-owner-dialog">
        <div className="wallet-owner-dialog-icon">!</div>
        <h2>{title}</h2>
        <p>{hint}</p>
        <button type="button" onClick={onConfirm}>
          로그인으로 이동
        </button>
      </div>
    </div>
  );
}
