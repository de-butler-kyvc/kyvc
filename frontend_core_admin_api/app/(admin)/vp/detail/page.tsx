"use client";

import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

export default function VpDetailPage() {
  return (
    <div>
      <PageHeader breadcrumb="VP 검증 > VP 검증 상세" title="VP 검증 상세" />
      <div className="mb-2">
        <p className="text-xs font-semibold text-slate-600 mb-3">설정값</p>
        <div className="space-y-2.5">
          {[
            { label: "VP ID", value: "vp:kyvc:2025:VP-0291" },
            { label: "Verifier", value: "did:kyvc:verifier:bank-001" },
            { label: "Holder", value: "did:kyvc:holder:abc123" },
            { label: "검증 결과", status: "성공" },
            { label: "검증 일시", value: "2025.05.02 14:30:22" },
          ].map((item) => (
            <div key={item.label} className="flex items-center justify-between">
              <span className="text-xs text-slate-500">{item.label}</span>
              {item.status ? <StatusBadge status={item.status} /> : <span className="text-xs font-mono text-slate-700">{item.value}</span>}
            </div>
          ))}
        </div>
      </div>
      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
