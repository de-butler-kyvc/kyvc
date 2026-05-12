"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const tabs = [
  { label: "AI 모델 설정", href: "/ai/settings" },
  { label: "Azure OpenAI 연동", href: "/ai/settings/azure" },
  { label: "프롬프트 목록", href: "/ai/settings/prompts" },
  { label: "임계치 설정", href: "/ai/settings/threshold" },
];

export default function AiSettingsLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  return (
    <div>
      <div className="flex items-center gap-1 mb-4 border-b border-slate-200">
        {tabs.map((tab) => {
          const isActive = pathname === tab.href;
          return (
            <Link
              key={tab.href}
              href={tab.href}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                isActive
                  ? "border-blue-600 text-blue-600"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              {tab.label}
            </Link>
          );
        })}
      </div>
      {children}
    </div>
  );
}
