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
  corporate,
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
import { mSession } from "@/lib/m/session";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "유효",
  ISSUED: "유효",
  REVOKED: "취소됨",
  EXPIRED: "만료",
};

const PALETTES = [
  "linear-gradient(135deg,#111827 0%,#183b8f 48%,#7c3aed 100%)",
  "linear-gradient(135deg,#052e2b 0%,#0f766e 48%,#2563eb 100%)",
  "linear-gradient(135deg,#231942 0%,#5e3bce 50%,#00a3ff 100%)",
];

const TEST_CREDENTIAL: CertItem = {
  issuer: "우리은행",
  title: "법인 KYC 증명서",
  status: "유효",
  id: "DID:kyvc:corp:240315",
  date: "2026.05.03",
  expiresAt: "2026.05.07",
  gradient: PALETTES[0]!,
};

function summaryToCert(s: CredentialSummary, i: number): CertItem {
  return {
    issuer: s.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
    title: s.credentialTypeCode ?? "법인 증명서",
    status: STATUS_LABEL[s.credentialStatusCode ?? ""] ?? "발급됨",
    id: `urn:cred:${s.credentialId}`,
    date: (s.issuedAt ?? "").slice(0, 10).replaceAll("-", ".") || "-",
    expiresAt: s.expiresAt
      ? s.expiresAt.slice(0, 10).replaceAll("-", ".")
      : undefined,
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

type HomeTestState =
  | "bridge"
  | "inactive"
  | "walletOnly"
  | "did"
  | "credential";

export default function MobileHomePage() {
  const router = useRouter();
  const [certs, setCerts] = useState<CertItem[]>([]);
  const [hidden, setHidden] = useState<string[]>([]);
  const [walletInfo, setWalletInfo] = useState<WalletInfo | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [walletSheetOpen, setWalletSheetOpen] = useState(false);
  const [didSheetOpen, setDidSheetOpen] = useState(false);
  const [inactiveQrOpen, setInactiveQrOpen] = useState(false);
  const [qrNoticeKind, setQrNoticeKind] = useState<
    "account" | "did" | "needCredential" | "credential"
  >("account");
  const [testPanelOpen, setTestPanelOpen] = useState(false);
  const [testState, setTestState] = useState<HomeTestState>("bridge");
  const [toast, setToast] = useState("");
  const [toastClosing, setToastClosing] = useState(false);

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

  // 설정 화면 진입 전에 법인 기본정보를 미리 캐시한다.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const profile = await corporate.me();
        if (cancelled) return;
        mSession.writeCorporateProfile({
          corporateName: profile.corporateName,
          businessRegistrationNo: profile.businessRegistrationNo,
          cachedAt: Date.now(),
        });
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          router.replace("/m/login");
        }
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
        status: c.active ? "유효" : "비활성",
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

  const showToast = (message: string) => {
    setToastClosing(false);
    setToast(message);
    window.setTimeout(() => setToastClosing(true), 1400);
    window.setTimeout(() => setToast(""), 1600);
  };

  const copyText = async (value: string) => {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(value);
      return;
    }

    const el = document.createElement("textarea");
    el.value = value;
    el.setAttribute("readonly", "");
    el.style.position = "fixed";
    el.style.left = "-9999px";
    document.body.appendChild(el);
    el.select();
    document.execCommand("copy");
    document.body.removeChild(el);
  };

  const testWalletInfo =
    testState === "inactive"
      ? null
      : testState === "walletOnly"
        ? {
            ok: true,
            account: "rKYvC1234567890TestWallet",
          }
        : testState === "did" || testState === "credential"
          ? {
              ok: true,
              account: "rKYvC1234567890TestWallet",
              did: "did:xrpl:rKYvC1234567890TestWallet",
          }
          : walletInfo;
  const visible =
    testState === "did"
      ? []
      : testState === "credential"
        ? [TEST_CREDENTIAL]
        : certs.filter((c) => !hidden.includes(c.title));
  const accountShort = testWalletInfo?.account
    ? `${testWalletInfo.account.slice(0, 6)}...${testWalletInfo.account.slice(-4)}`
    : null;
  const didShort = testWalletInfo?.did
    ? `${testWalletInfo.did.slice(0, 13)}...${testWalletInfo.did.slice(-4)}`
    : null;
  const didRegistered = Boolean(testWalletInfo?.did);
  const walletActivated = Boolean(accountShort);
  const hasCredential = visible.length > 0;
  const walletLabel = didRegistered
    ? (didShort ?? "DID 등록됨")
    : walletActivated
      ? "DID 등록하기"
      : "지갑 활성화 필요";
  const balanceKrw =
    walletActivated || visible.length > 0 ? "₩ 123,000" : "₩ 0";

  return (
    <section className="view wash home-view">
      <MTopBar
        logo
        onLogoLongPress={() => setTestPanelOpen(true)}
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
              className={`wallet-did-copy${
                didRegistered
                  ? " did-registered"
                  : walletActivated
                    ? " needs-did"
                    : " needs-wallet"
              }`}
              onClick={async () => {
                if (didRegistered) {
                  try {
                    await copyText(testWalletInfo?.did ?? "");
                    showToast("DID가 복사되었습니다.");
                  } catch {
                    showToast("DID를 복사할 수 없습니다.");
                  }
                } else if (walletActivated) {
                  router.push("/m/did/register");
                } else {
                  setWalletSheetOpen(true);
                }
              }}
            >
              <span>{walletLabel}</span>
              {didRegistered ? (
                <svg
                  className="wallet-did-copy-icon"
                  viewBox="0 0 14 14"
                  fill="none"
                  aria-hidden="true"
                >
                  <path
                    d="M11.6667 4.66667H5.83333C5.189 4.66667 4.66667 5.189 4.66667 5.83333V11.6667C4.66667 12.311 5.189 12.8333 5.83333 12.8333H11.6667C12.311 12.8333 12.8333 12.311 12.8333 11.6667V5.83333C12.8333 5.189 12.311 4.66667 11.6667 4.66667Z"
                    stroke="currentColor"
                    strokeWidth="1.16667"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  <path
                    d="M2.33333 9.33333C1.69167 9.33333 1.16667 8.80833 1.16667 8.16667V2.33333C1.16667 1.69167 1.69167 1.16667 2.33333 1.16667H8.16667C8.80833 1.16667 9.33333 1.69167 9.33333 2.33333"
                    stroke="currentColor"
                    strokeWidth="1.16667"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              ) : null}
            </button>
          </section>

          <section className="wallet-actions xrp-home-actions">
            <button type="button" onClick={() => router.push("/m/xrp/receive")}>
              <MIcon.arrowDown />
              <b>받기</b>
            </button>
            <button
              type="button"
              className={!hasCredential ? "inactive" : ""}
              onClick={() => router.push("/m/xrp/send")}
            >
              <MIcon.arrowUpRight />
              <b>보내기</b>
            </button>
            <button
              type="button"
              className={!hasCredential ? "inactive" : ""}
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
                  onClick={() => {
                    if (!walletActivated) {
                      setWalletSheetOpen(true);
                      return;
                    }
                    if (walletActivated && !didRegistered) {
                      setDidSheetOpen(true);
                      return;
                    }
                    if (didRegistered && !hasCredential) {
                      setQrNoticeKind("needCredential");
                      setInactiveQrOpen(true);
                      return;
                    }
                    router.push("/m/vc/issue");
                  }}
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
          if (didRegistered && hasCredential) {
            setQrNoticeKind("credential");
            setInactiveQrOpen(true);
            return;
          }
          setQrNoticeKind(
            didRegistered
              ? "needCredential"
              : walletActivated
                ? "did"
                : "account",
          );
          setInactiveQrOpen(true);
        }}
      />
      {inactiveQrOpen ? (
        <InactiveQrMenu
          kind={qrNoticeKind}
          onIssue={() => router.push("/m/vc/issue")}
          onSubmit={() => router.push("/m/vp/scan")}
          onClose={() => setInactiveQrOpen(false)}
        />
      ) : null}
      {testPanelOpen ? (
        <HomeStateTestPanel
          value={testState}
          onChange={setTestState}
          onClose={() => setTestPanelOpen(false)}
        />
      ) : null}
      {didSheetOpen ? (
        <DidRequiredSheet
          onClose={() => setDidSheetOpen(false)}
          onRegister={() => router.push("/m/did/register")}
        />
      ) : null}
      {walletSheetOpen ? (
        <WalletActivationSheet onClose={() => setWalletSheetOpen(false)} />
      ) : null}
      {toast ? (
        <div className={`m-toast${toastClosing ? " closing" : ""}`}>
          {toast}
        </div>
      ) : null}
    </section>
  );
}

