"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MToggle, MTopBar } from "@/components/m/parts";
import { ApiError, auth, corporate, type CorporateProfile } from "@/lib/api";
import { clearKyvcLocalStorage } from "@/lib/kyc-flow";
import { bridge, isBridgeAvailable, type AuthStatus } from "@/lib/m/android-bridge";
import { setMobileAutoLoginEnabled } from "@/lib/m/auto-login";
import { mSession } from "@/lib/m/session";
import { clearWalletUiState } from "@/lib/m/wallet-owner";

export default function MobileSettingsPage() {
  const router = useRouter();
  const [bio, setBio] = useState(true);
  const [push, setPush] = useState(true);
  const [profile, setProfile] = useState<CorporateProfile | null>(() => {
    const cached = mSession.readCorporateProfile();
    return cached
      ? {
          corporateId: 0,
          corporateName: cached.corporateName,
          businessRegistrationNo: cached.businessRegistrationNo,
          representativeName: "",
        }
      : null;
  });
  const [toast, setToast] = useState("");
  const [toastClosing, setToastClosing] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingWallet, setDeletingWallet] = useState(false);

  const showToast = (message: string) => {
    setToastClosing(false);
    setToast(message);
    window.setTimeout(() => setToastClosing(true), 1400);
    window.setTimeout(() => setToast(""), 1600);
  };

  const openNativePinSettings = async () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 사용할 수 있는 기능입니다");
      return;
    }
    const requested = bridge.requestPinReset("user-request");
    if (!requested) showToast("PIN 설정을 시작할 수 없습니다");
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        if (mSession.readCorporateProfile()) return;
        const res = await corporate.me();
        if (!cancelled) {
          setProfile(res);
          mSession.writeCorporateProfile({
            corporateName: res.corporateName,
            businessRegistrationNo: res.businessRegistrationNo,
            cachedAt: Date.now(),
          });
        }
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/m/login");
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [router]);

  const nativeAuthMethod = (status: AuthStatus) => {
    if (status.availableMethods?.includes("biometric")) return "biometric";
    if (status.availableMethods?.includes("pin")) return "pin";
    if (status.availableMethods?.includes("pattern")) return "pattern";
    return null;
  };

  const ensureNativeSession = async () => {
    const status = await bridge.getAuthStatus();
    if (status.sessionUnlocked) return;
    const method = nativeAuthMethod(status);
    if (!method) throw new Error("사용 가능한 네이티브 인증 수단이 없습니다.");
    const result = await bridge.requestNativeAuth(method, "wallet-login");
    if (!result.ok || !result.authenticated) {
      throw new Error(result.error ?? "네이티브 인증에 실패했습니다.");
    }
  };

  const onConfirmDeleteAndLogout = async () => {
    setDeletingWallet(true);
    try {
      if (isBridgeAvailable()) {
        await ensureNativeSession();
        const result = await bridge.logoutAndDeleteLocalWalletData();
        if (!result.ok) {
          throw new Error(result.error ?? "지갑 삭제와 로그아웃에 실패했습니다.");
        }
      }
      await auth.logout().catch(() => null);
      setMobileAutoLoginEnabled(false);
      clearWalletUiState();
      mSession.clearAll();
      clearKyvcLocalStorage();
      router.replace("/m");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "지갑 삭제와 로그아웃에 실패했습니다.");
    } finally {
      setDeletingWallet(false);
      setDeleteConfirmOpen(false);
    }
  };

  const onLogout = async () => {
    setDeleteConfirmOpen(true);
  };

  const onWebOnlyLogout = async () => {
    await auth.logout().catch(() => null);
    setMobileAutoLoginEnabled(false);
    clearWalletUiState();
    mSession.clearAll();
    clearKyvcLocalStorage();
    router.replace("/m");
  };

  return (
    <section className="view wash settings-view">
      <MTopBar title="설정" back="/m/home" />
      <div className="scroll settings-container">
        <div className="settings-profile">
          <div className="sp-avatar">
            <MIcon.building />
          </div>
          <div className="sp-info">
            <h2>{profile?.corporateName ?? "법인 정보"}</h2>
            <p>{profile?.businessRegistrationNo ?? ""}</p>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">보안 및 개인정보</div>
          <div className="sg-card">
            <div
              className="sg-item"
              onClick={openNativePinSettings}
            >
              <div className="sg-icon blue">
                <MIcon.lock />
              </div>
              <div className="sg-text">PIN 설정</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
            <div className="sg-item">
              <div className="sg-icon purple">
                <MIcon.fingerprint />
              </div>
              <div className="sg-text">생체 인증 로그인</div>
              <div className="sg-right">
                <MToggle active={bio} onClick={() => setBio((b) => !b)} />
              </div>
            </div>
            <div
              className="sg-item"
              onClick={() => router.push("/m/settings/recovery")}
            >
              <div className="sg-icon gray">
                <MIcon.wallet />
              </div>
              <div className="sg-text">기기 변경 및 지갑 복구</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">기본 설정</div>
          <div className="sg-card">
            <div className="sg-item">
              <div className="sg-icon ink">
                <MIcon.bell />
              </div>
              <div className="sg-text">푸시 알림</div>
              <div className="sg-right">
                <MToggle active={push} onClick={() => setPush((b) => !b)} />
              </div>
            </div>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">자산 관리</div>
          <div className="sg-card">
            <div
              className="sg-item"
              onClick={() => showToast("준비중입니다")}
            >
              <div className="sg-icon purple">
                <MIcon.cert />
              </div>
              <div className="sg-text">증명서(VC) 숨김 및 관리</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">지원</div>
          <div className="sg-card">
            <div
              className="sg-item"
              onClick={() => showToast("준비중입니다")}
            >
              <div className="sg-icon gray">
                <MIcon.help />
              </div>
              <div className="sg-text">도움말 및 지원센터</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
            <div
              className="sg-item"
              onClick={() => showToast("준비중입니다")}
            >
              <div className="sg-icon gray">
                <MIcon.cert />
              </div>
              <div className="sg-text">약관 및 개인정보 처리방침</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <button
          type="button"
          className="settings-logout"
          onClick={onLogout}
        >
          <MIcon.logout />
          <span>지갑 삭제 및 로그아웃</span>
        </button>
        <p className="settings-version">KYvC Wallet v1.0.4</p>
      </div>
      {toast ? (
        <div className={`m-toast${toastClosing ? " closing" : ""}`}>
          {toast}
        </div>
      ) : null}
      {deleteConfirmOpen ? (
        <WalletDeleteConfirmDialog
          busy={deletingWallet}
          onCancel={() => setDeleteConfirmOpen(false)}
          onConfirm={onConfirmDeleteAndLogout}
          onWebOnlyLogout={!isBridgeAvailable() ? onWebOnlyLogout : undefined}
        />
      ) : null}
    </section>
  );
}

function WalletDeleteConfirmDialog({
  busy,
  onCancel,
  onConfirm,
  onWebOnlyLogout,
}: {
  busy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
  onWebOnlyLogout?: () => void;
}) {
  return (
    <div className="wallet-owner-dialog-layer" role="dialog" aria-modal="true">
      <div className="wallet-owner-dialog">
        <div className="wallet-owner-dialog-icon">!</div>
        <h2>지갑을 삭제하고 로그아웃할까요?</h2>
        <p>
          이 기기에 저장된 로컬 지갑 데이터가 삭제됩니다. 원래 사용하던 지갑은
          복구 문구로 다시 복구할 수 있습니다.
        </p>
        <button type="button" onClick={onConfirm} disabled={busy}>
          {busy ? "삭제 중..." : "지갑 삭제 후 로그아웃"}
        </button>
        {onWebOnlyLogout ? (
          <button type="button" className="wallet-owner-dialog-secondary" onClick={onWebOnlyLogout}>
            웹 세션만 로그아웃
          </button>
        ) : null}
        <button type="button" className="wallet-owner-dialog-secondary" onClick={onCancel}>
          취소
        </button>
      </div>
    </div>
  );
}
