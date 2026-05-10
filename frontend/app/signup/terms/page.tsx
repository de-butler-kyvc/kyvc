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

const TERM_CONTENT: Record<
  TermKey,
  {
    title: string;
    version: string;
    summary: string;
    sections: { heading: string; body: string[] }[];
  }
> = {
  terms: {
    title: "서비스 이용약관",
    version: "시행일 2026.05.09 / 버전 1.0",
    summary:
      "본 약관은 KYvC 법인 KYC, VC 발급, VP 제출 서비스를 이용하기 위한 기본 조건과 이용자의 권리 및 의무를 정합니다.",
    sections: [
      {
        heading: "1. 목적",
        body: [
          "KYvC는 법인 및 단체의 신원확인(KYC), 검증 가능한 자격증명(VC) 발급, 검증 가능한 제출자료(VP) 제출을 지원하는 서비스입니다.",
          "이 약관은 회원이 서비스를 안전하고 투명하게 이용하는 데 필요한 기준을 정합니다."
        ]
      },
      {
        heading: "2. 회원의 의무",
        body: [
          "회원은 가입 및 심사 과정에서 정확하고 최신의 정보를 제공해야 하며, 타인의 정보 또는 허위 자료를 제출해서는 안 됩니다.",
          "계정, 인증수단, 발급받은 자격증명은 회원의 책임 하에 관리되어야 하며, 무단 사용이 의심되는 경우 즉시 KYvC에 알려야 합니다."
        ]
      },
      {
        heading: "3. 서비스 제공 및 제한",
        body: [
          "KYvC는 안정적인 서비스 제공을 위해 시스템 점검, 보안 조치, 정책 변경을 수행할 수 있습니다.",
          "회원이 법령, 본 약관, 보안 정책을 위반하거나 서비스 운영을 방해하는 경우 이용이 제한될 수 있습니다."
        ]
      },
      {
        heading: "4. 책임의 범위",
        body: [
          "KYvC는 고의 또는 중대한 과실이 없는 한 회원이 잘못 제출한 정보, 외부 기관의 심사 지연, 회원 단말 또는 네트워크 문제로 인한 손해에 대해 책임을 지지 않습니다.",
          "다만 관련 법령상 KYvC의 책임을 배제할 수 없는 경우에는 해당 법령을 따릅니다."
        ]
      }
    ]
  },
  privacy: {
    title: "개인정보 수집 및 이용 동의",
    version: "시행일 2026.05.09 / 버전 1.0",
    summary:
      "KYvC는 회원가입, 법인 확인, 인증 및 심사 진행을 위해 필요한 최소한의 개인정보를 수집하고 이용합니다.",
    sections: [
      {
        heading: "1. 수집 항목",
        body: [
          "담당자 이름, 이메일, 휴대전화번호, 소속 법인명, 직책, 인증 및 접속 기록, 서비스 이용 기록을 수집할 수 있습니다.",
          "법인 KYC 심사 과정에서 대표자, 대리인, 실소유자 등 관련자의 식별 정보와 제출 서류에 포함된 정보가 처리될 수 있습니다."
        ]
      },
      {
        heading: "2. 이용 목적",
        body: [
          "회원 식별, 계정 생성, 이메일 인증, 법인 KYC 접수 및 심사, VC 발급 안내, 보안 사고 예방, 고객 문의 응대를 위해 이용합니다.",
          "법령상 의무 이행, 분쟁 대응, 부정 이용 방지 및 서비스 품질 개선을 위해 필요한 범위에서 이용할 수 있습니다."
        ]
      },
      {
        heading: "3. 보유 기간",
        body: [
          "개인정보는 회원 탈퇴 또는 처리 목적 달성 시 지체 없이 파기합니다.",
          "단, 전자상거래, 금융거래, 신원확인, 감사 추적 등 관련 법령상 보관이 필요한 정보는 법령에서 정한 기간 동안 보관합니다."
        ]
      },
      {
        heading: "4. 동의 거부 권리",
        body: [
          "회원은 개인정보 수집 및 이용에 동의하지 않을 수 있습니다.",
          "다만 필수 항목에 대한 동의를 거부하는 경우 회원가입 및 KYC 서비스 이용이 제한될 수 있습니다."
        ]
      }
    ]
  },
  third: {
    title: "개인정보 제3자 제공 동의",
    version: "시행일 2026.05.09 / 버전 1.0",
    summary:
      "KYvC는 KYC 심사, 자격증명 발급, 검증 요청 처리에 필요한 범위에서 개인정보를 지정된 기관에 제공할 수 있습니다.",
    sections: [
      {
        heading: "1. 제공받는 자",
        body: [
          "KYC 심사기관, 자격증명 발급기관, 검증 요청기관, 관계 법령에 따라 정보 제공 권한이 있는 공공기관 또는 감독기관이 포함될 수 있습니다.",
          "제공 대상은 실제 서비스 이용 목적과 회원이 선택한 제출 절차에 따라 달라질 수 있습니다."
        ]
      },
      {
        heading: "2. 제공 항목",
        body: [
          "법인명, 사업자등록번호 또는 고유번호, 담당자 연락처, 대표자 및 대리인 식별 정보, 심사 상태, 제출 서류의 메타데이터 및 검증 결과가 제공될 수 있습니다.",
          "필요한 경우 VC 또는 VP에 포함된 클레임 정보가 제공될 수 있습니다."
        ]
      },
      {
        heading: "3. 제공 목적 및 보유 기간",
        body: [
          "제공 목적은 법인 실체 확인, 권한 확인, 자격증명 발급, 제출 자료 검증, 법령상 의무 이행입니다.",
          "제공받는 자는 목적 달성 후 또는 관계 법령에서 정한 기간이 지나면 해당 정보를 파기합니다."
        ]
      }
    ]
  },
  storage: {
    title: "원본서류 저장 동의",
    version: "시행일 2026.05.09 / 버전 1.0",
    summary:
      "KYvC는 KYC 심사와 사후 검증을 위해 회원이 제출한 원본서류 및 관련 파일을 안전하게 저장할 수 있습니다.",
    sections: [
      {
        heading: "1. 저장 대상",
        body: [
          "사업자등록증, 등기사항전부증명서, 정관, 위임장, 대표자 또는 대리인 확인 서류, 실소유자 확인 자료 등 KYC 과정에서 제출한 파일이 포함됩니다.",
          "파일명, 파일 형식, 업로드 시각, 제출자, 심사 상태와 같은 서류 메타데이터도 함께 저장될 수 있습니다."
        ]
      },
      {
        heading: "2. 저장 목적",
        body: [
          "KYC 심사 진행, 보완 요청, 분쟁 대응, 감사 추적, 위변조 방지, VC 발급 근거 확인을 위해 저장합니다.",
          "원본서류는 허가된 담당자와 시스템만 접근할 수 있도록 접근권한과 로그를 관리합니다."
        ]
      },
      {
        heading: "3. 보관 및 파기",
        body: [
          "원본서류는 처리 목적 달성 또는 법령상 보관 기간 종료 후 안전한 방식으로 삭제합니다.",
          "회원은 관련 법령과 내부 정책이 허용하는 범위에서 서류 삭제 또는 열람을 요청할 수 있습니다."
        ]
      }
    ]
  },
  marketing: {
    title: "마케팅 정보 수신 동의",
    version: "시행일 2026.05.09 / 버전 1.0",
    summary:
      "KYvC는 선택 동의한 회원에게 서비스 소식, 기능 업데이트, 이벤트, 교육 콘텐츠를 이메일 등으로 안내할 수 있습니다.",
    sections: [
      {
        heading: "1. 수신 내용",
        body: [
          "신규 기능, 점검 및 개선 안내, 보안 캠페인, 웨비나, 리포트, 이벤트 및 프로모션 정보가 포함될 수 있습니다.",
          "중요한 서비스 운영 고지나 보안 알림은 마케팅 수신 동의 여부와 관계없이 발송될 수 있습니다."
        ]
      },
      {
        heading: "2. 수신 방법",
        body: [
          "가입 이메일, 담당자 이메일, 서비스 내 알림 등 회원이 제공하거나 설정한 연락 수단으로 안내합니다."
        ]
      },
      {
        heading: "3. 철회",
        body: [
          "회원은 언제든지 마케팅 수신 동의를 철회할 수 있으며, 철회 후에는 선택 마케팅 안내가 발송되지 않습니다.",
          "동의 철회는 필수 서비스 이용에는 영향을 주지 않습니다."
        ]
      }
    ]
  }
};

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
  const [activeTerm, setActiveTerm] = useState<TermKey | null>(null);

  useEffect(() => {
    const draft = readSignupDraft();
    if (!draft.entityTypeId || !draft.email) {
      router.replace(draft.entityTypeId ? "/signup/info" : "/signup");
    }
  }, [router]);

  if (checking) return <SessionGateSplash />;

  const allRequired = ITEMS.filter((i) => i.required).every((i) => checked[i.key]);
  const allChecked = ITEMS.every((i) => checked[i.key]);
  const activeTermContent = activeTerm ? TERM_CONTENT[activeTerm] : null;

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
                  onClick={(e) => {
                    e.stopPropagation();
                    setActiveTerm(item.key);
                  }}
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

      {activeTermContent && (
        <div
          className="terms-modal-backdrop"
          role="presentation"
          onClick={() => setActiveTerm(null)}
        >
          <div
            className="terms-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="terms-modal-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="terms-modal-head">
              <div>
                <h2 id="terms-modal-title">{activeTermContent.title}</h2>
                <p>{activeTermContent.version}</p>
              </div>
              <button
                type="button"
                className="terms-modal-close"
                aria-label="약관 닫기"
                onClick={() => setActiveTerm(null)}
              >
                닫기
              </button>
            </div>
            <div className="terms-modal-body">
              <p className="terms-modal-summary">{activeTermContent.summary}</p>
              {activeTermContent.sections.map((section) => (
                <section key={section.heading} className="terms-modal-section">
                  <h3>{section.heading}</h3>
                  {section.body.map((paragraph) => (
                    <p key={paragraph}>{paragraph}</p>
                  ))}
                </section>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
