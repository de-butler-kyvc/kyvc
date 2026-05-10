"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { mSession, type XrpTransferResult } from "@/lib/m/session";

function shorten(value?: string) {
  if (!value) return "-";
  if (value.length <= 18) return value;
  return `${value.slice(0, 8)}...${value.slice(-6)}`;
}

export default function MobileXrpCompletePage() {
  const router = useRouter();
  const [result, setResult] = useState<XrpTransferResult | null>(null);

  useEffect(() => {
    const current = mSession.readXrpTransferResult();
    if (!current) {
      router.replace("/m/home");
      return;
    }
    setResult(current);
  }, [router]);

  const details = result
    ? [
        ["받는 주소", shorten(result.destinationAddress), "link"],
        ["전송 금액", `${result.amountXrp} XRP`, ""],
        ["수수료", `${result.feeXrp ?? "0.000012"} XRP`, ""],
        [
          "전송 시각",
          new Date(result.completedAt).toLocaleString("ko-KR"),
          "",
        ],
        ["트랜잭션 ID", shorten(result.txHash), "link"],
      ]
    : [];

  const goHome = () => {
    mSession.writeXrpTransfer(null);
    mSession.writeXrpTransferResult(null);
    router.replace("/m/home");
  };

  return (
    <section className="view xrp-flow-view">
      <div className="scroll content xrp-complete-content">
        <div className="success-circle">
          <MIcon.check />
        </div>
        <h1 className="headline">전송 완료</h1>
        <p className="subcopy">XRP가 성공적으로 전송되었습니다.</p>

        <div className="tx-details xrp-complete-details">
          {details.map(([label, value, tone]) => (
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
          onClick={goHome}
        >
          홈으로
        </button>
      </div>
    </section>
  );
}
