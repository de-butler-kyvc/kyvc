"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { writeSignupDraft } from "@/lib/signup-flow";

type Step = 1 | 2 | 3;
type TermKey = "service" | "privacy" | "esign";

type FormState = {
  corporateName: string;
  bizNo: string;
  userName: string;
  email: string;
  password: string;
  passwordConfirm: string;
  terms: { all: boolean } & Record<TermKey, boolean>;
};

const TERMS_LABELS: Record<TermKey, string> = {
  service: "서비스 이용약관 동의",
  privacy: "개인정보 처리방침 동의",
  esign: "전자서명 이용 동의",
};

const STEP_LABELS = ["기본 정보", "약관 동의", "비밀번호"] as const;

function pwScore(pw: string): "weak" | "medium" | "strong" | "" {
  if (!pw) return "";
  let s = 0;
  if (pw.length >= 8) s++;
  if (/[A-Z]/.test(pw)) s++;
  if (/[0-9]/.test(pw)) s++;
  if (/[^A-Za-z0-9]/.test(pw)) s++;
  if (s <= 1) return "weak";
  if (s === 2 || s === 3) return "medium";
  return "strong";
}

export default function MobileSignupPage() {
  const router = useRouter();
  const [step, setStep] = useState<Step>(1);
  const [form, setForm] = useState<FormState>({
    corporateName: "",
    bizNo: "",
    userName: "",
    email: "",
    password: "",
    passwordConfirm: "",
    terms: { all: false, service: false, privacy: false, esign: false },
  });

  const setField = <K extends keyof FormState>(k: K, v: FormState[K]) =>
    setForm((p) => ({ ...p, [k]: v }));

  const toggleTerm = (key: TermKey | "all") => {
    setForm((p) => {
      if (key === "all") {
        const next = !p.terms.all;
        return {
          ...p,
          terms: { all: next, service: next, privacy: next, esign: next },
        };
      }
      const t = { ...p.terms, [key]: !p.terms[key] };
      t.all = t.service && t.privacy && t.esign;
      return { ...p, terms: t };
    });
  };

  const back = () => {
    if (step > 1) setStep((step - 1) as Step);
    else router.push("/m/login");
  };

  const canStep1 =
    !!form.corporateName.trim() &&
    !!form.bizNo.trim() &&
    !!form.userName.trim() &&
    /\S+@\S+\.\S+/.test(form.email);
  const canStep2 = form.terms.service && form.terms.privacy && form.terms.esign;
  const score = pwScore(form.password);
  const pwMatch = form.password && form.password === form.passwordConfirm;
  const canStep3 = score === "strong" || score === "medium";
  const canSubmit = canStep3 && pwMatch;

  const onNext = () => {
    if (step === 1 && canStep1) setStep(2);
    else if (step === 2 && canStep2) setStep(3);
    else if (step === 3 && canSubmit) {
      // 입력값 sessionStorage 저장 → 이메일 인증 페이지에서 가입 API 호출
      writeSignupDraft({
        corporateName: form.corporateName.trim(),
        userName: form.userName.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: "",
        termsAcceptedAt: new Date().toISOString(),
        marketingAccepted: false,
      });
      router.push("/m/signup/verify");
    }
  };

  return (
    <section className="view wash">
      <MTopBar title="회원가입" back={back} />
      <div className="signup-steps">
        {STEP_LABELS.map((label, i) => {
          const num = (i + 1) as Step;
          const cls =
            step === num ? "active" : num < step ? "done" : "";
          return (
            <div key={label} style={{ display: "contents" }}>
              <div className={`ss-item${cls ? " " + cls : ""}`}>
                <div className="ss-dot">
                  {num < step ? <MIcon.check /> : num}
                </div>
                <span>{label}</span>
              </div>
              {i < STEP_LABELS.length - 1 ? <div className="ss-line" /> : null}
            </div>
          );
        })}
      </div>

      <div className="content scroll">
        {step === 1 ? <Step1 form={form} setField={setField} /> : null}
        {step === 2 ? (
          <Step2 form={form} toggleTerm={toggleTerm} />
        ) : null}
        {step === 3 ? <Step3 form={form} setField={setField} /> : null}
      </div>

      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onNext}
          disabled={
            (step === 1 && !canStep1) ||
            (step === 2 && !canStep2) ||
            (step === 3 && !canSubmit)
          }
        >
          {step < 3 ? "다음 단계" : "가입 완료"}
        </button>
      </div>
    </section>
  );
}

