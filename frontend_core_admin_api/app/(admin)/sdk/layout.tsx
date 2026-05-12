"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const tabs = [
  { label: "API 모니터링", href: "/sdk" },
  { label: "검증 메타데이터", href: "/sdk/metadata" },
  { label: "테스트 벡터", href: "/sdk/testvectors" },
  { label: "버전 호환성", href: "/sdk/compatibility" },
];

export default function SdkLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  return (
    <div>
      <div className="flex items-center gap-1 mb-4 border-b border-slate-200">
        {tabs.map((tab) => {
          const isActive = pathname === tab.href;
          return (
            <Link key={tab.href} href={tab.href}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                isActive ? "border-blue-600 text-blue-600" : "border-transparent text-slate-500 hover:text-slate-700"
              }`}>
              {tab.label}
            </Link>
          );
        })}
      </div>
      {children}
    </div>
  );
}
