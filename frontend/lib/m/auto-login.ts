"use client";

import { auth } from "@/lib/api";
export {
  readMobileAutoLoginEnabled,
  setMobileAutoLoginEnabled,
} from "@/lib/m/auto-login-storage";
import { readMobileAutoLoginEnabled } from "@/lib/m/auto-login-storage";
import { ensureMobileWallet } from "@/lib/m/wallet-bridge";
import { bindCurrentWebUserWithPrompt } from "@/lib/m/wallet-owner";

type MobileAutoLoginOptions = {
  requireEnabled?: boolean;
};

export async function tryMobileAutoLogin(options: MobileAutoLoginOptions = {}) {
  const { requireEnabled = true } = options;
  if (requireEnabled && !readMobileAutoLoginEnabled()) return null;

  const res = await auth.mobileAutoLogin();
  if (!res.autoLogin || typeof res.userId !== "number") return null;

  await bindCurrentWebUserWithPrompt({
    userId: res.userId,
    email: res.email,
  });
  await ensureMobileWallet().catch(() => null);

  return res;
}
