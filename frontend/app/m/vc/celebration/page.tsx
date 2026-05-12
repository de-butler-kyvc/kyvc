"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { mSession, type VcIssueResult } from "@/lib/m/session";

function shortTx(value?: string) {
  if (!value) return "-";
  if (value.length <= 18) return value;
  return `${value.slice(0, 12)}...`;
}

function formatDate(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("ko-KR");
}

export default function MobileVcCelebrationPage() {
  const router = useRouter();
  const [result, setResult] = useState<VcIssueResult | null>(null);

  useEffect(() => {
    setResult(mSession.readVcIssueResult());
  }, []);

  const credentialId = result?.credentialId;
  const credentialTitle = result?.credentialTitle ?? "법인등록증명서";
  const issuedAt = result?.issuedAt ?? result?.savedAt;
  const txHash = result?.txHash;

  return (
    <section className="view wash">
      <div className="content full-w scroll center">
        <div className="success-circle">
          <MIcon.check />
        </div>
        <h1 className="headline m-auth-title mt-24">발급 완료</h1>
        <p className="subcopy">
          법인등록증명서가 지갑에 안전하게 저장되었어요.
        </p>

        <div className="tx-details mt-24 text-left">
          <div className="tx-row">
            <span>발급 기관</span>
            <strong>{result?.issuerName ?? "KYvC 인증기관"}</strong>
          </div>
          <div className="tx-row">
            <span>증명서</span>
            <strong>{credentialTitle}</strong>
          </div>
          <div className="tx-row">
            <span>발급 시각</span>
            <strong>{formatDate(issuedAt)}</strong>
          </div>
          <div className="tx-row">
            <span>트랜잭션</span>
            <strong className="tx-hash">{shortTx(txHash)}</strong>
          </div>
        </div>

        {txHash ? (
          <div className="tx-banner mt-16">
            <div className="tx-banner-icon">
              <MIcon.shield />
            </div>
            <span>블록체인에 영구 기록되었어요</span>
          </div>
        ) : null}
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => {
            if (credentialId) {
              router.push(`/m/vc/detail?id=${encodeURIComponent(String(credentialId))}`);
              return;
            }
            router.push("/m/home");
          }}
        >
          증명서 보기
        </button>
        <button
          type="button"
          className="ghost"
          onClick={() => {
            mSession.writeVcIssueResult(null);
            router.replace("/m/home");
          }}
        >
          홈으로
        </button>
      </div>
    </section>
  );
}
