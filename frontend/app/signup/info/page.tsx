"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { Logo, SignupStepper } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { SessionGateSplash, useGuestSessionGate } from "@/lib/session-gate";
import { readSignupDraft, writeSignupDraft } from "@/lib/signup-flow";

type InfoForm = {
  email: string;
  password: string;
  passwordConfirm: string;
  userName: string;
  phone: string;
  corporateName: string;
};

export default function SignupInfoPage() {
  const router = useRouter();
  const checking = useGuestSessionGate();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isValid }
  } = useForm<InfoForm>({
    mode: "onChange",
    defaultValues: {
      email: "",
      password: "",
      passwordConfirm: "",
      userName: "",
      phone: "",
      corporateName: ""
    }
  });

  useEffect(() => {
    const draft = readSignupDraft();
    if (!draft.entityTypeId) {
      router.replace("/signup");
      return;
    }
    reset({
      email: draft.email ?? "",
      password: draft.password ?? "",
      passwordConfirm: draft.password ?? "",
      userName: draft.userName ?? "",
      phone: draft.phone ?? "",
      corporateName: draft.corporateName ?? ""
    });
  }, [reset, router]);

  const password = watch("password");
  const email = watch("email");
  const emailOk = /\S+@\S+\.\S+/.test(email);

  if (checking) return <SessionGateSplash />;

  const onSubmit = handleSubmit((values) => {
    writeSignupDraft({
      email: values.email.trim(),
      password: values.password,
      userName: values.userName.trim(),
      phone: values.phone.trim(),
      corporateName: values.corporateName.trim()
    });
    router.push("/signup/terms");
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
      </div>

      <SignupStepper step={2} />

      <div className="center-stage" style={{ alignItems: "flex-start", paddingTop: 40 }}>
        <form className="auth-card" noValidate onSubmit={onSubmit}>
          <h1 className="auth-title">정보 입력</h1>
          <p className="auth-subtitle">서비스 이용을 위한 기본 정보를 입력해 주세요.</p>

          <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            <label className="field">
              <span className="field-label">
                아이디 (이메일)<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <div className="input-with-icon">
                <span className="input-icon">
                  <Icon.Mail size={16} />
                </span>
                <input
                  className={`input${errors.email ? " error" : ""}`}
                  type="email"
                  placeholder="kim@company.co.kr"
                  {...register("email", {
                    required: "이메일을 입력해 주세요",
                    pattern: {
                      value: /\S+@\S+\.\S+/,
                      message: "이메일 형식이 올바르지 않습니다"
                    }
                  })}
                />
              </div>
              {errors.email ? (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.email.message}
                </span>
              ) : emailOk ? (
                <span className="field-success">
                  <Icon.Check size={12} /> 사용 가능한 이메일 형식입니다.
                </span>
              ) : null}
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
                  {...register("password", {
                    required: "비밀번호를 입력해 주세요",
                    minLength: { value: 8, message: "비밀번호는 8자 이상이어야 합니다" }
                  })}
                />
              </div>
              {errors.password ? (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.password.message}
                </span>
              ) : (
                <span className="field-help">8자 이상 · 영문 대소문자 · 숫자 · 특수문자 포함</span>
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
                  placeholder="••••••••"
                  {...register("passwordConfirm", {
                    required: "비밀번호를 한 번 더 입력해 주세요",
                    validate: (v) => v === password || "비밀번호가 일치하지 않습니다"
                  })}
                />
              </div>
              {errors.passwordConfirm ? (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.passwordConfirm.message}
                </span>
              ) : password && password === watch("passwordConfirm") ? (
                <span className="field-success">
                  <Icon.Check size={12} /> 비밀번호가 일치합니다.
                </span>
              ) : null}
            </label>

            <label className="field">
              <span className="field-label">
                이름<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <input
                className={`input${errors.userName ? " error" : ""}`}
                placeholder="김철수"
                {...register("userName", {
                  required: "이름을 입력해 주세요",
                  maxLength: { value: 100, message: "이름은 100자 이내로 입력해 주세요" }
                })}
              />
              {errors.userName && (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.userName.message}
                </span>
              )}
            </label>

            <label className="field">
              <span className="field-label">
                휴대폰 번호<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <div className="phone-row">
                <input
                  className={`input${errors.phone ? " error" : ""}`}
                  placeholder="010-0000-0000"
                  {...register("phone", {
                    required: "휴대폰 번호를 입력해 주세요",
                    pattern: {
                      value: /^[0-9-+\s]{9,30}$/,
                      message: "휴대폰 번호 형식을 확인해 주세요"
                    }
                  })}
                />
              </div>
              {errors.phone && (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.phone.message}
                </span>
              )}
            </label>

            <label className="field">
              <span className="field-label">
                법인명<span style={{ color: "var(--danger)" }}> *</span>
              </span>
              <input
                className={`input${errors.corporateName ? " error" : ""}`}
                placeholder="주식회사 케이원"
                {...register("corporateName", {
                  required: "법인명을 입력해 주세요",
                  maxLength: { value: 255, message: "법인명은 255자 이내여야 합니다" }
                })}
              />
              {errors.corporateName && (
                <span className="field-error">
                  <Icon.X size={12} /> {errors.corporateName.message}
                </span>
              )}
            </label>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block btn-lg"
            style={{ marginTop: 20 }}
            disabled={!isValid}
          >
            다음 — 약관 동의
          </button>
          <button
            type="button"
            className="btn btn-ghost btn-block"
            style={{ marginTop: 8 }}
            onClick={() => router.push("/signup")}
          >
            이전으로
          </button>
        </form>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
