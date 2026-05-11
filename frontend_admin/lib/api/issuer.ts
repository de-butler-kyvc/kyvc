import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import type { IssuerItem } from "@/types/kyc";
const API_BASE = "";
const ISSUER_POLICIES_URL = `${API_BASE}/api/admin/backend/issuer-policies`;

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

function formatPolicyPeriod(createdAt?: string, updatedAt?: string): string {
  if (!createdAt) return "-";
  const c = createdAt.slice(0, 10).replaceAll("-", ".");
  if (!updatedAt) return `${c}~`;
  return `${c}~${updatedAt.slice(0, 10).replaceAll("-", ".")}`;
}

function mapPolicyTypeToKo(policyType: string): IssuerItem["type"] {
  const u = String(policyType).toUpperCase();
  return u === "BLACKLIST" ? "블랙리스트" : "화이트리스트";
}

function mapApiStatusToKo(status: string): IssuerItem["status"] {
  const u = String(status).toUpperCase();
  if (status === "활성" || u === "ACTIVE") return "활성";
  if (status === "차단" || u === "BLOCKED" || u === "BANNED") return "차단";
  return "심사중";
}

function apiRowToIssuerItem(row: IssuerPolicyDetail): IssuerItem {
  const credential =
    row.credentialTypes?.length ? row.credentialTypes.join(", ") : "-";

  return {
    id: String(row.policyId),
    did: row.issuerDid,
    type: mapPolicyTypeToKo(row.policyType),
    credential,
    scope: "-",
    period: formatPolicyPeriod(row.createdAt, row.updatedAt),
    status: mapApiStatusToKo(row.status),
  };
}

export async function getIssuerList(filters?: { search?: string; type?: string; status?: string }): Promise<IssuerItem[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("keyword", filters.search.trim());
  if (filters?.type && filters.type !== "전체 정책 유형") {
    params.set("policyType", filters.type === "블랙리스트" ? "BLACKLIST" : "WHITELIST");
  }
  if (filters?.status && filters.status !== "전체 상태") {
    params.set("status", filters.status);
  }

  const query = params.toString();
  const url = query ? `${ISSUER_POLICIES_URL}?${query}` : ISSUER_POLICIES_URL;

  const response = await fetch(url, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  const json = (await response.json()) as CommonResponse<
    IssuerPolicyDetail[] | PageLike<IssuerPolicyDetail>
  >;

  let rows = unwrapListData(json.data);

  rows = rows.filter((row) => {
    const item = apiRowToIssuerItem(row);

    if (filters?.search?.trim()) {
      const q = filters.search.trim().toLowerCase();
      if (
        !item.id.toLowerCase().includes(q) &&
        !item.did.toLowerCase().includes(q)
      ) {
        return false;
      }
    }
    if (filters?.type && filters.type !== "전체 정책 유형" && item.type !== filters.type) {
      return false;
    }
    if (filters?.status && filters.status !== "전체 상태" && item.status !== filters.status) {
      return false;
    }
    return true;
  });

  return rows.map(apiRowToIssuerItem);
}

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface IssuerPolicyDetail {
  policyId: number;
  issuerDid: string;
  issuerName: string;
  policyType: "WHITELIST" | "BLACKLIST";
  credentialTypes?: string[];
  status: string;
  reason?: string;
  createdAt?: string;
  updatedAt?: string;
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
    if (!text.trim()) {
      return `API Error: ${response.status} ${response.statusText}`;
    }
    const parsed = JSON.parse(text) as { message?: string; error?: string };
    if (parsed.message) return parsed.message;
    if (typeof parsed.error === "string") return parsed.error;
    return text;
  } catch {
    return `API Error: ${response.status} ${response.statusText}`;
  }
}

export async function createIssuerWhitelist(data: {
  issuerDid: string;
  issuerName: string;
  credentialTypes?: string[];
  reason?: string;
  mfaToken: string;
}) {
  const response = await fetch(`${ISSUER_POLICIES_URL}/whitelist`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  return response.json() as Promise<CommonResponse<unknown>>;
}

export async function createIssuerBlacklist(data: {
  issuerDid: string;
  issuerName: string;
  reasonCode: string;
  reason: string;
  mfaToken: string;
}) {
  const response = await fetch(`${ISSUER_POLICIES_URL}/blacklist`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  return response.json() as Promise<CommonResponse<unknown>>;
}

export async function getIssuerPolicy(policyId: number): Promise<IssuerPolicyDetail> {
  const response = await fetch(`${ISSUER_POLICIES_URL}/${policyId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  const json = (await response.json()) as CommonResponse<IssuerPolicyDetail>;
  return json.data;
}

export async function updateIssuerPolicy(policyId: number, data: {
  issuerName?: string;
  credentialTypes?: string[];
  status?: string;
  reason?: string;
  mfaToken: string;
}) {
  const response = await fetch(`${ISSUER_POLICIES_URL}/${policyId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  const json = (await response.json()) as CommonResponse<IssuerPolicyDetail>;
  return json.data;
}

export async function disableIssuerPolicy(policyId: number) {
  const response = await fetch(`${ISSUER_POLICIES_URL}/${policyId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(await errorMessageFromResponse(response));
  }

  const json = (await response.json()) as CommonResponse<IssuerPolicyDetail>;
  return json.data;
}

/** POST /api/admin/backend/issuer-policies/{policyId}/submit-approval — 승인요청 */
export async function submitIssuerPolicyApproval(policyId: number): Promise<void> {
  const response = await fetch(`${ISSUER_POLICIES_URL}/${policyId}/submit-approval`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/issuer-policies/{policyId}/approve — 정책 승인 */
export async function approveIssuerPolicy(
  policyId: number,
  data?: { comment?: string }
): Promise<void> {
  const response = await fetch(`${ISSUER_POLICIES_URL}/${policyId}/approve`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/issuer-policies/{policyId}/reject — 정책 반려 */
export async function rejectIssuerPolicy(
  policyId: number,
  data: { rejectReason: string }
): Promise<void> {
  const response = await fetch(`${ISSUER_POLICIES_URL}/${policyId}/reject`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
