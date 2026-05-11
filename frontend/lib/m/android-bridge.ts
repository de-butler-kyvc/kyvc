/**
 * Android WebView Bridge (KYvC Mobile)
 *
 * - 모든 호출은 `window.Android.<method>(JSON.stringify(payload))`
 * - 응답은 `window.onAndroidResult(resultJson)` 단일 채널
 * - 다음 두 가지 호출 모드 제공:
 *   1) 직접 호출: `callAndroidVoid(method, payload)` — 결과는 이벤트 버스로 처리
 *   2) Promise 호출: `callBridge(method, payload, { expectedAction })` — 첫 번째 응답을 await
 * - action 단위 구독: `onBridgeAction("LIST_WALLETS", handler)`
 * - 브라우저(미리보기)에서 window.Android가 없으면 mock 결과를 즉시 dispatch (개발 편의)
 *
 * 참고: 레퍼런스 KYvC_MOB_01 / index.html
 */

type AnyJson = Record<string, unknown>;

export type BridgeResult = {
  action?: string;
  ok: boolean;
  error?: string;
  source?: string;
  requestId?: string;
} & AnyJson;

declare global {
  interface Window {
    Android?: Record<string, (json: string) => unknown>;
    onAndroidResult?: (resultJson: string | BridgeResult) => void;
    onVCStored?: (vcId: string) => void;
    onSignatureCreated?: (signature: string) => void;
    onQRCodeScanned?: (qrData: string) => void;
  }
}

// ── 운영 환경 상수 ─────────────────────────────────────────────
/** Core API (Issuer/Verifier) — 브리지의 coreBaseUrl 인자에 사용 */
export const CORE_BASE_URL =
  (typeof process !== "undefined" &&
    (process.env?.NEXT_PUBLIC_CORE_BASE_URL as string | undefined)) ||
  "https://dev-core-kyvc.khuoo.synology.me";

/** Verifier presentations endpoint default (coreBaseUrl + path) */
export const DEFAULT_VERIFIER_ENDPOINT = `${CORE_BASE_URL}/verifier/presentations/verify`;

/** XRPL ledger 기준 — payload.network 기본값 */
export const NETWORK = "testnet";

// ── method → response action 매핑 (예외 케이스) ────────────────
// 대부분 camelCase → SNAKE_CASE로 자동 변환되지만
// 일부 비대칭 매핑은 명시 오버라이드.
const METHOD_TO_ACTION_OVERRIDE: Record<string, string> = {
  requestIssuerCredential: "ISSUER_CREDENTIAL_RECEIVED",
  submitPresentationToVerifier: "SUBMIT_TO_VERIFIER",
  refreshAllCredentialStatuses: "REFRESH_CREDENTIAL_STATUSES",
  getCredentialSummaries: "GET_CREDENTIAL_SUMMARIES",
  listCredentials: "LIST_CREDENTIALS",
  submitToXRPL: "SUBMIT_TO_XRPL",
  getAuthStatus: "GET_AUTH_STATUS",
  copyTextToClipboard: "COPY_TEXT_TO_CLIPBOARD",
};

function methodToAction(method: string): string {
  if (METHOD_TO_ACTION_OVERRIDE[method]) return METHOD_TO_ACTION_OVERRIDE[method];
  // saveVC / scanQRCode / submitToXRPL 같은 약어 처리: 연속 대문자 유지
  return method
    .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1_$2")
    .toUpperCase();
}

// ── 이벤트 버스 ────────────────────────────────────────────────
type ActionHandler = (r: BridgeResult) => void;
const actionListeners = new Map<string, Set<ActionHandler>>();

export function onBridgeAction(action: string, handler: ActionHandler): () => void {
  let set = actionListeners.get(action);
  if (!set) {
    set = new Set();
    actionListeners.set(action, set);
  }
  set.add(handler);
  return () => {
    set!.delete(handler);
    if (set!.size === 0) actionListeners.delete(action);
  };
}

function emit(result: BridgeResult) {
  const action = typeof result.action === "string" ? result.action : "";
  if (!action) return;
  const listeners = actionListeners.get(action);
  if (!listeners) return;
  // 사본을 만들어 호출 중 add/remove 안전
  Array.from(listeners).forEach((h) => {
    try {
      h(result);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error("[bridge] handler error", action, e);
    }
  });
}

// ── promise pool ───────────────────────────────────────────────
type Pending = {
  resolve: (r: BridgeResult) => void;
  reject: (err: Error) => void;
  expectedAction: string;
  timeout: ReturnType<typeof setTimeout>;
};

