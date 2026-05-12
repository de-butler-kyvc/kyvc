"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import {
  bridge,
  CORE_BASE_URL,
  DEFAULT_VERIFIER_ENDPOINT,
  defaultPresentationDefinition,
  isBridgeAvailable,
} from "@/lib/m/android-bridge";
import { mobileVp } from "@/lib/api";
import { mSession } from "@/lib/m/session";

type Stage = "challenge" | "sign" | "submit" | "done";
const LABEL: Record<Stage, string> = {
  challenge: "검증 요청 정보를 확인하고 있어요",
  sign: "지갑에서 제출 증명을 만들고 있어요",
  submit: "검증 기관에 증명서를 제출하고 있어요",
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
        const selectedVcId = mSession.readSelectedVcId();
        const selectedCredentialId = selectedVcId ? Number(selectedVcId) : NaN;
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
        if (scan?.requestId && Number.isFinite(selectedCredentialId)) {
          await mobileVp.submitPresentation({
            requestId: scan.requestId,
            credentialId: selectedCredentialId,
            nonce,
            challenge: nonce,
            format: signed.format ?? "kyvc-sd-jwt-presentation-v1",
            vpJwt: signed.presentationJwt,
            presentation:
              signed.presentation ??
              ({
                format: signed.format ?? "kyvc-sd-jwt-presentation-v1",
                definitionId: "wallet-direct-kyc-test-v1",
                aud,
                nonce,
                sdJwtKb: signed.sdJwtKb,
              } as Record<string, unknown>),
          });
        } else {
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
        }

        // 완료
        setStage("done");
        mSession.writeScanResult(null);
        mSession.writeSelectedVcId(null);
        mSession.writeVpRequest(null);
        router.replace("/m/vp/complete");
      } catch (e) {
        setError(e instanceof Error ? e.message : "제출 실패");
      }
    };

    run();
  }, [router]);

  return (
    <section className="view wash mobile-process-view">
      <MTopBar title="증명서 제출" back="/m/home" />
      <div className="content scroll mobile-process-content">
        <section className={`process-panel${error ? " error" : ""}`}>
          <div className={`process-icon${error ? " error" : ""}`}>
            {error ? <MIcon.x /> : <MIcon.shield />}
          </div>
          <span className="process-eyebrow">Verifier</span>
          <h1>{error ? "증명서 제출에 실패했습니다" : "증명서를 제출하고 있어요"}</h1>
          <p>{error ?? LABEL[stage]}</p>
          {!error ? (
            <div className="process-steps" aria-label="증명서 제출 진행 상태">
              {(["challenge", "sign", "submit", "done"] as Stage[]).map((item) => (
                <span
                  key={item}
                  className={item === stage ? "active" : ""}
                  aria-current={item === stage ? "step" : undefined}
                />
              ))}
            </div>
          ) : null}
        </section>
        {error ? (
          <div className="process-actions">
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
