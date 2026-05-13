import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import { API_BASE } from "@/lib/api/api-base";
const VP_BASE = `${API_BASE}/api/admin/backend/vp-verifications`;

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

// ── 공통 유틸 ─────────────────────────────────────────────────

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

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

// ── VP 검증 API ────────────────────────────────────────────────

/** GET /api/admin/backend/vp-verifications */
export async function getVpList(filters?: {
  search?: string;
  result?: string;
  verifierId?: string;
  from?: string;
  to?: string;
}): Promise<{
  id: string;
  corp: string;
  verifier: string;
  purpose: string;
  vc: string;
  result: string;
  reason: string;
  date: string;
}[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("keyword", filters.search.trim());
  if (filters?.result && filters.result !== "전체 결과") params.set("status", filters.result);
  if (filters?.verifierId) params.set("verifierId", filters.verifierId);
  if (filters?.from) params.set("fromDate", filters.from);
  if (filters?.to) params.set("toDate", filters.to);
  const url = params.toString() ? `${VP_BASE}?${params}` : VP_BASE;

  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VpVerification[] | PageLike<VpVerification>>;

  return unwrapListData(json.data).map((v) => ({
    id: v.verificationId,
    corp: v.corporationName ?? "-",
    verifier: v.verifierName ?? "-",
    purpose: v.purpose ?? "-",
    vc: v.credentialId ?? "-",
    result: v.result ?? "-",
    reason: v.failReason ?? "-",
    date: fmtDt(v.createdAt),
  }));
}

/** GET /api/admin/backend/vp-verifications/{verificationId} */
export async function getVpDetail(verificationId: string): Promise<VpVerificationDetail> {
  const response = await fetch(`${VP_BASE}/${verificationId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VpVerificationDetail>;
  return json.data;
}
