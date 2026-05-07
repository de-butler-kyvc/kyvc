"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { cn } from "@/lib/utils";

export type NavItem = {
  href: string;
  label: string;
  description?: string;
};

export type NavSection = {
  title: string;
  items: NavItem[];
};

type SidebarProps = {
  brand: string;
  subtitle?: string;
  sections: NavSection[];
};

const normalize = (p: string) => p.replace(/\/+$/, "") || "/";

export function Sidebar({ sections }: SidebarProps) {
  const pathname = normalize(usePathname() ?? "/");
  const allHrefs = sections.flatMap((s) => s.items.map((i) => normalize(i.href)));
  const activeHref = allHrefs
    .filter((h) => pathname === h || pathname.startsWith(h + "/"))
    .sort((a, b) => b.length - a.length)[0];

  return (
    <aside className="hidden h-screen w-[220px] shrink-0 flex-col border-r border-border bg-card md:flex">
      <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 pt-5 pb-4">
        {sections.map((section, sectionIdx) => (
          <div key={section.title} className={sectionIdx === 0 ? "" : "mt-3.5"}>
            <div className="px-2.5 pb-1 text-[10px] font-bold uppercase tracking-[0.6px] text-subtle-foreground">
              {section.title}
            </div>
            <ul>
              {section.items.map((item) => {
                const active = normalize(item.href) === activeHref;
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={cn(
                        "block rounded-[10px] px-2.5 py-1.5 text-[13px] transition-colors",
                        active
                          ? "bg-accent font-semibold text-accent-foreground"
                          : "text-muted-foreground hover:bg-secondary hover:text-foreground"
                      )}
                    >
                      {item.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>
    </aside>
  );
}
