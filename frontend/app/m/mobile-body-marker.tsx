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

    let startY = 0;
    let scrollTarget: HTMLElement | null = null;
    let viewportRaf = 0;

    const syncVisualViewport = () => {
      if (viewportRaf) return;
      viewportRaf = window.requestAnimationFrame(() => {
        viewportRaf = 0;
        const vv = window.visualViewport;
        const height = vv?.height ?? window.innerHeight;
        const offsetTop = vv?.offsetTop ?? 0;
        const bottomInset = Math.max(0, window.innerHeight - height - offsetTop);
        const fallbackHeight = Math.max(
          height,
          document.documentElement.clientHeight,
          window.innerHeight,
        );
        document.documentElement.style.setProperty(
          "--m-visual-bottom",
          `${bottomInset}px`,
        );
        document.documentElement.style.setProperty(
          "--m-visual-height",
          `${fallbackHeight}px`,
        );
      });
    };

    const isAuthInputRoute = () => {
      const path = window.location.pathname;
      return path.startsWith("/m/login") || path.startsWith("/m/signup");
    };

    const isTextInputTarget = (target: EventTarget | null) => {
      if (!(target instanceof HTMLElement)) return false;
      if (target instanceof HTMLTextAreaElement) return true;
      if (target.isContentEditable) return true;
      if (!(target instanceof HTMLInputElement)) return false;

      const type = target.type.toLowerCase();
      return (
        type === "" ||
        type === "text" ||
        type === "email" ||
        type === "password" ||
        type === "tel" ||
        type === "url" ||
        type === "search" ||
        type === "number"
      );
    };

    const syncAuthInputFocus = () => {
      const focused = document.activeElement;
      document.body.toggleAttribute(
        "data-mobile-keyboard-open",
        isAuthInputRoute() && isTextInputTarget(focused),
      );
    };

    const onFocusOut = () => {
      window.setTimeout(syncAuthInputFocus, 0);
    };

    const getScrollTarget = (target: EventTarget | null) => {
      if (!(target instanceof Element)) return null;
      return target.closest<HTMLElement>(
        ".m-shell .activity-tabs, .m-shell .settings-container, .m-shell .signup-content, .m-shell .content.scroll, .m-shell .scroll, .m-shell .view",
      );
    };

    const onTouchStart = (event: TouchEvent) => {
      startY = event.touches[0]?.clientY ?? 0;
      scrollTarget = getScrollTarget(event.target);
    };

    const onTouchMove = (event: TouchEvent) => {
      const target = event.target;
      if (!(target instanceof Element) || !target.closest(".m-shell")) return;
      if (target.closest(".terms-sheet-handle")) return;
      if (target.closest(".activity-tabs")) return;

      const currentY = event.touches[0]?.clientY ?? startY;
      const deltaY = currentY - startY;
      const scroller = scrollTarget ?? getScrollTarget(target);

      if (!scroller) {
        event.preventDefault();
        return;
      }

      const canScroll = scroller.scrollHeight > scroller.clientHeight + 1;
      if (!canScroll) {
        event.preventDefault();
        return;
      }

      const atTop = scroller.scrollTop <= 0;
      const atBottom =
        scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 1;

      if ((atTop && deltaY > 0) || (atBottom && deltaY < 0)) {
        event.preventDefault();
      }
    };

    document.addEventListener("focusin", syncAuthInputFocus);
    document.addEventListener("focusout", onFocusOut);
    document.addEventListener("touchstart", onTouchStart, { passive: true });
    document.addEventListener("touchmove", onTouchMove, { passive: false });
    syncVisualViewport();
    window.visualViewport?.addEventListener("resize", syncVisualViewport);
    window.visualViewport?.addEventListener("scroll", syncVisualViewport);
    window.addEventListener("resize", syncVisualViewport);

    return () => {
      if (viewportRaf) window.cancelAnimationFrame(viewportRaf);
      window.visualViewport?.removeEventListener("resize", syncVisualViewport);
      window.visualViewport?.removeEventListener("scroll", syncVisualViewport);
      window.removeEventListener("resize", syncVisualViewport);
      document.documentElement.style.removeProperty("--m-visual-bottom");
      document.documentElement.style.removeProperty("--m-visual-height");
      document.removeEventListener("focusin", syncAuthInputFocus);
      document.removeEventListener("focusout", onFocusOut);
      document.removeEventListener("touchstart", onTouchStart);
      document.removeEventListener("touchmove", onTouchMove);
      document.body.removeAttribute("data-mobile-keyboard-open");
      document.body.removeAttribute("data-mobile");
    };
  }, []);

  return null;
}
