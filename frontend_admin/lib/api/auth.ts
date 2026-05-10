const API_BASE = "";
const AUTH_BASE = `${API_BASE}/api/admin/auth`;

// ────────────────────────────────────────────────────────────
// 타입 정의
// ────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginAdmin {
  adminId: number;
  email: string;
  displayName: string;
  status: string;
}

export interface LoginResponse {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
  admin: LoginAdmin;
  roles: string[];
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
}

export interface SessionInfo {
  authenticated?: boolean;
  adminUserId: string;
  loginId: string;
  name: string;
  roles?: string[];
  permissions?: string[];
  expiresAt?: string;
}

export interface PasswordResetRequestBody {
  email: string;
}

export interface PasswordResetConfirmBody {
  resetToken: string;
  newPassword: string;
}

export interface MfaChallengeRequest {
  mfaToken: string;
}

export interface MfaVerifyRequest {
  mfaToken: string;
  code: string;
}

export interface MfaVerifyResponse {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
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

const JSON_HEADERS = { "Content-Type": "application/json" } as const;

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
// 인증 API
// ────────────────────────────────────────────────────────────

/** POST /api/admin/auth/login */
export async function login(body: LoginRequest): Promise<LoginResponse> {
  const response = await fetch(`${AUTH_BASE}/login`, {
    method: "POST",
    headers: JSON_HEADERS,
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<LoginResponse>;
  return json.data;
}

/** POST /api/admin/auth/logout */
export async function logout(): Promise<void> {
  const response = await fetch(`${AUTH_BASE}/logout`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/auth/token/refresh */
export async function refreshAccessToken(): Promise<TokenRefreshResponse> {
  const response = await fetch(`${AUTH_BASE}/token/refresh`, {
    method: "POST",
    headers: JSON_HEADERS,
    credentials: "include",
    body: JSON.stringify({}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<TokenRefreshResponse>;
  return json.data;
}

/** GET /api/admin/auth/session */
export async function getSession(): Promise<SessionInfo> {
  const response = await fetch(`${AUTH_BASE}/session`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<SessionInfo>;
  return json.data;
}

/** POST /api/admin/auth/password-reset/request */
export async function requestPasswordReset(body: PasswordResetRequestBody): Promise<void> {
  const response = await fetch(`${AUTH_BASE}/password-reset/request`, {
    method: "POST",
    headers: JSON_HEADERS,
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/auth/password-reset/confirm */
export async function confirmPasswordReset(body: PasswordResetConfirmBody): Promise<void> {
  const response = await fetch(`${AUTH_BASE}/password-reset/confirm`, {
    method: "POST",
    headers: JSON_HEADERS,
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/auth/mfa/challenge — OTP 발송 요청 */
export async function requestMfaChallenge(body: MfaChallengeRequest): Promise<void> {
  const response = await fetch(`${AUTH_BASE}/mfa/challenge`, {
    method: "POST",
    headers: JSON_HEADERS,
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/auth/mfa/verify — OTP 검증 및 최종 토큰 발급 */
export async function verifyMfa(body: MfaVerifyRequest): Promise<MfaVerifyResponse> {
  const response = await fetch(`${AUTH_BASE}/mfa/verify`, {
    method: "POST",
    headers: JSON_HEADERS,
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<MfaVerifyResponse>;
  return json.data;
}
