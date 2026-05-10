const API_BASE = "";
const ME_BASE = `${API_BASE}/api/admin/me`;

// ────────────────────────────────────────────────────────────
// 타입 정의
// ────────────────────────────────────────────────────────────

export interface MeProfile {
  adminUserId: string;
  loginId: string;
  name: string;
  email?: string;
  phone?: string;
  department?: string;
  roleName?: string;
  roles?: string[];
  mfaEnabled?: boolean;
  lastLoginAt?: string;
  createdAt?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
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

// ────────────────────────────────────────────────────────────
// 내 정보 API
// ────────────────────────────────────────────────────────────

/** GET /api/admin/me */
export async function getMe(): Promise<MeProfile> {
  const response = await fetch(ME_BASE, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<MeProfile>;
  return json.data;
}

/** PATCH /api/admin/me/password */
export async function changeMyPassword(body: ChangePasswordRequest): Promise<void> {
  const response = await fetch(`${ME_BASE}/password`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
