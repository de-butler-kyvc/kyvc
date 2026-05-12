"use client";

import { useSearchParams } from "next/navigation";
import { QRCodeSVG } from "qrcode.react";
import { Suspense, useEffect, useMemo, useRef, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import {
  bridge,
  isBridgeAvailable,
  type NativeCredentialSummary,
} from "@/lib/m/android-bridge";
import { nativeCredentialTitle } from "@/lib/m/credential-summaries";

type QrPayload = Record<string, unknown>;

function compactPayload(payload: QrPayload): QrPayload {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) => value !== undefined && value !== null && value !== ""),
  );
}

function makeClientExpiresAt() {
  return new Date(Date.now() + 3 * 60 * 1000).toISOString();
}

function buildQrPayload(
  detail: NativeCredentialSummary,
  holderXrplAddress: string | null,
  fallbackExpiresAt: string,
) {
  return compactPayload({
    type: "KYVC_WALLET_CREDENTIAL",
    version: 1,
    source: "KYVC_MOBILE_WALLET",
    credentialId: detail.credentialId,
    credentialType: detail.credentialType,
    credentialKind: detail.credentialKind,
    format: detail.format,
    status: detail.status,
    issuerDid: detail.issuerDid,
    issuerAccount: detail.issuerAccount,
    holderDid: detail.holderDid,
    holderAccount: detail.holderAccount,
    holderXrplAddress,
    credentialExpiresAt: detail.expiresAt,
    qrExpiresAt: fallbackExpiresAt,
    expiresAt: fallbackExpiresAt,
  });
}

function formatRemaining(expiresAt: string | null) {
  if (!expiresAt) return "QR 유효 시간을 확인할 수 없습니다.";
  const diff = new Date(expiresAt).getTime() - Date.now();
  if (!Number.isFinite(diff)) return "QR 유효 시간을 확인할 수 없습니다.";
  if (diff <= 0) return "QR이 만료되었습니다.";
  const totalSeconds = Math.ceil(diff / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds} 동안 유효한 QR`;
}

function shortenAddress(value: string | null) {
  if (!value) return "주소 확인 중";
  if (value.length <= 18) return value;
  return `${value.slice(0, 8)}...${value.slice(-6)}`;
}

function MobileVcQrInner() {
  const sp = useSearchParams();
  const id = sp.get("id");

  const qrRef = useRef<HTMLDivElement>(null);
  const [detail, setDetail] = useState<NativeCredentialSummary | null>(null);
  const [holderXrplAddress, setHolderXrplAddress] = useState<string | null>(null);
  const [qrValue, setQrValue] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    if (!id) {
      setError("증명서 ID가 없습니다.");
      setLoading(false);
      return;
    }
    if (!isBridgeAvailable()) {
      setError("증명서 QR은 KYvC 앱 지갑에서 생성할 수 있습니다.");
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const summaries = await bridge.getCredentialSummaries();
        const d = (summaries.credentials ?? []).find(
          (credential) => credential.credentialId === id,
        );
        if (cancelled) return;
        if (!d) {
          setError("지갑에서 해당 증명서를 찾을 수 없습니다.");
          return;
        }
        setDetail(d);

        let bridgeAddress: string | null = null;
        try {
          const wallet = await bridge.getWalletInfo();
          bridgeAddress =
            wallet.ok && (wallet.holderAccount ?? wallet.account)
              ? (wallet.holderAccount ?? wallet.account ?? null)
              : null;
        } catch {
          bridgeAddress = null;
        }

        const address = d.holderAccount ?? bridgeAddress;
        const fallbackExpiresAt = makeClientExpiresAt();
        const payload = buildQrPayload(d, address, fallbackExpiresAt);

        if (!address) {
          setError("QR 생성에 필요한 XRPL 주소를 찾을 수 없습니다.");
          return;
        }

        if (cancelled) return;
        setHolderXrplAddress(address);
        setExpiresAt(fallbackExpiresAt);
        setQrValue(JSON.stringify(payload));
      } catch (e) {
        if (cancelled) return;
        setError(
          e instanceof Error
            ? `지갑 증명서 조회 실패: ${e.message}`
            : "지갑 증명서 조회 중 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  useEffect(() => {
    if (!expiresAt) return;
    const t = window.setInterval(() => setTick((v) => v + 1), 1000);
    return () => window.clearInterval(t);
  }, [expiresAt]);

  const title = detail ? nativeCredentialTitle(detail) : "법인 증명서";
  const remainingText = useMemo(() => {
    void tick;
    return formatRemaining(expiresAt);
  }, [expiresAt, tick]);
  const expired = remainingText.includes("만료");

  const copyQrValue = async () => {
    if (!qrValue) return;
    try {
      if (isBridgeAvailable()) {
        const r = await bridge.copyTextToClipboard(qrValue);
        if (r.ok) {
          setCopied(true);
          window.setTimeout(() => setCopied(false), 1800);
          return;
        }
      }
      if (navigator.clipboard) await navigator.clipboard.writeText(qrValue);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setError("QR payload 복사에 실패했습니다.");
    }
  };

  const saveQrSvg = () => {
    const svg = qrRef.current?.querySelector("svg");
    if (!svg) return;
    const source = new XMLSerializer().serializeToString(svg);
    const blob = new Blob([source], { type: "image/svg+xml;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `kyvc-${id ?? "credential"}-qr.svg`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const shareQr = async () => {
    if (!qrValue) return;
    try {
      if (navigator.share) {
        await navigator.share({
          title,
          text: qrValue,
        });
        return;
      }
      await copyQrValue();
    } catch {
      /* 사용자가 공유 시트를 닫은 경우는 무시 */
    }
  };

  return (
    <section className="view wash">
      <MTopBar
        title="내 QR 코드"
        back={
          id ? `/m/vc/detail?id=${encodeURIComponent(id)}` : "/m/home"
        }
      />
      <div className="content center">
        {loading ? (
          <p className="m-loading">불러오는 중…</p>
        ) : error ? (
          <p className="m-error">{error}</p>
        ) : (
          <div className="qr-panel">
            <div className="qr-panel-head">
              <span className="qr-chip">KYvC VC</span>
              <h2>{title}</h2>
              <p>{remainingText}</p>
            </div>
            <div className="qr-box real" ref={qrRef}>
              {qrValue ? (
                <QRCodeSVG
                  value={qrValue}
                  size={184}
                  bgColor="#ffffff"
                  fgColor="#07111f"
                  level="M"
                  marginSize={2}
                />
              ) : null}
            </div>
            <div className="qr-address">
              <span>XRPL Address</span>
              <strong>{shortenAddress(holderXrplAddress)}</strong>
            </div>
            {copied ? <p className="qr-toast">QR payload가 복사되었습니다.</p> : null}
          </div>
        )}
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="secondary"
          disabled={!qrValue || expired}
          onClick={saveQrSvg}
        >
          이미지 저장
        </button>
        <button
          type="button"
          className="primary"
          disabled={!qrValue || expired}
          onClick={shareQr}
          onDoubleClick={copyQrValue}
        >
          공유하기
        </button>
      </div>
    </section>
  );
}

export default function MobileVcQrPage() {
  return (
    <Suspense fallback={<div className="m-loading">불러오는 중…</div>}>
      <MobileVcQrInner />
    </Suspense>
  );
}
