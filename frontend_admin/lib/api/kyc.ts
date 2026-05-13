import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import type { KycItem, KycStatus, KycChannel, DashboardStats, SupplementRequest } from "@/types/kyc";
import { API_BASE } from "@/lib/api/api-base";
const KYC_BASE = `${API_BASE}/api/admin/backend/kyc/applications`;

// ── 상태/채널 코드 매핑 ──────────────────────────────────────

const STATUS_KO_TO_API: Record<string, string> = {
  수동심사필요: "MANUAL_REVIEW",
  보완필요: "NEED_SUPPLEMENT",
  심사중: "AI_REVIEWING",
  정상: "APPROVED",
  불충족: "REJECTED",
};

const STATUS_API_TO_KO: Record<string, KycStatus> = {
  NEEDS_MANUAL_REVIEW: "수동심사필요",
  NEED_MANUAL_REVIEW: "수동심사필요",
  MANUAL_REVIEW: "수동심사필요",
  NEEDS_SUPPLEMENT: "보완필요",
  NEED_SUPPLEMENT: "보완필요",
  SUPPLEMENT_REQUESTED: "보완필요",
  REVIEWING: "심사중",
  AI_REVIEWING: "심사중",
  SUBMITTED: "심사중",
  DRAFT: "심사중",
  NORMAL: "정상",
  APPROVED: "정상",
  UNSATISFACTORY: "불충족",
  REJECTED: "불충족",
  수동심사필요: "수동심사필요",
  보완필요: "보완필요",
  심사중: "심사중",
  정상: "정상",
  불충족: "불충족",
};

const AI_JUDGMENT_KO: Record<string, string> = {
  NORMAL: "정상",
  PASS: "정상",
  QUEUED: "심사중",
  PROCESSING: "심사중",
  LOW_CONFIDENCE: "수동심사필요",
  NEEDS_SUPPLEMENT: "보완필요",
  NEED_SUPPLEMENT: "보완필요",
  UNSATISFACTORY: "불충족",
  FAIL: "불충족",
  FAILED: "불충족",
  NEEDS_MANUAL_REVIEW: "수동심사필요",
  NEED_MANUAL_REVIEW: "수동심사필요",
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
  kycId?: string | number;
  applicationId?: string | number;
  id?: string | number;
  corporateName?: string;
  corporationName?: string;
  businessRegistrationNumber?: string;
  businessRegistrationNo?: string;
  corporateTypeCode?: string;
  corporationType?: string;
  applicationDate?: string;
  submittedAt?: string;
  channel?: string;
  kycStatus?: string;
  status?: string;
  aiJudgment?: string;
  aiReviewResult?: string;
  aiReviewStatus?: string;
  aiReviewStatusCode?: string;
  aiReviewResultCode?: string;
  reviewerName?: string;
}

export interface KycDocument {
  documentName: string;
  fileName: string;
  fileSize?: string;
  uploadedAt?: string;
}

export interface BackendKycDetail {
  kycId?: string | number;
  applicationId?: string | number;
  id?: string | number;
  corporateName?: string;
  corporationName?: string;
  businessRegistrationNumber?: string;
  businessRegistrationNo?: string;
  applicationDate?: string;
  submittedAt?: string;
  channel?: string;
  kycStatus?: string;
  status?: string;
  aiJudgment?: string;
  aiReviewResult?: string;
  aiReviewStatus?: string;
  aiReviewResultCode?: string;
  aiConfidenceScore?: number;
  aiReviewSummary?: string;
  reviewerName?: string;
  documents?: KycDocument[];
  recentHistories?: Array<{
    actionDate: string;
    actionContent: string;
    actionType?: string;
  }>;
}

