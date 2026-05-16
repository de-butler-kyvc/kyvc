export const CORPORATE_TYPE_OPTIONS = [
  {
    value: "CORPORATION",
    label: "주식회사",
    docs: "1차 MVP 지원 법인 유형",
  },
  {
    value: "JOINT_STOCK_COMPANY",
    label: "주식회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 주주명부 또는 주식변동상황명세서",
  },
  {
    value: "LIMITED_COMPANY",
    label: "유한회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개",
  },
  {
    value: "LIMITED_PARTNERSHIP",
    label: "합자회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개",
  },
  {
    value: "GENERAL_PARTNERSHIP",
    label: "합명회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개",
  },
  {
    value: "NON_PROFIT",
    label: "비영리법인",
    docs: "정관 · 설립목적 증빙서류 · 등기사항전부증명서",
  },
  {
    value: "ASSOCIATION",
    label: "조합·단체",
    docs: "고유번호증 · 대표자 확인서류 · 규약 문서 중 1개",
  },
  {
    value: "FOREIGN_COMPANY",
    label: "외국기업",
    docs: "국내 사업자등록증 · 등기 · 본국 설립서류 · 외국인투자등록증명서",
  },
  {
    value: "SOLE_PROPRIETOR",
    label: "개인사업자",
    docs: "후속 확장 회사 유형",
  },
];

export const KYC_DOCUMENT_SLOTS = [
  {
    documentTypeCode: "BUSINESS_REGISTRATION",
    label: "사업자등록증",
    required: true
  },
  {
    documentTypeCode: "CORPORATE_REGISTRATION",
    label: "등기사항전부증명서",
    required: true
  },
  {
    documentTypeCode: "SHAREHOLDER_LIST",
    label: "주주명부",
    required: true
  },
  {
    documentTypeCode: "ARTICLES_OF_INCORPORATION",
    label: "정관",
    required: false,
    hint: "해당 시 제출"
  },
  {
    documentTypeCode: "POWER_OF_ATTORNEY",
    label: "위임장",
    required: false,
    hint: "대리 신청 시 제출"
  }
];

export type KycDocumentSlot = (typeof KYC_DOCUMENT_SLOTS)[number];

type KycStorageSource = {
  kycId?: number | null;
  corporateTypeCode?: string | null;
  kycStatus?: string | null;
};

const CURRENT_KYC_ID_KEY = "kyvc.currentKycId";
const CORPORATE_TYPE_KEY = "kyvc.corporateType";
const SESSION_USER_ID_KEY = "kyvc.sessionUserId";
const EDITABLE_KYC_STATUS = "DRAFT";

export const DOCUMENT_LABELS = KYC_DOCUMENT_SLOTS.reduce<Record<string, string>>(
  (acc, slot) => {
    acc[slot.documentTypeCode] = slot.label;
    return acc;
  },
  {
    REPRESENTATIVE_ID: "대표자 신분확인",
    AGENT_ID: "대리인 신분확인",
    OTHER: "기타"
  }
);

export function getStoredCorporateType() {
  if (typeof window === "undefined") return "CORPORATION";
  return window.localStorage.getItem(CORPORATE_TYPE_KEY) ?? "CORPORATION";
}

export function setStoredCorporateType(corporateTypeCode: string) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(CORPORATE_TYPE_KEY, corporateTypeCode);
}

export function getCurrentKycId() {
  if (typeof window === "undefined") return 0;
  return Number(window.localStorage.getItem(CURRENT_KYC_ID_KEY));
}

export function setCurrentKycId(kycId: number) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(CURRENT_KYC_ID_KEY, String(kycId));
}

export function clearCurrentKycId() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(CURRENT_KYC_ID_KEY);
}

export function clearKyvcLocalStorage() {
  if (typeof window === "undefined") return;
  Object.keys(window.localStorage)
    .filter((key) => key.startsWith("kyvc."))
    .forEach((key) => window.localStorage.removeItem(key));
}

export function syncKyvcSessionUser(userId?: number | null) {
  if (typeof window === "undefined" || !userId) return;

  const nextUserId = String(userId);
  const previousUserId = window.localStorage.getItem(SESSION_USER_ID_KEY);
  const hasUnscopedKycState =
    !previousUserId &&
    (window.localStorage.getItem(CURRENT_KYC_ID_KEY) ||
      window.localStorage.getItem("kyvc.corporateExtras"));
  if ((previousUserId && previousUserId !== nextUserId) || hasUnscopedKycState) {
    clearKyvcLocalStorage();
  }
  window.localStorage.setItem(SESSION_USER_ID_KEY, nextUserId);
}

export function syncCurrentKycStorage(application?: KycStorageSource | null) {
  if (!application?.kycId || application.kycStatus !== EDITABLE_KYC_STATUS) {
    clearCurrentKycId();
    return 0;
  }

  setCurrentKycId(application.kycId);
  if (application.corporateTypeCode) {
    setStoredCorporateType(application.corporateTypeCode);
  }
  return application.kycId;
}

export async function refreshCurrentKycStorage(
  fetchCurrent: () => Promise<KycStorageSource>
) {
  try {
    return syncCurrentKycStorage(await fetchCurrent());
  } catch {
    clearCurrentKycId();
    return 0;
  }
}

export function formatFileSize(bytes?: number | null) {
  if (!bytes) return "-";
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  if (bytes >= 1024) return `${Math.round(bytes / 1024)}KB`;
  return `${bytes}B`;
}

export function compactHash(hash?: string | null) {
  if (!hash) return "-";
  if (hash.length <= 16) return hash;
  return `${hash.slice(0, 10)}...${hash.slice(-6)}`;
}

export function corporateTypeLabel(code?: string | null) {
  return CORPORATE_TYPE_OPTIONS.find((option) => option.value === code)?.label ?? code ?? "법인";
}
