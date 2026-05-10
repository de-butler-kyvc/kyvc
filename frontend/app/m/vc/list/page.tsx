"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { MCertCard, MTopBar, type CertItem } from "@/components/m/parts";
import { ApiError, credentials } from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  useBridgeAction,
} from "@/lib/m/android-bridge";
import { readHiddenCerts } from "@/lib/m/data";

const PALETTES = [
  "linear-gradient(135deg,#111827 0%,#183b8f 48%,#7c3aed 100%)",
  "linear-gradient(135deg,#052e2b 0%,#0f766e 48%,#2563eb 100%)",
  "linear-gradient(135deg,#231942 0%,#5e3bce 50%,#00a3ff 100%)",
];

type BridgeCred = {
  credentialId?: string;
  issuerAccount?: string;
  credentialType?: string;
  acceptedAt?: string;
  active?: boolean;
};

export default function MobileVcListPage() {
  const router = useRouter();
  const [certs, setCerts] = useState<CertItem[]>([]);
  const [hidden, setHidden] = useState<string[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // API 측 발급 이력
  useEffect(() => {
    setHidden(readHiddenCerts());
    let cancelled = false;
    (async () => {
      try {
        const list = await credentials.list();
        if (cancelled) return;
        setCerts(
          list.credentials.map((c, i) => ({
            issuer: c.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
            title: c.credentialTypeCode ?? "법인 증명서",
            status: "발급됨",
            id: `urn:cred:${c.credentialId}`,
            date:
              (c.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
            gradient: PALETTES[i % PALETTES.length]!,
          })),
        );
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          router.replace("/m/login");
          return;
        }
        setError(
          e instanceof ApiError
            ? `VC 목록 조회 실패: ${e.message}`
            : "VC 목록 조회 중 네트워크 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    if (isBridgeAvailable()) {
      bridge.listCredentials().catch(() => {});
    }
    return () => {
      cancelled = true;
    };
  }, [router]);

  useBridgeAction("LIST_CREDENTIALS", (r) => {
    if (!r.ok) return;
    const list = (r.credentials as BridgeCred[] | undefined) ?? [];
    if (!list.length) return;
    setCerts(
      list.map((c, i) => ({
        issuer: c.issuerAccount ?? "Issuer",
        title: c.credentialType ?? "법인 증명서",
        status: c.active ? "활성" : "비활성",
        id: c.credentialId ?? `bridge-${i}`,
        date: (c.acceptedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
        gradient: PALETTES[i % PALETTES.length]!,
      })),
    );
  });

  useBridgeAction("REFRESH_CREDENTIAL_STATUSES", (r) => {
    setRefreshing(false);
    if (!r.ok) {
      setError(r.error ?? "상태 갱신에 실패했습니다.");
      return;
    }
    if (isBridgeAvailable()) bridge.listCredentials().catch(() => {});
  });

  const onRefresh = async () => {
    if (!isBridgeAvailable()) {
      setError(
        "앱 내부 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.",
      );
      return;
    }
    setError(null);
    setRefreshing(true);
    try {
      await bridge.refreshAllCredentialStatuses();
    } catch (e) {
      setRefreshing(false);
      setError(
        e instanceof Error ? e.message : "상태 갱신 호출에 실패했습니다.",
      );
    }
  };

  const visible = certs.filter((c) => !hidden.includes(c.title));

  return (
    <section className="view wash">
      <MTopBar
        title="전체 증명서"
        back="/m/home"
        right={
          <button
            type="button"
            className="text-link"
            onClick={onRefresh}
            disabled={refreshing}
          >
            {refreshing ? "갱신 중..." : "상태 갱신"}
          </button>
        }
      />
      <div className="scroll content">
        {error ? <p className="m-error mt-16">{error}</p> : null}
        <div className="vc-grid mt-16">
          {loading ? (
            <p className="m-loading">불러오는 중…</p>
          ) : visible.length === 0 ? (
            <p
              className="subcopy"
              style={{ textAlign: "center", padding: "32px 0" }}
            >
              {error ? "VC를 불러올 수 없습니다." : "발급된 증명서가 없습니다."}
            </p>
          ) : (
            visible.map((c, i) => (
              <MCertCard
                key={c.id}
                cert={c}
                index={i}
                extra="flat-card"
                onClick={() =>
                  router.push(`/m/vc/detail?id=${encodeURIComponent(c.id)}`)
                }
              />
            ))
          )}
        </div>
      </div>
    </section>
  );
}
