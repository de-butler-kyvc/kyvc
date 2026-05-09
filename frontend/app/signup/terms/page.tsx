"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Checkbox, Logo, SignupStepper } from "@/components/design/primitives";
import { SessionGateSplash, useGuestSessionGate } from "@/lib/session-gate";
import { readSignupDraft, writeSignupDraft } from "@/lib/signup-flow";

type TermKey = "terms" | "privacy" | "third" | "storage" | "marketing";

const ITEMS: { key: TermKey; label: string; required: boolean }[] = [
  { key: "terms", label: "서비스 이용약관 (필수)", required: true },
  { key: "privacy", label: "개인정보 수집 및 이용 동의 (필수)", required: true },
  { key: "third", label: "개인정보 제3자 제공 동의 (필수)", required: true },
  { key: "storage", label: "원본서류 저장 동의 (필수)", required: true },
  { key: "marketing", label: "마케팅 정보 수신 동의 (선택)", required: false }
];

type CheckedMap = Record<TermKey, boolean>;

const ZERO: CheckedMap = {
  terms: false,
  privacy: false,
  third: false,
  storage: false,
  marketing: false
};

export default function SignupTermsPage() {
  const router = useRouter();
  const checking = useGuestSessionGate();
  const [checked, setChecked] = useState<CheckedMap>(ZERO);

  useEffect(() => {
    const draft = readSignupDraft();
    if (!draft.entityTypeId || !draft.email) {
      router.replace(draft.entityTypeId ? "/signup/info" : "/signup");
    }
  }, [router]);

  if (checking) return <SessionGateSplash />;

  const allRequired = ITEMS.filter((i) => i.required).every((i) => checked[i.key]);
  const allChecked = ITEMS.every((i) => checked[i.key]);

  const toggle = (key: TermKey) => setChecked((p) => ({ ...p, [key]: !p[key] }));
  const toggleAll = () => {
    const next = !allChecked;
    setChecked({
      terms: next,
      privacy: next,
      third: next,
      storage: next,
      marketing: next
    });
  };

  const onNext = () => {
    if (!allRequired) return;
    writeSignupDraft({
      termsAcceptedAt: new Date().toISOString(),
      marketingAccepted: checked.marketing
    });
    router.push("/signup/email-verify");
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

      <SignupStepper step={3} />

      <div className="center-stage" style={{ alignItems: "flex-start", paddingTop: 40 }}>
        <div className="auth-card">
          <h1 className="auth-title">약관 및 개인정보 동의</h1>
          <p className="auth-subtitle">서비스 이용을 위해 아래 약관에 동의해 주세요.</p>

          <div className="terms-all-row" style={{ cursor: "pointer" }} onClick={toggleAll}>
            <Checkbox checked={allChecked} onChange={toggleAll}>
              <span style={{ fontWeight: 600, fontSize: 14 }}>전체 동의</span>
            </Checkbox>
          </div>

          <div style={{ marginTop: 4 }}>
            {ITEMS.map((item) => (
              <div key={item.key} className="terms-row">
                <Checkbox checked={checked[item.key]} onChange={() => toggle(item.key)}>
                  <span className="terms-label">{item.label}</span>
                </Checkbox>
                <button
                  type="button"
                  className="terms-view-btn"
                  onClick={(e) => e.stopPropagation()}
                >
                  보기
                </button>
              </div>
            ))}
          </div>

          <button
            type="button"
            className="btn btn-primary btn-block btn-lg"
            style={{ marginTop: 20 }}
            onClick={onNext}
            disabled={!allRequired}
          >
            다음 — 이메일 인증
          </button>
          <button
            type="button"
            className="btn btn-ghost btn-block"
            style={{ marginTop: 8 }}
            onClick={() => router.push("/signup/info")}
          >
            이전으로
          </button>
        </div>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
