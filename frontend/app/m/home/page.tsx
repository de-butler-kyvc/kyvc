"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

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
  const [walletSheetOpen, setWalletSheetOpen] = useState(false);
  const [inactiveQrOpen, setInactiveQrOpen] = useState(false);

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
        if (e instanceof ApiError && e.status === 403) {
          setCerts([]);
          setApiError(null);
          return;
        }
        setApiError(
          e instanceof ApiError
            ? `VC 목록 조회 실패: ${e.message}`
            : "VC 목록 조회 중 네트워크 오류가 발생했습니다.",
        );
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [router]);

  // 브리지: 활성 지갑 정보 + 단말 저장본
  useEffect(() => {
    if (!isBridgeAvailable()) {
      return;
    }
    (async () => {
      try {
        const r = await bridge.getWalletInfo();
        if (r.ok) setWalletInfo(r);
      } catch {
        // Native-only wallet state is represented as inactive in the web UI.
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
  const walletLabel = accountShort
    ? accountShort
    : walletInfo?.did
      ? "지갑 활성화됨"
      : "지갑 활성화 필요";
  const walletActive = Boolean(accountShort || walletInfo?.did);
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
        {apiError ? (
          <div
            className="m-error"
            style={{ margin: "12px 18px 0", textAlign: "left" }}
          >
            {apiError}
          </div>
        ) : null}

        <div className="home-figma-stage">
          <section className="wallet-hero xrp-home-hero">
            <h1>{balanceKrw}</h1>
            <button
              type="button"
              className={`wallet-did-copy${accountShort ? "" : " needs-wallet"}`}
              onClick={() => {
                if (walletActive) {
                  router.push("/m/did/register");
                } else {
                  setWalletSheetOpen(true);
                }
              }}
            >
              <span>{walletLabel}</span>
              {accountShort ? <MIcon.link /> : null}
            </button>
          </section>

          <section className="wallet-actions xrp-home-actions">
            <button type="button" onClick={() => router.push("/m/xrp/receive")}>
              <MIcon.arrowDown />
              <b>받기</b>
            </button>
            <button
              type="button"
              className={!accountShort ? "inactive" : ""}
              onClick={() => router.push("/m/xrp/send")}
            >
              <MIcon.arrowUpRight />
              <b>보내기</b>
            </button>
            <button
              type="button"
              className={!accountShort ? "inactive" : ""}
              onClick={() => router.push("/m/transactions")}
            >
              <MIcon.history />
              <b>내역</b>
            </button>
          </section>

          <section className="stack-section">
            <div className="m-section-title section-title stack-title">
              <h2>내 증명서</h2>
            </div>
            <div className="credential-stack" aria-label="내 증명서 스택">
              {visible.length === 0 ? (
                <button
                  type="button"
                  className="empty-credential-card"
                  onClick={() => router.push("/m/vc/issue")}
                >
                  발급하기
                </button>
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
      </div>
      <MBottomNav
        active="home"
        onQrClick={() => {
          if (walletActive) {
            router.push("/m/vp/scan");
            return;
          }
          setInactiveQrOpen(true);
        }}
      />
      {inactiveQrOpen ? (
        <InactiveQrMenu onClose={() => setInactiveQrOpen(false)} />
      ) : null}
      {walletSheetOpen ? (
        <WalletActivationSheet onClose={() => setWalletSheetOpen(false)} />
      ) : null}
    </section>
  );
}

function InactiveQrMenu({ onClose }: { onClose: () => void }) {
  return (
    <div className="inactive-qr-layer" role="dialog" aria-modal="true">
      <button
        type="button"
        className="inactive-qr-dim"
        aria-label="QR 메뉴 닫기"
        onClick={onClose}
      />
      <section className="inactive-qr-notice" aria-live="polite">
        <span className="inactive-qr-notice-icon">
          <MIcon.help />
        </span>
        <div>
          <strong>안내</strong>
          <p>계정 활성화를 완료해주세요.</p>
        </div>
      </section>
      <section className="inactive-qr-menu" aria-label="비활성화된 QR 메뉴">
        <button type="button" aria-disabled="true">
          <MIcon.lockSlash />
          <span>
            <strong>증명서 발급</strong>
            <small>법인 KYC 증명서를 발급받아 지갑에 저장</small>
          </span>
        </button>
        <button type="button" aria-disabled="true">
          <MIcon.lockSlash />
          <span>
            <strong>증명서 제출</strong>
            <small>기관 QR을 스캔하여 증명서 제출</small>
          </span>
        </button>
      </section>
    </div>
  );
}

function WalletActivationSheet({ onClose }: { onClose: () => void }) {
  const [dragY, setDragY] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [closing, setClosing] = useState(false);
  const dragStart = useRef<number | null>(null);

  const closeWithAnimation = () => {
    setClosing(true);
    setDragY(window.innerHeight);
    window.setTimeout(onClose, 180);
  };

  const onDragStart = (clientY: number) => {
    dragStart.current = clientY;
    setDragging(true);
  };

  const onDragMove = (clientY: number) => {
    if (dragStart.current == null) return;
    setDragY(Math.max(0, clientY - dragStart.current));
  };

  const onDragEnd = () => {
    if (dragY > 120) closeWithAnimation();
    else setDragY(0);
    dragStart.current = null;
    setDragging(false);
  };

  return (
    <div className="wallet-sheet-layer" role="dialog" aria-modal="true">
      <button
        type="button"
        className="wallet-sheet-dim"
        aria-label="지갑 활성화 안내 닫기"
        onClick={closeWithAnimation}
      />
      <div
        className={`wallet-activation-sheet${dragging ? " dragging" : ""}${closing ? " closing" : ""}`}
        style={{ transform: `translateY(${dragY}px)` }}
      >
        <div
          className="wallet-sheet-handle"
          role="button"
          aria-label="지갑 활성화 안내 시트 이동"
          tabIndex={0}
          onPointerDown={(e) => {
            e.currentTarget.setPointerCapture(e.pointerId);
            onDragStart(e.clientY);
          }}
          onPointerMove={(e) => onDragMove(e.clientY)}
          onPointerUp={onDragEnd}
          onPointerCancel={onDragEnd}
        />
        <div className="wallet-sheet-body">
          <h2>지갑 활성화 필요</h2>
          <p>지갑을 활성화하려면 1 XRP를 예치해야 합니다.</p>
          <button type="button" className="wallet-sheet-link">
            자세히 보기
          </button>
        </div>
        <button
          type="button"
          className="wallet-sheet-primary"
          onClick={closeWithAnimation}
        >
          확인
        </button>
      </div>
    </div>
  );
}
