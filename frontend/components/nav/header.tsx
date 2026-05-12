"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Icon } from "@/components/design/icons";
import { ApiError, auth } from "@/lib/api";
import { useSession } from "@/lib/session-context";

type HeaderProps = {
  channel: string;
  channelTag?: string;
  initial?: string;
};

export function Header({ channel, initial = "K" }: HeaderProps) {
  const router = useRouter();
  const { session, refreshSession } = useSession();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    if (isLoggingOut) return;

    setIsLoggingOut(true);
    try {
      await auth.logout();
      await refreshSession();
      router.replace("/login");
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        await refreshSession();
        router.replace("/login");
        return;
      }
      window.alert("로그아웃에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <header className="dash-topbar">
      <div className="dash-topbar-title">{channel}</div>
      <div className="dash-topbar-right">
        <button className="icon-btn" title="Search" type="button">
          <Icon.Search />
        </button>
        <button className="icon-btn" title="Notifications" type="button">
          <Icon.Bell />
        </button>
        <div style={{ width: 1, height: 22, background: "var(--border)" }} />
        {session?.authenticated && (
          <button
            type="button"
            aria-label="로그아웃"
            className="topbar-nav-link"
            onClick={handleLogout}
            disabled={isLoggingOut}
            style={{
              background: "transparent",
              border: 0,
              padding: 0,
              cursor: isLoggingOut ? "not-allowed" : "pointer",
              opacity: isLoggingOut ? 0.6 : 1
            }}
          >
            로그아웃
          </button>
        )}
        <div className="avatar">{initial}</div>
      </div>
    </header>
  );
}
