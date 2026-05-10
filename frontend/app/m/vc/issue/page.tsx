"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import {
  bridge,
  CORE_BASE_URL,
  isBridgeAvailable,
  onBridgeAction,
} from "@/lib/m/android-bridge";

type Step = "idle" | "requesting" | "saving" | "submitting" | "checking";

const STEP_LABEL: Record<Step, string> = {
  idle: "대기",
  requesting: "Issuer 응답 대기 중...",
  saving: "VC 저장 중...",
  submitting: "XRPL CredentialAccept 제출 중...",
  checking: "상태 확인 중...",
};

export default function MobileVcIssuePage() {
  const router = useRouter();
  const [selected, setSelected] = useState<"corp" | "seal">("corp");
  const [step, setStep] = useState<Step>("idle");
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  // ISSUER_CREDENTIAL_RECEIVED는 비동기. 호출과 별도로 구독.
  useEffect(() => {
    const off = onBridgeAction("ISSUER_CREDENTIAL_RECEIVED", async (r) => {
      if (!startedRef.current) return; // 이 화면에서 트리거한 경우만
      if (!r.ok) {
        setError(r.error ?? "Issuer 발급에 실패했습니다.");
        setStep("idle");
        return;
      }
      const credentialId = (r.credentialId as string | undefined) ?? "";
      const issuerAccount = (r.issuerAccount as string | undefined) ?? "";
      const holderAccount = (r.holderAccount as string | undefined) ?? "";
      const credentialType = (r.credentialType as string | undefined) ?? "";
      const sdJwt = (r.sdJwt as string | undefined) ?? "";
      const vcJson = (r.vcJson as string | undefined) ?? "";

      try {
        // 2) VC 저장
        setStep("saving");
        await bridge.saveVC({
          credentialId,
          ...(sdJwt ? { sdJwt, credential: sdJwt } : {}),
          ...(vcJson ? { vcJson } : {}),
        });

        // 3) XRPL CredentialAccept 제출
        if (credentialId) {
          setStep("submitting");
          await bridge.submitToXRPL({
            credentialId,
            issuerAccount,
            holderAccount,
            credentialType,
          });
        }

        // 4) 상태 조회 (active && accepted)
        if (credentialId) {
          setStep("checking");
          const status = await bridge.checkCredentialStatus({
            credentialId,
            issuerAccount,
            holderAccount,
            credentialType,
          });
          if (!status.ok || !status.accepted) {
            setError("XRPL 수락 상태 확인에 실패했습니다. 잠시 후 다시 시도해주세요.");
            setStep("idle");
            return;
          }
        }

        // 완료 → 축하 화면
        setStep("idle");
        router.replace("/m/vc/celebration");
      } catch (e) {
        setError(e instanceof Error ? e.message : "발급 처리 중 오류");
        setStep("idle");
      } finally {
        startedRef.current = false;
      }
    });
    return off;
  }, [router]);

  const onIssue = async () => {
    setError(null);
    if (!isBridgeAvailable()) {
      setError(
        "앱 내부 발급 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.",
      );
      return;
    }
    try {
      // 1) 활성 지갑 확인 (선행조건)
      const wallet = await bridge.getWalletInfo();
      if (!wallet.ok) {
        throw new Error(wallet.error ?? "활성 지갑을 확인할 수 없습니다.");
      }
      // 2) Issuer 발급 요청 (응답은 ISSUER_CREDENTIAL_RECEIVED 이벤트로)
      startedRef.current = true;
      setStep("requesting");
      const ack = await bridge.requestIssuerCredential({
        coreBaseUrl: CORE_BASE_URL,
        format: "dc+sd-jwt",
        jurisdiction: "KR",
        assuranceLevel: "STANDARD",
      });
      // 일부 구현은 즉시 ok=false ack를 반환할 수 있음 — 그 경우 즉시 실패 처리
      if (!ack.ok && !ack.credentialId) {
        throw new Error(ack.error ?? "발급 요청이 거절되었습니다.");
      }
      // 응답이 ack로 오는지 늦게 오는지에 따라 결과 처리는 onBridgeAction에 위임
    } catch (e) {
      setError(e instanceof Error ? e.message : "발급 요청 실패");
      setStep("idle");
      startedRef.current = false;
    }
  };

  const busy = step !== "idle";
  const bridgeMissing = typeof window !== "undefined" && !isBridgeAvailable();

  return (
    <section className="view wash">
      <MTopBar title="증명서 발급 신청" back="/m/home" />
      <div className="content scroll">
        <h1 className="headline m-auth-title">발급 신청</h1>
        <p className="subcopy">발급기관에서 증명서를 받아오세요.</p>

        <div className="issuer-card">
          <span>발급기관</span>
          <h2>법원행정처</h2>
          <em>인증됨</em>
        </div>

        <div className="list no-pad">
          <div
            className={`m-row${selected === "corp" ? " selected" : ""}`}
            onClick={() => setSelected("corp")}
          >
            <div className="m-row-icon">
              {selected === "corp" ? <MIcon.check /> : "○"}
            </div>
            <div className="m-row-body">
              <div className="m-row-title">법인등록증명서</div>
              <div className="m-row-sub">최근 3개월 이내 발급본</div>
            </div>
          </div>
          <div
            className={`m-row${selected === "seal" ? " selected" : ""}`}
            onClick={() => setSelected("seal")}
          >
            <div className="m-row-icon">
              {selected === "seal" ? <MIcon.check /> : "○"}
            </div>
            <div className="m-row-body">
              <div className="m-row-title">인감증명서</div>
              <div className="m-row-sub">선택 발급</div>
            </div>
          </div>
        </div>

        {busy ? (
          <p className="subcopy" style={{ marginTop: 16 }}>
            {STEP_LABEL[step]}
          </p>
        ) : null}
        {bridgeMissing ? (
          <p className="m-error">
            앱 내부 발급 모듈에 연결할 수 없습니다.
            <br />
            KYvC 앱에서 다시 열어 주세요.
          </p>
        ) : null}
        {error ? <p className="m-error">{error}</p> : null}
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onIssue}
          disabled={busy || bridgeMissing}
        >
          {busy ? STEP_LABEL[step] : "발급 신청하기"}
        </button>
      </div>
    </section>
  );
}
