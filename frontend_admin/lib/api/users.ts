import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import type { UserItem } from "@/types/kyc";
import { API_BASE } from "@/lib/api/api-base";
const USERS_URL = `${API_BASE}/api/admin/backend/users`;

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

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

// API 응답 타입
export interface BackendUserItem {
  userId: string;
  name: string;
  role?: string;
  status: string;
  lastLoginAt?: string;
  createdAt?: string;
}

export interface BackendUserDetail {
  userId: string;
  name: string;
  email?: string;
  phoneNumber?: string;
  role?: string;
  status: string;
  lastLoginAt?: string;
  createdAt?: string;
  mfaEnabled?: boolean;
  corporation?: {
    corporationName?: string;
    businessRegistrationNumber?: string;
    corporateRegistrationNumber?: string;
    corporationType?: string;
    representativeName?: string;
    address?: string;
  };
  kycApplications?: Array<{
    applicationId: string;
    applicationDate?: string;
    status: string;
    aiScore?: number | string;
  }>;
  verifiableCredentials?: Array<{
    vcId: string;
    vcType?: string;
    issuedAt?: string;
    expiresAt?: string;
    status: string;
  }>;
  agents?: Array<{
    agentName: string;
    agentRole?: string;
    authorizedScope?: string;
    delegationExpiresAt?: string;
  }>;
}

export interface CorporateUserCreateRequest {
  loginId?: string;
  userId?: string;
  name: string;
  email: string;
  corporateName?: string;
  organization?: string;
}

const STATUS_KO_TO_API: Record<string, string> = {
  정상: "ACTIVE",
  잠금: "LOCKED",
  비활성: "INACTIVE",
};

const STATUS_API_TO_KO: Record<string, UserItem["status"]> = {
  ACTIVE: "정상",
  LOCKED: "잠금",
  INACTIVE: "비활성",
  정상: "정상",
  잠금: "잠금",
  비활성: "비활성",
};

function formatDate(iso?: string): string {
  if (!iso) return "-";
  return iso.slice(0, 10).replaceAll("-", ".");
}

function formatDateTime(iso?: string): string {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
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

/** GET /api/admin/backend/users — 법인 사용자 목록 조회 */
export async function getUserList(filters?: {
  search?: string;
  role?: string;
  status?: string;
}): Promise<UserItem[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("keyword", filters.search.trim());
  if (filters?.status && filters.status !== "전체 상태") {
    params.set("status", STATUS_KO_TO_API[filters.status] ?? filters.status);
  }

  const url = params.toString() ? `${USERS_URL}?${params}` : USERS_URL;
  const response = await fetch(url, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));

  const json = (await response.json()) as CommonResponse<
    BackendUserItem[] | PageLike<BackendUserItem>
  >;

  return unwrapListData(json.data).map((row) => ({
    id: row.userId,
    name: row.name,
    role: row.role ?? "법인 사용자",
    status: STATUS_API_TO_KO[row.status] ?? "정상",
    lastLogin: formatDateTime(row.lastLoginAt),
    regDate: formatDate(row.createdAt),
  }));
}

/** GET /api/admin/backend/users/{userId} — 법인 사용자 상세 조회 */
export async function getUserDetail(userId: string): Promise<BackendUserDetail> {
  const response = await fetch(`${USERS_URL}/${userId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));

  const json = (await response.json()) as CommonResponse<BackendUserDetail>;
  return json.data;
}

/** POST /api/admin/backend/users — 법인 사용자 계정 등록 */
export async function createUser(data: CorporateUserCreateRequest): Promise<BackendUserDetail> {
  const response = await fetch(USERS_URL, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));

  const json = (await response.json()) as CommonResponse<BackendUserDetail>;
  return json.data;
}

/** PATCH /api/admin/backend/users/{userId} — 법인 사용자 정보 수정 */
export async function updateUser(
  userId: string,
  data: { name?: string; email?: string; phoneNumber?: string }
): Promise<void> {
  const response = await fetch(`${USERS_URL}/${userId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** DELETE /api/admin/backend/users/{userId} — 법인 사용자 삭제 */
export async function deleteUser(userId: string): Promise<void> {
  const response = await fetch(`${USERS_URL}/${userId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
    credentials: "include",
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** PATCH /api/admin/backend/users/{userId}/status — 법인 사용자 상태 변경 */
export async function updateUserStatus(
  userId: string,
  statusKo: "정상" | "잠금" | "비활성"
): Promise<void> {
  const response = await fetch(`${USERS_URL}/${userId}/status`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({ status: STATUS_KO_TO_API[statusKo] }),
  });

  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

export interface CorporateDetail {
  corporateId: string;
  corporationName?: string;
  businessRegistrationNumber?: string;
  corporateRegistrationNumber?: string;
  corporationType?: string;
  representativeName?: string;
  address?: string;
  createdAt?: string;
}

/** GET /api/admin/backend/corporates/{corporateId} — 법인 상세 조회 */
export async function getCorporate(corporateId: string): Promise<CorporateDetail> {
  const response = await fetch(
    `${API_BASE}/api/admin/backend/corporates/${corporateId}`,
    { method: "GET", headers: getAuthHeaders(), credentials: "include" }
  );
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<CorporateDetail>;
  return json.data;
}

export { formatDate, formatDateTime };
