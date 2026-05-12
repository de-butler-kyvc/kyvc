"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Activity,
  CheckSquare,
  Code2,
  CreditCard,
  Database,
  GitBranch,
  Key,
  LayoutDashboard,
  Network,
  ScrollText,
  Settings,
  Shield,
} from "lucide-react";

const menus = [
  { label: "대시보드", href: "/dashboard", icon: LayoutDashboard },
  { label: "AI 처리현황", href: "/ai/status", icon: Activity, disabled: true },
  { label: "AI 설정", href: "/ai/settings", icon: Settings },
  { label: "Schema 관리", href: "/schema", icon: Database, disabled: true },
  { label: "VC 발급", href: "/vc", icon: CreditCard, disabled: true },
  { label: "VP 검증", href: "/vp", icon: Shield, disabled: true },
  { label: "XRPL", href: "/xrpl", icon: Network, disabled: true },
  { label: "Issuer / 키", href: "/issuer", icon: Key, disabled: true },
  { label: "SDK", href: "/sdk", icon: Code2, disabled: true },
  { label: "버전 / 배포", href: "/version", icon: GitBranch, disabled: true },
  { label: "설정 승인", href: "/approval", icon: CheckSquare, disabled: true },
  { label: "감사로그", href: "/audit-log", icon: ScrollText, disabled: true },
];

type SidebarProps = {
  onBlockedNavigate: () => void;
};

export default function Sidebar({ onBlockedNavigate }: SidebarProps) {
  const pathname = usePathname();

  return (
    <aside className="w-48 min-h-screen bg-slate-900 text-white flex flex-col flex-shrink-0">
      <div className="px-4 py-4 border-b border-slate-700">
        <Link href="/dashboard" className="flex items-center gap-3">
          <Image src="/kyvcwordmarkdark.png" alt="KYvC" width={80} height={24} className="object-contain" />
          <div className="flex flex-col gap-1">
            <span className="text-[10px] font-semibold bg-slate-700 text-slate-300 px-1.5 py-0.5 rounded w-fit">
              코어 어드민
            </span>
            <span className="text-[10px] font-bold bg-blue-600 text-white px-1.5 py-0.5 rounded w-fit">PROD</span>
          </div>
        </Link>
      </div>
      <nav className="flex-1 py-3">
        <p className="px-4 text-[10px] text-slate-500 mb-1 uppercase tracking-wider">코어 모듈</p>
        {menus.map((menu) => {
          const Icon = menu.icon;
          const isActive = pathname === menu.href || pathname.startsWith(menu.href + "/");
          const className = `flex w-full items-center gap-2.5 px-4 py-2 text-left text-[13px] transition-colors ${
            isActive
              ? "bg-slate-700 text-white font-medium border-r-2 border-blue-500"
              : "text-slate-400 hover:bg-slate-800 hover:text-white"
          } ${menu.disabled ? "cursor-not-allowed opacity-55 hover:bg-transparent hover:text-slate-400" : ""}`;

          if (menu.disabled) {
            return (
              <button key={menu.href} type="button" className={className} onClick={onBlockedNavigate}>
                <Icon size={14} />
                {menu.label}
              </button>
            );
          }

          return (
            <Link key={menu.href} href={menu.href} className={className}>
              <Icon size={14} />
              {menu.label}
            </Link>
          );
        })}
      </nav>
      <div className="px-4 py-3 border-t border-slate-700 text-[11px] text-slate-500">
        © 2025 KYvC. All rights reserved.
      </div>
    </aside>
  );
}
