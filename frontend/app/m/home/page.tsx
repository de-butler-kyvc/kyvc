"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

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
} from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  onBridgeAction,
  useBridgeAction,
  type NativeCredentialSummary,
  type WalletAssetsResult,
  type WalletInfo,
} from "@/lib/m/android-bridge";
import { nativeSummaryToCert } from "@/lib/m/credential-summaries";
import { readHiddenCerts } from "@/lib/m/data";
import { mSession } from "@/lib/m/session";
import {
  ensureMobileWallet,
  formatXrp,
  readXrpBalance,
} from "@/lib/m/wallet-bridge";

const PALETTES = [
  "linear-gradient(135deg,#111827 0%,#183b8f 48%,#7c3aed 100%)",
  "linear-gradient(135deg,#052e2b 0%,#0f766e 48%,#2563eb 100%)",
  "linear-gradient(135deg,#231942 0%,#5e3bce 50%,#00a3ff 100%)",
];

const TEST_CREDENTIAL: CertItem = {
  issuer: "did:xrpl:1:rIssuerTestWallet",
  title: "법인 KYC 증명서",
  status: "유효",
  id: "KYVC-CERT-240315",
  holderDid: "did:xrpl:1:rKYvC1234567890TestWallet",
  date: "2026.05.03",
  expiresAt: "2026.05.07",
  gradient: PALETTES[0]!,
};

type HomeTestState =
  | "bridge"
  | "inactive"
  | "walletOnly"
  | "did"
  | "credential";

function shortDid(value: string) {
  if (value.length <= 24) return value;
  return `${value.slice(0, 13)}...${value.slice(-6)}`;
}

function isWalletActivated(
  wallet?: WalletInfo | null,
  assets?: WalletAssetsResult | null,
) {
  const account = wallet?.holderAccount ?? wallet?.account;
  return Boolean(account) && Boolean(assets?.accountActivated) && !assets?.depositRequired;
}

