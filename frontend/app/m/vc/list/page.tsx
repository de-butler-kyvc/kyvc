"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { MCertCard, MTopBar, type CertItem } from "@/components/m/parts";
import { ApiError, credentials } from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  useBridgeAction,
  type NativeCredentialSummary,
} from "@/lib/m/android-bridge";
import { apiSummaryToCert, nativeSummaryToCert } from "@/lib/m/credential-summaries";
import { readHiddenCerts } from "@/lib/m/data";

export default function MobileVcListPage() {
  const router = useRouter();
  const [certs, setCerts] = useState<CertItem[]>([]);
  const [hidden, setHidden] = useState<string[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Android WebView에서는 네이티브 지갑의 credential summaries를 사용한다.
  useEffect(() => {
    setHidden(readHiddenCerts());
    let cancelled = false;
    (async () => {
      try {
        if (isBridgeAvailable()) {
          const list = await bridge.getCredentialSummaries();
          if (cancelled) return;
          setCerts((list.credentials ?? []).map(nativeSummaryToCert));
          setError(null);
          return;
        }
        const list = await credentials.list();
        if (cancelled) return;
        setCerts(list.credentials.map(apiSummaryToCert));
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
    return () => {
      cancelled = true;
    };
  }, [router]);

  useBridgeAction("GET_CREDENTIAL_SUMMARIES", (r) => {
    if (!r.ok) return;
    const list = (r.credentials as NativeCredentialSummary[] | undefined) ?? [];
    setCerts(list.map(nativeSummaryToCert));
  });

  useBridgeAction("REFRESH_CREDENTIAL_STATUSES", (r) => {
    setRefreshing(false);
    if (!r.ok) {
      setError(r.error ?? "상태 갱신에 실패했습니다.");
      return;
    }
    if (isBridgeAvailable()) bridge.getCredentialSummaries().catch(() => {});
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
