"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Logo } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { ApiError, auth } from "@/lib/api";
import {
  AUTHENTICATED_HOME,
  SessionGateSplash,
  useGuestSessionGate
} from "@/lib/session-gate";

type LoginForm = { email: string; password: string };

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const checkingSession = useGuestSessionGate();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginForm>({ defaultValues: { email: "", password: "" } });

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
        <div className="auth-card">
          <h1 className="auth-title">KYvC 로그인</h1>
          <p className="auth-subtitle">법인 사용자 / 금융사 / 모바일 Wallet 공용 로그인</p>

          <form onSubmit={onSubmit} className="col" noValidate>
            <label className="field">
              <span className="field-label">
                이메일<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <div className="input-with-icon">
                <span className="input-icon">
                  <Icon.Mail size={16} />
                </span>
                <input
                  className={`input${errors.email ? " error" : ""}`}
                  type="email"
                  placeholder="name@company.com"
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
                <span className="field-error">{errors.email.message}</span>
              )}
            </label>

            <label className="field">
              <span className="field-label">
                비밀번호<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <div className="input-with-icon">
                <span className="input-icon">
                  <Icon.Lock size={16} />
                </span>
                <input
                  className={`input${errors.password ? " error" : ""}`}
                  type="password"
                  placeholder="••••••••"
                  {...register("password", { required: "비밀번호를 입력해 주세요" })}
                />
              </div>
              {errors.password && (
                <span className="field-error">{errors.password.message}</span>
              )}
            </label>

            {error && (
              <div className="field-error" style={{ fontSize: 13 }}>
                <Icon.Alert size={14} /> {error}
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary btn-block btn-lg"
              disabled={isSubmitting}
              style={{ marginTop: 6 }}
            >
              {isSubmitting ? "로그인 중..." : "로그인"}
            </button>
          </form>
        </div>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