function InactiveQrMenu({
  kind,
  onIssue,
  onSubmit,
  onClose,
}: {
  kind: "account" | "did" | "needCredential" | "credential";
  onIssue: () => void;
  onSubmit: () => void;
  onClose: () => void;
}) {
  const issueReady = kind === "needCredential" || kind === "credential";
  const submitReady = kind === "credential";
  const menuReady = issueReady;

  return (
    <div className="inactive-qr-layer" role="dialog" aria-modal="true">
      <button
        type="button"
        className="inactive-qr-dim"
        aria-label="QR 메뉴 닫기"
        onClick={onClose}
      />
      {menuReady ? null : (
        <section className="inactive-qr-notice" aria-live="polite">
          <span className="inactive-qr-notice-icon">
            <MIcon.help />
          </span>
          <div>
            <strong>안내</strong>
            <p>
              {kind === "did"
                ? "DID를 등록해주세요."
                : "지갑 활성화를 완료해주세요."}
            </p>
          </div>
        </section>
      )}
      <section className="inactive-qr-menu" aria-label="QR 메뉴">
        <button
          type="button"
          className={issueReady ? "active" : ""}
          aria-disabled={issueReady ? undefined : "true"}
          onClick={issueReady ? onIssue : undefined}
        >
          <span className="inactive-qr-icon issue">
            {issueReady ? <MIcon.cert /> : <MIcon.lockSlash />}
          </span>
          <span className="inactive-qr-text">
            <strong>증명서 발급</strong>
            <small>법인 KYC 증명서를 발급받아 지갑에 저장</small>
          </span>
        </button>
        <button
          type="button"
          className={submitReady ? "active submit" : ""}
          aria-disabled={submitReady ? undefined : "true"}
          onClick={submitReady ? onSubmit : undefined}
        >
          <span className="inactive-qr-icon submit">
            {submitReady ? <MIcon.qr /> : <MIcon.lockSlash />}
          </span>
          <span className="inactive-qr-text">
            <strong>증명서 제출</strong>
            <small>기관 QR을 스캔하여 증명서 제출</small>
          </span>
        </button>
      </section>
    </div>
  );
}

