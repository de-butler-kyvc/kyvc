"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { useSession } from "@/lib/session-context";

/** 인증된 사용자의 기본 진입(법인 대시보드) */
export const AUTHENTICATED_HOME = "/corporate";

/** 비인증 사용자가 로그인 화면으로 보내질 기본 경로 */
export const GUEST_HOME = "/login";

/**
 * 비로그인 전용 페이지(로그인·회원가입 등)에서 사용.
 * 세션이 있으면 `redirectTo`로 replace, 없으면 본문을 그릴 수 있도록 false 반환.
 */
export function useGuestSessionGate(redirectTo: string = AUTHENTICATED_HOME): boolean {
  const router = useRouter();
  const { session, loading } = useSession();

  useEffect(() => {
    if (!loading && session?.authenticated) router.replace(redirectTo);
  }, [loading, session, router, redirectTo]);

  return loading || !!session?.authenticated;
}

/**
 * 로그인 전용 페이지에서 사용.
 * 세션이 없으면 `redirectTo`로 replace, 있으면 본문을 그릴 수 있도록 false 반환.
 */
export function useAuthSessionGate(redirectTo: string = GUEST_HOME): boolean {
  const router = useRouter();
  const { session, loading } = useSession();

  useEffect(() => {
    if (!loading && !session?.authenticated) router.replace(redirectTo);
  }, [loading, session, router, redirectTo]);

  return loading || !session?.authenticated;
}

export function SessionGateSplash({
  message = "세션 확인 중…"
}: {
  message?: string;
}) {
  return (
    <div className="app-shell page-enter">
      <div
        className="center-stage"
        style={{ minHeight: "50vh", justifyContent: "center" }}
      >
        <p className="auth-subtitle" style={{ textAlign: "center" }}>
          {message}
        </p>
      </div>
    </div>
  );
}

export function AuthSessionGate({ children }: { children: ReactNode }) {
  const pending = useAuthSessionGate();
  if (pending) return <SessionGateSplash />;
  return <>{children}</>;
}
