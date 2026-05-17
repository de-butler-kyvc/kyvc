"use client";

const MOBILE_AUTO_LOGIN_KEY = "kyvc.mobile.autoLogin";
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

function readCookieEnabled() {
  if (typeof document === "undefined") return false;
  return document.cookie
    .split(";")
    .map((part) => part.trim())
    .some((part) => part === `${MOBILE_AUTO_LOGIN_KEY}=true`);
}

function writeCookieEnabled(enabled: boolean) {
  if (typeof document === "undefined") return;
  const secure = window.location.protocol === "https:" ? "; Secure" : "";
  const maxAge = enabled ? COOKIE_MAX_AGE_SECONDS : 0;
  document.cookie =
    `${MOBILE_AUTO_LOGIN_KEY}=${enabled ? "true" : ""}; ` +
    `Max-Age=${maxAge}; Path=/; SameSite=Lax${secure}`;
}

export function readMobileAutoLoginEnabled() {
  if (typeof window === "undefined") return false;
  try {
    return (
      window.localStorage.getItem(MOBILE_AUTO_LOGIN_KEY) === "true" ||
      readCookieEnabled()
    );
  } catch {
    return readCookieEnabled();
  }
}

export function setMobileAutoLoginEnabled(enabled: boolean) {
  if (typeof window === "undefined") return;
  try {
    if (enabled) {
      window.localStorage.setItem(MOBILE_AUTO_LOGIN_KEY, "true");
    } else {
      window.localStorage.removeItem(MOBILE_AUTO_LOGIN_KEY);
    }
  } catch {
    // Keep the cookie fallback when WebView storage is unavailable.
  }
  writeCookieEnabled(enabled);
}
