"use client";

import Sidebar from "@/components/layout/Sidebar";
import Header from "@/components/layout/Header";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { getSession } from "@/lib/api/auth";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();

  useEffect(() => {
    let alive = true;

    const checkSession = async () => {
      const token = document.cookie
        .split("; ")
        .find((row) => row.startsWith("auth_token="))
        ?.split("=")[1];

      if (token && token !== "dev_bypass") return;

      try {
        const session = await getSession();
        if (alive && "authenticated" in session && !session.authenticated) {
          router.push("/login");
        }
      } catch {
        if (alive && !token) router.push("/login");
      }
    };

    checkSession();
    return () => {
      alive = false;
    };
  }, [router]);

  return (
    <div className="flex min-h-screen bg-slate-100">
      <Sidebar />
      <div className="flex-1 flex flex-col">
        <Header />
        <main className="flex-1 p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
