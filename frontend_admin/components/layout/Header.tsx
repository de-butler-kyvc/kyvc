"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { clearAuthSession } from "@/lib/auth-session";

export default function Header() {
  const router = useRouter();

  const handleLogout = () => {
    clearAuthSession();
    router.push("/login");
  };

  return (
    <header className="h-14 bg-slate-900 text-white flex items-center justify-between px-6 border-b border-slate-700 flex-shrink-0">
      <Link href="/dashboard" className="text-sm text-slate-400 hover:text-white transition-colors">
        백엔드 어드민
      </Link>
      <div className="flex items-center gap-3">
        <span className="text-sm text-slate-300">관리자</span>
        <button onClick={handleLogout} className="text-xs text-slate-400 hover:text-white transition-colors">
          로그아웃
        </button>
      </div>
    </header>
  );
}