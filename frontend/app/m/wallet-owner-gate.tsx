"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";

import { auth } from "@/lib/api";
import { isBridgeAvailable, type BridgeResult } from "@/lib/m/android-bridge";
import {
  bindSessionWalletOwner,
  logoutForWalletOwnerMismatch,
  WalletOwnerMismatchError,
} from "@/lib/m/wallet-owner";

export default function WalletOwnerGate({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const blockAndLogout = async (error: WalletOwnerMismatchError) => {
      if (!cancelled) setMessage(`${error.title}\n${error.hint}`);
      await logoutForWalletOwnerMismatch();
      if (!cancelled) {
        window.alert(`${error.title}\n${error.hint}`);
        router.replace("/m/login");
      }
    };

    const onMismatch = (event: Event) => {
      const detail = (event as CustomEvent<BridgeResult>).detail;
      void blockAndLogout(new WalletOwnerMismatchError(detail));
    };

    window.addEventListener("kyvc-wallet-owner-mismatch", onMismatch);

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
      {children}
    </>
  );
}
