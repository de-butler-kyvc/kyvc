import {
  getFinanceVpRequestList,
  type AdminVpRequestStatus,
  type AdminVpRequestSummary,
} from "@/lib/api/admin-vp-request";
import { API_BASE } from "@/lib/api/api-base";
import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const ADMIN_VP_VERIFICATION_BASE = `${API_BASE}/api/admin/backend/vp-verifications`;

// ── 타입 ──────────────────────────────────────────────────────

export interface VpVerification {
  verificationId: string;
  corporationName?: string;
  verifierName?: string;
  purpose?: string;
  credentialId?: string;
  result?: string;
  failReason?: string;
  createdAt?: string;
}

export interface VpVerificationDetail extends VpVerification {
  holderDid?: string;
  verifierDid?: string;
  vpJson?: string;
  requestedAt?: string;
  respondedAt?: string;
}

export interface AdminVpVerificationCredentialInfo {
  credentialId?: string | number | null;
  credentialExternalId?: string | null;
  credentialTypeCode?: string | null;
  credentialStatusCode?: string | null;
  issuerDid?: string | null;
  holderDid?: string | null;
  xrplTxHash?: string | null;
  walletSavedYn?: string | null;
}

export interface AdminVpVerificationVerifierInfo {
  verifierId?: string | number | null;
  verifierName?: string | null;
  verifierStatusCode?: string | null;
  contactEmail?: string | null;
}

export interface AdminVpVerificationDetailResponse {
  vpVerificationId: string | number;
  credentialId?: string | number | null;
  corporateId?: string | number | null;
  corporateName?: string | null;
  requestNonce?: string | null;
  vpRequestId?: string | null;
  purpose?: string | null;
  requesterName?: string | null;
  requiredClaimsJson?: string | null;
  vpVerificationStatusCode?: string | null;
  replaySuspectedYn?: string | null;
  resultSummary?: string | null;
  requestedAt?: string | null;
  presentedAt?: string | null;
  verifiedAt?: string | null;
  expiresAt?: string | null;
  coreRequestStatusCode?: string | null;
  callbackStatusCode?: string | null;
  callbackSentAt?: string | null;
  permissionResultJson?: string | null;
  credential?: AdminVpVerificationCredentialInfo | null;
  verifier?: AdminVpVerificationVerifierInfo | null;
}

export interface AdminVpVerificationSummaryResponse {
  vpVerificationId: string | number;
  vpRequestId?: string | null;
  corporateId?: string | number | null;
  corporateName?: string | null;
  credentialId?: string | number | null;
  requesterName?: string | null;
  purpose?: string | null;
  vpVerificationStatusCode?: string | null;
  replaySuspectedYn?: string | null;
  requestedAt?: string | null;
  presentedAt?: string | null;
  verifiedAt?: string | null;
  expiresAt?: string | null;
  callbackStatusCode?: string | null;
}

