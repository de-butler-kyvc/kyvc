"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { Logo } from "@/components/design/primitives";

export type NavItem = {
  href: string;
  label: string;
  description?: string;
  icon?: React.ReactNode;
};

export type NavSection = {
  title: string;
  items: NavItem[];
};

type SidebarProps = {
  brand?: string;
  subtitle?: string;
  sections: NavSection[];
  homeHref?: string;
};

const normalize = (p: string) => p.replace(/\/+$/, "") || "/";

export function Sidebar({ sections, homeHref = "/corporate" }: SidebarProps) {
  const pathname = normalize(usePathname() ?? "/");
  const allHrefs = sections.flatMap((s) => s.items.map((i) => normalize(i.href)));
  const activeHref = allHrefs
    .filter((h) => pathname === h || pathname.startsWith(h + "/"))
    .sort((a, b) => b.length - a.length)[0];

  return (
    <aside className="sidebar">
      <Link href={homeHref} className="sidebar-header" style={{ textDecoration: "none" }}>
        <Logo theme="dark" size={20} />
      </Link>
      {sections.map((section) => (
        <div className="sidebar-section" key={section.title}>
          <div className="sidebar-section-label">{section.title}</div>
          {section.items.map((item) => {
            const active = normalize(item.href) === activeHref;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`sidebar-link${active ? " active" : ""}`}
              >
                {item.icon}
                <span>{item.label}</span>
              </Link>
            );
          })}
        </div>
      ))}
    </aside>
  );
}
