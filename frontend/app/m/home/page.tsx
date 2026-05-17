"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";

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
  ensureMobileSessionOwner,
  formatXrp,
  isXrplAccountActivated,
  isXrplAccountActivationRequired,
  loadWalletAssets,
  readXrpBalance,
  readWalletAccount,
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
  return Boolean(readWalletAccount(wallet, assets)) && isXrplAccountActivated(assets);
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
  const [walletRefreshing, setWalletRefreshing] = useState(false);
  const walletBootstrappedRef = useRef(false);

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
    if (!isBridgeAvailable()) return null;
    try {
      const assets = await loadWalletAssets();
      if (!assets) return null;
      setWalletAssets(assets);
      if (assets.ok) {
        mSession.writeWalletAssets({ assets, cachedAt: Date.now() });
        setApiError(null);
      } else {
        setApiError(assets.error ?? "지갑 자산 조회에 실패했습니다.");
      }
      return assets;
    } catch {
      setApiError("지갑 자산 조회에 실패했습니다.");
      return null;
    }
  }, []);

  // 증명서 목록은 네이티브 지갑 브릿지를 단일 소스로 사용한다.
  useEffect(() => {
    setHidden(readHiddenCerts());
  }, []);

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

  // 브리지: 로그인 후 활성 지갑 보장.
  useEffect(() => {
    if (!isBridgeAvailable()) {
      return;
    }
    if (walletBootstrappedRef.current) return;
    walletBootstrappedRef.current = true;
    let cancelled = false;
    (async () => {
      try {
        const state = await ensureMobileSessionOwner();
        if (cancelled) return;
        await refreshHolderDidState(state.wallet);
        if (cancelled) return;
        if (state.created) showToast("새 지갑이 생성되었습니다.");
      } catch (e) {
        if (cancelled) return;
        setApiError(e instanceof Error ? e.message : "지갑 상태를 확인할 수 없습니다.");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [refreshHolderDidState]);

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
    const assets = r as WalletAssetsResult;
    setWalletAssets(assets);
    if (assets.ok) {
      mSession.writeWalletAssets({ assets, cachedAt: Date.now() });
      setApiError(null);
    } else {
      setApiError(assets.error ?? "지갑 자산 조회에 실패했습니다.");
    }
  });

  useEffect(() => {
    const off = onBridgeAction("ISSUER_CREDENTIAL_RECEIVED", (r) => {
      if (r.ok) {
        bridge.getCredentialSummaries().catch(() => {});
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
    if (isBridgeAvailable()) {
      try {
        const r = await bridge.copyTextToClipboard(value);
        if (r.ok) return;
      } catch {
        // 브라우저 클립보드로 한 번 더 시도한다.
      }
    }

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

  const copyActivationAddress = async (address?: string | null) => {
    if (!address) {
      showToast("복사할 지갑 주소를 찾을 수 없습니다.");
      return;
    }
    try {
      await copyText(address);
      showToast("지갑 주소가 복사되었습니다.");
    } catch {
      showToast("지갑 주소를 복사할 수 없습니다.");
    }
  };

  const recheckWalletActivation = async () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 활성화 상태를 확인할 수 있습니다.");
      return;
    }
    setWalletRefreshing(true);
    try {
      const assets = await refreshWalletAssets();
      if (!assets) {
        showToast("활성화 상태를 확인할 수 없습니다.");
        return;
      }
      if (!assets.ok) {
        showToast(assets.error ?? "지갑 자산 조회에 실패했습니다.");
        return;
      }
      if (isXrplAccountActivated(assets)) {
        void refreshHolderDidState(walletInfo);
        setWalletSheetOpen(false);
        setInactiveQrOpen(false);
        showToast("XRPL 계정 활성화가 확인되었습니다.");
        return;
      }
      if (isXrplAccountActivationRequired(assets) || assets.accountActivated !== true) {
        showToast("아직 XRPL 계정 활성화가 필요합니다.");
      }
    } finally {
      setWalletRefreshing(false);
    }
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
      ? ({
          ok: true,
          account: "rKYvC1234567890TestWallet",
          accountActivated: false,
          depositRequired: true,
          errorCode: "XRPL_ACCOUNT_NOT_ACTIVATED",
          errorTitle: "XRPL 계정 활성화 필요",
          errorHint: "이 주소로 XRP를 입금한 뒤 자산 조회를 다시 실행하세요.",
        } as WalletAssetsResult)
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
  const walletAccount = readWalletAccount(testWalletInfo, testWalletAssets);
  const accountShort = walletAccount
    ? `${walletAccount.slice(0, 6)}...${walletAccount.slice(-4)}`
    : null;
  const walletExists = Boolean(accountShort);
  const activationAddress = walletAccount;
  const didRegistrationLabel = testWalletInfo?.didRegistrationLabel;
  const registeredDid = testWalletInfo?.holderDid ?? testWalletInfo?.did;
  const walletActivationRequired =
    walletExists && isXrplAccountActivationRequired(testWalletAssets);
  const walletXrplActivated = walletExists && isWalletActivated(testWalletInfo, testWalletAssets);
  const walletStateCanRender =
    testState !== "bridge" ||
    Boolean(testWalletInfo);
  const didRegistrationRequired =
    testWalletInfo?.didRegistrationRequired ??
    (didRegistrationLabel === "DID 등록하기" ||
      (testWalletInfo?.didSetRegistered !== true && !registeredDid));
  const didRegistered =
    (testWalletInfo?.didSetRegistered === true ||
      didRegistrationLabel === "DID 등록됨" ||
      (Boolean(registeredDid) && didRegistrationRequired === false));
  const hasCredential = visible.length > 0;
  const walletLabel =
    !walletExists
      ? "지갑 생성 필요"
      : walletActivationRequired
        ? "XRPL 계정 활성화 필요"
        : didRegistered && registeredDid
          ? shortDid(registeredDid)
          : "DID 등록하기";
  const balanceValue = walletExists && testWalletAssets ? readXrpBalance(testWalletAssets) : null;
  const balanceXrp = balanceValue == null ? null : formatXrp(balanceValue);
  const homeStateReady =
    testState !== "bridge" ||
    Boolean(testWalletInfo && walletStateCanRender);
  const openWalletActivationSheet = () => {
    setInactiveQrOpen(false);
    setWalletSheetOpen(true);
  };
  const ensureWalletFeatureActivated = async () => {
    if (!homeStateReady) return false;
    if (!walletExists) {
      openWalletActivationSheet();
      return false;
    }

    const assets =
      testState === "bridge" ? await refreshWalletAssets() : testWalletAssets;
    if (!assets) {
      showToast("지갑 활성화 상태를 확인할 수 없습니다.");
      return false;
    }
    if (!assets.ok) {
      showToast(assets.error ?? "지갑 자산 조회에 실패했습니다.");
      return false;
    }
    if (isXrplAccountActivationRequired(assets) || !isXrplAccountActivated(assets)) {
      openWalletActivationSheet();
      return false;
    }
    return true;
  };
  const handleIssueQrAction = async () => {
    const ready = await ensureWalletFeatureActivated();
    if (!ready) return;
    if (!didRegistered) {
      setInactiveQrOpen(false);
      setDidSheetOpen(true);
      return;
    }
    setInactiveQrOpen(false);
    await startIssueQrScan();
  };
  const handleSubmitQrAction = async () => {
    const ready = await ensureWalletFeatureActivated();
    if (!ready) return;
    if (!didRegistered) {
      setInactiveQrOpen(false);
      setDidSheetOpen(true);
      return;
    }
    if (!hasCredential) {
      showToast("제출할 증명서가 없습니다.");
      return;
    }
    setInactiveQrOpen(false);
    await startPresentationQrScan();
  };
  const handleSendXrp = async () => {
    const ready = await ensureWalletFeatureActivated();
    if (!ready) return;
    router.push("/m/xrp/send");
  };

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
                    if (walletActivationRequired) {
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
              className={homeStateReady && walletActivationRequired ? "inactive" : ""}
              onClick={() => {
                void handleSendXrp();
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

          {homeStateReady && walletActivationRequired ? (
            <XrplActivationNotice
              address={activationAddress}
              busy={walletRefreshing}
              onCopy={copyActivationAddress}
              onRefresh={recheckWalletActivation}
            />
          ) : null}

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
                    void handleIssueQrAction();
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
            !walletExists || walletActivationRequired
              ? "account"
              : "needCredential",
          );
          setInactiveQrOpen(true);
        }}
      />
      {inactiveQrOpen ? (
        <InactiveQrMenu
          kind={qrNoticeKind}
          activationAddress={walletActivationRequired ? activationAddress : null}
          activationBusy={walletRefreshing}
          onCopyActivationAddress={copyActivationAddress}
          onRefreshActivation={recheckWalletActivation}
          onActivationRequired={openWalletActivationSheet}
          onIssue={() => {
            void handleIssueQrAction();
          }}
          onSubmit={() => {
            void handleSubmitQrAction();
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
      {walletSheetOpen && walletActivationRequired ? (
        <WalletActivationSheet
          address={activationAddress}
          refreshing={walletRefreshing}
          onCopyAddress={copyActivationAddress}
          onClose={() => setWalletSheetOpen(false)}
          onRefresh={recheckWalletActivation}
        />
      ) : walletSheetOpen ? (
        <WalletSetupSheet onClose={() => setWalletSheetOpen(false)} />
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
  activationAddress,
  activationBusy,
  onCopyActivationAddress,
  onRefreshActivation,
  onActivationRequired,
  onIssue,
  onSubmit,
  onClose,
}: {
  kind: "account" | "did" | "needCredential" | "credential";
  activationAddress?: string | null;
  activationBusy?: boolean;
  onCopyActivationAddress?: (address?: string | null) => void;
  onRefreshActivation?: () => void;
  onActivationRequired?: () => void;
  onIssue: () => void;
  onSubmit: () => void;
  onClose: () => void;
}) {
  const activationRequired = kind === "account" && Boolean(activationAddress);
  const issueReady = kind === "needCredential" || kind === "credential";
  const submitReady = kind === "credential";
  const menuReady = issueReady;
  const blockedAction =
    activationRequired && onActivationRequired ? onActivationRequired : undefined;

  return (
    <div className="inactive-qr-layer" role="dialog" aria-modal="true">
      <button
        type="button"
        className="inactive-qr-dim"
        aria-label="QR 메뉴 닫기"
        onClick={onClose}
      />
      {menuReady ? null : activationRequired ? (
        <XrplActivationNotice
          className="inactive-qr-notice xrpl-activation-card xrpl-activation-qr-card"
          address={activationAddress}
          busy={activationBusy}
          onCopy={(address) => onCopyActivationAddress?.(address)}
          onRefresh={() => onRefreshActivation?.()}
        />
      ) : (
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
          onClick={issueReady ? onIssue : blockedAction}
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
          onClick={submitReady ? onSubmit : blockedAction}
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

function XrplActivationNotice({
  address,
  busy,
  className,
  onCopy,
  onRefresh,
}: {
  address?: string | null;
  busy?: boolean;
  className?: string;
  onCopy: (address?: string | null) => void;
  onRefresh: () => void;
}) {
  return (
    <section className={className ?? "xrpl-activation-card"} aria-live="polite">
      <div className="xrpl-activation-copy">
        <span>Testnet</span>
        <h2>XRPL 계정 활성화 필요</h2>
        <p>지갑 주소로 Testnet XRP를 입금한 뒤 다시 확인해 주세요.</p>
      </div>
      <div className="xrpl-activation-address">
        <span>지갑 주소</span>
        <strong>{address ?? "주소 확인 중"}</strong>
      </div>
      <div className="xrpl-activation-actions">
        <button
          type="button"
          className="secondary"
          onClick={() => onCopy(address)}
          disabled={!address}
        >
          주소 복사
        </button>
        <button
          type="button"
          className="primary"
          onClick={onRefresh}
          disabled={busy}
        >
          {busy ? "확인 중..." : "활성화 상태 다시 확인"}
        </button>
      </div>
    </section>
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

function WalletSetupSheet({ onClose }: { onClose: () => void }) {
  return (
    <WalletGuideSheet
      title="지갑 준비 필요"
      description="모바일 지갑을 먼저 생성하거나 복구해 주세요."
      linkLabel=""
      primaryLabel="확인"
      onClose={onClose}
      onPrimary={onClose}
      ariaLabel="지갑 준비 안내 닫기"
      handleLabel="지갑 준비 안내 시트 이동"
    />
  );
}

function WalletActivationSheet({
  address,
  refreshing,
  onCopyAddress,
  onClose,
  onRefresh,
}: {
  address?: string | null;
  refreshing: boolean;
  onCopyAddress: (address?: string | null) => void;
  onClose: () => void;
  onRefresh: () => void;
}) {
  return (
    <WalletGuideSheet
      title="XRPL 계정 활성화 필요"
      description="지갑 주소로 Testnet XRP를 입금한 뒤 다시 확인해 주세요."
      linkLabel="주소 복사"
      primaryLabel={refreshing ? "확인 중..." : "활성화 상태 다시 확인"}
      onClose={onClose}
      onPrimary={onRefresh}
      onLink={() => onCopyAddress(address)}
      linkDisabled={!address}
      primaryDisabled={refreshing}
      ariaLabel="지갑 활성화 안내 닫기"
      handleLabel="지갑 활성화 안내 시트 이동"
      sheetClassName="xrpl-activation-sheet"
    >
      <div className="wallet-sheet-address">
        <span>지갑 주소</span>
        <strong>{address ?? "주소 확인 중"}</strong>
      </div>
    </WalletGuideSheet>
  );
}

function WalletGuideSheet({
  title,
  description,
  linkLabel,
  primaryLabel,
  children,
  onClose,
  onPrimary,
  onLink,
  linkDisabled,
  primaryDisabled,
  ariaLabel,
  handleLabel,
  sheetClassName,
}: {
  title: string;
  description: string;
  linkLabel: string;
  primaryLabel: string;
  children?: ReactNode;
  onClose: () => void;
  onPrimary: () => void | Promise<void>;
  onLink?: () => void;
  linkDisabled?: boolean;
  primaryDisabled?: boolean;
  ariaLabel: string;
  handleLabel: string;
  sheetClassName?: string;
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
        className={`wallet-activation-sheet${sheetClassName ? ` ${sheetClassName}` : ""}${dragging ? " dragging" : ""}${closing ? " closing" : ""}`}
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
          {children}
          {onLink ? (
            <button
              type="button"
              className="wallet-sheet-link"
              onClick={onLink}
              disabled={linkDisabled}
            >
              {linkLabel}
            </button>
          ) : null}
        </div>
        <button
          type="button"
          className="wallet-sheet-primary"
          disabled={primaryDisabled}
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
