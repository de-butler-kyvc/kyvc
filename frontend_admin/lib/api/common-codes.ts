import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const API_BASE = "";
const CODE_BASE = `${API_BASE}/api/admin/backend/common-codes`;
const GROUP_BASE = `${API_BASE}/api/admin/backend/common-code-groups`;

// ────────────────────────────────────────────────────────────
// 타입 정의
// ────────────────────────────────────────────────────────────

export interface CommonCode {
  codeId: string;
  codeGroupId?: string;
  codeName: string;
  codeValue: string;
  description?: string;
  sortOrder?: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CommonCodeGroup {
  codeGroupId: string;
  groupName: string;
  description?: string;
  codeCount?: number;
  enabled?: boolean;
  createdAt?: string;
}

export interface CommonCodeGroupDetail extends CommonCodeGroup {
  codes?: CommonCode[];
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
// 공통코드 API
// ────────────────────────────────────────────────────────────

/** GET /api/admin/backend/common-codes */
export async function getCommonCodes(filters?: {
  codeGroupId?: string;
  enabled?: boolean;
}): Promise<CommonCode[]> {
  const params = new URLSearchParams();
  if (filters?.codeGroupId) params.set("codeGroup", filters.codeGroupId);
  if (filters?.enabled !== undefined) params.set("enabledYn", filters.enabled ? "Y" : "N");
  const url = params.toString() ? `${CODE_BASE}?${params}` : CODE_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CommonCode[] | PageLike<CommonCode>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/common-codes/{codeId} */
export async function getCommonCode(codeId: string): Promise<CommonCode> {
  const response = await fetch(`${CODE_BASE}/${codeId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CommonCode>;
  return json.data;
}

/** POST /api/admin/backend/common-codes/{codeId}/enable */
export async function enableCommonCode(codeId: string): Promise<void> {
  const response = await fetch(`${CODE_BASE}/${codeId}/enable`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/common-codes/{codeId}/disable */
export async function disableCommonCode(codeId: string): Promise<void> {
  const response = await fetch(`${CODE_BASE}/${codeId}/disable`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/common-codes — 공통코드 등록 */
export async function createCommonCode(data: {
  codeGroupId: string;
  codeName: string;
  codeValue: string;
  description?: string;
  sortOrder?: number;
}): Promise<CommonCode> {
  const response = await fetch(CODE_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CommonCode>;
  return json.data;
}

/** PATCH /api/admin/backend/common-codes/{codeId} — 공통코드 수정 */
export async function updateCommonCode(
  codeId: string,
  data: { codeName?: string; codeValue?: string; description?: string; sortOrder?: number }
): Promise<CommonCode> {
  const response = await fetch(`${CODE_BASE}/${codeId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CommonCode>;
  return json.data;
}

/** DELETE /api/admin/backend/common-codes/{codeId} — 공통코드 삭제 */
export async function deleteCommonCode(codeId: string): Promise<void> {
  const response = await fetch(`${CODE_BASE}/${codeId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

// ────────────────────────────────────────────────────────────
// 공통코드 그룹 API
// ────────────────────────────────────────────────────────────

/** GET /api/admin/backend/common-code-groups */
export async function getCommonCodeGroups(filters?: {
  enabled?: boolean;
}): Promise<CommonCodeGroup[]> {
  const params = new URLSearchParams();
  if (filters?.enabled !== undefined) params.set("enabledYn", filters.enabled ? "Y" : "N");
  const url = params.toString() ? `${GROUP_BASE}?${params}` : GROUP_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CommonCodeGroup[] | PageLike<CommonCodeGroup>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/common-code-groups/{codeGroupId} */
export async function getCommonCodeGroup(codeGroupId: string): Promise<CommonCodeGroupDetail> {
  const response = await fetch(`${GROUP_BASE}/${codeGroupId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CommonCodeGroupDetail>;
  return json.data;
}
