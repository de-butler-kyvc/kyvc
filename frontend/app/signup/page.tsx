"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Logo } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { ApiError, auth } from "@/lib/api";

type SignupForm = {
  email: string;
  password: string;
  passwordConfirm: string;
};

export default function SignupPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting }
  } = useForm<SignupForm>({
    defaultValues: { email: "", password: "", passwordConfirm: "" }
  });

  const password = watch("password");

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setError(null);
    try {
      await auth.signup(email, password);
      router.push("/login?signup=ok");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "회원가입에 실패했습니다.");
    }
  });

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
        <div className="topbar-right">
          <Link className="topbar-nav-link" href="/login">
            로그인
          </Link>
        </div>
      </div>

      <div className="center-stage">
        <div className="auth-card">
          <h1 className="auth-title">회원가입</h1>
          <p className="auth-subtitle">
            KYvC 법인 KYC 플랫폼 이용을 위한 계정을 만드세요.
          </p>

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
                  placeholder="8자 이상"
                  {...register("password", {
                    required: "비밀번호를 입력해 주세요",
                    minLength: {
                      value: 8,
                      message: "비밀번호는 8자 이상이어야 합니다"
                    }
                  })}
                />
              </div>
              {errors.password ? (
                <span className="field-error">{errors.password.message}</span>
              ) : (
                <span className="field-help">8자 이상으로 입력해 주세요.</span>
              )}
            </label>

            <label className="field">
              <span className="field-label">
                비밀번호 확인<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <div className="input-with-icon">
                <span className="input-icon">
                  <Icon.Lock size={16} />
                </span>
                <input
                  className={`input${errors.passwordConfirm ? " error" : ""}`}
                  type="password"
                  placeholder="비밀번호 재입력"
                  {...register("passwordConfirm", {
                    required: "비밀번호를 한 번 더 입력해 주세요",
                    validate: (v) => v === password || "비밀번호가 일치하지 않습니다"
                  })}
                />
              </div>
              {errors.passwordConfirm && (
                <span className="field-error">{errors.passwordConfirm.message}</span>
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
              {isSubmitting ? "가입 중..." : "회원가입"}
            </button>

            <div className="text-center text-muted" style={{ fontSize: 13 }}>
              이미 계정이 있으신가요? <Link href="/login">로그인</Link>
            </div>
          </form>
        </div>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
