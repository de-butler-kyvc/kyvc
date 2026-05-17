export type CorporateTypeCode =
  | "CORPORATION"
  | "LIMITED_COMPANY"
  | "LIMITED_PARTNERSHIP"
  | "GENERAL_PARTNERSHIP"
  | "NON_PROFIT"
  | "ASSOCIATION"
  | "FOREIGN_COMPANY"
  | "SOLE_PROPRIETOR";

export type CorporateTypeOption = {
  value: CorporateTypeCode;
  label: string;
  docs: string;
  sortOrder: number;
  disabled?: boolean;
  badge?: string;
};

export const CORPORATE_TYPE_OPTIONS: CorporateTypeOption[] = [
  {
    value: "CORPORATION",
    label: "주식회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 주주명부 또는 주식변동상황명세서",
    sortOrder: 1
  },
  {
    value: "LIMITED_COMPANY",
    label: "유한회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개",
    sortOrder: 2
  },
  {
    value: "LIMITED_PARTNERSHIP",
    label: "합자회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개",
    sortOrder: 3
  },
  {
    value: "GENERAL_PARTNERSHIP",
    label: "합명회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개",
    sortOrder: 4
  },
  {
    value: "NON_PROFIT",
    label: "비영리법인",
    docs: "정관 · 설립목적 증빙서류 · 등기사항전부증명서",
    sortOrder: 5
  },
  {
    value: "ASSOCIATION",
    label: "조합·단체",
    docs: "고유번호증 · 대표자 확인서류 · 규약 문서 중 1개",
    sortOrder: 6
  },
  {
    value: "FOREIGN_COMPANY",
    label: "외국기업",
    docs: "국내 사업자등록증 · 등기 · 본국 설립서류 · 외국인투자등록증명서",
    sortOrder: 7
  },
  {
    value: "SOLE_PROPRIETOR",
    label: "개인사업자",
    docs: "후속 확장 회사 유형",
    sortOrder: 8,
    disabled: true,
    badge: "후속 확장"
  }
];

export const KYC_DOCUMENT_SLOTS = [
  {
    documentTypeCode: "BUSINESS_REGISTRATION",
    label: "사업자등록증",
    required: true
  },
  {
    documentTypeCode: "CORPORATE_REGISTRY",
    label: "등기사항전부증명서",
    required: true
  },
  {
    documentTypeCode: "SHAREHOLDER_REGISTRY",
    label: "주주명부",
    required: false,
    hint: "소유구조 확인 문서 중 1개로 제출할 수 있습니다.",
    groupCode: "OWNERSHIP_DOC",
    groupName: "소유구조 확인 문서",
    minRequiredCount: 1,
    groupCandidate: true
  },
  {
    documentTypeCode: "STOCK_CHANGE_STATEMENT",
    label: "주식변동상황명세서",
    required: false,
    hint: "소유구조 확인 문서 중 1개로 제출할 수 있습니다.",
    groupCode: "OWNERSHIP_DOC",
    groupName: "소유구조 확인 문서",
    minRequiredCount: 1,
    groupCandidate: true
  },
  {
    documentTypeCode: "ARTICLES_OF_ASSOCIATION",
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
const DEFAULT_CORPORATE_TYPE: CorporateTypeCode = "CORPORATION";

const CORPORATE_TYPE_DEFINITION_MAP = new Map(
  CORPORATE_TYPE_OPTIONS.map((option) => [option.value, option])
);

export function normalizeCorporateTypeCode(
  value?: string | null
): CorporateTypeCode | null {
  if (!value) return null;
  const normalized = value.trim().toUpperCase();
  switch (normalized) {
    case "JOINT_STOCK_COMPANY":
      return "CORPORATION";
    case "LIMITED":
      return "LIMITED_COMPANY";
    case "NONPROFIT":
      return "NON_PROFIT";
    case "FOREIGN":
      return "FOREIGN_COMPANY";
    default:
      return CORPORATE_TYPE_DEFINITION_MAP.has(normalized as CorporateTypeCode)
        ? (normalized as CorporateTypeCode)
        : null;
  }
}

export function getSupportedCorporateTypeOptions(
  options: Array<{ value?: string | null; code?: string | null }> = CORPORATE_TYPE_OPTIONS
) {
  const seen = new Set<CorporateTypeCode>();
  const merged: CorporateTypeOption[] = [];

  for (const option of options) {
    const code = normalizeCorporateTypeCode(option.value ?? option.code);
    if (!code || seen.has(code)) continue;
    const definition = CORPORATE_TYPE_DEFINITION_MAP.get(code);
    if (!definition) continue;
    seen.add(code);
    merged.push(definition);
  }

  return merged.sort((left, right) => left.sortOrder - right.sortOrder);
}

export function getSelectableCorporateTypeCode(value?: string | null) {
  const code = normalizeCorporateTypeCode(value);
  const option = code ? CORPORATE_TYPE_DEFINITION_MAP.get(code) : null;
  return option && !option.disabled ? option.value : DEFAULT_CORPORATE_TYPE;
}

export const DOCUMENT_LABELS = KYC_DOCUMENT_SLOTS.reduce<Record<string, string>>(
  (acc, slot) => {
    acc[slot.documentTypeCode] = slot.label;
    return acc;
  },
  {
    BUSINESS_REGISTRATION: "사업자등록증",
    CORPORATE_REGISTRY: "등기사항전부증명서",
    CORPORATE_REGISTRATION: "등기사항전부증명서",
    SHAREHOLDER_REGISTRY: "주주명부",
    SHAREHOLDER_LIST: "주주명부",
    STOCK_CHANGE_STATEMENT: "주식변동상황명세서",
    INVESTOR_REGISTRY: "투자자명부",
    MEMBER_REGISTRY: "사원명부",
    ARTICLES_OF_ASSOCIATION: "정관",
    ARTICLES_OF_INCORPORATION: "정관",
    PURPOSE_PROOF_DOCUMENT: "설립목적 증빙서류",
    ORGANIZATION_IDENTITY_CERTIFICATE: "고유번호증",
    REPRESENTATIVE_ID: "대표자 확인서류",
    REPRESENTATIVE_PROOF_DOCUMENT: "대표자 확인서류",
    OPERATING_RULES: "규약 문서",
    REGULATIONS: "규약 문서",
    FOREIGN_INVESTMENT_REGISTRATION_CERTIFICATE: "외국인투자등록증명서",
    INVESTMENT_REGISTRATION_CERTIFICATE: "외국인투자등록증명서",
    POWER_OF_ATTORNEY: "위임장",
    SEAL_CERTIFICATE: "인감증명서",
    AGENT_ID: "대리인 신분확인",
    OTHER: "기타"
  }
);

export function getStoredCorporateType() {
  if (typeof window === "undefined") return DEFAULT_CORPORATE_TYPE;
  return getSelectableCorporateTypeCode(window.localStorage.getItem(CORPORATE_TYPE_KEY));
}

export function setStoredCorporateType(corporateTypeCode: string) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(CORPORATE_TYPE_KEY, getSelectableCorporateTypeCode(corporateTypeCode));
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
  const normalizedCode = normalizeCorporateTypeCode(code);
  return normalizedCode
    ? CORPORATE_TYPE_DEFINITION_MAP.get(normalizedCode)?.label ?? normalizedCode
    : code ?? "법인";
}