export interface BackendKycCorporate {
  corporateName?: string;
  corporationName?: string;
  businessRegistrationNumber?: string;
  businessRegistrationNo?: string;
  corporateRegistrationNumber?: string;
  corporateType?: string;
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

type PageLike<T> = { content?: T[]; items?: T[]; list?: T[]; documents?: T[]; mismatches?: T[]; beneficialOwners?: T[] };

function unwrapListData<T>(data: T[] | PageLike<T> | null | undefined): T[] {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];
  const o = data as PageLike<T>;
  if (Array.isArray(o.content)) return o.content;
  if (Array.isArray(o.items)) return o.items;
  if (Array.isArray(o.list)) return o.list;
  if (Array.isArray(o.documents)) return o.documents;
  if (Array.isArray(o.mismatches)) return o.mismatches;
  if (Array.isArray(o.beneficialOwners)) return o.beneficialOwners;
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

function getMfaAuthHeaders(mfaToken?: string) {
  const headers = getAuthHeaders();
  if (mfaToken) headers["X-MFA-Session-Token"] = mfaToken;
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
  if (filters?.search?.trim()) params.set("keyword", filters.search.trim());
  if (filters?.status && filters.status !== "전체 상태") {
    params.set("status", STATUS_KO_TO_API[filters.status] ?? filters.status);
  }
  const channelFilter =
    filters?.channel && filters.channel !== "전체 채널"
      ? CHANNEL_KO_TO_API[filters.channel] ?? filters.channel
      : "";
  params.set("size", "50");

  const url = params.toString() ? `${KYC_BASE}?${params}` : KYC_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));

  const json = (await response.json()) as CommonResponse<
    BackendKycItem[] | PageLike<BackendKycItem>
  >;

  const rows = unwrapListData(json.data);
  return rows.map((row) => {
    const statusCode = row.kycStatus ?? row.status ?? "";
    const aiCode = row.aiJudgment ?? row.aiReviewResult ?? row.aiReviewResultCode ?? row.aiReviewStatus ?? row.aiReviewStatusCode ?? "";
    return {
      id: String(row.kycId ?? row.applicationId ?? row.id ?? ""),
      corp: row.corporateName ?? row.corporationName ?? "-",
      biz: row.businessRegistrationNumber ?? row.businessRegistrationNo ?? "-",
      type: row.corporationType ?? row.corporateTypeCode ?? "-",
      date: fmtDt(row.applicationDate ?? row.submittedAt),
      channel: (CHANNEL_API_TO_KO[row.channel ?? ""] ?? row.channel ?? "-") as KycChannel,
      status: (STATUS_API_TO_KO[statusCode] ?? statusCode) as KycStatus,
      ai: AI_JUDGMENT_KO[aiCode] ?? aiCode ?? "-",
      reviewer: row.reviewerName ?? "-",
    };
  }).filter((row) => !channelFilter || CHANNEL_KO_TO_API[row.channel] === channelFilter || row.channel === channelFilter);
}

