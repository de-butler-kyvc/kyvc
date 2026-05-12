"use client";

import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

const modules = [
  { name: "AI 모듈",   slug: "ai",   version: "2.2.0", env: "전체",    deployedAt: "2025.05.01", rollback: false },
  { name: "VC 모듈",   slug: "vc",   version: "1.5.2", env: "mainnet", deployedAt: "2025.04.15", rollback: false },
  { name: "XRPL 모듈", slug: "xrpl", version: "3.0.9", env: "testnet", deployedAt: "2025.04.10", rollback: true  },
];

export default function VersionPage() {
  const router = useRouter();

  return (
    <div>
      <PageHeader breadcrumb="버전 / 배포" title="코어 패키지 버전 관리" />

      <div className="bg-white rounded-lg border border-slate-200 mb-4">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">배포 모듈</p>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["모듈명", "배포 버전", "적용 환경", "배포일", "롤백 여부", "상세"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {modules.map((m) => (
              <tr key={m.name} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs text-slate-700">{m.name}</td>
                <td className="px-5 py-3 text-xs font-mono text-slate-600">{m.version}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{m.env}</td>
                <td className="px-5 py-3 text-xs text-slate-500">{m.deployedAt}</td>
                <td className="px-5 py-3">
                  {m.rollback ? <StatusBadge status="지연" /> : <span className="text-xs text-slate-400">-</span>}
                </td>
                <td className="px-5 py-3">
                  <button
                    onClick={() => router.push(`/version/${m.slug}`)}
                    className="text-xs text-blue-500 hover:underline"
                  >
                    상세 →
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