const pendingByRequestId = new Map<string, Pending>();
const pendingByAction = new Map<string, Pending[]>();

function dispatchResult(result: BridgeResult) {
  const reqId =
    typeof result.requestId === "string" ? result.requestId : undefined;

  // 1) requestId 매칭 우선
  if (reqId && pendingByRequestId.has(reqId)) {
    const p = pendingByRequestId.get(reqId)!;
    pendingByRequestId.delete(reqId);
    clearTimeout(p.timeout);
    const queue = pendingByAction.get(p.expectedAction);
    if (queue) {
      const idx = queue.indexOf(p);
      if (idx >= 0) queue.splice(idx, 1);
      if (queue.length === 0) pendingByAction.delete(p.expectedAction);
    }
    p.resolve(result);
  } else if (typeof result.action === "string") {
    // 2) action FIFO 매칭
    const queue = pendingByAction.get(result.action);
    if (queue && queue.length) {
      const p = queue.shift()!;
      clearTimeout(p.timeout);
      // requestId 등록도 정리
      for (const [rid, item] of pendingByRequestId.entries()) {
        if (item === p) {
          pendingByRequestId.delete(rid);
          break;
        }
      }
      if (queue.length === 0) pendingByAction.delete(result.action);
      p.resolve(result);
    }
  }

  // 3) action 구독자에게도 fan-out
  emit(result);
}

let bridgeInstalled = false;

export function setupBridge() {
  if (typeof window === "undefined" || bridgeInstalled) return;
  // 결과 수신
  window.onAndroidResult = (resultJson) => {
    try {
      const r: BridgeResult =
        typeof resultJson === "string" ? JSON.parse(resultJson) : resultJson;
      dispatchResult(r);
    } catch {
      dispatchResult({ ok: false, action: "INVALID_RESULT_JSON" } as BridgeResult);
    }
  };
  // 추가 콜백 (참고용)
  if (!window.onVCStored) window.onVCStored = () => {};
  if (!window.onSignatureCreated) window.onSignatureCreated = () => {};
  if (!window.onQRCodeScanned) window.onQRCodeScanned = () => {};
  bridgeInstalled = true;
}

export function isBridgeAvailable(): boolean {
  return typeof window !== "undefined" && !!window.Android;
}

