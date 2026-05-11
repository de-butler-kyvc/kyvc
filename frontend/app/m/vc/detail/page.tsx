"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import {
  MCertCard,
  MTopBar,
  type CertItem,
} from "@/components/m/parts";
import {
  bridge,
  isBridgeAvailable,
  type NativeCredentialSummary,
} from "@/lib/m/android-bridge";
import {
  nativeCredentialIssuer,
  nativeSummaryToCert,
} from "@/lib/m/credential-summaries";

function MobileVcDetailInner() {
  const sp = useSearchParams();
  const id = sp.get("id");

  const [cert, setCert] = useState<CertItem | null>(null);
  const [nativeDetail, setNativeDetail] = useState<NativeCredentialSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<{
    active?: boolean;
    accepted?: boolean;
  } | null>(null);

  useEffect(() => {
    if (!id) {
      setError("증명서 ID가 없습니다.");
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        if (!isBridgeAvailable()) {
          if (!cancelled) {
            setError("증명서 상세는 KYvC 앱 지갑에서 확인할 수 있습니다.");
          }
          return;
        }
        const list = await bridge.getCredentialSummaries();
        if (cancelled) return;
        const found = (list.credentials ?? []).find(
          (credential) => credential.credentialId === id,
        );
        if (!found) {
          setError("지갑에서 해당 증명서를 찾을 수 없습니다.");
          return;
        }
        setNativeDetail(found);
        setCert(nativeSummaryToCert(found, 0));
      } catch (e) {
        if (cancelled) return;
        setError(
          e instanceof Error
            ? `지갑 증명서 상세 조회 실패: ${e.message}`
            : "지갑 증명서 상세 조회 중 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  // 브리지로 XRPL 상태 조회 (있을 때만)
  useEffect(() => {
    if (!nativeDetail || !isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.checkCredentialStatus({
          credentialId: nativeDetail.credentialId,
          ...(nativeDetail?.holderAccount
            ? { holderAccount: nativeDetail.holderAccount }
            : {}),
          ...(nativeDetail?.issuerAccount
            ? { issuerAccount: nativeDetail.issuerAccount }
            : {}),
          ...(nativeDetail?.credentialType
            ? { credentialType: nativeDetail.credentialType }
            : {}),
        });
        if (r.ok) {
          setStatus({ active: r.active, accepted: r.accepted });
        }
      } catch {
        /* 무시 — 상태 영역에만 영향 */
      }
    })();
  }, [nativeDetail]);

  return (
    <section className="view wash vc-detail-view">
      <MTopBar title="증명서 상세" back="/m/home" />
      <div className="scroll vc-detail-scroll">
        {loading ? <p className="m-loading">불러오는 중…</p> : null}
        {error ? <p className="m-error mt-16">{error}</p> : null}
        {cert ? (
          <>
            <MCertCard cert={cert} index={0} extra="detail-card" />
            <div className="vc-detail-list">
              <div className="vc-detail-row">
                <div className="vc-detail-row-icon">발</div>
                <div className="vc-detail-row-body">
                  <strong>발급기관</strong>
                  <span>{nativeDetail ? nativeCredentialIssuer(nativeDetail) : cert.issuer}</span>
                </div>
              </div>
              <div className="vc-detail-row">
                <div className="vc-detail-row-icon">유</div>
                <div className="vc-detail-row-body">
                  <strong>유효기간</strong>
                  <span>
                    {(nativeDetail?.issuedAt ?? cert.date)
                      .slice(0, 10)
                      .replaceAll("-", ".")}
                    {nativeDetail?.expiresAt
                      ? ` - ${nativeDetail.expiresAt.slice(0, 10).replaceAll("-", ".")}`
                      : " -"}
                  </span>
                </div>
              </div>
              <div className="vc-detail-row">
                <div className="vc-detail-row-icon">
                  <MIcon.check />
                </div>
                <div className="vc-detail-row-body">
                  <strong>상태</strong>
                  <span>
                    {nativeDetail?.statusLabel ??
                      (status && !status.active ? "비활성 · 검증 불가" : "정상 · 검증 가능")}
                  </span>
                </div>
              </div>
            </div>
          </>
        ) : null}
      </div>
    </section>
  );
}

export default function MobileVcDetailPage() {
  return (
    <Suspense fallback={<div className="m-loading">불러오는 중…</div>}>
      <MobileVcDetailInner />
    </Suspense>
  );
}
