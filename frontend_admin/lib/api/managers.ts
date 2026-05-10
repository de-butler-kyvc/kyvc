const API_BASE = "";
const ADMIN_USERS_URL = `${API_BASE}/api/admin/backend/admin-users`;
const ADMIN_ROLES_URL = `${API_BASE}/api/admin/backend/admin-roles`;

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

export interface AdminRole {
  roleId: number | string;
  roleName: string;
  description?: string;
  memberCount?: number;
  permissions?: string[];
}

// ── 관리자 계정 타입 ──────────────────────────────────────────

export interface AdminUser {
  adminUserId: string;
  loginId?: string;
  name: string;
  email?: string;
  phone?: string;
  roleName?: string;
  status?: string;
  lastLoginAt?: string;
  createdAt?: string;
  mfaEnabled?: boolean;
}

export interface AdminUserDetail extends AdminUser {
  roles?: AdminRole[];
  department?: string;
}

function getAuthHeaders() {
  return { "Content-Type": "application/json" };
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

// ── 역할 API ─────────────────────────────────────────────────

/** GET /api/admin/backend/admin-roles — 전체 역할 목록 조회 */
export async function getAllAdminRoles(): Promise<AdminRole[]> {
  const response = await fetch(ADMIN_ROLES_URL, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminRole[] | PageLike<AdminRole>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/admin-users/{adminUserId}/roles — 특정 관리자의 역할 목록 조회 */
export async function getAdminUserRoles(adminUserId: string): Promise<AdminRole[]> {
  const response = await fetch(`${ADMIN_USERS_URL}/${adminUserId}/roles`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminRole[] | PageLike<AdminRole>>;
  return unwrapListData(json.data);
}

/** POST /api/admin/backend/admin-users/{adminUserId}/roles — 역할 부여 */
export async function assignAdminUserRole(
  adminUserId: string,
  roleId: string | number
): Promise<void> {
  const response = await fetch(`${ADMIN_USERS_URL}/${adminUserId}/roles`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({ roleId }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** DELETE /api/admin/backend/admin-users/{adminUserId}/roles/{roleId} — 역할 제거 */
export async function removeAdminUserRole(
  adminUserId: string,
  roleId: string | number
): Promise<void> {
  const response = await fetch(`${ADMIN_USERS_URL}/${adminUserId}/roles/${roleId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

// ── 관리자 계정 API ───────────────────────────────────────────

/** GET /api/admin/backend/admin-users — 관리자 목록 조회 */
export async function getAdminUsers(filters?: {
  search?: string;
  role?: string;
  status?: string;
}): Promise<AdminUser[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("search", filters.search.trim());
  if (filters?.role && filters.role !== "전체 역할") params.set("role", filters.role);
  if (filters?.status && filters.status !== "전체 상태") params.set("status", filters.status);
  const url = params.toString() ? `${ADMIN_USERS_URL}?${params}` : ADMIN_USERS_URL;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminUser[] | PageLike<AdminUser>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/admin-users/{adminUserId} — 관리자 상세 조회 */
export async function getAdminUser(adminUserId: string): Promise<AdminUserDetail> {
  const response = await fetch(`${ADMIN_USERS_URL}/${adminUserId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminUserDetail>;
  return json.data;
}

// ── 페이지 호환 래퍼 (managers/page.tsx에서 사용) ─────────────

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

/** POST /api/admin/backend/admin-users — 관리자 계정 생성 */
export async function createAdminUser(data: {
  loginId: string;
  name: string;
  email: string;
  phone?: string;
  roleIds?: (string | number)[];
}): Promise<AdminUser> {
  const response = await fetch(ADMIN_USERS_URL, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminUser>;
  return json.data;
}

/** PATCH /api/admin/backend/admin-users/{adminUserId} — 관리자 계정 수정 */
export async function updateAdminUser(
  adminUserId: string,
  data: { name?: string; email?: string; phone?: string; status?: string }
): Promise<AdminUser> {
  const response = await fetch(`${ADMIN_USERS_URL}/${adminUserId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AdminUser>;
  return json.data;
}

export async function getManagers(filters?: {
  search?: string;
  role?: string;
  status?: string;
}): Promise<{
  id: string;
  name: string;
  role: string;
  status: string;
  lastLogin: string;
  mfa: string;
}[]> {
  const users = await getAdminUsers(filters);
  return users.map((u) => ({
    id: u.adminUserId,
    name: u.name,
    role: u.roleName ?? "-",
    status: u.status ?? "-",
    lastLogin: fmtDt(u.lastLoginAt),
    mfa: u.mfaEnabled === true ? "설정됨" : u.mfaEnabled === false ? "미설정" : "-",
  }));
}