function newRequestId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `req-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

// ── 핵심 호출 함수 ─────────────────────────────────────────────

/** 브리지/네이티브 호출 실패를 명시하는 에러 코드 */
export const BRIDGE_UNAVAILABLE = "BRIDGE_UNAVAILABLE";
export const BRIDGE_METHOD_NOT_FOUND = "BRIDGE_METHOD_NOT_FOUND";
export const BRIDGE_TIMEOUT = "BRIDGE_TIMEOUT";

/**
 * 결과를 기다리지 않는 호출 (응답은 이벤트 버스로 처리).
 * 브리지가 없거나 메서드가 미구현이면 false 반환 (조용한 성공 X).
 */
export function callAndroidVoid(method: string, payload: AnyJson = {}): boolean {
  setupBridge();
  if (typeof window === "undefined") return false;
  if (!window.Android) {
    // eslint-disable-next-line no-console
    console.warn(`[bridge] window.Android missing, cannot call ${method}`);
    return false;
  }

  const fn = window.Android[method];
  if (typeof fn !== "function") {
    // eslint-disable-next-line no-console
    console.warn(`[bridge] Android.${method} not implemented`);
    return false;
  }
  try {
    fn.call(window.Android, JSON.stringify(payload));
    return true;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error(`[bridge] Android.${method} threw`, e);
    return false;
  }
}

/**
 * Promise 기반 호출. 응답 한 번을 기다림.
 *
 * @param method 브리지 메서드명
 * @param payload 페이로드 (requestId/issuedAt 자동 주입)
 * @param options.expectedAction 매칭할 응답 action (생략 시 method에서 추론)
 * @param options.timeoutMs 기본 30000
 */
export function callBridge<T extends BridgeResult = BridgeResult>(
  method: string,
  payload: AnyJson = {},
  options: { expectedAction?: string; timeoutMs?: number } = {},
): Promise<T> {
  setupBridge();

  const expectedAction = options.expectedAction ?? methodToAction(method);
  const timeoutMs = options.timeoutMs ?? 30_000;

  if (typeof window === "undefined") {
    return Promise.reject(new Error(BRIDGE_UNAVAILABLE));
  }

  if (!window.Android) {
    // 브리지가 없는 환경(브라우저 미리보기 등)에서는 명시적으로 실패시킨다.
    // 조용히 mock 응답을 만들어 흐름을 진행시키지 않는다.
    return Promise.reject(new Error(BRIDGE_UNAVAILABLE));
  }

  const requestId =
    typeof payload.requestId === "string" ? payload.requestId : newRequestId();
  const issuedAt =
    typeof payload.issuedAt === "string"
      ? payload.issuedAt
      : new Date().toISOString();

  const finalPayload: AnyJson = { ...payload, requestId, issuedAt };

  const fn = window.Android[method];
  if (typeof fn !== "function") {
    return Promise.reject(new Error(`${BRIDGE_METHOD_NOT_FOUND}:${method}`));
  }

  return new Promise<T>((resolve, reject) => {
    const timeout = setTimeout(() => {
      pendingByRequestId.delete(requestId);
      const queue = pendingByAction.get(expectedAction);
      if (queue) {
        const idx = queue.findIndex((p) => p.timeout === timeout);
        if (idx >= 0) queue.splice(idx, 1);
        if (queue.length === 0) pendingByAction.delete(expectedAction);
      }
      reject(new Error(`${BRIDGE_TIMEOUT}:${expectedAction}`));
    }, timeoutMs);

    const entry: Pending = {
      resolve: (r) => resolve(r as T),
      reject,
      expectedAction,
      timeout,
    };

    pendingByRequestId.set(requestId, entry);
    const queue = pendingByAction.get(expectedAction) ?? [];
    queue.push(entry);
    pendingByAction.set(expectedAction, queue);

    try {
      fn.call(window.Android, JSON.stringify(finalPayload));
    } catch (err) {
      clearTimeout(timeout);
      pendingByRequestId.delete(requestId);
      const q = pendingByAction.get(expectedAction);
      if (q) {
        const idx = q.indexOf(entry);
        if (idx >= 0) q.splice(idx, 1);
      }
      reject(err instanceof Error ? err : new Error(String(err)));
    }
  });
}

// ── 자주 쓰는 메서드 단축 호출 ─────────────────────────────────

export type AuthStatus = BridgeResult & {
  lockConfigured?: boolean;
  pinConfigured?: boolean;
  patternConfigured?: boolean;
  biometricEnabled?: boolean;
  availableMethods?: ("pin" | "pattern" | "biometric")[];
  walletReady?: boolean;
  failedAttempts?: number;
  remainingAttempts?: number;
  failureThreshold?: number;
  emailVerificationRequired?: boolean;
  sessionUnlocked?: boolean;
  sessionRemainingMs?: number;
  sessionExpiresAtMs?: number;
  xrpPaymentAuthReady?: boolean;
  xrpPaymentAuthRemainingMs?: number;
};

export type WalletInfo = BridgeResult & {
  account?: string;
  publicKey?: string;
  authPublicKey?: string;
  did?: string;
  mnemonic?: string;
  didDocument?: string;
};

export type WalletItem = {
  account: string;
  did?: string;
  name?: string;
  derivationIndex?: number;
  mnemonicHash?: string;
  hasMnemonic?: boolean;
  isActive?: boolean;
};

export type ListWalletsResult = BridgeResult & {
  activeAccount?: string;
  wallets?: WalletItem[];
};

export type NativeCredentialStatus =
  | "active"
  | "issued"
  | "inactive"
  | "expired"
  | "notYetValid"
  | string;

export type NativeCredentialSummary = {
  credentialId: string;
  status?: NativeCredentialStatus;
  statusLabel?: string;
  issuedAt?: string;
  expiresAt?: string;
  issuerDid?: string;
  issuerAccount?: string;
  holderDid?: string;
  holderAccount?: string;
  credentialType?: string;
  credentialKind?: string;
  format?: string;
  accepted?: boolean;
};

export type CredentialSummariesResult = BridgeResult & {
  count?: number;
  credentials?: NativeCredentialSummary[];
};

export const bridge = {
  // 인증/세션
  getAuthStatus: () => callBridge<AuthStatus>("getAuthStatus", {}),
  requestNativeAuth: (
    method: "pin" | "pattern" | "biometric",
    reason: "wallet-login" | "xrp-payment" | string,
    backendRequest?: AnyJson,
  ) =>
    callBridge<AuthStatus & { authenticated?: boolean }>(
      "requestNativeAuth",
      {
        action: "REQUEST_NATIVE_AUTH",
        method,
        reason,
        ...(backendRequest ? { backendRequest } : {}),
      },
    ),
  requestPinReset: (reason = "user-request") =>
    callAndroidVoid("requestPinReset", {
      action: "REQUEST_PIN_RESET",
      requestId: newRequestId(),
      issuedAt: new Date().toISOString(),
      reason,
    }),
  completeEmailVerification: () =>
    callBridge("completeEmailVerification", {
      action: "COMPLETE_EMAIL_VERIFICATION",
    }),
  logout: () => callBridge("logout", { action: "LOGOUT" }),

  // 지갑
  createWallet: (overwrite = false) =>
    callBridge<WalletInfo>("createWallet", { overwrite }),
  getWalletInfo: () => callBridge<WalletInfo>("getWalletInfo", {}),
  listWallets: () => callBridge<ListWalletsResult>("listWallets", {}),
  switchWallet: (account: string) =>
    callBridge("switchWallet", { account }),
  setAccountName: (name: string, account?: string) =>
    callBridge("setAccountName", {
      action: "SET_ACCOUNT_NAME",
      name,
      ...(account ? { account } : {}),
    }),
  upgradeToMnemonic: () =>
    callBridge<WalletInfo & { mnemonic?: string }>(
      "upgradeToMnemonic",
      { action: "UPGRADE_TO_MNEMONIC" },
    ),
  deriveNextAccount: (name?: string) =>
    callBridge("deriveNextAccount", {
      action: "DERIVE_NEXT_ACCOUNT",
      ...(name ? { name } : {}),
    }),
  exportWalletSeed: () =>
    callBridge<WalletInfo & { seed?: string }>("exportWalletSeed", {
      action: "EXPORT_WALLET_SEED",
    }),
  exportWalletMnemonic: () =>
    callBridge<WalletInfo & { mnemonic?: string }>(
      "exportWalletMnemonic",
      { action: "EXPORT_WALLET_MNEMONIC" },
    ),
  restoreWallet: (
    payload:
      | { seed: string; overwrite?: boolean; autoRegisterDidSet?: boolean }
      | { mnemonic: string; overwrite?: boolean; autoRegisterDidSet?: boolean },
  ) =>
    callBridge<
      WalletInfo & {
        restored?: boolean;
        reusedExistingAccount?: boolean;
        holderDidSetRegistrationRequired?: boolean;
      }
    >("restoreWallet", {
      autoRegisterDidSet: true,
      ...payload,
    }),
  submitHolderDidSet: (didDocumentUri?: string) =>
    callBridge("submitHolderDidSet", {
      ...(didDocumentUri ? { didDocumentUri } : {}),
    }),

  // 자산/거래
  getWalletAssets: () => callBridge("getWalletAssets", {}),
  getWalletDepositInfo: () => callBridge("getWalletDepositInfo", {}),
  copyWalletAddress: () => callBridge("copyWalletAddress", {}),
  getWalletTransactions: (limit = 10) =>
    callBridge("getWalletTransactions", { limit }),
  submitXrpPayment: (params: {
    destinationAddress: string;
    destinationTag?: string;
    amountXrp?: string;
    amountDrops?: string;
  }) => callBridge("submitXrpPayment", { ...params, network: NETWORK }),
  copyTextToClipboard: (text: string) =>
    callBridge("copyTextToClipboard", { text }),

  // VC
  saveVC: (payload: AnyJson) => callBridge("saveVC", payload),
  checkCredentialStatus: (params: {
    credentialId: string;
    issuerAccount?: string;
    holderAccount?: string;
    credentialType?: string;
  }) =>
    callBridge<
      BridgeResult & {
        found?: boolean;
        active?: boolean;
        accepted?: boolean;
      }
    >("checkCredentialStatus", params),
  refreshAllCredentialStatuses: () =>
    callBridge("refreshAllCredentialStatuses", {}),
  getCredentialSummaries: () =>
    callBridge<CredentialSummariesResult>("getCredentialSummaries", {}),
  submitToXRPL: (params: {
    credentialId: string;
    issuerAccount?: string;
    holderAccount?: string;
    credentialType?: string;
  }) =>
    callBridge("submitToXRPL", {
      ...params,
      type: "CredentialAccept",
      network: NETWORK,
    }),
  listCredentials: () => callBridge("listCredentials", {}),

  // Issuer / Verifier
  /**
   * 실서버 SD-JWT 발급. 응답은 비동기(`ISSUER_CREDENTIAL_RECEIVED`)로 도착할 수 있으므로
   * `onBridgeAction("ISSUER_CREDENTIAL_RECEIVED", ...)` 구독을 권장.
   */
  requestIssuerCredential: (params?: {
    coreBaseUrl?: string;
    format?: string;
    jurisdiction?: string;
    assuranceLevel?: string;
    kycLevel?: string;
  }) =>
    callBridge("requestIssuerCredential", {
      coreBaseUrl: CORE_BASE_URL,
      format: "dc+sd-jwt",
      jurisdiction: "KR",
      assuranceLevel: "STANDARD",
      ...params,
    }),
  verifyCredentialWithServer: (params: {
    vcJson?: string;
    vcJwt?: string | null;
    sdJwt?: string | null;
    coreBaseUrl?: string;
    require_status?: boolean;
    status_mode?: "xrpl" | "core" | string;
  }) =>
    callBridge("verifyCredentialWithServer", {
      coreBaseUrl: CORE_BASE_URL,
      require_status: true,
      status_mode: "xrpl",
      ...params,
    }),
  requestVerifierChallenge: (params?: {
    coreBaseUrl?: string;
    aud?: string;
    presentationDefinition?: AnyJson;
  }) =>
    callBridge<
      BridgeResult & {
        nonce?: string;
        challenge?: string;
        aud?: string;
        domain?: string;
        endpoint?: string;
        expiresAt?: string;
      }
    >("requestVerifierChallenge", {
      coreBaseUrl: CORE_BASE_URL,
      aud: CORE_BASE_URL,
      domain: CORE_BASE_URL,
      presentationDefinition: defaultPresentationDefinition(),
      ...params,
    }),
  registerVerifierChallenge: (params: {
    challenge: string;
    nonce?: string;
    domain?: string;
    aud?: string;
    expiresAt?: string;
  }) =>
    callBridge("registerVerifierChallenge", {
      issuedAt: new Date().toISOString(),
      ...params,
    }),
  signMessage: (params: {
    challenge: string;
    domain: string;
    credentialId?: string;
    credential?: string;
    selectedDisclosures?: string[];
    vcJson?: string;
    vcJwt?: string | null;
    sdJwt?: string | null;
  }) =>
    callBridge<
      BridgeResult & {
        format?: string;
        sdJwtKb?: string;
        proofValue?: string;
        holder?: string;
        presentation?: AnyJson;
        presentationJwt?: string;
        didDocument?: AnyJson;
        sdHash?: string;
        nonce?: string;
        aud?: string;
        domain?: string;
      }
    >("signMessage", params),
  submitPresentationToVerifier: (params: AnyJson) =>
    callBridge("submitPresentationToVerifier", {
      endpoint: DEFAULT_VERIFIER_ENDPOINT,
      require_status: true,
      status_mode: "xrpl",
      ...params,
    }),

  // QR
  scanQRCode: (purpose?: string) =>
    callBridge<
      BridgeResult & {
        qrData?: string;
        actionType?: string;
        coreBaseUrl?: string;
        challenge?: string;
        domain?: string;
        endpoint?: string;
      }
    >("scanQRCode", {
      ...(purpose ? { purpose } : {}),
      requestId: newRequestId(),
    }),
};

export function defaultPresentationDefinition() {
  return {
    id: "wallet-direct-kyc-test-v1",
    acceptedFormat: "dc+sd-jwt",
    acceptedVct: ["https://kyvc.example/vct/legal-entity-kyc-v1"],
    acceptedJurisdictions: ["KR"],
    minimumAssuranceLevel: "STANDARD",
    requiredDisclosures: [
      "legalEntity.type",
      "representative.name",
      "representative.birthDate",
      "representative.nationality",
      "beneficialOwners[].name",
      "beneficialOwners[].birthDate",
      "beneficialOwners[].nationality",
    ],
    documentRules: [],
  };
}

// ── React 훅 ───────────────────────────────────────────────────
import { useEffect } from "react";

/**
 * 특정 action 응답을 구독한다. 컴포넌트 언마운트 시 자동 해제.
 *
 * @example
 * useBridgeAction("LIST_WALLETS", (r) => {
 *   if (r.ok) setWallets(r.wallets ?? []);
 * });
 */
export function useBridgeAction(action: string, handler: ActionHandler) {
  useEffect(() => {
    const off = onBridgeAction(action, handler);
    return off;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [action]);
}
