"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MLogo, MTopBar } from "@/components/m/parts";
import { ApiError, auth } from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  type AuthStatus,
} from "@/lib/m/android-bridge";
import { ensureMobileWallet } from "@/lib/m/wallet-bridge";

type Tab = "business" | "email";

const FIGMA_PIN_ICON =
  "https://www.figma.com/api/mcp/asset/97ea3c8d-9277-424d-9bed-37c4631674f5";
const FIGMA_BIOMETRIC_ICON =
  "https://www.figma.com/api/mcp/asset/915135d4-9f0a-4722-a5d4-44424d45817b";

export default function MobileLoginPage() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("business");
  const [bizNo, setBizNo] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keep, setKeep] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState("");
  const [toastClosing, setToastClosing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [authStatus, setAuthStatus] = useState<AuthStatus | null>(null);

  // 진입 즉시 네이티브 인증 상태 조회 → availableMethods/emailVerificationRequired 게이트
  useEffect(() => {
    if (!isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.getAuthStatus();
        setAuthStatus(r);
        if (r.emailVerificationRequired) {
          setError(
            "인증 5회 실패로 이메일 인증이 필요합니다. 계정 찾기에서 이메일 인증을 완료해주세요.",
          );
        }
      } catch {
        // 브리지 미설치/오류 → 일반 모드
      }
    })();
  }, []);

  const methods = authStatus?.availableMethods ?? ["pin", "biometric"];
  const canUsePin = methods.includes("pin");
  const canUseBiometric = methods.includes("biometric");
  const remaining = authStatus?.remainingAttempts;
  const blocked = !!authStatus?.emailVerificationRequired;

  const showToast = (message: string) => {
    setToastClosing(false);
    setToast(message);
    window.setTimeout(() => setToastClosing(true), 1400);
    window.setTimeout(() => setToast(""), 1600);
  };

  const onLogin = async () => {
    setError(null);
    setBusy(true);
    try {
      const id = tab === "email" ? email.trim() : bizNo.trim();
      if (!id || !password) {
        setError("아이디와 비밀번호를 입력해 주세요.");
        return;
      }
      // 백엔드는 이메일 기준 로그인. 사업자번호 탭일 때도 입력값을 그대로 보낸다.
      await auth.login(id, password);
      if (isBridgeAvailable()) {
        await ensureMobileWallet().catch(() => null);
      }
      router.replace("/m/home");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const onQuickAuth = async (method: "pin" | "biometric") => {
    if (blocked) {
      setError("이메일 인증 후 다시 시도해 주세요.");
      return;
    }
    if (!isBridgeAvailable()) {
      showToast("앱에서만 사용할 수 있는 기능입니다");
      return;
    }
    try {
      const r = await bridge.requestNativeAuth(method, "wallet-login");
      if (r.ok && r.authenticated) {
        await ensureMobileWallet().catch(() => null);
        router.replace("/m/home");
      } else if (r.emailVerificationRequired) {
        setError("인증 5회 실패로 이메일 인증이 필요합니다.");
      } else {
        setError(r.error ?? "인증에 실패했습니다.");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "브리지 호출 실패");
    }
  };

  return (
    <section className="view auth-view">
      <MTopBar back={false} />
      <div className="content scroll">
        <MLogo />
        <h1 className="headline m-auth-title">
          KYvC 계정으로 로그인하세요
        </h1>
        <p className="subcopy">등록된 사업자번호 또는 이메일로 로그인합니다.</p>

        {typeof remaining === "number" && remaining < 5 && remaining > 0 ? (
          <p className="subcopy" style={{ color: "var(--m-warn, #f59e0b)" }}>
            남은 시도 {remaining}회
          </p>
        ) : null}

        <div className="tabs mt-24">
          <button
            type="button"
            className={`tab${tab === "business" ? " active" : ""}`}
            onClick={() => setTab("business")}
          >
            사업자번호
          </button>
          <button
            type="button"
            className={`tab${tab === "email" ? " active" : ""}`}
            onClick={() => setTab("email")}
          >
            이메일
          </button>
        </div>

        <label className="m-field-label">
          {tab === "email" ? "이메일" : "사업자번호"}
        </label>
        <div
          className={`input-box${(tab === "email" ? email : bizNo) ? " focus" : ""}`}
        >
          {tab === "email" ? (
            <input
              type="email"
              placeholder="hong@technova.co.kr"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
            />
          ) : (
            <input
              inputMode="numeric"
              placeholder="123-45-67890"
              value={bizNo}
              onChange={(e) => setBizNo(e.target.value)}
            />
          )}
        </div>

        <label className="m-field-label">비밀번호</label>
        <div className={`input-box${password ? " focus" : ""}`}>
          <input
            type="password"
            placeholder="비밀번호 입력"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </div>

        {error && <p className="m-error">{error}</p>}

        <div className="auth-meta">
          <label>
            <input
              type="checkbox"
              checked={keep}
              onChange={(e) => setKeep(e.target.checked)}
            />
            자동 로그인
          </label>
          <button
            type="button"
            className="text-link"
            onClick={() => router.push("/m/login/find")}
          >
            계정 찾기
          </button>
        </div>

        <div className="divider">
          <i />
          <span>간편 인증</span>
          <i />
        </div>

        <div className="quick-auth">
          {canUsePin ? (
            <button
              type="button"
              className="quick-card"
              onClick={() => onQuickAuth("pin")}
              disabled={blocked}
            >
              <img className="quick-auth-icon pin" src={FIGMA_PIN_ICON} alt="" />
              <span>PIN 로그인</span>
            </button>
          ) : null}
          {canUseBiometric ? (
            <button
              type="button"
              className="quick-card"
              onClick={() => onQuickAuth("biometric")}
              disabled={blocked}
            >
              <img
                className="quick-auth-icon biometric"
                src={FIGMA_BIOMETRIC_ICON}
                alt=""
              />
              <span>생체인증</span>
            </button>
          ) : null}
        </div>
      </div>

      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onLogin}
          disabled={busy}
        >
          {busy ? "로그인 중..." : "로그인"}
        </button>
        <button
          type="button"
          className="ghost"
          onClick={() => router.push("/m/signup")}
        >
          계정이 없나요? 회원가입
        </button>
      </div>
      {toast ? (
        <div className={`m-toast${toastClosing ? " closing" : ""}`}>
          {toast}
        </div>
      ) : null}
    </section>
  );
}
