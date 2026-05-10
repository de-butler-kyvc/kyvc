"use client";

import { useRouter } from "next/navigation";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";

export default function MobileXrpSendPage() {
  const router = useRouter();

  return (
    <section className="view xrp-flow-view">
      <MTopBar title="XRP 보내기" back="/m/home" />
      <div className="scroll content xrp-send-content">
        <h1 className="headline">XRP 보내기</h1>
        <p className="subcopy">받는 사람의 XRP 주소와 금액을 입력하세요.</p>

        <div className="xrp-balance-chip">
          <span>보낼 수 있는 XRP</span>
          <strong>2.48 XRP</strong>
        </div>

        <label className="m-field-label">받는 주소</label>
        <div className="input-box">
          <input placeholder="r로 시작하는 XRP 주소 입력" />
          <button type="button" className="scan-addr-btn" aria-label="주소 QR 스캔">
            <MIcon.qr />
          </button>
        </div>

        <label className="m-field-label">
          데스티네이션 태그 <em>(선택)</em>
        </label>
        <div className="input-box">
          <input placeholder="거래소 입금 시 필요할 수 있음" />
        </div>

        <label className="m-field-label">보낼 금액 (XRP)</label>
        <div className="input-box focus">
          <input defaultValue="1.00" inputMode="decimal" />
          <span className="send-unit">XRP</span>
        </div>

        <div className="send-fee-row">
          <span>예상 수수료</span>
          <span>≈ 0.000012 XRP</span>
        </div>

        <div className="m-info-box xrp-info-box">
          <div className="info-icon">
            <MIcon.shield />
          </div>
          <div className="info-text">
            <strong>전송 전 확인</strong>
            <p>잘못된 주소로 전송된 XRP는 복구할 수 없습니다.</p>
          </div>
        </div>
      </div>
      <div className="bottom-action xrp-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.push("/m/xrp/confirm")}
        >
          다음 — 전송 확인
        </button>
        <button type="button" className="secondary" onClick={() => router.back()}>
          취소
        </button>
      </div>
    </section>
  );
}
