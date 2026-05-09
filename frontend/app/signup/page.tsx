"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { Logo, SignupStepper } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { SessionGateSplash, useGuestSessionGate } from "@/lib/session-gate";
import {
  SIGNUP_ENTITY_TYPES,
  readSignupDraft,
  setSignupEntityType,
  type SignupEntityTypeId
} from "@/lib/signup-flow";

const TypeIcon = ({ kind }: { kind: string }) => {
  switch (kind) {
    case "Users":
      return <Icon.Users />;
    case "UserCheck":
      return <Icon.UserCheck />;
    case "Shield":
      return <Icon.Shield />;
    case "Grid":
      return <Icon.Grid />;
    case "Hash":
      return <Icon.Hash />;
    case "File":
      return <Icon.File />;
    default:
      return <Icon.Building />;
  }
};

export default function SignupTypePage() {
  const router = useRouter();
  const checking = useGuestSessionGate();
  const [selected, setSelected] = useState<SignupEntityTypeId>("jusik");

  useEffect(() => {
    const draft = readSignupDraft();
    if (draft.entityTypeId) setSelected(draft.entityTypeId);
  }, []);

  const types = useMemo(() => SIGNUP_ENTITY_TYPES, []);

  if (checking) return <SessionGateSplash />;

  const onNext = () => {
    setSignupEntityType(selected);
    router.push("/signup/info");
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
        <div className="topbar-right">
          <Link className="topbar-nav-link" href="/login">
            로그인
          </Link>
          <Link className="topbar-nav-link active" href="/signup">
            회원가입
          </Link>
        </div>
      </div>

      <SignupStepper step={1} />

      <div className="center-stage" style={{ alignItems: "flex-start", paddingTop: 40 }}>
        <div className="auth-card">
          <h1 className="auth-title">회원가입 유형 선택</h1>
          <p className="auth-subtitle">가입 또는 진입할 유형을 선택하세요.</p>

          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {types.map((ty) => (
              <button
                key={ty.id}
                type="button"
                className={`type-option ${selected === ty.id ? "selected" : ""}`}
                onClick={() => setSelected(ty.id)}
              >
                <div className="type-option-icon">
                  <TypeIcon kind={ty.iconKey} />
                </div>
                <div>
                  <div className="type-option-title">{ty.label}</div>
                  <div className="type-option-desc">{ty.description}</div>
                </div>
              </button>
            ))}
          </div>

          <button
            type="button"
            className="btn btn-primary btn-block btn-lg"
            style={{ marginTop: 18 }}
            onClick={onNext}
          >
            다음
          </button>

          <div className="text-center mt-16 text-muted" style={{ fontSize: 13 }}>
            이미 계정이 있으신가요? <Link href="/login">로그인</Link>
          </div>
        </div>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
