"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { Checkbox, Logo } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { ApiError, auth } from "@/lib/api";
import {
  AUTHENTICATED_HOME,
  SessionGateSplash,
  useGuestSessionGate
} from "@/lib/session-gate";

type LoginForm = { email: string; password: string };

function LoginPageInner() {
  const router = useRouter();
  const params = useSearchParams();
  const checkingSession = useGuestSessionGate();
  const [error, setError] = useState<string | null>(null);
  const [keepLogin, setKeepLogin] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);

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

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setError(null);
    try {
      await auth.login(email, password);
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
              onClick={() => setError("KYvC 법인증명서 로그인은 준비 중입니다.")}
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