export default function MobileHomePage() {
  const router = useRouter();
  const [certs, setCerts] = useState<CertItem[]>([]);
  const [hidden, setHidden] = useState<string[]>([]);
  const [walletInfo, setWalletInfo] = useState<WalletInfo | null>(null);
  const [walletAssets, setWalletAssets] = useState<WalletAssetsResult | null>(null);
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
  const [inactiveStateConfirmed, setInactiveStateConfirmed] = useState(false);

  const normalizeWalletInfo = useCallback((info: WalletInfo): WalletInfo => {
    const holderAccount = info.holderAccount ?? info.account;
    const holderDid = info.holderDid ?? info.did;
    return {
      ...info,
      ...(holderAccount ? { account: holderAccount, holderAccount } : {}),
      ...(holderDid ? { did: holderDid, holderDid } : {}),
    };
  }, []);

  const refreshHolderDidState = useCallback(
    async (base?: WalletInfo | null) => {
      if (!isBridgeAvailable()) return base ?? null;

      let next = base ? normalizeWalletInfo(base) : null;
      try {
        const info = await bridge.getWalletInfo();
        if (info.ok) {
          next = normalizeWalletInfo({ ...(next ?? { ok: true }), ...info });
        }
      } catch {
        // DID 상태 확인은 아래 checkHolderDidSet 결과로 한 번 더 시도한다.
      }

      try {
        const did = await bridge.checkHolderDidSet();
        if (did.ok) {
          next = normalizeWalletInfo({ ...(next ?? { ok: true }), ...did });
        }
      } catch {
        // 구버전 앱에서는 getWalletInfo 결과만 사용한다.
      }

      if (next) setWalletInfo(next);
      return next;
    },
    [normalizeWalletInfo],
  );

  const refreshWalletAssets = useCallback(async () => {
    if (!isBridgeAvailable()) return;
    try {
      const assets = await bridge.getWalletAssets();
      if (assets.ok) {
        setWalletAssets(assets);
        mSession.writeWalletAssets({ assets, cachedAt: Date.now() });
      }
    } catch {
      // 홈 잔액 새로고침 실패는 다음 진입/포커스 때 다시 시도한다.
    }
  }, []);

  // 증명서 목록은 네이티브 지갑 브릿지를 단일 소스로 사용한다.
  useEffect(() => {
    setHidden(readHiddenCerts());
    let cancelled = false;
    (async () => {
      try {
        if (!isBridgeAvailable()) {
          if (!cancelled) {
            setCerts([]);
            setApiError("증명서는 KYvC 앱 지갑에서 확인할 수 있습니다.");
          }
          return;
        }
        const list = await bridge.getCredentialSummaries();
        if (cancelled) return;
        setCerts((list.credentials ?? []).map(nativeSummaryToCert));
        setApiError(null);
      } catch (e) {
        if (cancelled) return;
        setApiError(
          e instanceof Error
            ? `지갑 증명서 조회 실패: ${e.message}`
            : "지갑 증명서 조회 중 오류가 발생했습니다.",
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
        if (mSession.readCorporateProfile()) return;
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

  // 브리지: 로그인 후 활성 지갑 보장 + 자산 조회.
  useEffect(() => {
    if (!isBridgeAvailable()) {
      return;
    }
    (async () => {
      try {
        const state = await ensureMobileWallet();
        await refreshHolderDidState(state.wallet);
        if (state.assets?.ok) {
          setWalletAssets(state.assets);
          mSession.writeWalletAssets({ assets: state.assets, cachedAt: Date.now() });
        }
        void refreshWalletAssets();
        if (state.created) showToast("새 지갑이 생성되었습니다.");
      } catch (e) {
        setApiError(e instanceof Error ? e.message : "지갑 상태를 확인할 수 없습니다.");
      }
    })();
    bridge.listWallets().catch(() => {});
    bridge.getCredentialSummaries().catch(() => {});
  }, [refreshHolderDidState, refreshWalletAssets]);

  useEffect(() => {
    if (!isBridgeAvailable()) return;

    const onVisible = () => {
      if (document.visibilityState === "visible") void refreshWalletAssets();
    };
    const onFocus = () => {
      void refreshWalletAssets();
    };

    document.addEventListener("visibilitychange", onVisible);
    window.addEventListener("focus", onFocus);
    void refreshWalletAssets();
    const warmupTimers = [800, 1800, 3200, 5000].map((delay) =>
      window.setTimeout(() => {
        if (document.visibilityState === "visible") void refreshWalletAssets();
      }, delay),
    );
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") void refreshWalletAssets();
    }, 5000);

    return () => {
      document.removeEventListener("visibilitychange", onVisible);
      window.removeEventListener("focus", onFocus);
      warmupTimers.forEach((id) => window.clearTimeout(id));
      window.clearInterval(timer);
    };
  }, [refreshWalletAssets]);

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
      const next = normalizeWalletInfo({
        ok: true,
        account: active.account,
        did: active.did,
      });
      setWalletInfo(next);
      void refreshHolderDidState(next);
    }
  });

  useBridgeAction("GET_CREDENTIAL_SUMMARIES", (r) => {
    if (!r.ok) return;
    const list = (r.credentials as NativeCredentialSummary[] | undefined) ?? [];
    setCerts(list.map(nativeSummaryToCert));
  });

  useBridgeAction("CREATE_WALLET", (r) => {
    if (!r.ok) return;
    const next = normalizeWalletInfo(r as WalletInfo);
    setWalletInfo(next);
    void refreshHolderDidState(next);
    void refreshWalletAssets();
  });

  useBridgeAction("GET_WALLET_INFO", (r) => {
    if (r.ok) setWalletInfo(normalizeWalletInfo(r as WalletInfo));
  });

  useBridgeAction("CHECK_HOLDER_DID_SET", (r) => {
    if (r.ok) {
      setWalletInfo((prev) =>
        normalizeWalletInfo({ ...(prev ?? { ok: true }), ...(r as WalletInfo) }),
      );
    }
  });

  useBridgeAction("GET_WALLET_ASSETS", (r) => {
    if (r.ok) setWalletAssets(r as WalletAssetsResult);
  });

  useEffect(() => {
    const off = onBridgeAction("ISSUER_CREDENTIAL_RECEIVED", (r) => {
      if (r.ok) {
        bridge.getCredentialSummaries().catch(() => {});
      }
    });
    return off;
  }, []);

  useEffect(() => {
    if (testState !== "bridge") {
      setInactiveStateConfirmed(true);
      return;
    }
    setInactiveStateConfirmed(false);
    if (!walletInfo || !walletAssets) return;
    if (isWalletActivated(walletInfo, walletAssets)) return;

    let cancelled = false;
    bridge
      .getWalletAssets()
      .then((assets) => {
        if (cancelled || !assets.ok) return;
        setWalletAssets(assets);
        mSession.writeWalletAssets({ assets, cachedAt: Date.now() });
        if (!isWalletActivated(walletInfo, assets)) {
          setInactiveStateConfirmed(true);
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [testState, walletInfo, walletAssets]);

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

  const startIssueQrScan = async () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 사용할 수 있는 기능입니다");
      return;
    }
    try {
      const r = await bridge.scanIssueQrCode();
      if (!r.ok || !r.qrData) {
        showToast(r.error ?? "증명서 발급 QR 스캔에 실패했습니다.");
        return;
      }
      mSession.writeScanResult({
        qrData: r.qrData,
        actionType: r.actionType ?? "VC_ISSUE",
        endpoint: r.endpoint,
        requestId: r.requestId,
        receivedAt: Date.now(),
      });
      const saved = mSession.readScanResult();
      if (!saved?.qrData) {
        showToast("QR 정보를 저장하지 못했습니다. 다시 스캔해주세요.");
        return;
      }
      router.push("/m/vc/issue");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "증명서 발급 QR 스캔에 실패했습니다.");
    }
  };

  const startPresentationQrScan = async () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 사용할 수 있는 기능입니다");
      return;
    }
    try {
      const r = await bridge.scanPresentationQrCode();
      if (!r.ok || !r.qrData) {
        showToast(r.error ?? "증명서 제출 QR 스캔에 실패했습니다.");
        return;
      }
      mSession.writeScanResult({
        qrData: r.qrData,
        actionType: r.actionType ?? "VP_REQUEST",
        requestId: r.requestId,
        challenge: r.challenge,
        domain: r.domain,
        endpoint: r.endpoint,
        receivedAt: Date.now(),
      });
      router.push("/m/vp/submit");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "증명서 제출 QR 스캔에 실패했습니다.");
    }
  };

  const testWalletInfo =
    testState === "inactive"
      ? {
          ok: true,
          account: "rKYvC1234567890TestWallet",
        }
      : testState === "walletOnly"
        ? {
            ok: true,
            account: "rKYvC1234567890TestWallet",
            didRegistrationRequired: true,
            didRegistrationLabel: "DID 등록하기",
          }
        : testState === "did" || testState === "credential"
          ? {
              ok: true,
              account: "rKYvC1234567890TestWallet",
              holderDid: "did:xrpl:rKYvC1234567890TestWallet",
              didSetRegistered: true,
              didRegistrationRequired: false,
              didRegistrationLabel: "DID 등록됨",
          }
          : walletInfo;
  const testWalletAssets =
    testState === "inactive"
      ? ({ ok: true, accountActivated: false, depositRequired: true } as WalletAssetsResult)
      : testState === "walletOnly"
        ? ({ ok: true, accountActivated: true, depositRequired: false, xrpBalanceXrp: "12.345678" } as WalletAssetsResult)
        : testState === "did" || testState === "credential"
          ? ({ ok: true, accountActivated: true, depositRequired: false, xrpBalanceXrp: "12.345678" } as WalletAssetsResult)
          : walletAssets;
  const visible =
    testState === "did"
      ? []
      : testState === "credential"
        ? [TEST_CREDENTIAL]
        : certs.filter((c) => !hidden.includes(c.title));
  const accountShort = testWalletInfo?.account
    ? `${testWalletInfo.account.slice(0, 6)}...${testWalletInfo.account.slice(-4)}`
    : null;
  const walletExists = Boolean(accountShort);
  const didRegistrationLabel = testWalletInfo?.didRegistrationLabel;
  const registeredDid = testWalletInfo?.holderDid ?? testWalletInfo?.did;
  const ledgerActivated = Boolean(testWalletAssets?.accountActivated);
  const depositRequired = Boolean(testWalletAssets?.depositRequired);
  const walletXrplActivated = walletExists && ledgerActivated && !depositRequired;
  const walletStateCanRender =
    testState !== "bridge" ||
    !walletExists ||
    walletXrplActivated ||
    inactiveStateConfirmed;
  const didRegistrationRequired =
    testWalletInfo?.didRegistrationRequired ??
    (walletXrplActivated
      ? didRegistrationLabel === "DID 등록하기" ||
        (testWalletInfo?.didSetRegistered !== true && !registeredDid)
      : false);
  const didRegistered =
    walletXrplActivated &&
    (testWalletInfo?.didSetRegistered === true ||
      didRegistrationLabel === "DID 등록됨" ||
      (Boolean(registeredDid) && didRegistrationRequired === false));
  const hasCredential = visible.length > 0;
  const walletLabel =
    !walletExists || !walletXrplActivated
      ? "지갑 활성화 필요"
      : didRegistered && registeredDid
        ? shortDid(registeredDid)
        : "DID 등록하기";
  const balanceValue = walletExists && testWalletAssets ? readXrpBalance(testWalletAssets) : null;
  const balanceXrp = balanceValue == null ? null : formatXrp(balanceValue);
  const homeStateReady =
    testState !== "bridge" ||
    Boolean(testWalletInfo && testWalletAssets && walletStateCanRender);

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
            <div className={`home-wallet-state${homeStateReady ? " ready" : ""}`}>
              {homeStateReady ? (
                <>
                  {balanceXrp ? <h1>{balanceXrp}</h1> : null}
                <button
                  type="button"
                  className={`wallet-did-copy${
                    didRegistered
                      ? " did-registered"
                      : walletXrplActivated
                        ? " needs-did"
                        : " needs-wallet"
                  }`}
                  onClick={async () => {
                    if (!walletExists) {
                      setWalletSheetOpen(true);
                      return;
                    }
                    if (!walletXrplActivated) {
                      setWalletSheetOpen(true);
                      return;
                    }
                    if (didRegistered && !didRegistrationRequired) {
                      if (!registeredDid) {
                        showToast("복사할 DID를 찾을 수 없습니다.");
                        return;
                      }
                      try {
                        await copyText(registeredDid);
                        showToast("DID가 복사되었습니다.");
                      } catch {
                        showToast("DID를 복사할 수 없습니다.");
                      }
                      return;
                    }
                    router.push("/m/did/register");
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
                </>
              ) : null}
            </div>
          </section>

          <section className="wallet-actions xrp-home-actions">
            <button type="button" onClick={() => router.push("/m/xrp/receive")}>
              <MIcon.arrowDown />
              <b>받기</b>
            </button>
            <button
              type="button"
              className={homeStateReady && !walletXrplActivated ? "inactive" : ""}
              onClick={() => {
                if (!homeStateReady) return;
                if (!walletExists) {
                  setWalletSheetOpen(true);
                  return;
                }
                if (!walletXrplActivated) {
                  router.push("/m/xrp/receive");
                  return;
                }
                router.push("/m/xrp/send");
              }}
            >
              <MIcon.arrowUpRight />
              <b>보내기</b>
            </button>
            <button
              type="button"
              className={homeStateReady && !walletExists ? "inactive" : ""}
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
                    if (!homeStateReady) return;
                    if (!walletExists) {
                      setWalletSheetOpen(true);
                      return;
                    }
                    if (!walletXrplActivated) {
                      router.push("/m/xrp/receive");
                      return;
                    }
                    if (!didRegistered) {
                      setDidSheetOpen(true);
                      return;
                    }
                    if (didRegistered && !hasCredential) {
                      startIssueQrScan();
                      return;
                    }
                    startIssueQrScan();
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
          if (inactiveQrOpen) {
            setInactiveQrOpen(false);
            return;
          }
          if (didRegistered && hasCredential) {
            setQrNoticeKind("credential");
            setInactiveQrOpen(true);
            return;
          }
          setQrNoticeKind(
            didRegistered
              ? "needCredential"
              : walletXrplActivated
                ? "did"
                : "account",
          );
          setInactiveQrOpen(true);
        }}
      />
      {inactiveQrOpen ? (
        <InactiveQrMenu
          kind={qrNoticeKind}
          onIssue={() => {
            setInactiveQrOpen(false);
            startIssueQrScan();
          }}
          onSubmit={() => {
            setInactiveQrOpen(false);
            startPresentationQrScan();
          }}
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
        <WalletActivationSheet
          onClose={() => setWalletSheetOpen(false)}
          onReceive={() => {
            setWalletSheetOpen(false);
            router.push("/m/xrp/receive");
          }}
        />
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

function WalletActivationSheet({
  onClose,
  onReceive,
}: {
  onClose: () => void;
  onReceive: () => void;
}) {
  return (
    <WalletGuideSheet
      title="지갑 활성화 필요"
      description="지갑을 활성화하려면 1 XRP를 예치해야 합니다."
      linkLabel="입금 주소 보기"
      primaryLabel="확인"
      onClose={onClose}
      onPrimary={onClose}
      onLink={onReceive}
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
