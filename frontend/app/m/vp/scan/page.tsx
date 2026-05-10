"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

export default function MobileVpScanPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    if (!isBridgeAvailable()) return; // 미리보기 환경에선 수동 클릭으로 진행
    startedRef.current = true;
    (async () => {
      try {
        const r = await bridge.scanQRCode("VP_REQUEST");
        if (!r.ok) {
          setError(r.error ?? "QR 스캔에 실패했습니다.");
          return;
        }
        // 결과를 sessionStorage에 보관 → submit/submitting에서 사용
        mSession.writeScanResult({
          qrData: r.qrData,
          actionType: r.actionType,
          coreBaseUrl: r.coreBaseUrl,
          challenge: r.challenge,
          domain: r.domain,
          endpoint: r.endpoint,
          receivedAt: Date.now(),
        });

        // actionType 기반 라우팅
        const type = r.actionType ?? "VP_REQUEST";
        if (type === "VC_ISSUE") {
          router.replace("/m/vc/issue");
        } else if (type === "LOGIN_REQUEST") {
          router.replace("/m/home");
        } else {
          router.replace("/m/vp/submit");
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : "브리지 호출 실패");
        startedRef.current = false;
      }
    })();
  }, [router]);

  return (
    <section className="view scan-view">
      <MTopBar title="QR Scan" back="/m/home" glass />
      <button
        type="button"
        className="scanner"
        aria-label="QR 스캔 완료(데모)"
        onClick={() => router.push("/m/vp/submit")}
      />
      <div className="content center">
        <h1 className="headline light">QR 코드를 스캔하세요</h1>
        <p className="subcopy light">제출 요청 QR을 화면 안에 맞춰주세요.</p>
        {error ? <p className="m-error">{error}</p> : null}
      </div>
    </section>
  );
}
