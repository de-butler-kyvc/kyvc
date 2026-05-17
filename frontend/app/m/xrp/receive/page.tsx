"use client";

import { useRouter } from "next/navigation";
import { QRCodeSVG } from "qrcode.react";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { ensureMobileSessionOwner } from "@/lib/m/wallet-bridge";

export default function MobileXrpReceivePage() {
  const router = useRouter();
  const [address, setAddress] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) {
      setError("KYvC 앱에서 지갑 주소를 확인할 수 있습니다.");
      return;
    }
    (async () => {
      try {
        await ensureMobileSessionOwner();
        const r = await bridge.getWalletDepositInfo();
        const addr =
          (r.receiveAddress as string | undefined) ??
          (r.account as string | undefined);
        if (r.ok && addr) {
          setAddress(addr);
          return;
        }
        setError(r.error ?? "입금 주소를 가져올 수 없습니다.");
      } catch (e) {
        setError(e instanceof Error ? e.message : "입금 주소 조회에 실패했습니다.");
      }
    })();
  }, []);

  const onCopy = async () => {
    if (isBridgeAvailable()) {
      try {
        const r = await bridge.copyWalletAddress();
        if (r.ok) {
          setCopied(true);
          setTimeout(() => setCopied(false), 1800);
          return;
        }
      } catch {
        /* browser fallback */
      }
    }
    if (address && navigator.clipboard) await navigator.clipboard.writeText(address);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <section className="view xrp-flow-view">
      <MTopBar title="XRP 받기" back="/m/home" />
      <div className="scroll content xrp-receive-content">
        <h1 className="headline">
          내 XRP 주소로
          <br />
          받으세요
        </h1>

        <div className="xrp-address-card">
          <h2>내 XRP 주소</h2>
          <div className="xrp-address-qr" aria-label="XRP 주소 QR">
            {address ? (
              <QRCodeSVG
                value={address}
                size={148}
                bgColor="#ffffff"
                fgColor="#07111f"
                level="M"
                marginSize={2}
              />
            ) : null}
          </div>
          <p>{address ?? "주소 확인 중"}</p>
          <button
            type="button"
            className="copy-pill"
            onClick={onCopy}
            disabled={!address}
          >
            <MIcon.link /> {copied ? "복사 완료" : "주소 복사"}
          </button>
        </div>
        <p className="subcopy xrp-receive-copy">
          아래 주소나 QR 코드를 공유하면 XRP를 받을 수 있습니다.
        </p>

        <div className="m-info-box xrp-info-box">
          <div className="info-icon">
            <MIcon.x />
          </div>
          <div className="info-text">
            <strong>데스티네이션 태그 불필요</strong>
            <p>개인 지갑 주소로, 별도의 태그 없이 받을 수 있습니다.</p>
          </div>
        </div>

        <div className="m-info-box xrp-info-box">
          <div className="info-icon">
            <MIcon.shield />
          </div>
          <div className="info-text">
            <strong>주의사항</strong>
            <p>XRP Ledger 네트워크의 XRP만 이 주소로 받을 수 있습니다.</p>
          </div>
        </div>
        {error ? <p className="m-error">{error}</p> : null}
      </div>
    </section>
  );
}
