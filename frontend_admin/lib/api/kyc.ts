import type { KycItem, KycStatus, KycChannel, DashboardStats, SupplementRequest } from "@/types/kyc";
import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const API_BASE = "";
const KYC_BASE = `${API_BASE}/api/admin/backend/kyc/applications`;

// ── 상태/채널 코드 매핑 ──────────────────────────────────────

const STATUS_KO_TO_API: Record<string, string> = {
  수동심사필요: "NEEDS_MANUAL_REVIEW",
  보완필요: "NEEDS_SUPPLEMENT",
  심사중: "REVIEWING",
  정상: "NORMAL",
  불충족: "UNSATISFACTORY",
};

const STATUS_API_TO_KO: Record<string, KycStatus> = {
  NEEDS_MANUAL_REVIEW: "수동심사필요",
  NEEDS_SUPPLEMENT: "보완필요",
  REVIEWING: "심사중",
  NORMAL: "정상",
  UNSATISFACTORY: "불충족",
  수동심사필요: "수동심사필요",
  보완필요: "보완필요",
  심사중: "심사중",
  정상: "정상",
  불충족: "불충족",
};

const AI_JUDGMENT_KO: Record<string, string> = {
  NORMAL: "정상",
  NEEDS_SUPPLEMENT: "보완필요",
  UNSATISFACTORY: "불충족",
  NEEDS_MANUAL_REVIEW: "수동심사필요",
};

const CHANNEL_KO_TO_API: Record<string, string> = {
  웹: "WEB",
  금융사: "FINANCIAL",
};

const CHANNEL_API_TO_KO: Record<string, KycChannel> = {
  WEB: "웹",
  FINANCIAL: "금융사",
  웹: "웹",
  금융사: "금융사",
};

// ── KYC 신청 관련 API 타입 ────────────────────────────────────

interface BackendKycItem {
  applicationId: string;
  corporationName?: string;
  businessRegistrationNumber?: string;
  corporationType?: string;
  applicationDate?: string;
  channel?: string;
  status: string;
  aiJudgment?: string;
  reviewerName?: string;
}

export interface KycDocument {
  documentName: string;
  fileName: string;
  fileSize?: string;
  uploadedAt?: string;
}

export interface BackendKycDetail {
  applicationId: string;
  corporationName?: string;
  businessRegistrationNumber?: string;
  applicationDate?: string;
  channel?: string;
  status: string;
  aiJudgment?: string;
  reviewerName?: string;
  documents?: KycDocument[];
  recentHistories?: Array<{
    actionDate: string;
    actionContent: string;
    actionType?: string;
  }>;
}

export interface BackendKycCorporate {
  corporationName?: string;
  businessRegistrationNumber?: string;
  corporateRegistrationNumber?: string;
  corporationType?: string;
  representativeName?: string;
  establishedDate?: string;
  address?: string;
  businessType?: string;
}

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

function fmtDate(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 10).replaceAll("-", ".");
}

type PageLike<T> = { content?: T[]; items?: T[]; list?: T[] };

function unwrapListData<T>(data: T[] | PageLike<T> | null | undefined): T[] {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];
  const o = data as PageLike<T>;
  if (Array.isArray(o.content)) return o.content;
  if (Array.isArray(o.items)) return o.items;
  if (Array.isArray(o.list)) return o.list;
  return [];
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

/** GET /api/admin/backend/kyc/applications — KYC 신청 목록 조회 */
export async function getKycList(filters?: {
  search?: string;
  status?: string;
  channel?: string;
}): Promise<KycItem[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("search", filters.search.trim());
  if (filters?.status && filters.status !== "전체 상태") {
    params.set("status", STATUS_KO_TO_API[filters.status] ?? filters.status);
  }
  if (filters?.channel && filters.channel !== "전체 채널") {
    params.set("channel", CHANNEL_KO_TO_API[filters.channel] ?? filters.channel);
  }

  const url = params.toString() ? `${KYC_BASE}?${params}` : KYC_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders() });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));

  const json = (await response.json()) as CommonResponse<
    BackendKycItem[] | PageLike<BackendKycItem>
  >;

  return unwrapListData(json.data).map((row) => ({
    id: row.applicationId,
    corp: row.corporationName ?? "-",
    biz: row.businessRegistrationNumber ?? "-",
    type: row.corporationType ?? "-",
    date: fmtDt(row.applicationDate),
    channel: (CHANNEL_API_TO_KO[row.channel ?? ""] ?? row.channel ?? "-") as KycChannel,
    status: (STATUS_API_TO_KO[row.status] ?? row.status) as KycStatus,
    ai: AI_JUDGMENT_KO[row.aiJudgment ?? ""] ?? row.aiJudgment ?? "-",
    reviewer: row.reviewerName ?? "-",
  }));
}

