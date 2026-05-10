"use client";

import { useRouter } from "next/navigation";

import { MCertCard, MLogo } from "@/components/m/parts";
import { MOCK_CERTS } from "@/lib/m/data";

export default function MobileOnboardingPage() {
  const router = useRouter();
  return (
    <section className="view wallet-dark intro">
      <div className="hero-orbit" />
      <div className="intro-cards">
        <MCertCard cert={MOCK_CERTS[0]} index={0} extra="float-a" />
        <MCertCard cert={MOCK_CERTS[1]} index={1} extra="float-b" />
      </div>
      <div className="content intro-copy">
        <MLogo className="intro-logo" />
        <h1 className="headline light">
          검증 가능한 법인 증명서를
          <br />
          가볍고 안전하게
        </h1>
        <p className="subcopy light">
          KYvC 지갑에서 VC 발급, 보관, QR 제출까지 한 번에 관리하세요.
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
