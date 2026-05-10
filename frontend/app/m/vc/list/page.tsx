"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { MCertCard, MTopBar, type CertItem } from "@/components/m/parts";
import { credentials } from "@/lib/api";
import { bridge, isBridgeAvailable, useBridgeAction } from "@/lib/m/android-bridge";
import { MOCK_CERTS, readHiddenCerts } from "@/lib/m/data";

type BridgeCred = {
  credentialId?: string;
  issuerAccount?: string;
  credentialType?: string;
  acceptedAt?: string;
  active?: boolean;
};

export default function MobileVcListPage() {
  const router = useRouter();
  const [certs, setCerts] = useState<CertItem[]>(MOCK_CERTS);
  const [hidden, setHidden] = useState<string[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  // API 측 발급 이력 (서버 인덱스)
  useEffect(() => {
    setHidden(readHiddenCerts());
    (async () => {
      try {
        const list = await credentials.list();
        if (list.credentials.length) {
          setCerts(
            list.credentials.map((c, i) => ({
              issuer: c.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
              title: c.credentialTypeCode ?? "법인 증명서",
              status: "발급됨",
              id: `urn:cred:${c.credentialId}`,
              date:
                (c.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
              gradient: MOCK_CERTS[i % MOCK_CERTS.length]!.gradient,
            })),
          );
        }
      } catch {
        /* mock */
      }
    })();
    // 브리지: 단말 저장본 + 상태 일괄 갱신
    if (isBridgeAvailable()) {
      bridge.listCredentials().catch(() => {});
    }
  }, []);

  // LIST_CREDENTIALS 응답 수신 시 화면 갱신
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
        gradient: MOCK_CERTS[i % MOCK_CERTS.length]!.gradient,
      })),
    );
  });

  // 갱신 응답
  useBridgeAction("REFRESH_CREDENTIAL_STATUSES", () => {
    setRefreshing(false);
    if (isBridgeAvailable()) bridge.listCredentials().catch(() => {});
  });

  const onRefresh = async () => {
    if (!isBridgeAvailable()) return;
    setRefreshing(true);
    try {
      await bridge.refreshAllCredentialStatuses();
    } catch {
      setRefreshing(false);
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
        <div className="vc-grid mt-16">
          {visible.map((c, i) => (
            <MCertCard
              key={c.id}
              cert={c}
              index={i}
              extra="flat-card"
              onClick={() =>
                router.push(`/m/vc/detail?id=${encodeURIComponent(c.id)}`)
              }
            />
          ))}
        </div>
      </div>
    </section>
  );
}
