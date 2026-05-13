import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import { API_BASE } from "@/lib/api/api-base";
const POLICY_BASE = `${API_BASE}/api/admin/backend/ai-review-policies`;

// ────────────────────────────────────────────────────────────
// 타입 정의
// ────────────────────────────────────────────────────────────

export interface AiReviewPolicyRule {
  ruleId?: string;
  fieldName?: string;
  condition?: string;
  threshold?: number;
  action?: string;
  description?: string;
}

export interface AiReviewPolicy {
  aiPolicyId: string | number;
  policyName: string;
  description?: string;
  modelVersion?: string;
  corporateTypeCode?: string;
  autoApproveYn?: string;
  autoApproveThreshold?: number;
  supplementThreshold?: number;
  enabledYn?: string;
  autoApprovalThreshold?: number;
  autoRejectionThreshold?: number;
  manualReviewThreshold?: number;
  corporationType?: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AiReviewPolicyDetail extends AiReviewPolicy {
  rules?: AiReviewPolicyRule[];
  appliedCount?: number;
}

export interface UpdateAiReviewPolicyBody {
  policyName?: string;
  description?: string;
  modelVersion?: string;
  corporateTypeCode?: string;
  autoApproveYn?: string;
  autoApproveThreshold?: number;
  supplementThreshold?: number;
  autoApprovalThreshold?: number;
  autoRejectionThreshold?: number;
  manualReviewThreshold?: number;
  corporationType?: string;
  rules?: AiReviewPolicyRule[];
}

// ────────────────────────────────────────────────────────────
// 공통 유틸
// ────────────────────────────────────────────────────────────

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
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

function getAuthHeaders() {
  const token = getAccessTokenForApi();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (!isPlaceholderAccessToken(token)) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function normalizePolicy(policy: AiReviewPolicy): AiReviewPolicy {
  const autoApproveThreshold = policy.autoApproveThreshold ?? policy.autoApprovalThreshold;
  const supplementThreshold = policy.supplementThreshold ?? policy.autoRejectionThreshold;

  return {
    ...policy,
    autoApproveThreshold,
    supplementThreshold,
    autoApprovalThreshold: autoApproveThreshold,
    autoRejectionThreshold: supplementThreshold,
    corporationType: policy.corporationType ?? policy.corporateTypeCode,
    enabled: policy.enabled ?? policy.enabledYn === "Y",
  };
}

function toApiPolicyBody(body: UpdateAiReviewPolicyBody): Record<string, unknown> {
  return {
    policyName: body.policyName,
    description: body.description,
    modelVersion: body.modelVersion,
    corporateTypeCode: body.corporateTypeCode ?? body.corporationType,
    autoApproveYn: body.autoApproveYn,
    autoApproveThreshold: body.autoApproveThreshold ?? body.autoApprovalThreshold,
    manualReviewThreshold: body.manualReviewThreshold,
    supplementThreshold: body.supplementThreshold ?? body.autoRejectionThreshold,
    rules: body.rules,
  };
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

// ────────────────────────────────────────────────────────────
// AI 심사 정책 API
// ────────────────────────────────────────────────────────────

/** GET /api/admin/backend/ai-review-policies */
export async function getAiReviewPolicies(filters?: {
  enabled?: boolean;
  corporationType?: string;
}): Promise<AiReviewPolicy[]> {
  const params = new URLSearchParams();
  if (filters?.enabled !== undefined) params.set("enabledYn", filters.enabled ? "Y" : "N");
  if (filters?.corporationType) params.set("corporateType", filters.corporationType);
  const url = params.toString() ? `${POLICY_BASE}?${params}` : POLICY_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicy[] | PageLike<AiReviewPolicy>>;
  return unwrapListData(json.data).map(normalizePolicy);
}

/** GET /api/admin/backend/ai-review-policies/{aiPolicyId} */
export async function getAiReviewPolicy(aiPolicyId: string | number): Promise<AiReviewPolicyDetail> {
  const response = await fetch(`${POLICY_BASE}/${aiPolicyId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicyDetail>;
  return normalizePolicy(json.data) as AiReviewPolicyDetail;
}

/** PATCH /api/admin/backend/ai-review-policies/{aiPolicyId} */
export async function updateAiReviewPolicy(
  aiPolicyId: string | number,
  body: UpdateAiReviewPolicyBody
): Promise<AiReviewPolicyDetail> {
  const response = await fetch(`${POLICY_BASE}/${aiPolicyId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(toApiPolicyBody(body)),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicyDetail>;
  return normalizePolicy(json.data) as AiReviewPolicyDetail;
}

/** PATCH /api/admin/backend/ai-review-policies/{aiPolicyId}/enabled */
export async function setAiReviewPolicyEnabled(
  aiPolicyId: string | number,
  enabled: boolean
): Promise<void> {
  const response = await fetch(`${POLICY_BASE}/${aiPolicyId}/enabled`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({ enabledYn: enabled ? "Y" : "N" }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/ai-review-policies — AI 심사 정책 등록 */
export async function createAiReviewPolicy(
  body: UpdateAiReviewPolicyBody & { policyName: string }
): Promise<AiReviewPolicyDetail> {
  const response = await fetch(POLICY_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(toApiPolicyBody(body)),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicyDetail>;
  return normalizePolicy(json.data) as AiReviewPolicyDetail;
}
