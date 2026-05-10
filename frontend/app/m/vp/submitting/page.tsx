"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import {
  bridge,
  CORE_BASE_URL,
  DEFAULT_VERIFIER_ENDPOINT,
  defaultPresentationDefinition,
  isBridgeAvailable,
} from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

type Stage = "challenge" | "sign" | "submit" | "done";
const LABEL: Record<Stage, string> = {
  challenge: "Verifier에 nonce 요청 중...",
  sign: "SD-JWT+KB 서명 중...",
  submit: "Verifier에 제출 중...",
  done: "완료",
};

export default function MobileVpSubmittingPage() {
  const router = useRouter();
  const [stage, setStage] = useState<Stage>("challenge");
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const run = async () => {
      try {
        if (!isBridgeAvailable()) {
          throw new Error(
            "앱 내부 서명 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.",
          );
        }

        const scan = mSession.readScanResult();
        const aud = scan?.domain || scan?.coreBaseUrl || CORE_BASE_URL;
        const endpoint =
          scan?.endpoint?.replace("/challenges", "/verify") ||
          DEFAULT_VERIFIER_ENDPOINT;

        // 1) Verifier challenge (nonce/aud)
        setStage("challenge");
        let nonce = scan?.challenge;
        if (!nonce) {
          const ch = await bridge.requestVerifierChallenge({
            coreBaseUrl: scan?.coreBaseUrl || CORE_BASE_URL,
            aud,
            presentationDefinition: defaultPresentationDefinition(),
          });
          if (!ch.ok) throw new Error(ch.error ?? "nonce 요청 실패");
          nonce = ch.nonce ?? ch.challenge ?? "";
        }
        if (!nonce) throw new Error("nonce가 비어 있습니다.");

        // 2) SD-JWT+KB 생성
        setStage("sign");
        const signed = await bridge.signMessage({
          challenge: nonce,
          domain: aud,
        });
        if (!signed.ok) throw new Error(signed.error ?? "KB-JWT 생성 실패");

        // 3) Verifier 제출
        setStage("submit");
        const sub = await bridge.submitPresentationToVerifier({
          endpoint,
          presentation: {
            format: signed.format ?? "kyvc-sd-jwt-presentation-v1",
            definitionId: "wallet-direct-kyc-test-v1",
            aud,
            nonce,
            sdJwtKb: signed.sdJwtKb,
          },
          presentationJwt: signed.presentationJwt,
          sdJwtKb: signed.sdJwtKb,
          didDocument: signed.didDocument,
          challenge: nonce,
          domain: aud,
        });
        if (!sub.ok) throw new Error(sub.error ?? "Verifier 제출 실패");

        // 완료
        setStage("done");
        mSession.writeScanResult(null);
        router.replace("/m/vp/complete");
      } catch (e) {
        setError(e instanceof Error ? e.message : "제출 실패");
      }
    };

    run();
  }, [router]);

  return (
    <section className="view wash center">
      <div className="progress-card" />
      <div className="content">
        <h1 className="headline m-auth-title">{error ? "제출 실패" : "제출 중..."}</h1>
        <p className="subcopy">
          {error ?? LABEL[stage]}
        </p>
        {error ? (
          <div className="bottom-action" style={{ marginTop: 24 }}>
            <button
              type="button"
              className="primary"
              onClick={() => router.replace("/m/vp/submit")}
            >
              다시 시도
            </button>
            <button
              type="button"
              className="ghost"
              onClick={() => router.replace("/m/home")}
            >
              홈으로
            </button>
          </div>
        ) : null}
      </div>
    </section>
  );
}
