"use client";

import { usePathname, useRouter } from "next/navigation";

const pageTitles: Record<string, string> = {
  "/dashboard": "코어 어드민 대시보드",
  "/ai/settings": "AI 설정",
};

type HeaderProps = {
  onBlockedNavigate: () => void;
};

export default function Header({ onBlockedNavigate }: HeaderProps) {
  const router = useRouter();
  const pathname = usePathname();
  const title = pageTitles[pathname] ?? "코어 어드민";

  return (
    <header className="h-13 bg-slate-900 text-white flex items-center justify-between px-5 border-b border-slate-700 flex-shrink-0">
      <span className="text-sm text-slate-300 font-medium">{title}</span>
      <div className="flex items-center gap-3">
        <button
          onClick={() => router.push("/dashboard")}
          className="text-xs text-slate-400 hover:text-white border border-slate-700 px-2.5 py-1 rounded transition-colors"
        >
          시스템 상태
        </button>
        <button
          onClick={onBlockedNavigate}
          className="text-xs text-slate-400 hover:text-white border border-slate-700 px-2.5 py-1 rounded transition-colors"
        >
          API 문서
        </button>
        <div
          onClick={() => router.push("/dashboard")}
          className="w-7 h-7 bg-blue-600 rounded flex items-center justify-center text-xs font-bold cursor-pointer hover:bg-blue-500 transition-colors"
        >
          코
        </div>
      </div>
    </header>
  );
}
