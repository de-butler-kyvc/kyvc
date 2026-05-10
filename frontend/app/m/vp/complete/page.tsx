"use client";

import { useRouter } from "next/navigation";

import { MIcon } from "@/components/m/icons";

export default function MobileVpCompletePage() {
  const router = useRouter();
  return (
    <section className="view wash">
      <div className="content full-w scroll center">
        <div className="success-circle">
          <MIcon.check />
        </div>
        <h1 className="headline m-auth-title mt-24">제출 완료</h1>
        <p className="subcopy">신한은행에 증명서 제출이 완료되었어요.</p>

        <div className="tx-details mt-24 text-left">
          <div className="tx-row">
            <span>제출 기관</span>
            <strong>신한은행</strong>
          </div>
          <div className="tx-row">
            <span>증명서</span>
            <strong>법인등록증명서 외 1건</strong>
          </div>
          <div className="tx-row">
            <span>제출 시각</span>
            <strong>{new Date().toLocaleString("ko-KR")}</strong>
          </div>
          <div className="tx-row">
            <span>트랜잭션</span>
            <strong className="tx-hash">0x7f3a92e1c4d28b1a...</strong>
          </div>
        </div>

        <div className="tx-banner mt-16">
          <div className="tx-banner-icon">
            <MIcon.shield />
          </div>
          <span>블록체인에 영구 기록되었어요</span>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.push("/m/transactions")}
        >
          거래 내역 보기
        </button>
        <button
          type="button"
          className="ghost"
          onClick={() => router.replace("/m/home")}
        >
          홈으로
        </button>
      </div>
    </section>
  );
}
