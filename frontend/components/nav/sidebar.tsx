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

export function Sidebar({ brand, subtitle, sections }: SidebarProps) {
  const pathname = normalize(usePathname() ?? "/");
  const allHrefs = sections.flatMap((s) => s.items.map((i) => normalize(i.href)));
  const activeHref = allHrefs
    .filter((h) => pathname === h || pathname.startsWith(h + "/"))
    .sort((a, b) => b.length - a.length)[0];

  return (
    <aside className="hidden h-screen w-64 shrink-0 flex-col border-r bg-card md:flex">
      <div className="flex h-16 items-center border-b px-6">
        <div>
          <div className="text-base font-semibold tracking-tight">{brand}</div>
          {subtitle ? (
            <div className="text-xs text-muted-foreground">{subtitle}</div>
          ) : null}
        </div>
      </div>
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        {sections.map((section) => (
          <div key={section.title} className="mb-6">
            <div className="px-3 pb-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">
              {section.title}
            </div>
            <ul className="space-y-1">
              {section.items.map((item) => {
                const active = normalize(item.href) === activeHref;
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={cn(
                        "block rounded-md px-3 py-2 text-sm transition-colors",
                        active
                          ? "bg-accent text-accent-foreground font-medium"
                          : "text-muted-foreground hover:bg-accent/50 hover:text-foreground"
                      )}
                    >
                      {item.label}
                      {item.description ? (
                        <div className="text-xs text-muted-foreground/80">
                          {item.description}
                        </div>
                      ) : null}
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