export interface AdminVpVerificationListResponse {
  items: AdminVpVerificationSummaryResponse[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

function getAuthHeaders() {
  const token = getAccessTokenForApi();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (!isPlaceholderAccessToken(token)) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function errorMessageFromResponse(response: Response): Promise<string> {
  try {
    const text = await response.text();
    if (!text.trim()) return `API Error: ${response.status} ${response.statusText}`;
    const parsed = JSON.parse(text) as { message?: string; error?: string };
    if (parsed.message) return parsed.message;
    if (typeof parsed.error === "string") return parsed.error;
    return text;
  } catch {
    return `API Error: ${response.status} ${response.statusText}`;
  }
}

function fmtDt(iso?: string | null) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

function displayPurpose(purpose?: string | null) {
  if (!purpose) return "-";
  return purpose === "ACCOUNT_OPENING" ? "KYC 인증 확인" : purpose;
}

function statusToResult(status?: string | null) {
  if (status === "VALID") return "성공";
  if (status === "INVALID" || status === "REPLAY_SUSPECTED") return "실패";
  if (status === "EXPIRED") return "만료";
  return status ?? "-";
}

function resultFilterToStatus(
  result?: string
): AdminVpRequestStatus | undefined {
  if (result === "성공") return "VALID";
  if (result === "실패") return "INVALID";
  if (result === "만료") return "EXPIRED";
  return undefined;
}

function isNumericId(value: string | number) {
  return /^\d+$/.test(String(value));
}

function toRow(summary: AdminVpRequestSummary) {
  return {
    id: summary.requestId,
    corp: summary.corporateName ?? "-",
    verifier: "KYvC Finance",
    purpose: displayPurpose(summary.purpose),
    vc:
      summary.requestedClaims && summary.requestedClaims.length > 0
        ? "KYC VC"
        : "-",
    result: statusToResult(summary.status),
    reason: "-",
    date: fmtDt(summary.verifiedAt ?? summary.createdAt ?? summary.requestedAt),
  };
}

function matchesSearch(
  row: ReturnType<typeof toRow>,
  search?: string
) {
  const keyword = search?.trim().toLowerCase();
  if (!keyword) return true;

  return [row.id, row.corp, row.verifier, row.purpose, row.vc]
    .join(" ")
    .toLowerCase()
    .includes(keyword);
}

// ── VP 검증 API ────────────────────────────────────────────────

/** GET /api/finance/verifier/vp-requests */
export async function getVpList(filters?: {
  search?: string;
  result?: string;
  verifierId?: string;
  from?: string;
  to?: string;
}): Promise<
  {
    id: string;
    corp: string;
    verifier: string;
    purpose: string;
    vc: string;
    result: string;
    reason: string;
    date: string;
  }[]
> {
  const response = await getFinanceVpRequestList({
    status: resultFilterToStatus(filters?.result),
    from: filters?.from,
    to: filters?.to,
  });

  return response.items.map(toRow).filter((row) => matchesSearch(row, filters?.search));
}

/** GET /api/admin/backend/vp-verifications */
export async function getAdminVpVerificationList(params?: {
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
}): Promise<AdminVpVerificationListResponse> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set("status", params.status);
  if (params?.keyword) searchParams.set("keyword", params.keyword);
  if (params?.page !== undefined) searchParams.set("page", String(params.page));
  if (params?.size !== undefined) searchParams.set("size", String(params.size));

  const url = searchParams.toString()
    ? `${ADMIN_VP_VERIFICATION_BASE}?${searchParams}`
    : ADMIN_VP_VERIFICATION_BASE;
  const response = await fetch(url, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminVpVerificationListResponse>;
  return json.data;
}

export async function getAdminVpVerificationDetailByReference(
  verificationIdOrRequestId: string | number
): Promise<AdminVpVerificationDetailResponse> {
  if (isNumericId(verificationIdOrRequestId)) {
    return getAdminVpVerificationDetail(verificationIdOrRequestId);
  }

  const requestId = String(verificationIdOrRequestId);
  const list = await getAdminVpVerificationList({
    keyword: requestId,
    page: 0,
    size: 5,
  });
  const matched = list.items.find((item) => item.vpRequestId === requestId);
  if (!matched?.vpVerificationId) {
    throw new Error("VP 검증 상세 정보를 찾을 수 없습니다.");
  }
  return getAdminVpVerificationDetail(matched.vpVerificationId);
}

/** GET /api/admin/backend/vp-verifications/{verificationId} */
export async function getVpDetail(
  verificationId: string
): Promise<VpVerificationDetail> {
  const detail = await getAdminVpVerificationDetailByReference(verificationId);
  const credentialId =
    detail.credentialId ?? detail.credential?.credentialId ?? undefined;

  return {
    verificationId: String(detail.vpVerificationId),
    corporationName: detail.corporateName ?? "-",
    verifierName:
      detail.verifier?.verifierName ?? detail.requesterName ?? "KYvC Finance",
    purpose: displayPurpose(detail.purpose),
    credentialId: credentialId != null ? String(credentialId) : undefined,
    result: statusToResult(detail.vpVerificationStatusCode),
    failReason: detail.resultSummary ?? "-",
    createdAt: detail.requestedAt ?? detail.expiresAt ?? undefined,
    requestedAt: detail.requestedAt ?? undefined,
    respondedAt: detail.verifiedAt ?? undefined,
    holderDid: detail.credential?.holderDid ?? undefined,
  };
}

/** GET /api/admin/backend/vp-verifications/{verificationId} */
export async function getAdminVpVerificationDetail(
  verificationId: string | number
): Promise<AdminVpVerificationDetailResponse> {
  const response = await fetch(
    `${ADMIN_VP_VERIFICATION_BASE}/${encodeURIComponent(String(verificationId))}`,
    {
      method: "GET",
      headers: getAuthHeaders(),
      credentials: "include",
    }
  );
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminVpVerificationDetailResponse>;
  return json.data;
}
