import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import { API_BASE } from "@/lib/api/api-base";
const VERIFIER_BASE = `${API_BASE}/api/admin/backend/verifiers`;
const VERIFIER_LOGS_URL = `${API_BASE}/api/admin/backend/verifier-logs`;

const VERIFIER_STATUS_KO_TO_API: Record<string, string> = {
  활성: "APPROVED",
  심사중: "PENDING",
  비활성: "SUSPENDED",
};

// ── 타입 ──────────────────────────────────────────────────────

export interface Verifier {
  verifierId: string;
  id?: string;
  name: string;
  domain?: string;
  callbackUrl?: string;
  verifierType?: string;
  type?: string;
  credentialTypes?: string[];
  credential?: string;
  status?: string;
  regDate?: string;
  managerEmail?: string;
  approvedAt?: string;
  createdAt?: string;
}

export interface VerifierDetail extends Verifier {
  contactEmail?: string;
  description?: string;
  trustPolicyId?: string;
  callbacks?: VerifierCallback[];
  apiKeys?: VerifierApiKey[];
  usageStats?: VerifierUsageStats;
}

export interface VerifierApiKey {
  keyId: string;
  keyName?: string;
  keyPrefix?: string;
  keyStatusCode?: string;
  status?: string;
  issuedAt?: string;
  createdAt?: string;
  expiresAt?: string;
  lastUsedAt?: string;
  secret?: string;
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

function fmtDate(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 10).replaceAll("-", ".");
}

function normalizeVerifier(verifier: Verifier): Verifier {
  const status = verifier.status === "APPROVED" ? "ACTIVE" : verifier.status;
  return {
    ...verifier,
    verifierId: String(verifier.verifierId),
    id: String(verifier.verifierId),
    status,
    domain: verifier.domain ?? verifier.callbackUrl ?? "-",
    contactEmail: (verifier as VerifierDetail).contactEmail ?? verifier.managerEmail,
    verifierType: verifier.verifierType ?? verifier.type ?? "-",
    type: verifier.type ?? verifier.verifierType ?? "-",
    credential: verifier.credential ?? verifier.credentialTypes?.join(", ") ?? "-",
    regDate: verifier.regDate ?? fmtDate(verifier.createdAt),
  } as Verifier;
}

function normalizeVerifierDetail(data: VerifierDetail | { verifier?: Verifier; callbacks?: VerifierCallback[]; apiKeys?: VerifierApiKey[]; usageStats?: VerifierUsageStats }): VerifierDetail {
  if ("verifier" in data && data.verifier) {
    const callback = data.callbacks?.[0];
    return {
      ...normalizeVerifier(data.verifier),
      callbackUrl: callback?.callbackUrl,
      domain: callback?.callbackUrl ?? data.verifier.domain,
      contactEmail: data.verifier.managerEmail,
      callbacks: data.callbacks,
      apiKeys: data.apiKeys,
      usageStats: data.usageStats,
    };
  }
  return normalizeVerifier(data as VerifierDetail) as VerifierDetail;
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
  if (filters?.search?.trim()) params.set("keyword", filters.search.trim());
  if (filters?.status && filters.status !== "전체 상태") params.set("status", VERIFIER_STATUS_KO_TO_API[filters.status] ?? filters.status);
  const url = params.toString() ? `${VERIFIER_BASE}?${params}` : VERIFIER_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<Verifier[] | PageLike<Verifier>>;
  return unwrapListData(json.data).map(normalizeVerifier);
}

/** POST /api/admin/backend/verifiers */
export async function createVerifier(data: {
  name: string;
  businessNo?: string;
  callbackUrl?: string;
  managerEmail?: string;
  managerName?: string;
  domain?: string;
  contactEmail?: string;
}): Promise<VerifierDetail> {
  const response = await fetch(VERIFIER_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({
      name: data.name,
      businessNo: data.businessNo,
      callbackUrl: data.callbackUrl ?? data.domain,
      managerEmail: data.managerEmail ?? data.contactEmail,
      managerName: data.managerName,
    }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierDetail>;
  return normalizeVerifierDetail(json.data);
}

/** GET /api/admin/backend/verifiers/{verifierId} */
export async function getVerifier(verifierId: string): Promise<VerifierDetail> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierDetail | { verifier?: Verifier; callbacks?: VerifierCallback[]; apiKeys?: VerifierApiKey[]; usageStats?: VerifierUsageStats }>;
  return normalizeVerifierDetail(json.data);
}

/** PATCH /api/admin/backend/verifiers/{verifierId} */
export async function updateVerifier(
  verifierId: string,
  data: { name?: string; domain?: string; contactEmail?: string; managerEmail?: string; managerName?: string; callbackUrl?: string; description?: string }
): Promise<VerifierDetail> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({
      name: data.name,
      managerEmail: data.managerEmail ?? data.contactEmail,
      managerName: data.managerName,
      callbackUrl: data.callbackUrl ?? data.domain,
    }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierDetail>;
  return normalizeVerifierDetail(json.data);
}

/** POST /api/admin/backend/verifiers/{verifierId}/approve */
export async function approveVerifier(verifierId: string, data: { comment?: string; mfaToken: string }): Promise<void> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/approve`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/verifiers/{verifierId}/suspend */
export async function suspendVerifier(verifierId: string, data: { reasonCode?: string; comment?: string; mfaToken: string }): Promise<void> {
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
  return unwrapListData(json.data).map((key) => ({
    ...key,
    keyId: String(key.keyId),
    status: key.status ?? key.keyStatusCode,
    createdAt: key.createdAt ?? key.issuedAt,
  }));
}

/** POST /api/admin/backend/verifiers/{verifierId}/keys */
export async function createVerifierKey(
  verifierId: string,
  data: { name: string; expiresAt?: string; mfaToken: string }
): Promise<VerifierApiKey> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierApiKey>;
  return {
    ...json.data,
    keyId: String(json.data.keyId),
    status: json.data.status ?? json.data.keyStatusCode ?? "ACTIVE",
    createdAt: json.data.createdAt ?? json.data.issuedAt,
  };
}

/** POST /api/admin/backend/verifiers/{verifierId}/keys/{keyId}/rotate */
export async function rotateVerifierKey(
  verifierId: string,
  keyId: string,
  data: { mfaToken: string }
): Promise<VerifierApiKey> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys/${keyId}/rotate`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierApiKey>;
  return {
    ...json.data,
    keyId: String(json.data.keyId),
    status: json.data.status ?? json.data.keyStatusCode ?? "ACTIVE",
    createdAt: json.data.createdAt ?? json.data.issuedAt,
  };
}

/** POST /api/admin/backend/verifiers/{verifierId}/keys/{keyId}/revoke */
export async function revokeVerifierKey(
  verifierId: string,
  keyId: string,
  data: { reason?: string; mfaToken: string }
): Promise<void> {
  const response = await fetch(`${VERIFIER_BASE}/${verifierId}/keys/${keyId}/revoke`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
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
  if (filters?.result) params.set("statusCode", filters.result);
  if (filters?.from) params.set("fromDate", filters.from);
  if (filters?.to) params.set("toDate", filters.to);
  const url = params.toString() ? `${VERIFIER_LOGS_URL}?${params}` : VERIFIER_LOGS_URL;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<VerifierLog[] | PageLike<VerifierLog>>;
  return unwrapListData(json.data);
}
