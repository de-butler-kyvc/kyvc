const AUTH_COOKIE = "auth_token";
const STORAGE_KEY = "accessToken";
const REFRESH_KEY = "refreshToken";
const PLACEHOLDER_TOKENS = new Set(["dummy_token", "sso_token", "mock_mfa_token"]);

function readCookieAuthToken(): string | null {
  if (typeof document === "undefined") return null;
  const row = document.cookie
    .split("; ")
    .find((r) => r.startsWith(`${AUTH_COOKIE}=`));
  if (!row) return null;
  const raw = row.slice(`${AUTH_COOKIE}=`.length);
  try {
    return decodeURIComponent(raw) || null;
  } catch {
    return raw || null;
  }
}

export function getAccessTokenForApi(): string {
  if (typeof window === "undefined") return "";

  const fromStorage = localStorage.getItem(STORAGE_KEY)?.trim();
  if (fromStorage) return fromStorage;

  const fromCookie = readCookieAuthToken()?.trim();
  if (fromCookie) return fromCookie;

  const fromEnv = process.env.NEXT_PUBLIC_ADMIN_ACCESS_TOKEN?.trim();
  return fromEnv ?? "";
}

export function isPlaceholderAccessToken(token: string | null | undefined): boolean {
  if (!token) return true;
  return PLACEHOLDER_TOKENS.has(token.trim());
}

export function persistAuthToken(token: string, options?: { maxAgeSec?: number }) {
  const maxAge = options?.maxAgeSec ?? 86400;
  const safe = encodeURIComponent(token);
  document.cookie = `${AUTH_COOKIE}=${safe}; path=/; max-age=${maxAge}; SameSite=Lax`;
  localStorage.setItem(STORAGE_KEY, token);
}

export function persistRefreshToken(token: string) {
  localStorage.setItem(REFRESH_KEY, token);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(REFRESH_KEY) ?? null;
}

export function clearAuthSession() {
  document.cookie = `${AUTH_COOKIE}=; path=/; max-age=0`;
  localStorage.removeItem(STORAGE_KEY);
  localStorage.removeItem(REFRESH_KEY);
}
