"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import {
  MBottomNav,
  MCertCard,
  MTopBar,
  type CertItem,
} from "@/components/m/parts";
import {
  ApiError,
  credentials,
  type CredentialSummary,
} from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  onBridgeAction,
  useBridgeAction,
  type WalletInfo,
} from "@/lib/m/android-bridge";
import { readHiddenCerts } from "@/lib/m/data";

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

function summaryToCert(s: CredentialSummary, i: number): CertItem {
  return {
    issuer: s.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
    title: s.credentialTypeCode ?? "법인 증명서",
    status: STATUS_LABEL[s.credentialStatusCode ?? ""] ?? "발급됨",
    id: `urn:cred:${s.credentialId}`,
    date: (s.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
    gradient: PALETTES[i % PALETTES.length]!,
  };
}

type BridgeCred = {
  credentialId?: string;
  issuerAccount?: string;
  holderAccount?: string;
  credentialType?: string;
  acceptedAt?: string;
  active?: boolean;
};

export default function MobileHomePage() {
  const router = useRouter();
  const [certs, setCerts] = useState<CertItem[]>([]);
  const [hidden, setHidden] = useState<string[]>([]);
  const [walletInfo, setWalletInfo] = useState<WalletInfo | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [bridgeError, setBridgeError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // 백엔드 API: 발급된 VC 목록
  useEffect(() => {
    setHidden(readHiddenCerts());
    let cancelled = false;
    (async () => {
      try {
        const list = await credentials.list();
        if (cancelled) return;
        setCerts(list.credentials.map(summaryToCert));
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          // 세션 만료 → 로그인 화면
          router.replace("/m/login");
          return;
        }
        setApiError(
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

  // 브리지: 활성 지갑 정보 + 단말 저장본
  useEffect(() => {
    if (!isBridgeAvailable()) {
      setBridgeError(
        "앱 내부 지갑 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.",
      );
      return;
    }
    (async () => {
      try {
        const r = await bridge.getWalletInfo();
        if (r.ok) setWalletInfo(r);
        else setBridgeError(r.error ?? "지갑 정보를 가져올 수 없습니다.");
      } catch (e) {
        setBridgeError(
          e instanceof Error ? e.message : "브리지 호출에 실패했습니다.",
        );
      }
    })();
    bridge.listWallets().catch(() => {});
    bridge.listCredentials().catch(() => {});
  }, []);

  // 다른 화면에서 활성 지갑이 바뀌어도 반영
  useBridgeAction("LIST_WALLETS", (r) => {
    if (!r.ok) return;
    const list = r.wallets as
      | Array<{
          account?: string;
          isActive?: boolean;
          did?: string;
          name?: string;
        }>
      | undefined;
    const active = list?.find((w) => w.isActive);
    if (active?.account) {
      setWalletInfo((prev) => ({
        ...(prev ?? { ok: true }),
        account: active.account,
        did: active.did,
      }));
    }
  });

  useBridgeAction("LIST_CREDENTIALS", (r) => {
    if (!r.ok) return;
    const list = (r.credentials as BridgeCred[] | undefined) ?? [];
    if (list.length === 0) return;
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

  useEffect(() => {
    const off = onBridgeAction("ISSUER_CREDENTIAL_RECEIVED", (r) => {
      if (r.ok) {
        bridge.listCredentials().catch(() => {});
      }
    });
    return off;
  }, []);

  const visible = certs.filter((c) => !hidden.includes(c.title));
  const accountShort = walletInfo?.account
    ? `${walletInfo.account.slice(0, 6)}...${walletInfo.account.slice(-4)}`
    : null;
  const didShort = walletInfo?.did
    ? `${walletInfo.did.slice(0, 13)}...${walletInfo.did.slice(-4)}`
    : walletInfo?.account
      ? `${walletInfo.account.slice(0, 10)}...${walletInfo.account.slice(-4)}`
      : "did:xrpl:rhod...orpd";
  const balanceKrw = visible.length > 0 ? "₩ 123,000" : "₩ 0";

  return (
    <section className="view wash home-view">
      <MTopBar
        logo
        right={
          <button
            type="button"
            className="m-icon-btn"
            aria-label="설정"
            onClick={() => router.push("/m/settings")}
          >
            <MIcon.gear />
          </button>
        }
      />
      <div className="scroll home-scroll">
        {bridgeError ? (
          <div
            className="m-error"
            style={{ margin: "12px 18px 0", textAlign: "left" }}
          >
            {bridgeError}
          </div>
        ) : null}
        {apiError ? (
          <div
            className="m-error"
            style={{ margin: "12px 18px 0", textAlign: "left" }}
          >
            {apiError}
          </div>
        ) : null}

        <section className="wallet-hero xrp-home-hero">
          <h1>{balanceKrw}</h1>
          <button
            type="button"
            className="wallet-did-copy"
            onClick={() => router.push(walletInfo?.did ? "/m/did/register" : "/m/did/register")}
          >
            <span>{accountShort ?? didShort}</span>
            <MIcon.link />
          </button>
        </section>

        <section className="wallet-actions xrp-home-actions">
          <button type="button" onClick={() => router.push("/m/xrp/receive")}>
            <MIcon.arrowDown />
            <b>받기</b>
          </button>
          <button type="button" onClick={() => router.push("/m/xrp/send")}>
            <MIcon.arrowUpRight />
            <b>보내기</b>
          </button>
          <button type="button" onClick={() => router.push("/m/transactions")}>
            <MIcon.history />
            <b>내역</b>
          </button>
        </section>

        <section className="stack-section">
          <div className="m-section-title section-title stack-title">
            <h2>내 증명서</h2>
            <button
              type="button"
              className="text-link"
              onClick={() => router.push("/m/vc/list")}
            >
              전체보기
            </button>
          </div>
          <div className="credential-stack" aria-label="내 증명서 스택">
            {loading ? (
              <p className="m-loading">불러오는 중…</p>
            ) : visible.length === 0 ? (
              <p
                className="subcopy"
                style={{ padding: "20px 18px", textAlign: "center" }}
              >
                {apiError
                  ? "VC를 불러올 수 없습니다."
                  : "발급된 증명서가 없습니다."}
              </p>
            ) : (
              visible.map((c, i) => (
                <MCertCard
                  key={c.id}
                  cert={c}
                  index={i}
                  extra="stacked"
                  onClick={() =>
                    router.push(`/m/vc/detail?id=${encodeURIComponent(c.id)}`)
                  }
                />
              ))
            )}
          </div>
        </section>
      </div>
      <MBottomNav active="home" />
    </section>
  );
}
