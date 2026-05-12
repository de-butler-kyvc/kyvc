"use client";

import { useEffect, useState } from "react";

import { getCoreHealth, getCoreStatus, type CoreHealth } from "@/lib/api/core-admin";

function pickValue(data: Record<string, unknown> | null, keys: string[]) {
  if (!data) return "-";

  for (const key of keys) {
    const value = data[key];
    if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
      return String(value);
    }
  }

  return "-";
}

function getComponentStatus(status: Record<string, unknown> | null, name: string) {
  const components = status?.components;
  if (!components || typeof components !== "object") return "-";

  const component = (components as Record<string, unknown>)[name];
  if (!component || typeof component !== "object") return "-";

  const value = (component as Record<string, unknown>).status;
  return typeof value === "string" ? value : "-";
}

export default function DashboardPage() {
  const [health, setHealth] = useState<CoreHealth | null>(null);
  const [status, setStatus] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        const [healthData, statusData] = await Promise.all([getCoreHealth(), getCoreStatus()]);
        if (!cancelled) {
          setHealth(healthData);
          setStatus(statusData);
          setError(null);
        }
      } catch {
        if (!cancelled) {
          setError("코어 서버 연결을 확인해주세요.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, []);

  const cards = [
    { name: "Admin API", status: health?.status ?? "-", sub: health?.service ?? "-" },
    { name: "Core API", status: pickValue(status, ["status", "state"]), sub: pickValue(status, ["service", "environment"]) },
    { name: "Database", status: getComponentStatus(status, "database"), sub: "Core status component" },
    { name: "Environment", status: health?.environment ?? "-", sub: "Current deployment" },
  ];

  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <p className="text-xs text-slate-400">
          코어 어드민 &gt; <span className="text-slate-600">대시보드</span>
        </p>
      </div>
      <h1 className="mb-4 text-lg font-semibold text-slate-800">코어 어드민 대시보드</h1>

      {error ? (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      ) : null}

      <div className="mb-4 grid grid-cols-4 gap-3">
        {cards.map((card) => {
          const ok = card.status === "UP" || card.status === "dev";
          return (
            <div key={card.name} className="rounded-lg border border-slate-200 bg-white p-4">
              <p className="mb-2 text-xs text-slate-500">{card.name}</p>
              <div className="flex items-center gap-1.5">
                <span className={`h-2 w-2 rounded-full ${ok ? "bg-emerald-500" : "bg-yellow-400"}`} />
                <span className={`text-sm font-bold ${ok ? "text-emerald-600" : "text-yellow-500"}`}>
                  {loading ? "..." : card.status}
                </span>
              </div>
              <p className="mt-1 text-[11px] text-slate-400">{loading ? "불러오는 중..." : card.sub}</p>
            </div>
          );
        })}
      </div>

      <div className="rounded-lg border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-5 py-3">
          <p className="text-xs font-semibold text-slate-600">코어 상태 원본 응답</p>
        </div>
        <pre className="max-h-[420px] overflow-auto p-5 text-xs text-slate-700">
          {loading ? "불러오는 중..." : JSON.stringify({ health, status }, null, 2)}
        </pre>
      </div>
    </div>
  );
}
