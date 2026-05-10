/**
 * 모바일 웹뷰에서 페이지 간 휘발성 상태를 주고받기 위한 sessionStorage 래퍼.
 * - VP 제출 흐름: scan → submit → submitting 사이에 nonce/aud/credentialId 전달
 * - 상태값은 이메일 인증 트리거 등 짧은 라이프사이클 데이터만 담는다.
 * - 민감정보(seed/mnemonic)는 절대 저장하지 않는다.
 */

const KEYS = {
  scan: "kyvc.m.scanResult",
  vpRequest: "kyvc.m.vpRequest",
  selectedVcId: "kyvc.m.selectedVcId",
  xrpTransfer: "kyvc.m.xrpTransfer",
  xrpTransferResult: "kyvc.m.xrpTransferResult",
} as const;

export type ScanResult = {
  qrData?: string;
  actionType?: string;
  coreBaseUrl?: string;
  requestId?: string;
  requesterName?: string;
  purpose?: string;
  nonce?: string;
  challenge?: string;
  domain?: string;
  endpoint?: string;
  receivedAt: number;
};

export type VpRequest = {
  nonce: string;
  aud: string;
  endpoint: string;
  credentialId?: string;
  receivedAt: number;
};

export type XrpTransferDraft = {
  destinationAddress: string;
  destinationTag?: string;
  amountXrp: string;
  feeXrp?: string;
  createdAt: number;
};

export type XrpTransferResult = XrpTransferDraft & {
  txHash?: string;
  ledgerIndex?: string | number;
  completedAt: number;
};

function read<T>(key: string): T | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.sessionStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function write<T>(key: string, value: T | null) {
  if (typeof window === "undefined") return;
  if (value === null) window.sessionStorage.removeItem(key);
  else window.sessionStorage.setItem(key, JSON.stringify(value));
}

export const mSession = {
  readScanResult: () => read<ScanResult>(KEYS.scan),
  writeScanResult: (v: ScanResult | null) => write(KEYS.scan, v),
  readVpRequest: () => read<VpRequest>(KEYS.vpRequest),
  writeVpRequest: (v: VpRequest | null) => write(KEYS.vpRequest, v),
  readSelectedVcId: () => read<{ id: string }>(KEYS.selectedVcId)?.id ?? null,
  writeSelectedVcId: (id: string | null) =>
    write(KEYS.selectedVcId, id ? { id } : null),
  readXrpTransfer: () => read<XrpTransferDraft>(KEYS.xrpTransfer),
  writeXrpTransfer: (v: XrpTransferDraft | null) => write(KEYS.xrpTransfer, v),
  readXrpTransferResult: () =>
    read<XrpTransferResult>(KEYS.xrpTransferResult),
  writeXrpTransferResult: (v: XrpTransferResult | null) =>
    write(KEYS.xrpTransferResult, v),
  clearAll: () => {
    write(KEYS.scan, null);
    write(KEYS.vpRequest, null);
    write(KEYS.selectedVcId, null);
    write(KEYS.xrpTransfer, null);
    write(KEYS.xrpTransferResult, null);
  },
};
