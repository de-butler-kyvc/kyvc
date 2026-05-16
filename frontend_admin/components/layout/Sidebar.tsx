"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  FileText,
  CreditCard,
  QrCode,
  Shield,
  Users,
  BookOpen,
  BadgeCheck,
  Monitor,
  Code2,
  ClipboardList,
  BarChart2,
  UserCog,
} from "lucide-react";

const menus = [
  { label: "대시보드", href: "/dashboard", icon: LayoutDashboard },
  { label: "KYC 신청", href: "/kyc", icon: FileText },
  { label: "VC 관리", href: "/vc", icon: CreditCard },
  { label: "VP 요청/검증", href: "/vp-requests", icon: QrCode },
  { label: "VP 이력", href: "/vp", icon: Shield },
  { label: "사용자", href: "/users", icon: Users },
  { label: "정책/규칙", href: "/policy", icon: BookOpen },
  { label: "Issuer", href: "/issuer", icon: BadgeCheck },
  { label: "Verifier", href: "/verifier", icon: Monitor },
  { label: "SDK 관리", href: "/sdk", icon: Code2 },
  { label: "감사로그", href: "/audit-log", icon: ClipboardList },
  { label: "리포트", href: "/report", icon: BarChart2 },
  { label: "관리자", href: "/managers", icon: UserCog },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-60 min-h-screen bg-slate-900 text-white flex flex-col">
      {/* 로고 */}
      <div className="px-6 py-5 border-b border-slate-700">
        <Image
          src="/kyvcwordmarkdark.png"
          alt="KYvC"
          width={80}
          height={24}
          className="object-contain"
        />
        <span className="block text-xs text-slate-400 mt-1">백엔드 어드민</span>
      </div>

      {/* 메뉴 */}
      <nav className="flex-1 py-4">
        <p className="px-6 text-xs text-slate-500 mb-2">메뉴</p>
        {menus.map((menu) => {
          const Icon = menu.icon;
          const isActive =
            pathname === menu.href || pathname.startsWith(`${menu.href}/`);
          return (
            <Link
              key={menu.href}
              href={menu.href}
              className={`flex items-center gap-3 px-6 py-2.5 text-sm transition-colors ${
                isActive
                  ? "bg-slate-700 text-white font-medium"
                  : "text-slate-400 hover:bg-slate-800 hover:text-white"
              }`}
            >
              <Icon size={16} />
              {menu.label}
            </Link>
          );
        })}
      </nav>

      {/* 하단 */}
      <div className="px-6 py-4 border-t border-slate-700 text-xs text-slate-500">
        © 2025 KYvC
      </div>
    </aside>
  );
}
