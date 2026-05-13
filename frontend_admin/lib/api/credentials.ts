import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import { API_BASE } from "@/lib/api/api-base";
const CRED_BASE = `${API_BASE}/api/admin/backend/credentials`;

export interface KycCredential {
  credentialId: string;
  applicationId?: string;
  corporationName?: string;
  issuedAt?: string;
  expiresAt?: string;
  status?: string;
  credentialType?: string;
  issuerDid?: string;
  holderDid?: string;
  xrplTxHash?: string;
  transactionHash?: string;
  mobileStored?: boolean | string;
}

export interface KycCredentialDetail extends KycCredential {
  vcJson?: string;
  vcData?: Record<string, unknown>;
}

export interface CredentialReissueRequest {
  reason: string;
}

export interface CredentialRevokeRequest {
  reason: string;
  detail?: string;
}

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

type PageLike<T> = { content?: T[]; items?: T[]; list?: T[]; data?: T[] };

function unwrapListData<T>(data: T[] | PageLike<T> | null | undefined): T[] {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];
  const o = data as PageLike<T>;
  if (Array.isArray(o.content)) return o.content;
  if (Array.isArray(o.items)) return o.items;
  if (Array.isArray(o.list)) return o.list;
  if (Array.isArray(o.data)) return o.data;
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

/** GET /api/admin/backend/credentials */
export async function getCredentials(filters?: {
  applicationId?: string;
  status?: string;
}): Promise<KycCredential[]> {
  const params = new URLSearchParams();
  if (filters?.applicationId) params.set("keyword", filters.applicationId);
  if (filters?.status) params.set("status", filters.status);
  const url = params.toString() ? `${CRED_BASE}?${params}` : CRED_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<KycCredential[] | PageLike<KycCredential>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/credentials/{credentialId} */
export async function getCredential(credentialId: string): Promise<KycCredentialDetail> {
  const response = await fetch(`${CRED_BASE}/${encodeURIComponent(credentialId)}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<KycCredentialDetail>;
  return json.data;
}

/** POST /api/admin/backend/credentials/{credentialId}/reissue */
export async function requestCredentialReissue(
  credentialId: string,
  body: CredentialReissueRequest
): Promise<void> {
  const response = await fetch(`${CRED_BASE}/${encodeURIComponent(credentialId)}/reissue`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/credentials/{credentialId}/revoke */
export async function requestCredentialRevoke(
  credentialId: string,
  body: CredentialRevokeRequest
): Promise<void> {
  const response = await fetch(`${CRED_BASE}/${encodeURIComponent(credentialId)}/revoke`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

export interface CredentialStatusHistory {
  historyId?: string;
  status: string;
  changedAt?: string;
  changedBy?: string;
  reason?: string;
}

export interface CredentialRequest {
  requestId?: string;
  requestType?: string;
  requestedBy?: string;
  reason?: string;
  status?: string;
  createdAt?: string;
}

/** GET /api/admin/backend/credentials/{credentialId}/status-histories */
export async function getCredentialStatusHistories(
  credentialId: string
): Promise<CredentialStatusHistory[]> {
  const response = await fetch(
    `${CRED_BASE}/${encodeURIComponent(credentialId)}/status-histories`,
    { method: "GET", headers: getAuthHeaders(), credentials: "include" }
  );
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<
    CredentialStatusHistory[] | PageLike<CredentialStatusHistory>
  >;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/credentials/{credentialId}/requests */
export async function getCredentialRequests(
  credentialId: string
): Promise<CredentialRequest[]> {
  const response = await fetch(
    `${CRED_BASE}/${encodeURIComponent(credentialId)}/requests`,
    { method: "GET", headers: getAuthHeaders(), credentials: "include" }
  );
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<
    CredentialRequest[] | PageLike<CredentialRequest>
  >;
  return unwrapListData(json.data);
}
