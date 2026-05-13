"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { useForm } from "react-hook-form";

import { Checkbox, Logo } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { ApiError, auth } from "@/lib/api";
import {
  AUTHENTICATED_HOME,
  SessionGateSplash,
  useGuestSessionGate
} from "@/lib/session-gate";
import { useSession } from "@/lib/session-context";

type LoginForm = { email: string; password: string };

const VP_POLLING_INTERVAL_MS = 2000;

const VP_STATUS_TEXT = {
  creating: "QR 생성 중",
  waiting: "모바일 앱에서 QR 코드를 스캔하세요",
  verifying: "모바일 앱에서 인증을 진행 중입니다.",
  completing: "로그인을 완료하고 있습니다.",
  completed: "로그인이 완료되었습니다.",
  expired: "QR 코드가 만료되었습니다. 다시 시도해주세요.",
  failed: "VP 검증에 실패했습니다. 다시 시도해주세요.",
  completeFailed: "로그인 완료 처리에 실패했습니다. 다시 시도해주세요.",
  createFailed: "법인증명서 로그인 QR 생성에 실패했습니다.",
} as const;

const VP_FAILED_STATUSES = new Set(["INVALID", "FAILED", "REPLAY_SUSPECTED"]);

type VpLoginQrModalProps = {
  open: boolean;
  qrText: string | null;
  statusText: string;
  onClose: () => void;
};

function VpLoginQrModal({
  open,
  qrText,
  statusText,
  onClose
}: VpLoginQrModalProps) {
  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="vp-login-qr-title"
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 1000,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "rgba(15, 23, 42, 0.64)",
        padding: 24
      }}
    >
      <div
        style={{
          width: "min(92vw, 380px)",
          borderRadius: 8,
          background: "#fff",
          boxShadow: "0 24px 80px rgba(15, 23, 42, 0.22)",
          padding: 24,
          textAlign: "center"
        }}
      >
        <h2
          id="vp-login-qr-title"
          style={{
            margin: 0,
            color: "#0f172a",
            fontSize: 20,
            fontWeight: 800,
            letterSpacing: 0
          }}
        >
          법인증명서 로그인 QR 코드
        </h2>
        <p
          style={{
            margin: "8px 0 20px",
            color: "#64748b",
            fontSize: 14,
            lineHeight: 1.5
          }}
        >
          모바일 앱에서 QR 코드를 스캔하세요
        </p>

        <div
          style={{
            display: "flex",
            minHeight: 256,
            alignItems: "center",
            justifyContent: "center",
            border: "1px solid #e2e8f0",
            borderRadius: 8,
            background: "#fff",
            padding: 16
          }}
        >
          {qrText ? (
            <QRCodeSVG value={qrText} size={224} marginSize={1} />
          ) : (
            <span style={{ color: "#64748b", fontSize: 14 }}>
              QR 생성 중
            </span>
          )}
        </div>

        <p
          aria-live="polite"
          style={{
            minHeight: 22,
            margin: "16px 0 18px",
            color: "#334155",
            fontSize: 14,
            lineHeight: 1.55
          }}
        >
          {statusText}
        </p>

        <button
          type="button"
          className="btn btn-dark btn-block"
          onClick={onClose}
        >
          닫기
        </button>
      </div>
    </div>
  );
}

