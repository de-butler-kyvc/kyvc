export type AdminVpRequestStatus =
  | "REQUESTED"
  | "PRESENTED"
  | "VALID"
  | "INVALID"
  | "EXPIRED"
  | "CANCELLED";

export interface AdminVpRequestPayload {
  type: "VP_REQUEST";
  requestType: "FINANCIAL_KYC_CHECK";
  vpRequestId: number;
  qrToken: string;
  nonce: string;
  purpose: string;
  expiresAt: string;
}

export interface AdminVpRequestSession {
  payload: AdminVpRequestPayload;
  status: AdminVpRequestStatus;
  requestedAt: string;
  submittedAt: string | null;
  verifiedAt: string | null;
}

export interface AdminVpSubmittedClaim {
  label: string;
  value: string;
  source: string;
}

const MOCK_VP_REQUEST_ID = 100;
const MOCK_QR_TOKEN = "mock-qr-token";
const MOCK_NONCE = "mock-nonce";
const FIVE_MINUTES_MS = 5 * 60 * 1000;

export function buildMockAdminVpRequest(now = new Date()): AdminVpRequestSession {
  return {
    payload: {
      type: "VP_REQUEST",
      requestType: "FINANCIAL_KYC_CHECK",
      vpRequestId: MOCK_VP_REQUEST_ID,
      qrToken: MOCK_QR_TOKEN,
      nonce: MOCK_NONCE,
      purpose: "KYC 인증 확인",
      expiresAt: new Date(now.getTime() + FIVE_MINUTES_MS).toISOString(),
    },
    status: "REQUESTED",
    requestedAt: now.toISOString(),
    submittedAt: null,
    verifiedAt: null,
  };
}

export async function createMockAdminVpRequest(): Promise<AdminVpRequestSession> {
  return buildMockAdminVpRequest();
}

export async function getMockAdminVpSubmittedClaims(): Promise<
  AdminVpSubmittedClaim[]
> {
  return [
    { label: "법인명", value: "한국무역(주)", source: "KYC VC" },
    { label: "사업자등록번호", value: "123-45-67890", source: "KYC VC" },
    { label: "법인번호", value: "110111-1234567", source: "KYC VC" },
    { label: "대표자명", value: "김대표", source: "KYC VC" },
    { label: "KYC 상태", value: "PASSED", source: "KYC VC" },
    { label: "VC 발급일", value: "2026.05.16", source: "KYC VC" },
    { label: "VC 만료일", value: "2026.12.31", source: "KYC VC" },
  ];
}
