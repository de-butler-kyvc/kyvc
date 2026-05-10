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
import { credentials, type CredentialSummary } from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  onBridgeAction,
  useBridgeAction,
  type WalletInfo,
} from "@/lib/m/android-bridge";
import { MOCK_CERTS, readHiddenCerts } from "@/lib/m/data";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "활성",
  ISSUED: "발급됨",
  REVOKED: "취소됨",
  EXPIRED: "만료",
};

function summaryToCert(s: CredentialSummary, i: number): CertItem {
  const palettes = [
    MOCK_CERTS[0]!.gradient,
    MOCK_CERTS[1]!.gradient,
    MOCK_CERTS[2]!.gradient,
  ];
  return {
    issuer: s.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
    title: s.credentialTypeCode ?? "법인 증명서",
    status: STATUS_LABEL[s.credentialStatusCode ?? ""] ?? "발급됨",
    id: `urn:cred:${s.credentialId}`,
    date: (s.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
    gradient: palettes[i % palettes.length] ?? MOCK_CERTS[0]!.gradient,
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
  const [certs, setCerts] = useState<CertItem[]>(MOCK_CERTS);
  const [hidden, setHidden] = useState<string[]>([]);
  const [walletInfo, setWalletInfo] = useState<WalletInfo | null>(null);

  // 1) 백엔드 API: 발급된 VC 목록 (서버 source of truth)
  useEffect(() => {
    setHidden(readHiddenCerts());
    (async () => {
      try {
        const list = await credentials.list();
        if (list.credentials.length) {
          setCerts(list.credentials.map(summaryToCert));
        }
      } catch {
        /* 비로그인/네트워크 실패 시 mock 유지 */
      }
    })();
  }, []);

  // 2) 브리지: 활성 지갑 정보
  useEffect(() => {
    if (!isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.getWalletInfo();
        if (r.ok) setWalletInfo(r);
      } catch {
        /* 세션 만료 가능성, 로그인 화면으로 보내지 않음 — 사용자가 명시적 인증 후 진입 */
      }
    })();
    // 지갑 목록도 가져와 LIST_WALLETS 이벤트로 갱신
    bridge.listWallets().catch(() => {});
    // 저장된 VC 동기화
    bridge.listCredentials().catch(() => {});
  }, []);

  // 3) 이벤트 구독: 다른 화면에서 활성 지갑이 바뀌어도 반영
  useBridgeAction("LIST_WALLETS", (r) => {
    if (!r.ok) return;
    const list = r.wallets as
      | Array<{ account?: string; isActive?: boolean; did?: string; name?: string }>
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
        gradient:
          MOCK_CERTS[i % MOCK_CERTS.length]!.gradient,
      })),
    );
  });

  // 4) 발급 완료 콜백 직접 구독 — 다른 화면에서 발급되어 돌아왔을 때도 새로고침
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
        <section className="wallet-hero">
          <div>
            <span>KYvC Business Wallet</span>
            <h1>{visible.length}개 VC 보유</h1>
            <p>
              {accountShort
                ? `Holder ${accountShort} · 동기화됨`
                : "DID 연결 정상 · 마지막 동기화 방금 전"}
            </p>
          </div>
        </section>

        <section className="wallet-actions">
          <button type="button" onClick={() => router.push("/m/vc/issue")}>
            <div className="action-icon-wrap">
              <MIcon.arrowDown />
            </div>
            <b>발급</b>
            <small>VC 가져오기</small>
          </button>
          <button type="button" onClick={() => router.push("/m/vp/scan")}>
            <div className="action-icon-wrap qr">
              <MIcon.qr />
            </div>
            <b>제출</b>
            <small>QR 스캔</small>
          </button>
        </section>

        <div className="xrp-banner">
          <div className="xrp-banner-top">
            <div className="xrp-banner-left">
              <div className="xrp-logo-wrap">
                <MIcon.xrp />
              </div>
              <div>
                <p className="xrp-title">XRP Ledger 기반 DID</p>
                <p className="xrp-sub">testnet · 블록체인으로 검증된 신원 증명</p>
              </div>
            </div>
            <span className="xrp-badge">연동됨</span>
          </div>
          <div className="xrp-btn-row">
            <button
              type="button"
              className="xrp-action-btn"
              onClick={() => router.push("/m/xrp/charge")}
            >
              <MIcon.zap /> XRP 충전
            </button>
            <button type="button" className="xrp-action-btn">
              <MIcon.link /> Explorer
            </button>
          </div>
        </div>

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
            {visible.map((c, i) => (
              <MCertCard
                key={c.id}
                cert={c}
                index={i}
                extra="stacked"
                onClick={() =>
                  router.push(`/m/vc/detail?id=${encodeURIComponent(c.id)}`)
                }
              />
            ))}
          </div>
        </section>
      </div>
      <MBottomNav active="home" />
    </section>
  );
}
