"use client";

const MOBILE_AUTO_LOGIN_KEY = "kyvc.mobile.autoLogin";

export function readMobileAutoLoginEnabled() {
  if (typeof window === "undefined") return false;
  return window.localStorage.getItem(MOBILE_AUTO_LOGIN_KEY) === "true";
}

export function setMobileAutoLoginEnabled(enabled: boolean) {
  if (typeof window === "undefined") return;
  if (enabled) {
    window.localStorage.setItem(MOBILE_AUTO_LOGIN_KEY, "true");
  } else {
    window.localStorage.removeItem(MOBILE_AUTO_LOGIN_KEY);
  }
}
