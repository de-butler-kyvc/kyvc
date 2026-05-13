"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { mSession, type XrpTransferDraft } from "@/lib/m/session";

function toTotal(amountXrp?: string, feeXrp?: string) {
  const amount = Number(amountXrp ?? 0);
  const fee = Number(feeXrp ?? 0);
  if (!Number.isFinite(amount + fee)) return "-";
  return `${(amount + fee).toFixed(6).replace(/0+$/, "").replace(/\.$/, "")} XRP`;
}

export default function MobileXrpConfirmPage() {
  const router = useRouter();
  const [draft, setDraft] = useState<XrpTransferDraft | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const current = mSession.readXrpTransfer();
    if (!current) {
      router.replace("/m/xrp/send");
      return;
    }
    setDraft(current);
  }, [router]);

  const rows = draft
    ? [
        ["받는 주소", draft.destinationAddress, "mono"],
        ...(draft.destinationTag
          ? [["데스티네이션 태그", draft.destinationTag, ""] as const]
          : []),
        ["보낼 금액", `${draft.amountXrp} XRP`, "strong"],
        ["수수료", `${draft.feeXrp ?? "0.000012"} XRP`, ""],
        ["합계", toTotal(draft.amountXrp, draft.feeXrp), "accent"],
      ]
    : [];

  const onSubmit = async () => {
    if (!draft) return;
    setError(null);
    if (!isBridgeAvailable()) {
      setError("앱 내부 결제 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.");
      return;
    }
    setBusy(true);
    try {
      const authStatus = await bridge.getAuthStatus();
      if (authStatus.emailVerificationRequired) {
        setError("이메일 인증 후 송금할 수 있습니다.");
        setBusy(false);
        return;
      }
      const method = authStatus.availableMethods?.includes("biometric")
        ? "biometric"
        : authStatus.availableMethods?.includes("pin")
          ? "pin"
          : authStatus.availableMethods?.includes("pattern")
            ? "pattern"
            : null;
      if (!method) {
        setError("송금 재인증에 사용할 인증 수단이 없습니다.");
        setBusy(false);
        return;
      }
      if (!authStatus.xrpPaymentAuthReady) {
        const auth = await bridge.requestNativeAuth(method, "xrp-payment");
        if (!auth.ok || !auth.authenticated) {
          setError(auth.error ?? "송금 전 재인증이 필요합니다.");
          setBusy(false);
          return;
        }
      }
      const r = await bridge.submitXrpPayment({
        destinationAddress: draft.destinationAddress,
        ...(draft.destinationTag ? { destinationTag: draft.destinationTag } : {}),
        amountXrp: draft.amountXrp,
      });
      if (!r.ok) {
        setError(r.error ?? "XRP 전송에 실패했습니다.");
        setBusy(false);
        return;
      }
      mSession.writeXrpTransferResult({
        ...draft,
        txHash:
          (r.txHash as string | undefined) ??
          (r.hash as string | undefined) ??
          (r.transactionHash as string | undefined),
        ledgerIndex:
          (r.ledgerIndex as string | number | undefined) ??
          (r.ledger_index as string | number | undefined),
        completedAt: Date.now(),
      });
      router.replace("/m/xrp/complete");
    } catch (e) {
      setError(e instanceof Error ? e.message : "XRP 전송에 실패했습니다.");
      setBusy(false);
    }
  };

  return (
    <section className="view xrp-flow-view">
      <MTopBar title="전송 확인" back="/m/xrp/send" />
      <div className="scroll content xrp-confirm-content">
        <div className="send-confirm-card">
          {rows.map(([label, value, tone]) => (
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
        {error ? <p className="m-error">{error}</p> : null}
      </div>
      <div className="bottom-action xrp-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onSubmit}
          disabled={!draft || busy}
        >
          {busy ? "전송 중..." : "전송하기"}
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
