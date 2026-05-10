"use client";

import { useRouter } from "next/navigation";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";

const SEND_ROWS = [
  ["받는 주소", "rHb9CJAWyB4rj91VRWn96DkukG4bwdtyTh", "mono"],
  ["보낼 금액", "1.00 XRP", "strong"],
  ["수수료", "0.000012 XRP", ""],
  ["합계", "1.000012 XRP", "accent"],
] as const;

export default function MobileXrpConfirmPage() {
  const router = useRouter();

  return (
    <section className="view xrp-flow-view">
      <MTopBar title="전송 확인" back="/m/xrp/send" />
      <div className="scroll content xrp-confirm-content">
        <div className="send-confirm-card">
          {SEND_ROWS.map(([label, value, tone]) => (
            <div key={label} className="send-confirm-row">
              <span>{label}</span>
              <strong className={tone}>{value}</strong>
            </div>
          ))}
        </div>
        <div className="m-info-box xrp-info-box">
          <div className="info-icon">
            <MIcon.lock />
          </div>
          <div className="info-text">
            <strong>블록체인 전송</strong>
            <p>전송이 완료되면 XRP Ledger에 영구 기록되며 취소할 수 없습니다.</p>
          </div>
        </div>
      </div>
      <div className="bottom-action xrp-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.replace("/m/xrp/complete")}
        >
          전송하기
        </button>
        <button
          type="button"
          className="secondary"
          onClick={() => router.replace("/m/xrp/send")}
        >
          수정하기
        </button>
      </div>
    </section>
  );
}
