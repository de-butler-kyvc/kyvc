"use client";

import { useRouter } from "next/navigation";

import { MIcon } from "@/components/m/icons";

const DETAILS = [
  ["받는 주소", "rHb9CJA...tyTh", "link"],
  ["전송 금액", "1.00 XRP", ""],
  ["수수료", "0.000012 XRP", ""],
  ["전송 시각", "2026.05.09 14:32", ""],
  ["트랜잭션 ID", "7F3A92E1C4D2...", "link"],
] as const;

export default function MobileXrpCompletePage() {
  const router = useRouter();

  return (
    <section className="view xrp-flow-view">
      <div className="scroll content xrp-complete-content">
        <div className="success-circle">
          <MIcon.check />
        </div>
        <h1 className="headline">전송 완료</h1>
        <p className="subcopy">XRP가 성공적으로 전송되었습니다.</p>

        <div className="tx-details xrp-complete-details">
          {DETAILS.map(([label, value, tone]) => (
            <div key={label} className="tx-row">
              <span>{label}</span>
              <strong className={tone === "link" ? "tx-hash" : ""}>{value}</strong>
            </div>
          ))}
        </div>

        <div className="tx-banner xrp-ledger-banner">
          <div className="tx-banner-icon">
            <MIcon.shield />
          </div>
          <span>XRP Ledger에 영구 기록되었어요</span>
        </div>
      </div>
      <div className="bottom-action xrp-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.replace("/m/home")}
        >
          홈으로
        </button>
      </div>
    </section>
  );
}
