"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import Header from "@/components/layout/Header";
import Sidebar from "@/components/layout/Sidebar";

const allowedPaths = new Set(["/dashboard", "/ai/settings"]);

function normalizePath(pathname: string) {
  if (pathname.length > 1 && pathname.endsWith("/")) {
    return pathname.slice(0, -1);
  }

  return pathname;
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [toast, setToast] = useState<string | null>(null);
  const normalizedPath = normalizePath(pathname);
  const allowed = allowedPaths.has(normalizedPath);

  const showDevelopmentToast = () => {
    setToast("개발중입니다");
    window.setTimeout(() => setToast(null), 2200);
  };

  useEffect(() => {
    if (!allowed) {
      showDevelopmentToast();
      router.replace("/dashboard");
    }
  }, [allowed, router]);

  return (
    <div className="flex min-h-screen bg-slate-100">
      <Sidebar onBlockedNavigate={showDevelopmentToast} />
      <div className="flex-1 flex flex-col">
        <Header onBlockedNavigate={showDevelopmentToast} />
        <main className="flex-1 p-6 bg-slate-100">{allowed ? children : null}</main>
      </div>
      {toast ? (
        <div className="fixed right-5 top-5 z-50 rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white shadow-lg">
          {toast}
        </div>
      ) : null}
    </div>
  );
}