function Step1({
  form,
  setField,
}: {
  form: FormState;
  setField: <K extends keyof FormState>(k: K, v: FormState[K]) => void;
}) {
  return (
    <>
      <h1 className="headline m-auth-title">
        사업자 정보를
        <br />
        입력하세요
      </h1>
      <p className="subcopy">법인 VC 발급을 위해 기본 정보를 확인합니다.</p>
      <label className="m-field-label">회사명</label>
      <div className={`input-box${form.corporateName ? " focus" : ""}`}>
        <input
          placeholder="테크노바 주식회사"
          value={form.corporateName}
          onChange={(e) => setField("corporateName", e.target.value)}
        />
      </div>
      <label className="m-field-label">사업자번호</label>
      <div className={`input-box${form.bizNo ? " focus" : ""}`}>
        <input
          inputMode="numeric"
          placeholder="123-45-67890"
          value={form.bizNo}
          onChange={(e) => setField("bizNo", e.target.value)}
        />
        {form.bizNo ? (
          <span className="ok">
            <MIcon.check />
          </span>
        ) : null}
      </div>
      <label className="m-field-label">담당자 이름</label>
      <div className={`input-box${form.userName ? " focus" : ""}`}>
        <input
          placeholder="홍길동"
          value={form.userName}
          onChange={(e) => setField("userName", e.target.value)}
        />
      </div>
      <label className="m-field-label">담당자 이메일</label>
      <div className={`input-box${form.email ? " focus" : ""}`}>
        <input
          type="email"
          inputMode="email"
          placeholder="hong@technova.co.kr"
          value={form.email}
          onChange={(e) => setField("email", e.target.value)}
        />
      </div>
    </>
  );
}

function Step2({
  form,
  toggleTerm,
}: {
  form: FormState;
  toggleTerm: (k: TermKey | "all") => void;
}) {
  const t = form.terms;
  return (
    <>
      <h1 className="headline m-auth-title">
        서비스 약관에
        <br />
        동의해 주세요
      </h1>
      <p className="subcopy">KYvC 지갑 이용을 위한 필수 약관입니다.</p>
      <div className="terms-box">
        <label
          className="m-terms-row terms-all"
          onClick={() => toggleTerm("all")}
        >
          <div className={`chk${t.all ? " on" : ""}`}>
            {t.all ? <MIcon.check /> : null}
          </div>
          <span className="terms-all-label">전체 동의</span>
        </label>
        <div className="terms-divider" />
        {(Object.keys(TERMS_LABELS) as TermKey[]).map((k) => (
          <label key={k} className="m-terms-row" onClick={() => toggleTerm(k)}>
            <div className={`chk${t[k] ? " on" : ""}`}>
              {t[k] ? <MIcon.check /> : null}
            </div>
            <div className="terms-text">
              <span>
                {TERMS_LABELS[k]} <em>(필수)</em>
              </span>
            </div>
          </label>
        ))}
      </div>
    </>
  );
}

function Step3({
  form,
  setField,
}: {
  form: FormState;
  setField: <K extends keyof FormState>(k: K, v: FormState[K]) => void;
}) {
  const score = pwScore(form.password);
  const pwMatch = form.password && form.password === form.passwordConfirm;
  return (
    <>
      <h1 className="headline m-auth-title">
        비밀번호를
        <br />
        설정하세요
      </h1>
      <p className="subcopy">영문, 숫자, 특수문자 포함 8자 이상으로 설정하세요.</p>
      <label className="m-field-label">비밀번호</label>
      <div className={`input-box${form.password ? " focus" : ""}`}>
        <input
          type="password"
          placeholder="••••••••"
          value={form.password}
          onChange={(e) => setField("password", e.target.value)}
        />
        <MIcon.eye />
      </div>
      {form.password ? (
        <div className="pw-strength">
          <div
            className={`pw-bar${score === "weak" ? " pw-weak" : score === "medium" ? " pw-medium" : score === "strong" ? " pw-strong" : ""}`}
          />
          <span
            className={`pw-label${score ? " " + score : ""}`}
          >
            {score === "strong"
              ? "강함"
              : score === "medium"
                ? "보통"
                : score === "weak"
                  ? "약함"
                  : ""}
          </span>
        </div>
      ) : null}
      <label className="m-field-label">비밀번호 확인</label>
      <div
        className={`input-box${form.passwordConfirm ? " focus" : ""}`}
      >
        <input
          type="password"
          placeholder="••••••••"
          value={form.passwordConfirm}
          onChange={(e) => setField("passwordConfirm", e.target.value)}
        />
        {pwMatch ? (
          <span className="ok">
            <MIcon.check />
          </span>
        ) : null}
      </div>
      {pwMatch ? (
        <p className="pw-match-ok">비밀번호가 일치합니다</p>
      ) : null}
      <div className="notice-card mt-16">
        <b>
          <MIcon.shield /> 보안 안내
        </b>
        <p>비밀번호는 암호화되어 안전하게 보호됩니다.</p>
      </div>
    </>
  );
}
