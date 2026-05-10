"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

export default function MobileXrpChargePage() {
  const router = useRouter();
  const [address, setAddress] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.getWalletDepositInfo();
        if (r.ok) {
          const addr =
            (r.receiveAddress as string | undefined) ??
            (r.account as string | undefined);
          if (addr) setAddress(addr);
        } else {
          setError(r.error ?? "입금 정보 조회 실패");
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : "브리지 호출 실패");
      }
    })();
  }, []);

  const onCopy = async () => {
    setCopied(false);
    if (isBridgeAvailable()) {
      try {
        const r = await bridge.copyWalletAddress();
        if (r.ok) {
          setCopied(true);
          setTimeout(() => setCopied(false), 2000);
          return;
        }
      } catch {
        /* fallthrough */
      }
    }
    if (address && navigator.clipboard) {
      await navigator.clipboard.writeText(address);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <section className="view wash">
      <MTopBar title="XRP 충전" back="/m/home" />
      <div className="scroll content">
        <h1 className="headline m-auth-title">
          지갑 활성화를 위해
          <br />
          XRP를 충전하세요
        </h1>
        <p className="subcopy">
          DID 및 증명서 발급 수수료로 소량의 XRP가 사용됩니다.
        </p>

        <div className="qr-panel mt-24">
          <h2>내 XRP 주소</h2>
          <div className="qr-box" />
          <p className="mt-8">
            <strong>{address ?? "(브리지 없음)"}</strong>
          </p>
          <p className="subcopy">
            데스티네이션 태그(Destination Tag)가
            <br />
            필요 없는 개인 지갑 주소입니다.
          </p>
          <button
            type="button"
            className="xrp-action-btn mt-16"
            onClick={onCopy}
            disabled={!address}
          >
            <MIcon.link /> {copied ? "복사 완료" : "주소 복사"}
          </button>
        </div>

        {error ? <p className="m-error">{error}</p> : null}

        <div className="m-info-box info-box mt-24">
          <div className="info-icon">
            <MIcon.xrp />
          </div>
          <div className="info-text">
            <strong>최초 활성화 (testnet)</strong>
            <p>
              XRP Ledger 규정에 따라 지갑 최초 활성화를 위해 최소 10 XRP가
              예약금으로 보관됩니다. 테스트넷에서는 faucet으로 충전하세요.
            </p>
          </div>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.replace("/m/home")}
        >
          충전 완료
        </button>
      </div>
    </section>
  );
}
