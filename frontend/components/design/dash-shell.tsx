"use client";

import * as React from "react";
import { usePathname, useRouter } from "next/navigation";
import { Icon } from "./icons";
import { Logo } from "./primitives";
import { useT, type Lang } from "@/lib/i18n";

export type DashShellProps = {
  children: React.ReactNode;
  current?: string;
  lang?: Lang;
};

const ROUTES: Record<string, string> = {
  dashboard: "/corporate",
  "kyc-start": "/corporate/kyc/apply",
  "kyc-status": "/corporate/kyc",
  "corp-info": "/corporate/profile",
  "rep-info": "/corporate/representative",
  "agent-info": "/corporate/agents",
  "kyc-history": "/corporate/kyc/history",
  "vc-history": "/corporate/vc",
  "vp-history": "/corporate/vp",
  "doc-manage": "/corporate/documents",
  "doc-delete": "/corporate/documents/delete",
  "agent-manage": "/corporate/agents/manage",
};

export function DashShell({ children, current, lang = "ko" }: DashShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const t = useT(lang);

  const go = (id: string) => {
    const dest = ROUTES[id];
    if (dest) router.push(dest);
  };

  const isActive = (id: string) => {
    if (current) return current === id;
    const dest = ROUTES[id];
    if (!dest) return false;
    if (dest === "/corporate") return pathname === "/corporate";
    return pathname === dest || pathname.startsWith(dest + "/");
  };

  const navItems = [
    {
      section: t("nav_kyc"),
      items: [
        { id: "dashboard", icon: <Icon.Grid />, label: t("nav_dashboard") },
        { id: "kyc-start", icon: <Icon.FilePlus />, label: t("nav_kyc_apply") },
        { id: "kyc-status", icon: <Icon.Activity />, label: t("nav_kyc_status") },
      ],
    },
    {
      section: t("nav_corp"),
      items: [
        { id: "corp-info", icon: <Icon.Home />, label: t("nav_corp_basic") },
        { id: "rep-info", icon: <Icon.User />, label: t("nav_rep") },
        { id: "agent-info", icon: <Icon.UserCheck />, label: t("nav_agent") },
      ],
    },
    {
      section: t("nav_history"),
      items: [
        { id: "kyc-history", icon: <Icon.Activity />, label: t("nav_kyc_history") },
        { id: "doc-manage", icon: <Icon.File />, label: "제출서류 관리" },
        { id: "vc-history", icon: <Icon.CheckSquare />, label: t("nav_vc_history") },
        { id: "vp-history", icon: <Icon.Upload />, label: "VP 제출 이력" },
      ],
    },
  ];

  return (
    <div className="dash-shell page-enter">
      <aside className="sidebar">
        <div
          className="sidebar-header"
          onClick={() => router.push("/corporate")}
          style={{ cursor: "pointer" }}
        >
          <Logo theme="dark" size={20} />
        </div>
        {navItems.map((sec, i) => (
          <div className="sidebar-section" key={i}>
            <div className="sidebar-section-label">{sec.section}</div>
            {sec.items.map((it) => (
              <a
                key={it.id}
                href="#"
                className={`sidebar-link ${isActive(it.id) ? "active" : ""}`}
                onClick={(e) => {
                  e.preventDefault();
                  go(it.id);
                }}
              >
                {it.icon}
                <span>{it.label}</span>
              </a>
            ))}
          </div>
        ))}
      </aside>

      <main className="dash-main">
        <div className="dash-topbar">
          <div className="dash-topbar-title">{t("dashboard_title")}</div>
          <div className="dash-topbar-right">
            <button className="icon-btn" title="Search" type="button">
              <Icon.Search />
            </button>
            <button className="icon-btn" title="Notifications" type="button">
              <Icon.Bell />
            </button>
            <div style={{ width: 1, height: 22, background: "var(--border)" }} />
            <a
              href="#"
              className="topbar-nav-link"
              onClick={(e) => {
                e.preventDefault();
                router.push("/");
              }}
            >
              {t("logout")}
            </a>
            <div className="avatar">김</div>
          </div>
        </div>

        <div className="dash-content">{children}</div>
      </main>
    </div>
  );
}

export function KycStepper({
  current,
  lang = "ko",
}: {
  current: "start" | "type" | "guide" | "upload" | "confirm";
  lang?: Lang;
}) {
  const t = useT(lang);
  const steps = [
    { id: "start", label: t("step_start") },
    { id: "type", label: t("step_corp_type") },
    { id: "guide", label: t("step_doc_guide") },
    { id: "upload", label: t("step_upload") },
    { id: "confirm", label: t("step_confirm") },
  ];
  const idx = steps.findIndex((s) => s.id === current);
  return (
    <div className="stepper">
      {steps.map((s, i) => {
        const cls = i === idx ? "active" : i < idx ? "done" : "";
        return (
          <React.Fragment key={s.id}>
            <div className={`stepper-item ${cls}`}>
              <div className="stepper-num">{i < idx ? "✓" : i + 1}</div>
              <div className="stepper-label">{s.label}</div>
            </div>
            {i < steps.length - 1 && <div className="stepper-line" />}
          </React.Fragment>
        );
      })}
    </div>
  );
}
