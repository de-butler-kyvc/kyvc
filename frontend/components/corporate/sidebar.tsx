"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Award,
  Building2,
  FilePlus2,
  History,
  LayoutDashboard,
  ListChecks,
  type LucideIcon,
  UserRound
} from "lucide-react";

import { cn } from "@/lib/utils";

type SidebarItem = {
  href: string;
  label: string;
  icon: LucideIcon;
};

type SidebarSection = {
  title: string;
  items: SidebarItem[];
};

const sections: SidebarSection[] = [
  {
    title: "KYC 신청",
    items: [
      { href: "/corporate", label: "대시보드", icon: LayoutDashboard },
      { href: "/corporate/kyc/apply", label: "KYC 신청 시작", icon: FilePlus2 },
      { href: "/corporate/kyc", label: "진행상태 조회", icon: ListChecks }
    ]
  },
  {
    title: "법인정보",
    items: [
      { href: "/corporate/profile", label: "법인 기본정보", icon: Building2 },
      { href: "/corporate/agents", label: "대표자 정보", icon: UserRound }
    ]
  },
  {
    title: "이력/관리",
    items: [
      { href: "/corporate/kyc/history", label: "KYC 신청 이력", icon: History },
      { href: "/corporate/vc/history", label: "VC 발급 이력", icon: Award }
    ]
  }
];

export function CorporateSidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden h-full w-[220px] shrink-0 flex-col border-r border-[#e5e5e2] bg-white px-3 py-5 md:flex">
      {sections.map((section, sectionIdx) => (
        <div key={section.title} className={cn(sectionIdx === 0 ? "" : "mt-3.5")}>
          <div className="px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.6px] text-[#a1a19d]">
            {section.title}
          </div>
          <ul className="mt-1 space-y-px">
            {section.items.map((item) => {
              const active =
                pathname === item.href ||
                (item.href !== "/corporate" && pathname.startsWith(item.href));
              const Icon = item.icon;
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={cn(
                      "flex items-center gap-2 rounded-[10px] px-2.5 py-[7px] text-[13px] transition-colors",
                      active
                        ? "bg-[#eff6ff] font-semibold text-[#2563eb]"
                        : "text-[#6b6b68] hover:bg-[#f4f4f1] hover:text-[#111110]"
                    )}
                  >
                    <Icon className="size-[15px] shrink-0" strokeWidth={1.75} />
                    <span className="leading-none">{item.label}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </aside>
  );
}
