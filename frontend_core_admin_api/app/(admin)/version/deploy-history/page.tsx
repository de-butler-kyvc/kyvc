"use client";

import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

const deployHistory = [
  { date: "2025.05.02 14:30", item: "신뢰도 임계치", content: "0.75 → 0.80", by: "admin_core", status: "승인" },
  { date: "2025.05.01 10:00", item: "프롬프트 버전", content: "v2.1 → v2.2", by: "admin_core", status: "승인" },
  { date: "2025.04.28 09:15", item: "XRPL 네트워크", content: "testnet → mainnet", by: "admin_core", status: "검토중" },
];

export default function DeployHistoryPage() {
  return (
    <div>
      <PageHeader breadcrumb="버전 / 배포 > 배포 이력" title="코어 배포 이력 조회" />

      <div className="mb-2">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">설정값</p>
        </div>
        <div className="p-5">
          <div className="grid grid-cols-3 gap-4 mb-4">
            {[
              { label: "AI 모듈", ver: "2.2.0", env: "전체" },
              { label: "VC 모듈", ver: "1.5.2", env: "mainnet" },
              { label: "XRPL 모듈", ver: "3.0.9", env: "testnet", rollback: true },
            ].map((m) => (
              <div key={m.label} className="border border-slate-200 rounded-lg p-3">
                <p className="text-xs text-slate-500 mb-1">{m.label}</p>
                <p className="text-sm font-mono font-bold text-slate-700">{m.ver}</p>
                <p className="text-xs text-slate-400">{m.env}</p>
                {m.rollback && <StatusBadge status="롤백" />}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">변경 이력</p>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["변경 일시", "변경 항목", "변경 내용", "처리자", "상태"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {deployHistory.map((row, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs text-slate-500">{row.date}</td>
                <td className="px-5 py-3 text-xs text-slate-700">{row.item}</td>
                <td className="px-5 py-3 text-xs font-mono text-slate-600">{row.content}</td>
                <td className="px-5 py-3 text-xs text-slate-500">{row.by}</td>
                <td className="px-5 py-3"><StatusBadge status={row.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
