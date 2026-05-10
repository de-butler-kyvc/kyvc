import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const API_BASE = "";
const VERIFIER_BASE = `${API_BASE}/api/admin/backend/verifiers`;
const VERIFIER_LOGS_URL = `${API_BASE}/api/admin/backend/verifier-logs`;

// ── 타입 ──────────────────────────────────────────────────────

export interface Verifier {
  verifierId: string;
  name: string;
  domain?: string;
  verifierType?: string;
  credentialTypes?: string[];
  status?: string;
  approvedAt?: string;
  createdAt?: string;
}

export interface VerifierDetail extends Verifier {
  contactEmail?: string;
  description?: string;
  trustPolicyId?: string;
}

export interface VerifierApiKey {
  keyId: string;
  keyPrefix?: string;
  status?: string;
  createdAt?: string;
  expiresAt?: string;
}

export interface VerifierCallback {
  callbackUrl?: string;
  callbackEvents?: string[];
  enabled?: boolean;
}

export interface VerifierUsageStats {
  totalRequests?: number;
  successCount?: number;
  failCount?: number;
  lastUsedAt?: string;
}

export interface VerifierLog {
  logId: string;
  verifierId?: string;
  verifierName?: string;
  requestType?: string;
  result?: string;
  ipAddress?: string;
  createdAt?: string;
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

// ── Verifier CRUD ─────────────────────────────────────────────

/** GET /api/admin/backend/verifiers */
export async function getVerifierList(filters?: {
  search?: string;
  status?: string;
}): Promise<Verifier[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("search", filters.search.trim());
  if (filters?.status && filters.status !== "전체 상태") params.set("status", filters.status);
  const url = params.toString() ? `${VERIFIER_BASE}?${params}` : VERIFIER_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<Verifier[] | PageLike<Verifier>>;
  return unwrapListData(json.data);
}

/** POST /api/admin/backend/verifiers */
export async function createVerifier(data: {
  name: string;
  domain?: string;
  verifierType?: string;
  credentialTypes?: string[];
  contactEmail?: string;
  description?: string;
}): Promise<VerifierDetail> {
  const response = await fetch(VERIFIER_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierDetail>;
  return json.data;
}

/** GET /api/admin/backend/verifiers/{verifierId} */
export async function getVerifier(verifierId: string): Promise<VerifierDetail> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierDetail>;
  return json.data;
}

/** PATCH /api/admin/backend/verifiers/{verifierId} */
export async function updateVerifier(
  verifierId: string,
  data: { name?: string; domain?: string; contactEmail?: string; description?: string }
): Promise<VerifierDetail> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierDetail>;
  return json.data;
}

/** POST /api/admin/backend/verifiers/{verifierId}/approve */
export async function approveVerifier(verifierId: string, data?: { comment?: string }): Promise<void> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/approve`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/verifiers/{verifierId}/suspend */
export async function suspendVerifier(verifierId: string, data?: { reason?: string }): Promise<void> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/suspend`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

// ── API Key ────────────────────────────────────────────────────

/** GET /api/admin/backend/verifiers/{verifierId}/keys */
export async function getVerifierKeys(verifierId: string): Promise<VerifierApiKey[]> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierApiKey[] | PageLike<VerifierApiKey>>;
  return unwrapListData(json.data);
}

/** POST /api/admin/backend/verifiers/{verifierId}/keys */
export async function createVerifierKey(verifierId: string, data?: { expiresAt?: string }): Promise<VerifierApiKey> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierApiKey>;
  return json.data;
}

/** POST /api/admin/backend/verifiers/{verifierId}/keys/{keyId}/rotate */
export async function rotateVerifierKey(verifierId: string, keyId: string): Promise<VerifierApiKey> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys/${keyId}/rotate`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierApiKey>;
  return json.data;
}

/** POST /api/admin/backend/verifiers/{verifierId}/keys/{keyId}/revoke */
export async function revokeVerifierKey(verifierId: string, keyId: string): Promise<void> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys/${keyId}/revoke`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

// ── Callback ──────────────────────────────────────────────────

/** GET /api/admin/backend/verifiers/{verifierId}/callbacks */
export async function getVerifierCallbacks(verifierId: string): Promise<VerifierCallback> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/callbacks`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierCallback>;
  return json.data;
}

/** PATCH /api/admin/backend/verifiers/{verifierId}/callbacks */
export async function updateVerifierCallbacks(
  verifierId: string,
  data: VerifierCallback
): Promise<VerifierCallback> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/callbacks`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierCallback>;
  return json.data;
}

/** GET /api/admin/backend/verifiers/{verifierId}/usage-stats */
export async function getVerifierUsageStats(verifierId: string): Promise<VerifierUsageStats> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/usage-stats`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierUsageStats>;
  return json.data;
}

// ── Verifier Logs ─────────────────────────────────────────────

/** GET /api/admin/backend/verifier-logs */
export async function getVerifierLogs(filters?: {
  verifierId?: string;
  result?: string;
  from?: string;
  to?: string;
}): Promise<VerifierLog[]> {
  const params = new URLSearchParams();
  if (filters?.verifierId) params.set("verifierId", filters.verifierId);
  if (filters?.result) params.set("result", filters.result);
  if (filters?.from) params.set("from", filters.from);
  if (filters?.to) params.set("to", filters.to);
  const url = params.toString() ? `${VERIFIER_LOGS_URL}?${params}` : VERIFIER_LOGS_URL;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierLog[] | PageLike<VerifierLog>>;
  return unwrapListData(json.data);
}
