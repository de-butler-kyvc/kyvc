"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

function extractXrpAddress(raw?: string) {
  const value = raw?.trim();
  if (!value) return "";
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    const candidate =
      parsed.destinationAddress ??
      parsed.address ??
      parsed.account ??
      parsed.holderXrplAddress ??
      parsed.xrplAddress;
    if (typeof candidate === "string") return candidate.trim();
  } catch {
    /* plain address or URI */
  }
  if (value.startsWith("xrpl:")) {
    try {
      const url = new URL(value);
      return url.pathname || url.searchParams.get("address") || "";
    } catch {
      return value.replace(/^xrpl:/, "");
    }
  }
  return value;
}

export default function MobileXrpSendPage() {
  const router = useRouter();
  const [destinationAddress, setDestinationAddress] = useState("");
  const [destinationTag, setDestinationTag] = useState("");
  const [amountXrp, setAmountXrp] = useState("1.00");
  const [availableXrp, setAvailableXrp] = useState("확인 중");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) {
      setAvailableXrp("앱에서 확인");
      return;
    }
    bridge
      .getWalletAssets()
      .then((r) => {
        if (!r.ok) {
          setAvailableXrp("확인 실패");
          return;
        }
        const balance =
          (r.availableXrp as string | number | undefined) ??
          (r.balanceXrp as string | number | undefined) ??
          (r.xrpBalance as string | number | undefined) ??
          (r.balance as string | number | undefined);
        setAvailableXrp(balance == null ? "확인 실패" : `${balance} XRP`);
      })
      .catch(() => setAvailableXrp("확인 실패"));
  }, []);

  const onScanAddress = async () => {
    setError(null);
    if (!isBridgeAvailable()) {
      setError("앱 내부 QR 스캐너에 연결할 수 없습니다.");
      return;
    }
    try {
      const r = await bridge.scanQRCode("XRP_ADDRESS");
      if (!r.ok) {
        setError(r.error ?? "주소 QR 스캔에 실패했습니다.");
        return;
      }
      const address = extractXrpAddress(r.qrData);
      if (!address) {
        setError("QR에서 XRP 주소를 찾을 수 없습니다.");
        return;
      }
      setDestinationAddress(address);
    } catch (e) {
      setError(e instanceof Error ? e.message : "주소 QR 스캔에 실패했습니다.");
    }
  };

  const onNext = () => {
    setError(null);
    const address = destinationAddress.trim();
    const amount = Number(amountXrp);
    if (!address.startsWith("r")) {
      setError("r로 시작하는 XRP 주소를 입력해 주세요.");
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setError("보낼 XRP 금액을 확인해 주세요.");
      return;
    }
    mSession.writeXrpTransfer({
      destinationAddress: address,
      ...(destinationTag.trim() ? { destinationTag: destinationTag.trim() } : {}),
      amountXrp,
      feeXrp: "0.000012",
      createdAt: Date.now(),
    });
    mSession.writeXrpTransferResult(null);
    router.push("/m/xrp/confirm");
  };

  return (
    <section className="view xrp-flow-view">
      <MTopBar title="XRP 보내기" back="/m/home" />
      <div className="scroll content xrp-send-content">
        <h1 className="headline">XRP 보내기</h1>
        <p className="subcopy">받는 사람의 XRP 주소와 금액을 입력하세요.</p>

        <div className="xrp-balance-chip">
          <span>보낼 수 있는 XRP</span>
          <strong>{availableXrp}</strong>
        </div>

        <label className="m-field-label">받는 주소</label>
        <div className="input-box">
          <input
            value={destinationAddress}
            onChange={(e) => setDestinationAddress(e.target.value)}
            placeholder="r로 시작하는 XRP 주소 입력"
          />
          <button
            type="button"
            className="scan-addr-btn"
            aria-label="주소 QR 스캔"
            onClick={onScanAddress}
          >
            <MIcon.qr />
          </button>
        </div>

        <label className="m-field-label">
          데스티네이션 태그 <em>(선택)</em>
        </label>
        <div className="input-box">
          <input
            value={destinationTag}
            onChange={(e) => setDestinationTag(e.target.value)}
            inputMode="numeric"
            placeholder="거래소 입금 시 필요할 수 있음"
          />
        </div>

        <label className="m-field-label">보낼 금액 (XRP)</label>
        <div className="input-box focus">
          <input
            value={amountXrp}
            onChange={(e) => setAmountXrp(e.target.value)}
            inputMode="decimal"
          />
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
        {error ? <p className="m-error">{error}</p> : null}
      </div>
      <div className="bottom-action xrp-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onNext}
        >
          다음 - 전송 확인
        </button>
        <button type="button" className="secondary" onClick={() => router.back()}>
          취소
        </button>
      </div>
    </section>
  );
}