function LoginPageInner() {
  const router = useRouter();
  const params = useSearchParams();
  const checkingSession = useGuestSessionGate();
  const { refreshSession, refreshProfile } = useSession();
  const [error, setError] = useState<string | null>(null);
  const [keepLogin, setKeepLogin] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);
  const [vpModalOpen, setVpModalOpen] = useState(false);
  const [vpRequestId, setVpRequestId] = useState<string | null>(null);
  const [vpQrText, setVpQrText] = useState<string | null>(null);
  const [vpExpiresAt, setVpExpiresAt] = useState<string | null>(null);
  const [vpStatusText, setVpStatusText] = useState<string>(VP_STATUS_TEXT.waiting);
  const [vpLoading, setVpLoading] = useState(false);
  const [vpCompleting, setVpCompleting] = useState(false);
  const vpPollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const vpCompletingRef = useRef(false);
  const vpFlowIdRef = useRef(0);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginForm>({ defaultValues: { email: "", password: "" } });

  useEffect(() => {
    if (params.get("signup") === "ok") {
      setNotice("회원가입이 완료되었습니다. 가입한 계정으로 로그인해 주세요.");
    }
  }, [params]);

  const stopVpPolling = useCallback(() => {
    if (vpPollingRef.current) {
      clearInterval(vpPollingRef.current);
      vpPollingRef.current = null;
    }
  }, []);

  const resetVpLogin = useCallback(() => {
    vpFlowIdRef.current += 1;
    vpCompletingRef.current = false;
    stopVpPolling();
    setVpModalOpen(false);
    setVpRequestId(null);
    setVpQrText(null);
    setVpExpiresAt(null);
    setVpStatusText(VP_STATUS_TEXT.waiting);
    setVpLoading(false);
    setVpCompleting(false);
  }, [stopVpPolling]);

  useEffect(() => {
    return () => {
      vpFlowIdRef.current += 1;
      stopVpPolling();
    };
  }, [stopVpPolling]);

  const completeVpLogin = useCallback(
    async (requestId: string, flowId: number) => {
      if (vpCompletingRef.current || vpFlowIdRef.current !== flowId) return;

      vpCompletingRef.current = true;
      stopVpPolling();
      setVpCompleting(true);
      setVpStatusText(VP_STATUS_TEXT.completing);

      try {
        await auth.completeVpLogin(requestId);
        if (vpFlowIdRef.current !== flowId) return;

        setVpStatusText(VP_STATUS_TEXT.completed);
        const s = await refreshSession();
        if (vpFlowIdRef.current !== flowId) return;

        if (s?.authenticated && s.corporateRegistered) {
          await refreshProfile();
          if (vpFlowIdRef.current !== flowId) return;
        }
        router.replace(AUTHENTICATED_HOME);
      } catch {
        if (vpFlowIdRef.current !== flowId) return;
        vpCompletingRef.current = false;
        setVpStatusText(VP_STATUS_TEXT.completeFailed);
      } finally {
        if (vpFlowIdRef.current === flowId) {
          setVpCompleting(false);
        }
      }
    },
    [refreshProfile, refreshSession, router, stopVpPolling]
  );

  const pollVpLoginStatus = useCallback(
    async (requestId: string, flowId: number) => {
      if (vpFlowIdRef.current !== flowId) return;

      try {
        const status = await auth.getVpLoginStatus(requestId);
        if (vpFlowIdRef.current !== flowId) return;

        setVpExpiresAt(status.expiresAt);

        if (status.canComplete) {
          await completeVpLogin(requestId, flowId);
          return;
        }

        if (status.status === "EXPIRED") {
          stopVpPolling();
          setVpStatusText(VP_STATUS_TEXT.expired);
          return;
        }

        if (VP_FAILED_STATUSES.has(status.status)) {
          stopVpPolling();
          setVpStatusText(VP_STATUS_TEXT.failed);
          return;
        }

        setVpStatusText(
          status.status === "PRESENTED"
            ? VP_STATUS_TEXT.verifying
            : VP_STATUS_TEXT.waiting
        );
      } catch (err) {
        if (vpFlowIdRef.current !== flowId) return;
        stopVpPolling();
        setVpStatusText(
          err instanceof ApiError && err.code.includes("EXPIRED")
            ? VP_STATUS_TEXT.expired
            : VP_STATUS_TEXT.failed
        );
      }
    },
    [completeVpLogin, stopVpPolling]
  );

  const startVpPolling = useCallback(
    (requestId: string, flowId: number) => {
      stopVpPolling();
      void pollVpLoginStatus(requestId, flowId);
      vpPollingRef.current = setInterval(() => {
        void pollVpLoginStatus(requestId, flowId);
      }, VP_POLLING_INTERVAL_MS);
    },
    [pollVpLoginStatus, stopVpPolling]
  );

  const handleVpLoginStart = useCallback(async () => {
    const flowId = vpFlowIdRef.current + 1;
    vpFlowIdRef.current = flowId;
    vpCompletingRef.current = false;
    stopVpPolling();
    setError(null);
    setVpModalOpen(true);
    setVpRequestId(null);
    setVpQrText(null);
    setVpExpiresAt(null);
    setVpStatusText(VP_STATUS_TEXT.creating);
    setVpLoading(true);
    setVpCompleting(false);

    try {
      const data = await auth.startVpLogin();
      if (vpFlowIdRef.current !== flowId) return;

      setVpRequestId(data.requestId);
      setVpQrText(JSON.stringify(data.qrPayload));
      setVpExpiresAt(data.expiresAt);
      setVpStatusText(VP_STATUS_TEXT.waiting);
      startVpPolling(data.requestId, flowId);
    } catch {
      if (vpFlowIdRef.current !== flowId) return;
      stopVpPolling();
      setVpStatusText(VP_STATUS_TEXT.createFailed);
    } finally {
      if (vpFlowIdRef.current === flowId) {
        setVpLoading(false);
      }
    }
  }, [startVpPolling, stopVpPolling]);

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setError(null);
    try {
      await auth.login(email, password);
      const s = await refreshSession();
      if (s?.authenticated && s.corporateRegistered) {
        await refreshProfile();
      }
      router.replace(AUTHENTICATED_HOME);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했습니다.");
    }
  });

  if (checkingSession) {
    return <SessionGateSplash />;
  }

  return (
    <div className="app-shell page-enter">
      <div className="topbar">
        <div
          className="topbar-logo"
          onClick={() => router.push("/")}
          style={{ cursor: "pointer" }}
        >
          <Logo size={22} />
        </div>
      </div>

      <div className="center-stage">
        <form className="auth-card" onSubmit={onSubmit} noValidate>
          <h1 className="auth-title">로그인</h1>
          <p className="auth-subtitle">KYvC 서비스에 로그인하세요.</p>

          {notice && (
            <div className="info-box" style={{ marginBottom: 16 }}>
              <div className="info-box-title">알림</div>
              <div>{notice}</div>
            </div>
          )}

          <div className="col" style={{ gap: 14 }}>
            <label className="field">
              <span className="field-label">아이디</span>
              <div className="input-with-icon">
                <span className="input-icon">
                  <Icon.Mail size={16} />
                </span>
                <input
                  className={`input${errors.email ? " error" : ""}`}
                  type="email"
                  placeholder="user@company.co.kr"
                  autoComplete="username"
                  {...register("email", {
                    required: "이메일을 입력해 주세요",
                    pattern: {
                      value: /\S+@\S+\.\S+/,
                      message: "이메일 형식이 올바르지 않습니다"
                    }
                  })}
                />
              </div>
              {errors.email && (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.email.message}
                </span>
              )}
            </label>

            <label className="field">
              <span className="field-label">비밀번호</span>
              <div className="input-with-icon">
                <span className="input-icon">
                  <Icon.Lock size={16} />
                </span>
                <input
                  className={`input${errors.password ? " error" : ""}`}
                  type="password"
                  placeholder="••••••••"
                  autoComplete="current-password"
                  {...register("password", {
                    required: "비밀번호를 입력해 주세요"
                  })}
                />
              </div>
              {errors.password && (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.password.message}
                </span>
              )}
            </label>

            <div className="row-between" style={{ marginTop: 4 }}>
              <Checkbox checked={keepLogin} onChange={setKeepLogin}>
                로그인 상태 유지
              </Checkbox>
              <Link className="link-button" href="/signup" prefetch={false}>
                비밀번호 찾기
              </Link>
            </div>

            {error && (
              <div className="field-error" style={{ fontSize: 13 }}>
                <Icon.Alert size={14} /> {error}
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary btn-block btn-lg"
              disabled={isSubmitting}
            >
              {isSubmitting ? "확인 중..." : "로그인"}
            </button>

            <div className="divider-text">또는</div>

            <button
              type="button"
              className="btn btn-dark btn-block btn-lg"
              disabled={isSubmitting || vpLoading || vpCompleting}
              onClick={handleVpLoginStart}
            >
              <Icon.Shield size={16} /> KYvC 법인증명서 로그인
            </button>

            <div
              className="text-center mt-8 text-muted"
              style={{ fontSize: 13 }}
            >
              계정이 없으신가요? <Link href="/signup">회원가입</Link>
            </div>
          </div>
        </form>
      </div>

      <VpLoginQrModal
        open={vpModalOpen}
        qrText={vpQrText}
        statusText={vpStatusText}
        onClose={resetVpLogin}
      />

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<SessionGateSplash />}>
      <LoginPageInner />
    </Suspense>
  );
}
