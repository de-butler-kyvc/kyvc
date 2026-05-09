"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { Logo, SignupStepper } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { auth } from "@/lib/api";
import { useSession } from "@/lib/session-context";
import {
  clearSignupDraft,
  readSignupDraft,
  type SignupDraft
} from "@/lib/signup-flow";

function formatDate(iso?: string) {
  if (!iso) return "-";
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}

export default function SignupCompletePage() {
  const router = useRouter();
  const { refreshSession } = useSession();
  const [draft, setDraft] = useState<SignupDraft | null>(null);

  useEffect(() => {
    const d = readSignupDraft();
    if (!d.signedUpAt) {
      router.replace("/signup");
      return;
    }
    setDraft(d);
  }, [router]);

  const dateStr = useMemo(() => formatDate(draft?.signedUpAt), [draft]);

  if (!draft) return null;

  const goDashboard = async () => {
    clearSignupDraft();
    await refreshSession();
    router.replace("/corporate");
  };

  const goLogin = async () => {
    clearSignupDraft();
    try {
      await auth.logout();
    } catch {
      // 세션이 이미 만료되었을 수 있음
    }
    await refreshSession();
    router.replace("/login?signup=ok");
  };

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

      <SignupStepper step={5} />

      <div className="center-stage">
        <div className="auth-card" style={{ textAlign: "center" }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: "50%",
              background: "var(--success-soft)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              margin: "0 auto 20px",
              color: "var(--success)"
            }}
          >
            <Icon.Check size={26} />
          </div>

          <h1 className="auth-title">가입이 완료되었습니다!</h1>
          <p className="auth-subtitle">
            KYvC 서비스에 오신 것을 환영합니다. 지금 바로 KYC 신청을 시작해 보세요.
          </p>

          <div className="signup-summary">
            <div className="kv-row">
              <span className="kv-key">가입 유형</span>
              <span className="kv-val">{draft.entityLabel ?? "주식회사"}</span>
            </div>
            <div className="kv-row">
              <span className="kv-key">아이디</span>
              <span className="kv-val">{draft.email}</span>
            </div>
            <div className="kv-row">
              <span className="kv-key">가입 일시</span>
              <span className="kv-val">{dateStr}</span>
            </div>
            <div className="kv-row">
              <span className="kv-key">계정 상태</span>
              <span
                className="kv-val"
                style={{ color: "var(--success)", fontWeight: 600 }}
              >
                활성
              </span>
            </div>
            <div className="kv-row">
              <span className="kv-key">법인명</span>
              <span className="kv-val">{draft.corporateName}</span>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-primary btn-block btn-lg"
            onClick={goDashboard}
          >
            서비스 시작하기
          </button>
          <button
            type="button"
            className="btn btn-ghost btn-block"
            style={{ marginTop: 8 }}
            onClick={goLogin}
          >
            로그인 화면으로
          </button>
        </div>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