function HomeStateTestPanel({
  value,
  onChange,
  onClose,
}: {
  value: HomeTestState;
  onChange: (value: HomeTestState) => void;
  onClose: () => void;
}) {
  const options: Array<{ value: HomeTestState; label: string }> = [
    { value: "bridge", label: "실제 상태" },
    { value: "inactive", label: "지갑 비활성" },
    { value: "walletOnly", label: "지갑 활성 / DID 없음" },
    { value: "did", label: "DID 등록 / 증명서 없음" },
    { value: "credential", label: "증명서 있음" },
  ];

  return (
    <div className="home-test-layer" role="dialog" aria-modal="true">
      <button
        type="button"
        className="home-test-dim"
        aria-label="상태 선택 닫기"
        onClick={onClose}
      />
      <section className="home-test-panel">
        <h2>홈 상태 테스트</h2>
        <div className="home-test-options">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              className={value === option.value ? "selected" : ""}
              onClick={() => onChange(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
        <button type="button" className="home-test-close" onClick={onClose}>
          닫기
        </button>
      </section>
    </div>
  );
}

function DidRequiredSheet({
  onClose,
  onRegister,
}: {
  onClose: () => void;
  onRegister: () => void;
}) {
  return (
    <WalletGuideSheet
      title="DID 등록 필요"
      description="증명서를 발급받으려면 DID를 먼저 등록해주세요."
      linkLabel="DID 등록하기"
      primaryLabel="확인"
      onClose={onClose}
      onPrimary={onClose}
      onLink={onRegister}
      ariaLabel="DID 등록 안내 닫기"
      handleLabel="DID 등록 안내 시트 이동"
    />
  );
}

function WalletActivationSheet({ onClose }: { onClose: () => void }) {
  return (
    <WalletGuideSheet
      title="지갑 활성화 필요"
      description="지갑을 활성화하려면 1 XRP를 예치해야 합니다."
      linkLabel="자세히 보기"
      primaryLabel="확인"
      onClose={onClose}
      onPrimary={onClose}
      ariaLabel="지갑 활성화 안내 닫기"
      handleLabel="지갑 활성화 안내 시트 이동"
    />
  );
}

function WalletGuideSheet({
  title,
  description,
  linkLabel,
  primaryLabel,
  onClose,
  onPrimary,
  onLink,
  ariaLabel,
  handleLabel,
}: {
  title: string;
  description: string;
  linkLabel: string;
  primaryLabel: string;
  onClose: () => void;
  onPrimary: () => void;
  onLink?: () => void;
  ariaLabel: string;
  handleLabel: string;
}) {
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
        aria-label={ariaLabel}
        onClick={closeWithAnimation}
      />
      <div
        className={`wallet-activation-sheet${dragging ? " dragging" : ""}${closing ? " closing" : ""}`}
        style={{ transform: `translateY(${dragY}px)` }}
      >
        <div
          className="wallet-sheet-handle"
          role="button"
          aria-label={handleLabel}
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
          <h2>{title}</h2>
          <p>{description}</p>
          <button
            type="button"
            className="wallet-sheet-link"
            onClick={onLink}
          >
            {linkLabel}
          </button>
        </div>
        <button
          type="button"
          className="wallet-sheet-primary"
          onClick={() => {
            if (onPrimary === onClose) closeWithAnimation();
            else onPrimary();
          }}
        >
          {primaryLabel}
        </button>
      </div>
    </div>
  );
}
