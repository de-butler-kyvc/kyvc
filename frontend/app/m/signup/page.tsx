"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { writeSignupDraft } from "@/lib/signup-flow";

type Step = 1 | 2;
type TermKey = "service" | "privacy" | "marketing";

type FormState = {
  corporateName: string;
  bizNo: string;
  userName: string;
  email: string;
  terms: { all: boolean } & Record<TermKey, boolean>;
};

const TERMS_LABELS: Record<TermKey, string> = {
  service: "(필수) 서비스 이용약관 동의",
  privacy: "(필수) 개인정보 처리방침 동의",
  marketing: "(선택) 마케팅 정보 수신 동의",
};

const STEP_LABELS = ["기본 정보", "PIN 등록"] as const;

export default function MobileSignupPage() {
  const router = useRouter();
  const [step, setStep] = useState<Step>(1);
  const [form, setForm] = useState<FormState>({
    corporateName: "테크노바 주식회사",
    bizNo: "123-45-67890",
    userName: "홍길동",
    email: "hong@technova.co.kr",
    terms: { all: false, service: false, privacy: false, marketing: false },
  });

  const setField = <K extends keyof FormState>(k: K, v: FormState[K]) =>
    setForm((p) => ({ ...p, [k]: v }));

  const toggleTerm = (key: TermKey | "all") => {
    setForm((p) => {
      if (key === "all") {
        const next = !p.terms.all;
        return {
          ...p,
          terms: { all: next, service: next, privacy: next, marketing: next },
        };
      }
      const t = { ...p.terms, [key]: !p.terms[key] };
      t.all = t.service && t.privacy && t.marketing;
      return { ...p, terms: t };
    });
  };

  const back = () => {
    if (step === 2) setStep(1);
    else router.push("/m/login");
  };

  const canGoNext =
    !!form.corporateName.trim() &&
    !!form.bizNo.trim() &&
    !!form.userName.trim() &&
    /\S+@\S+\.\S+/.test(form.email) &&
    form.terms.service &&
    form.terms.privacy;

  const onNext = () => {
    if (step === 1) {
      if (canGoNext) setStep(2);
      return;
    }

    writeSignupDraft({
      corporateName: form.corporateName.trim(),
      userName: form.userName.trim(),
      email: form.email.trim(),
      password: "Kyvc1234!",
      phone: "",
      termsAcceptedAt: new Date().toISOString(),
      marketingAccepted: form.terms.marketing,
    });
    router.push("/m/signup/verify");
  };

  return (
    <section className="view signup-view">
      <MTopBar title="회원가입" back={back} />
      <div className="signup-steps">
        {STEP_LABELS.map((label, i) => {
          const num = (i + 1) as Step;
          const cls = step === num ? "active" : num < step ? "done" : "";
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

      <div className="content scroll signup-content">
        {step === 1 ? (
          <BasicInfoStep
            form={form}
            setField={setField}
            toggleTerm={toggleTerm}
          />
        ) : (
          <PinStep />
        )}
      </div>

      <div className="bottom-action signup-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onNext}
          disabled={step === 1 && !canGoNext}
        >
          {step === 1 ? "다음 단계" : "가입 완료"}
        </button>
      </div>
    </section>
  );
}

function BasicInfoStep({
  form,
  setField,
  toggleTerm,
}: {
  form: FormState;
  setField: <K extends keyof FormState>(k: K, v: FormState[K]) => void;
  toggleTerm: (k: TermKey | "all") => void;
}) {
  const t = form.terms;

  return (
    <>
      <h1 className="headline m-auth-title">사업자 정보를 입력하세요</h1>
      <p className="subcopy">법인 VC 발급을 위해 기본 정보를 확인합니다.</p>

      <label className="m-field-label">회사명</label>
      <div className="input-box">
        <input
          value={form.corporateName}
          onChange={(e) => setField("corporateName", e.target.value)}
        />
      </div>

      <label className="m-field-label">사업자번호</label>
      <div className="input-box focus">
        <input
          inputMode="numeric"
          value={form.bizNo}
          onChange={(e) => setField("bizNo", e.target.value)}
        />
        <span className="ok">
          <MIcon.check />
        </span>
      </div>

      <label className="m-field-label">담당자 이름</label>
      <div className="input-box">
        <input
          value={form.userName}
          onChange={(e) => setField("userName", e.target.value)}
        />
      </div>

      <label className="m-field-label">담당자 이메일</label>
      <div className="input-box">
        <input
          type="email"
          inputMode="email"
          value={form.email}
          onChange={(e) => setField("email", e.target.value)}
        />
      </div>

      <p className="signup-terms-guide">
        서비스 이용을 위해 약관 동의가 필요합니다.
      </p>

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
          <div key={k} className="checkbox-item">
            <label className="m-terms-row" onClick={() => toggleTerm(k)}>
              <div className={`chk${t[k] ? " on" : ""}`}>
                {t[k] ? <MIcon.check /> : null}
              </div>
              <div className="terms-text">
                <span>{TERMS_LABELS[k]}</span>
              </div>
            </label>
            <button type="button" className="terms-link">
              보기
            </button>
          </div>
        ))}
      </div>
    </>
  );
}

function PinStep() {
  return (
    <div className="signup-pin-panel">
      <div className="success-circle">
        <MIcon.lock />
      </div>
      <h1 className="headline m-auth-title">PIN 등록</h1>
      <p className="subcopy">
        다음 단계에서 지갑 로그인에 사용할 PIN을 등록합니다.
      </p>
    </div>
  );
}
