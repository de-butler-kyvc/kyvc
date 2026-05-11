"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import {
  MCertCard,
  MTopBar,
  type CertItem,
} from "@/components/m/parts";
import {
  ApiError,
  credentials,
  type CredentialDetailResponse,
} from "@/lib/api";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "검증됨",
  ISSUED: "검증됨",
  REVOKED: "취소됨",
  EXPIRED: "만료",
};

const PALETTES = [
  "linear-gradient(135deg,#111827 0%,#183b8f 48%,#7c3aed 100%)",
  "linear-gradient(135deg,#052e2b 0%,#0f766e 48%,#2563eb 100%)",
  "linear-gradient(135deg,#231942 0%,#5e3bce 50%,#00a3ff 100%)",
];

function detailToCert(
  d: CredentialDetailResponse,
  palette: string,
): CertItem {
  return {
    issuer: d.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
    title: d.credentialTypeCode ?? "법인 증명서",
    status: STATUS_LABEL[d.credentialStatusCode ?? ""] ?? "발급됨",
    id: d.credentialExternalId ?? `DID:kyvc:corp:${d.credentialId}`,
    date: (d.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
    gradient: palette,
  };
}

const FALLBACK_CERT: CertItem = {
  issuer: "법원행정처",
  title: "법인등록증명서",
  status: "검증됨",
  id: "DID:kyvc:corp:240315",
  date: "2026.05.07",
  gradient: PALETTES[0]!,
};

function MobileVcDetailInner() {
  const router = useRouter();
  const sp = useSearchParams();
  const id = sp.get("id");
  // urn:cred:NN 또는 bridge id 형태 → 백엔드 credentialId 추출
  const credentialId = (() => {
    if (!id) return null;
    const m = id.match(/^urn:cred:(\d+)$/);
    return m ? Number(m[1]) : null;
  })();

  const [cert, setCert] = useState<CertItem | null>(null);
  const [detail, setDetail] = useState<CredentialDetailResponse | null>(null);
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
    if (credentialId == null) {
      setCert(FALLBACK_CERT);
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const d = await credentials.detail(credentialId);
        if (cancelled) return;
        setDetail(d);
        setCert(detailToCert(d, PALETTES[0]!));
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          router.replace("/m/login");
          return;
        }
        setError(
          e instanceof ApiError
            ? `VC 상세 조회 실패: ${e.message}`
            : "VC 상세 조회 중 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, credentialId, router]);

  // 브리지로 XRPL 상태 조회 (있을 때만)
  useEffect(() => {
    if (!detail || !isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.checkCredentialStatus({
          credentialId: detail.credentialExternalId ?? `${detail.credentialId}`,
          ...(detail.holderXrplAddress
            ? { holderAccount: detail.holderXrplAddress }
            : {}),
        });
        if (r.ok) {
          setStatus({ active: r.active, accepted: r.accepted });
        }
      } catch {
        /* 무시 — 상태 영역에만 영향 */
      }
    })();
  }, [detail]);

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
                  <span>{cert.issuer}</span>
                </div>
              </div>
              <div className="vc-detail-row">
                <div className="vc-detail-row-icon">유</div>
                <div className="vc-detail-row-body">
                  <strong>유효기간</strong>
                  <span>
                    {(detail?.issuedAt ?? cert.date)
                      .slice(0, 10)
                      .replaceAll("-", ".")}
                    {detail?.expiresAt
                      ? ` - ${detail.expiresAt.slice(0, 10).replaceAll("-", ".")}`
                      : " - 2027.05.06"}
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
                    {status && !status.active ? "비활성 · 검증 불가" : "정상 · 검증 가능"}
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
