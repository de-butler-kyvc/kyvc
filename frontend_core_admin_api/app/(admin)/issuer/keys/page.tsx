"use client";

import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

const keys = [
  { id: "KMS-KEY-ISSUER-001", type: "Ed25519", expiry: "2026.01.15", accessible: true, status: "정상" },
  { id: "KMS-KEY-ISSUER-002", type: "Ed25519", expiry: "2025.07.15", accessible: true, status: "만료 임박" },
];

export default function IssuerKeysPage() {
  return (
    <div>
      <PageHeader breadcrumb="Issuer / 키 > 키 참조 상태 조회" title="키 참조 상태 조회" />

            <div className="bg-white rounded-lg border border-slate-200 mb-4">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">설정값</p>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["키 참조 ID", "키 유형", "만료일", "접근 가능", "상태"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {keys.map((k) => (
              <tr key={k.id} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs font-mono text-slate-700">{k.id}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{k.type}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{k.expiry}</td>
                <td className="px-5 py-3"><StatusBadge status="가능" /></td>
                <td className="px-5 py-3"><StatusBadge status={k.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
