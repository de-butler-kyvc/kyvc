"use client";

import { useRouter } from "next/navigation";

import { MCertCard } from "@/components/m/parts";

const FIGMA_KYVC_LOGO =
  "https://www.figma.com/api/mcp/asset/c6fbedff-e5eb-4d29-9815-ca33494f9a9e";

const INTRO_CERTS = [
  {
    issuer: "국세청",
    title: "사업자등록증",
    status: "검증됨",
    id: "DID:kyvc:corp:240315",
    date: "2026.05.07",
    gradient: "linear-gradient(143.34deg,#472187 0%,#472187 100%)",
  },
  {
    issuer: "법원행정처",
    title: "법인등록증명서",
    status: "검증됨",
    id: "did:xrpl:rholder",
    date: "2026.05.07",
    gradient: "linear-gradient(143.34deg,#183b8f 0%,#7c3aed 100%)",
  },
  {
    issuer: "KYvC",
    title: "법인 KYC 증명서",
    status: "검증됨",
    id: "did:xrpl:rholder",
    date: "2026.04.28",
    gradient: "linear-gradient(141.57deg,#2563eb 0%,#2563eb 100%)",
  },
];

export default function MobileOnboardingPage() {
  const router = useRouter();
  return (
    <section className="view wallet-dark intro">
      <div className="hero-orbit" />
      <div className="intro-cards">
        <MCertCard cert={INTRO_CERTS[0]} index={0} extra="float-a" />
        <MCertCard cert={INTRO_CERTS[1]} index={1} extra="float-b" />
        <MCertCard cert={INTRO_CERTS[2]} index={2} extra="float-c" />
      </div>
      <div className="content intro-copy">
        <img className="intro-figma-logo" src={FIGMA_KYVC_LOGO} alt="KYvC" />
        <h1 className="headline light">
          기업 디지털 신원,
          <br />
          블록체인으로 더 안전하게
        </h1>
        <p className="subcopy light">
          스마트폰에 법인 KYC 증명서를 발급받고 QR코드 스캔으로 간편하게 인증하세요.
        </p>
        <button
          type="button"
          className="primary mt-24"
          onClick={() => router.push("/m/login")}
        >
          시작하기
        </button>
        <button
          type="button"
          className="ghost light mt-8"
          onClick={() => router.push("/m/signup")}
        >
          회원가입
        </button>
      </div>
    </section>
  );
}
