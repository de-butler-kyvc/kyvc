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
  ACTIVE: "활성",
  ISSUED: "발급됨",
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
    id: `urn:cred:${d.credentialId}`,
    date: (d.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
    gradient: palette,
  };
}

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
      setError("증명서 ID 형식이 잘못되었습니다.");
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
    <section className="view wash">
      <MTopBar title="VC 상세" back="/m/home" />
      <div className="scroll">
        {loading ? <p className="m-loading">불러오는 중…</p> : null}
        {error ? <p className="m-error mt-16">{error}</p> : null}
        {cert ? (
          <>
            <MCertCard cert={cert} index={0} extra="flat-card" />
            <div className="list">
              <div className="m-row">
                <div className="m-row-icon">발</div>
                <div className="m-row-body">
                  <div className="m-row-title">발급기관</div>
                  <div className="m-row-sub">{cert.issuer}</div>
                </div>
              </div>
              <div className="m-row">
                <div className="m-row-icon">기</div>
                <div className="m-row-body">
                  <div className="m-row-title">유효기간</div>
                  <div className="m-row-sub">
                    {detail?.issuedAt?.slice(0, 10) ?? "-"}
                    {detail?.expiresAt
                      ? ` ~ ${detail.expiresAt.slice(0, 10)}`
                      : ""}
                  </div>
                </div>
              </div>
              <div className="m-row">
                <div
                  className={`m-row-icon${status?.active ? " green" : ""}`}
                >
                  {status?.active ? <MIcon.check /> : "-"}
                </div>
                <div className="m-row-body">
                  <div className="m-row-title">상태</div>
                  <div className="m-row-sub">
                    {status
                      ? `${status.active ? "활성" : "비활성"} · ${status.accepted ? "수락됨" : "미수락"}`
                      : "확인 중"}
                  </div>
                </div>
              </div>
            </div>
          </>
        ) : null}
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          disabled={!cert}
          onClick={() =>
            cert &&
            router.push(`/m/vc/qr?id=${encodeURIComponent(cert.id)}`)
          }
        >
          내 QR 보기
        </button>
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
