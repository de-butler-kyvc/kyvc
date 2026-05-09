import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const API_BASE = "https://dev-admin-api-kyvc.khuoo.synology.me";
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
}

export interface KycCredentialDetail extends KycCredential {
  vcJson?: string;
  vcData?: Record<string, unknown>;
}

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

/** GET /api/admin/backend/credentials */
export async function getCredentials(filters?: {
  applicationId?: string;
  status?: string;
}): Promise<KycCredential[]> {
  const params = new URLSearchParams();
  if (filters?.applicationId) params.set("applicationId", filters.applicationId);
  if (filters?.status) params.set("status", filters.status);
  const url = params.toString() ? `${CRED_BASE}?${params}` : CRED_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders() });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<KycCredential[] | PageLike<KycCredential>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/credentials/{credentialId} */
export async function getCredential(credentialId: string): Promise<KycCredentialDetail> {
  const response = await fetch(`${CRED_BASE}/${credentialId}`, {
    method: "GET",
    headers: getAuthHeaders(),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<KycCredentialDetail>;
  return json.data;
}
