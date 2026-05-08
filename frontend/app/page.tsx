import Link from "next/link";

import { Logo } from "@/components/design/primitives";

const features = [
  {
    num: "01",
    title: "AI 서류 자동 분석",
    description: "등기사항전부증명서, 사업자등록증 자동 OCR · 검증"
  },
  {
    num: "02",
    title: "Verifiable Credential 발급",
    description: "표준 W3C VC 규격 기반 디지털 신원증명서"
  },
  {
    num: "03",
    title: "재사용 가능한 인증",
    description: "한 번 발급으로 여러 금융기관에 즉시 제출"
  }
];

export default function Home() {
  return (
    <div className="app-shell page-enter">
      <div className="entry-frame">
        <div className="entry-left">
          <div className="entry-nav" style={{ position: "relative", zIndex: 1 }}>
            <Logo theme="dark" size={26} />
          </div>
          <div className="entry-hero" style={{ position: "relative", zIndex: 1 }}>
            <div className="entry-badge">법인 KYC 자동 심사 플랫폼</div>
            <h1 className="entry-headline">
              서류 제출부터<br />
              VC 발급까지,<br />
              <em>자동으로</em>
            </h1>
            <p className="entry-desc">
              멀티모달 AI가 법인 KYC 서류를 자동 분석하고, 검증된 결과를 Verifiable Credential로
              발급합니다. 한 번 인증으로 여러 금융기관에서 재사용하세요.
            </p>
          </div>
          <div className="entry-meta" style={{ position: "relative", zIndex: 1 }}>
            © 2025 KYvC. All rights reserved.
          </div>
        </div>

        <div className="entry-right">
          <div className="entry-right-inner">
            <div className="feature-list">
              {features.map((f) => (
                <div key={f.num} className="feature-item">
                  <div className="feature-num">{f.num}</div>
                  <div>
                    <div className="feature-title">{f.title}</div>
                    <div className="feature-desc">{f.description}</div>
                  </div>
                </div>
              ))}
            </div>
            <div className="entry-actions">
              <Link
                href="/login"
                className="btn btn-primary btn-block btn-lg"
                style={{ textDecoration: "none" }}
              >
                로그인
              </Link>
              <div className="text-center text-muted" style={{ fontSize: 13 }}>
                계정이 없으신가요?{" "}
                <Link href="/signup">회원가입</Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
