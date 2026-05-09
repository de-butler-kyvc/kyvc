import { setStoredCorporateType } from "@/lib/kyc-flow";

export type SignupEntityTypeId =
  | "jusik"
  | "yuhan"
  | "hapja"
  | "hapmyong"
  | "sadan"
  | "jaedan"
  | "johap"
  | "goyuho"
  | "foreign";

type EntityTypeMeta = {
  id: SignupEntityTypeId;
  label: string;
  description: string;
  /** kyc-flow.CORPORATE_TYPE_OPTIONS 코드와 매핑 */
  corporateTypeCode: "CORPORATION" | "LIMITED" | "NONPROFIT" | "ASSOCIATION" | "FOREIGN";
  iconKey: "Building" | "Users" | "UserCheck" | "Shield" | "Grid" | "Hash" | "File";
};

export const SIGNUP_ENTITY_TYPES: EntityTypeMeta[] = [
  {
    id: "jusik",
    label: "주식회사",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "CORPORATION",
    iconKey: "Building"
  },
  {
    id: "yuhan",
    label: "유한회사",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "LIMITED",
    iconKey: "Building"
  },
  {
    id: "hapja",
    label: "합자회사",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "CORPORATION",
    iconKey: "Users"
  },
  {
    id: "hapmyong",
    label: "합명회사",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "CORPORATION",
    iconKey: "Users"
  },
  {
    id: "sadan",
    label: "사단법인",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "NONPROFIT",
    iconKey: "UserCheck"
  },
  {
    id: "jaedan",
    label: "재단법인",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "NONPROFIT",
    iconKey: "Shield"
  },
  {
    id: "johap",
    label: "조합",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "ASSOCIATION",
    iconKey: "Grid"
  },
  {
    id: "goyuho",
    label: "고유번호를 부여받은 단체",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "ASSOCIATION",
    iconKey: "Hash"
  },
  {
    id: "foreign",
    label: "외국기업",
    description: "KYC 신청, VC 발급, VP 제출",
    corporateTypeCode: "FOREIGN",
    iconKey: "File"
  }
];

export type SignupDraft = {
  entityTypeId?: SignupEntityTypeId;
  entityLabel?: string;
  email?: string;
  password?: string;
  userName?: string;
  phone?: string;
  corporateName?: string;
  termsAcceptedAt?: string;
  marketingAccepted?: boolean;
  signedUpAt?: string;
};

const STORAGE_KEY = "kyvc.signupDraft";

export function readSignupDraft(): SignupDraft {
  if (typeof window === "undefined") return {};
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as SignupDraft) : {};
  } catch {
    return {};
  }
}

export function writeSignupDraft(patch: Partial<SignupDraft>) {
  if (typeof window === "undefined") return;
  const next = { ...readSignupDraft(), ...patch };
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  return next;
}

export function clearSignupDraft() {
  if (typeof window === "undefined") return;
  window.sessionStorage.removeItem(STORAGE_KEY);
}

export function setSignupEntityType(id: SignupEntityTypeId) {
  const meta = SIGNUP_ENTITY_TYPES.find((t) => t.id === id);
  if (!meta) return;
  writeSignupDraft({ entityTypeId: id, entityLabel: meta.label });
  // KYC 플로우와 연동: 회원가입 시 선택한 유형을 KYC 신청에서 재사용
  setStoredCorporateType(meta.corporateTypeCode);
}

export function getEntityMeta(id?: SignupEntityTypeId | null) {
  if (!id) return undefined;
  return SIGNUP_ENTITY_TYPES.find((t) => t.id === id);
}
