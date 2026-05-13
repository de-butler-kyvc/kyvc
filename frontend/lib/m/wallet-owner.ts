"use client";

import { auth, type SessionResponse } from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  isWalletOwnerMismatch,
  type SetCurrentWebUserResult,
} from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

export class WalletOwnerMismatchError extends Error {
  title: string;
  hint: string;

  constructor(result?: Partial<SetCurrentWebUserResult>) {
    const forceOwnerMismatchMessage =
      result?.walletAccess === "previous_wallet_deleted";
    const title =
      !forceOwnerMismatchMessage && typeof result?.errorTitle === "string"
        ? result.errorTitle
        : "다른 사용자의 지갑이 있습니다.";
    const hint =
      !forceOwnerMismatchMessage && typeof result?.errorHint === "string"
        ? result.errorHint
        : "해당 사용자로 로그인해 지갑을 삭제한 뒤 다시 시도하세요.";
    super(`${title} ${hint}`);
    this.name = "WalletOwnerMismatchError";
    this.title = title;
    this.hint = hint;
  }
}

export function maskEmail(value?: string | null) {
  if (!value || !value.includes("@")) return undefined;
  const [name, domain] = value.split("@");
  if (!name || !domain) return undefined;
  const head = name.slice(0, 2);
  return `${head}${"*".repeat(Math.max(2, name.length - head.length))}@${domain}`;
}

export function clearWalletUiState() {
  mSession.writeScanResult(null);
  mSession.writeVpRequest(null);
  mSession.writeSelectedVcId(null);
  mSession.writeXrpTransfer(null);
  mSession.writeXrpTransferResult(null);
  mSession.writeVcIssueResult(null);
}

export async function logoutForWalletOwnerMismatch() {
  clearWalletUiState();
  await Promise.all([
    auth.logout().catch(() => null),
    isBridgeAvailable() ? bridge.logout().catch(() => null) : Promise.resolve(null),
  ]);
}

export async function bindCurrentWebUser(params: {
  userId: string | number;
  email?: string | null;
  bindIfUnbound?: boolean;
}) {
  if (!isBridgeAvailable()) return null;

  const result = await bridge.setCurrentWebUser({
    userId: params.userId,
    displayHint: maskEmail(params.email),
    bindIfUnbound: params.bindIfUnbound ?? false,
  });

  if (isWalletOwnerMismatch(result)) {
    throw new WalletOwnerMismatchError(result);
  }

  if (
    result.ok &&
    (result.walletAccess === "allowed" || result.walletAccess === "no_wallet")
  ) {
    return result;
  }

  if (result.ok && result.walletAccess === "binding_required") {
    return result;
  }

  throw new Error(result.error ?? "지갑 사용자 확인에 실패했습니다.");
}

export async function bindCurrentWebUserWithPrompt(params: {
  userId: string | number;
  email?: string | null;
}) {
  const result = await bindCurrentWebUser({ ...params, bindIfUnbound: false });
  if (result?.walletAccess !== "binding_required") return result;

  const confirmed = window.confirm(
    "기존 앱 버전에서 생성된 지갑이 있습니다.\n현재 로그인 계정에 이 지갑을 연결하시겠습니까?",
  );
  if (!confirmed) {
    throw new Error("기존 지갑 연결이 필요합니다.");
  }

  const status = await bridge.getAuthStatus();
  if (!status.sessionUnlocked) {
    const method = status.availableMethods?.includes("biometric")
      ? "biometric"
      : status.availableMethods?.includes("pin")
        ? "pin"
        : status.availableMethods?.includes("pattern")
          ? "pattern"
          : null;
    if (!method) throw new Error("사용 가능한 네이티브 인증 수단이 없습니다.");

    const authResult = await bridge.requestNativeAuth(method, "wallet-login");
    if (!authResult.ok || !authResult.authenticated) {
      throw new Error(authResult.error ?? "네이티브 인증에 실패했습니다.");
    }
  }

  return bindCurrentWebUser({ ...params, bindIfUnbound: true });
}

export async function bindSessionWalletOwner(session?: SessionResponse | null) {
  if (!session?.authenticated || typeof session.userId !== "number") return null;
  return bindCurrentWebUserWithPrompt({
    userId: session.userId,
    email: session.email,
  });
}
