import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const API_BASE = "https://dev-admin-api-kyvc.khuoo.synology.me";
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
  aiPolicyId: string;
  policyName: string;
  description?: string;
  modelVersion?: string;
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
  if (isPlaceholderAccessToken(token)) {
    throw new Error("유효한 인증 토큰이 없습니다. 로그인 후 다시 시도해주세요.");
  }
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
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
  if (filters?.enabled !== undefined) params.set("enabled", String(filters.enabled));
  if (filters?.corporationType) params.set("corporationType", filters.corporationType);
  const url = params.toString() ? `${POLICY_BASE}?${params}` : POLICY_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders() });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicy[] | PageLike<AiReviewPolicy>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/ai-review-policies/{aiPolicyId} */
export async function getAiReviewPolicy(aiPolicyId: string): Promise<AiReviewPolicyDetail> {
  const response = await fetch(`${POLICY_BASE}/${aiPolicyId}`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicyDetail>;
  return json.data;
}

/** PATCH /api/admin/backend/ai-review-policies/{aiPolicyId} */
export async function updateAiReviewPolicy(
  aiPolicyId: string,
  body: UpdateAiReviewPolicyBody
): Promise<AiReviewPolicyDetail> {
  const response = await fetch(`${POLICY_BASE}/${aiPolicyId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AiReviewPolicyDetail>;
  return json.data;
}

/** PATCH /api/admin/backend/ai-review-policies/{aiPolicyId}/enabled */
export async function setAiReviewPolicyEnabled(
  aiPolicyId: string,
  enabled: boolean
): Promise<void> {
  const response = await fetch(`${POLICY_BASE}/${aiPolicyId}/enabled`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    body: JSON.stringify({ enabled }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
