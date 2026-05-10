"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

export default function MobileBiometricPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [bridgeReady, setBridgeReady] = useState<boolean | null>(null);
  const autoTriedRef = useRef(false);

  useEffect(() => {
    setBridgeReady(isBridgeAvailable());
  }, []);

  const onAuth = async () => {
    setError(null);
    setBusy(true);
    try {
      const r = await bridge.requestNativeAuth("biometric", "wallet-login");
      if (r.ok && r.authenticated) {
        router.replace("/m/home");
        return;
      }
      if (r.emailVerificationRequired) {
        setError("인증 5회 실패. 이메일 인증으로 잠금을 풀어주세요.");
        return;
      }
      setError(r.error ?? "생체 인증에 실패했습니다.");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "생체 인증 호출에 실패했습니다.",
      );
    } finally {
      setBusy(false);
    }
  };

  // 진입 시 브리지가 있으면 자동으로 한 번 시도 (네이티브 인증 다이얼로그 표시)
  useEffect(() => {
    if (autoTriedRef.current) return;
    if (bridgeReady !== true) return;
    autoTriedRef.current = true;
    onAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bridgeReady]);

  return (
    <section className="view wash">
      <MTopBar title="생체인증" back="/m/login" />
      <div className="content center spread">
        <h1 className="headline m-auth-title">지문을 인식해주세요</h1>
        <p className="subcopy">등록한 생체정보로 로그인합니다.</p>

        {bridgeReady === false ? (
          <p className="m-error">
            앱 내부 인증 모듈에 연결할 수 없습니다.
            <br />
            KYvC 앱에서 다시 열어 주세요.
          </p>
        ) : null}

        <button
          type="button"
          className="bio-circle"
          aria-label="생체인증 시작"
          onClick={onAuth}
          disabled={busy || bridgeReady !== true}
        >
          <MIcon.fingerprint />
        </button>
        <span className="status-pill">
          {busy ? "인증 중..." : "Touch ID 대기 중"}
        </span>
        {error && <p className="m-error">{error}</p>}
        <button
          type="button"
          className="ghost"
          onClick={() => router.push("/m/login/pin")}
        >
          PIN으로 로그인
        </button>
      </div>
    </section>
  );
}
