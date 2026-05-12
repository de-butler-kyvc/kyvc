"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const tabs = [
  { label: "패키지 버전 관리", href: "/version" },
  { label: "배포 이력 조회", href: "/version/deploy-history" },
];

export default function VersionLayout({ children }: { children: React.ReactNode }) {
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
