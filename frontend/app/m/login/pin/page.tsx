"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

const PIN_LENGTH = 4;

/**
 * PIN 입력 화면.
 * 보안 정책에 따라 PIN 원문은 웹에서 브리지로 보내지 않는다.
 * 4자리 입력이 완료되면 네이티브 PIN 인증창을 띄우는 트리거(`requestNativeAuth`)만 호출한다.
 *
 * 브리지가 없으면(브라우저 직접 접근) 진행 불가 — 인증을 건너뛰고 홈 진입은 허용하지 않는다.
 */
export default function MobilePinPage() {
  const router = useRouter();
  const [filled, setFilled] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [remaining, setRemaining] = useState<number | null>(null);
  const [bridgeReady, setBridgeReady] = useState<boolean | null>(null);

  useEffect(() => {
    setBridgeReady(isBridgeAvailable());
  }, []);

  const trigger = async () => {
    setError(null);
    setBusy(true);
    try {
      const r = await bridge.requestNativeAuth("pin", "wallet-login");
      setRemaining(r.remainingAttempts ?? null);
      if (r.ok && r.authenticated) {
        router.replace("/m/home");
        return;
      }
      if (r.emailVerificationRequired) {
        setError("인증 5회 실패. 이메일 인증으로 잠금을 풀어주세요.");
        return;
      }
      setError(r.error ?? "PIN 인증에 실패했습니다.");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "PIN 인증 호출에 실패했습니다.",
      );
    } finally {
      setBusy(false);
      setFilled(0);
    }
  };

  const onKey = (key: string) => {
    if (busy) return;
    if (bridgeReady === false) return;
    if (key === "⌫") {
      setFilled((f) => Math.max(0, f - 1));
      return;
    }
    if (/^\d$/.test(key)) {
      const next = Math.min(PIN_LENGTH, filled + 1);
      setFilled(next);
      if (next === PIN_LENGTH) {
        trigger();
      }
    }
  };

  const keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"];

  return (
    <section className="view wash">
      <MTopBar title="PIN 로그인" back="/m/login" />
      <div className="content center spread">
        <h1 className="headline m-auth-title">
          PIN 번호를
          <br />
          입력하세요
        </h1>
        <p className="subcopy">지갑 잠금을 해제합니다.</p>

        {bridgeReady === false ? (
          <p className="m-error">
            앱 내부 인증 모듈에 연결할 수 없습니다.
            <br />
            KYvC 앱에서 다시 열어 주세요.
          </p>
        ) : null}

        <div className="pin-dots">
          {Array.from({ length: PIN_LENGTH }).map((_, i) => (
            <i key={i} className={i < filled ? "filled" : ""} />
          ))}
        </div>
        {typeof remaining === "number" && remaining < 5 && remaining > 0 ? (
          <p className="subcopy" style={{ color: "var(--m-warn,#f59e0b)" }}>
            남은 시도 {remaining}회
          </p>
        ) : null}
        {error && <p className="m-error">{error}</p>}
        <div className="keypad">
          {keys.map((k, i) => (
            <button
              key={i}
              type="button"
              className="key"
              disabled={k === "" || busy || bridgeReady === false}
              style={k === "" ? { visibility: "hidden" } : undefined}
              onClick={() => onKey(k)}
            >
              {k}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
