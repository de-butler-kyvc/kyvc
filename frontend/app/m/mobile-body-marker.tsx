"use client";

import { useEffect } from "react";

import { setupBridge } from "@/lib/m/android-bridge";

/**
 * /m 하위 페이지에서만 body[data-mobile="true"]를 설정해
 * 모바일 CSS 오버라이드(min-width 등)를 활성화한다.
 * 동시에 Android WebView 브리지 콜백도 설치한다.
 */
export default function MobileBodyMarker() {
  useEffect(() => {
    document.body.setAttribute("data-mobile", "true");
    setupBridge();
    return () => {
      document.body.removeAttribute("data-mobile");
    };
  }, []);

  return null;
}