/** GET /api/admin/backend/kyc/applications/{kycId} — KYC 신청 상세 조회 */
export async function getKycDetail(kycId: string): Promise<BackendKycDetail> {
  const response = await fetch(`${KYC_BASE}/${kycId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
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
    credentials: "include",
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
export async function getDocumentRequirements(filters?: {
  corporationType?: string;
}): Promise<DocumentRequirement[]> {
  const params = new URLSearchParams();
  if (filters?.corporationType) params.set("corporateType", filters.corporationType);
  const url = params.toString()
    ? `${API_BASE}/api/admin/backend/document-requirements?${params}`
    : `${API_BASE}/api/admin/backend/document-requirements`;
  const response = await fetch(url, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
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
  documentTypeName?: string;
  documentName: string;
  fileName: string;
  fileSize?: string | number;
  uploadedAt?: string;
  submittedAt?: string;
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
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<KycSubmittedDocument[] | PageLike<KycSubmittedDocument>>;
  return unwrapListData(json.data).map((doc) => ({
    ...doc,
    documentId: String(doc.documentId),
    documentName: doc.documentName ?? doc.documentTypeName ?? doc.documentType ?? "-",
    uploadedAt: doc.uploadedAt ?? doc.submittedAt,
  }));
}

/** GET /api/admin/backend/kyc/applications/{kycId}/documents/{documentId}/preview */
export async function getKycDocumentPreview(
  kycId: string,
  documentId: string
): Promise<DocumentPreviewData> {
  const response = await fetch(`${KYC_BASE}/${kycId}/documents/${documentId}/preview`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
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
  data: { mfaToken: string; comment?: string }
): Promise<{ credentialId: string }> {
  const response = await fetch(`${KYC_BASE}/${kycId}/credentials/issue`, {
    method: "POST",
    headers: getMfaAuthHeaders(data.mfaToken),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<{ credentialId: string | number }>;
  return { credentialId: String(json.data.credentialId) };
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
    credentials: "include",
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
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  const json = (await response.json()) as CommonResponse<
    SupplementRequest[] | PageLike<SupplementRequest>
  >;
  return unwrapListData(json.data);
}

/** POST /api/admin/backend/kyc/applications/{kycId}/supplements/{supplementId}/complete — 보완 제출 처리 완료 */
export async function completeKycSupplement(
  kycId: string,
  supplementId: string | number,
  data: { decision: string; reason: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/supplements/${supplementId}/complete`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({
      decision: data.decision,
      reason: data.reason,
    }),
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

export async function approveKycManualReview(
  kycId: string,
  data: { mfaToken: string; comment?: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/manual-review/approve`, {
    method: "POST",
    headers: getMfaAuthHeaders(data.mfaToken),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

export async function rejectKycManualReview(
  kycId: string,
  data: { mfaToken: string; rejectReasonCode: string; comment: string }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/manual-review/reject`, {
    method: "POST",
    headers: getMfaAuthHeaders(data.mfaToken),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

// ────────────────────────────────────────────────────────────
// AI 심사 관련 타입
// ────────────────────────────────────────────────────────────

export interface AiReviewResult {
  status?: string;
  overallJudgment: string;
  confidenceScore: number;
  modelVersion?: string;
  reviewedAt?: string;
  summaryReason?: string;
  manualReviewReason?: string;
  detailJson?: string;
}

export interface AiMismatch {
  fieldName: string;
  extractedValue?: string;
  confidenceScore?: number;
  judgment: string;          // "일치" | "불일치" | "검토 필요" 또는 영문 코드
  mismatchTypeCode?: string;
  mismatchTypeName?: string;
  sourceValue?: string;
  targetValue?: string;
  matchedYn?: string;
  reason?: string;
}

export interface BeneficialOwner {
  ownerName: string;
  ownershipRatio?: number;
  shareRatio?: number;
  beneficialOwnerYn?: string;
  controlTypeCode?: string;
  judgementReason?: string;
  judgment?: string;
}

export interface AgentAuthority {
  agentName: string;
  authorityScope?: string;
  authorityType?: string;
  signatureVerifiedYn?: string;
  sealVerifiedYn?: string;
  authorityValidYn?: string;
  confidenceScore?: number;
  judgementReason?: string;
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
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<Record<string, unknown>>;
  const d = json.data ?? {};
  return {
    status: (d.status ?? d.aiReviewStatusCode) as string | undefined,
    overallJudgment: (d.result ?? d.overallJudgment ?? d.aiReviewResultCode ?? "") as string,
    confidenceScore: (d.confidence ?? d.confidenceScore ?? d.aiConfidenceScore ?? 0) as number,
    summaryReason: (d.summary ?? d.summaryReason ?? d.aiReviewSummary) as string | undefined,
    manualReviewReason: d.manualReviewReason as string | undefined,
    detailJson: (d.detailJson ?? d.aiReviewDetailJson) as string | undefined,
    modelVersion: d.modelVersion as string | undefined,
    reviewedAt: (d.reviewedAt ?? d.updatedAt) as string | undefined,
  };
}

/** GET /kyc/applications/{kycId}/ai-review/mismatches — AI 불일치 항목 조회 */
export async function getAiReviewMismatches(kycId: string): Promise<AiMismatch[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/mismatches`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiMismatch[] | PageLike<AiMismatch>>;
  return unwrapListData(json.data).map((item) => ({
    ...item,
    fieldName: item.fieldName ?? item.mismatchTypeName ?? item.mismatchTypeCode ?? "-",
    extractedValue: item.extractedValue ?? item.sourceValue ?? item.targetValue,
    judgment: item.judgment ?? (item.matchedYn === "Y" ? "MATCH" : "MISMATCH"),
  }));
}

/** GET /kyc/applications/{kycId}/ai-review/beneficial-owners — 실제소유자 조회 */
export async function getAiReviewBeneficialOwners(kycId: string): Promise<BeneficialOwner[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/beneficial-owners`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<BeneficialOwner[] | PageLike<BeneficialOwner>>;
  return unwrapListData(json.data).map((owner) => ({
    ...owner,
    shareRatio: owner.shareRatio ?? owner.ownershipRatio,
    judgment: owner.judgment ?? (owner.beneficialOwnerYn === "Y" ? "NORMAL" : "NEEDS_REVIEW"),
  }));
}

/** GET /kyc/applications/{kycId}/ai-review/agent-authority — 대리권 조회 */
export async function getAiReviewAgentAuthority(kycId: string): Promise<AgentAuthority[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/agent-authority`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<
    AgentAuthority[] | PageLike<AgentAuthority> | { agentAuthority?: AgentAuthority | null }
  >;
  const data = json.data as { agentAuthority?: AgentAuthority | null };
  const rows = data?.agentAuthority ? [data.agentAuthority] : unwrapListData(json.data as AgentAuthority[] | PageLike<AgentAuthority>);
  return rows.map((agent) => ({
    ...agent,
    authorityType: agent.authorityType ?? agent.authorityScope,
    judgment: agent.judgment ?? (agent.authorityValidYn === "Y" ? "VALID" : "NEEDS_REVIEW"),
  }));
}

/** GET /kyc/applications/{kycId}/review-histories — 심사 이력 조회 */
export async function getReviewHistories(kycId: string): Promise<ReviewHistory[]> {
  const response = await fetch(`${KYC_BASE}/${kycId}/review-histories`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<ReviewHistory[] | PageLike<ReviewHistory>>;
  return unwrapListData(json.data).map((history) => ({
    ...history,
    actionDate: history.actionDate ?? (history as unknown as { createdAt?: string }).createdAt ?? "",
    actionType: history.actionType ?? (history as unknown as { actionTypeCode?: string }).actionTypeCode ?? "-",
    actionContent: history.actionContent ?? (history as unknown as { comment?: string; reason?: string }).comment ?? (history as unknown as { reason?: string }).reason ?? "-",
  }));
}

/** POST /kyc/applications/{kycId}/ai-review/retry — AI 재심사 요청 */
export async function retryAiReview(
  kycId: string,
  data: { reason: string; documentIds?: number[] }
): Promise<void> {
  const response = await fetch(`${KYC_BASE}/${kycId}/ai-review/retry`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/kyc/applications/{kycId}/supplements — 보완요청 생성 */
export async function createKycSupplement(
  kycId: string,
  data: { supplementReason: string; requiredDocuments?: string[]; dueDate?: string }
): Promise<void> {
  const body = {
    supplementReasonCode: "MISSING_REQUIRED_DOC",
    title: "보완서류 제출 요청",
    message: data.supplementReason,
    documentTypes: data.requiredDocuments ?? [],
    dueAt: data.dueDate ? `${data.dueDate}T23:59:59` : undefined,
  };
  const response = await fetch(`${KYC_BASE}/${kycId}/supplements`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(body),
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
    credentials: "include",
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
      credentials: "include",
      body: JSON.stringify(data),
    }
  );
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