/** GET /api/admin/backend/kyc/applications/{kycId} — KYC 신청 상세 조회 */
export async function getKycDetail(kycId: string): Promise<BackendKycDetail> {
  const response = await fetch(`${KYC_BASE}/${kycId}`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<BackendKycDetail>;
  return json.data;
}

/** GET /api/admin/backend/kyc/applications/{kycId}/corporate — KYC 법인 정보 조회 */
export async function getKycCorporate(kycId: string): Promise<BackendKycCorporate> {
  const response = await fetch(`${KYC_BASE}/${kycId}/corporate`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<BackendKycCorporate>;
  return json.data;
}

// ────────────────────────────────────────────────────────────
// 필수서류 정책 API
// ────────────────────────────────────────────────────────────

export interface DocumentRequirement {
  requirementId?: string;
  documentType: string;
  documentName: string;
  required: boolean;
  description?: string;
  corporationType?: string;
}

/** GET /api/admin/backend/document-requirements */
export async function getDocumentRequirements(): Promise<DocumentRequirement[]> {
  const response = await fetch(`${API_BASE}/api/admin/backend/document-requirements`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<DocumentRequirement[] | PageLike<DocumentRequirement>>;
  return unwrapListData(json.data);
}

// ────────────────────────────────────────────────────────────
// KYC 제출 문서 API
// ────────────────────────────────────────────────────────────

export interface KycSubmittedDocument {
  documentId: string;
  documentType?: string;
  documentName: string;
  fileName: string;
  fileSize?: string;
  uploadedAt?: string;
  status?: string;
  hashVerified?: boolean;
}

export interface DocumentPreviewData {
  previewUrl?: string;
  contentType?: string;
}

/** GET /api/admin/backend/kyc/applications/{kycId}/documents */
export async function getKycDocuments(kycId: string): Promise<KycSubmittedDocument[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/documents`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<KycSubmittedDocument[] | PageLike<KycSubmittedDocument>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/kyc/applications/{kycId}/documents/{documentId}/preview */
export async function getKycDocumentPreview(
  kycId: string,
  documentId: string
): Promise<DocumentPreviewData> {
  const response = await fetch(`${KYC_BASE}/${kycId}/documents/${documentId}/preview`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<DocumentPreviewData>;
  return json.data;
}

// ────────────────────────────────────────────────────────────
// VC 발급 API
// ────────────────────────────────────────────────────────────

/** POST /api/admin/backend/kyc/applications/{kycId}/credentials/issue */
export async function issueKycCredential(
  kycId: string,
  data?: { comment?: string }
): Promise<{ credentialId: string }> {
  const response = await fetch(`${KYC_BASE}/${kycId}/credentials/issue`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<{ credentialId: string }>;
  return json.data;
}

export { fmtDt, fmtDate };

interface BackendDashboardData {
  todayKyc?: number;
  todayKycCount?: number;
  pendingManual?: number;
  pendingManualReviewCount?: number;
  pendingSupplement?: number;
  pendingSupplementCount?: number;
  vcIssued?: number;
  vcIssuedCount?: number;
  vpCount?: number;
  vpVerificationCount?: number;
}

/** GET /api/admin/backend/dashboard — 대시보드 집계 조회 */
export async function getDashboardStats(): Promise<DashboardStats> {
  const response = await fetch(`${API_BASE}/api/admin/backend/dashboard`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<BackendDashboardData>;
  const d = json.data ?? {};
  return {
    todayKyc: d.todayKyc ?? d.todayKycCount ?? 0,
    pendingManual: d.pendingManual ?? d.pendingManualReviewCount ?? 0,
    pendingSupplement: d.pendingSupplement ?? d.pendingSupplementCount ?? 0,
    vcIssued: d.vcIssued ?? d.vcIssuedCount ?? 0,
    vpCount: d.vpCount ?? d.vpVerificationCount ?? 0,
  };
}

export async function getKycSupplements(kycId: string): Promise<SupplementRequest[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/supplements`, {
    method: "GET",
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  const json = (await response.json()) as CommonResponse<
    SupplementRequest[] | PageLike<SupplementRequest>
  >;
  return unwrapListData(json.data);
}

export async function approveKycManualReview(
  kycId: string,
  data: { reviewComment: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/manual-review/approve`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }
}

export async function rejectKycManualReview(
  kycId: string,
  data: { rejectReason: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/manual-review/reject`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }
}

// ────────────────────────────────────────────────────────────
// AI 심사 관련 타입
// ────────────────────────────────────────────────────────────

export interface AiReviewResult {
  reviewId?: string | number;
  overallJudgment: string;   // "정상" | "보완필요" | "불충족" | "수동심사필요" 또는 영문 코드
  confidenceScore: number;   // 0~100
  modelVersion?: string;
  reviewedAt?: string;
  summaryReason?: string;
}

export interface AiMismatch {
  fieldName: string;
  extractedValue?: string;
  confidenceScore?: number;
  judgment: string;          // "일치" | "불일치" | "검토 필요" 또는 영문 코드
}

export interface BeneficialOwner {
  ownerName: string;
  shareRatio?: number;
  judgment?: string;
}

export interface AgentAuthority {
  agentName: string;
  authorityType?: string;
  validFrom?: string;
  validTo?: string;
  judgment?: string;
}

export interface ReviewHistory {
  historyId?: string | number;
  actionDate: string;
  actionType: string;        // "AI" | "수동" | "시스템" 또는 영문 코드
  actionContent: string;
  actorName?: string;
  detail?: string;
}

// ────────────────────────────────────────────────────────────
// AI 심사 API 함수
// ────────────────────────────────────────────────────────────

/** GET /kyc/applications/{kycId}/ai-review — AI 심사 결과 조회 */
export async function getAiReview(kycId: string): Promise<AiReviewResult> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewResult>;
  return json.data;
}

/** GET /kyc/applications/{kycId}/ai-review/mismatches — AI 불일치 항목 조회 */
export async function getAiReviewMismatches(kycId: string): Promise<AiMismatch[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/mismatches`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiMismatch[] | PageLike<AiMismatch>>;
  return unwrapListData(json.data);
}

/** GET /kyc/applications/{kycId}/ai-review/beneficial-owners — 실제소유자 조회 */
export async function getAiReviewBeneficialOwners(kycId: string): Promise<BeneficialOwner[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/beneficial-owners`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<BeneficialOwner[] | PageLike<BeneficialOwner>>;
  return unwrapListData(json.data);
}

/** GET /kyc/applications/{kycId}/ai-review/agent-authority — 대리권 조회 */
export async function getAiReviewAgentAuthority(kycId: string): Promise<AgentAuthority[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/agent-authority`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AgentAuthority[] | PageLike<AgentAuthority>>;
  return unwrapListData(json.data);
}

/** GET /kyc/applications/{kycId}/review-histories — 심사 이력 조회 */
export async function getReviewHistories(kycId: string): Promise<ReviewHistory[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/review-histories`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<ReviewHistory[] | PageLike<ReviewHistory>>;
  return unwrapListData(json.data);
}

/** POST /kyc/applications/{kycId}/ai-review/retry — AI 재심사 요청 */
export async function retryAiReview(
  kycId: string,
  data: { reason: string; priority?: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/retry`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/kyc/applications/{kycId}/supplements — 보완요청 생성 */
export async function createKycSupplement(
  kycId: string,
  data: { supplementReason: string; requiredDocuments?: string[]; dueDate?: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/supplements`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/document-requirements — 필수서류 정책 등록 */
export async function createDocumentRequirement(data: {
  documentType: string;
  corporationType?: string;
  required: boolean;
  description?: string;
}): Promise<void> {
  const response = await fetch(`${API_BASE}/api/admin/backend/document-requirements`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** PATCH /api/admin/backend/document-requirements/{requirementId} — 필수서류 정책 수정 */
export async function updateDocumentRequirement(
  requirementId: string,
  data: { required?: boolean; description?: string }
): Promise<void> {
  const response = await fetch(
    `${API_BASE}/api/admin/backend/document-requirements/${requirementId}`,
    {
      method: "PATCH",
      headers: getAuthHeaders(),
      body: JSON.stringify(data),
    }
  );
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
