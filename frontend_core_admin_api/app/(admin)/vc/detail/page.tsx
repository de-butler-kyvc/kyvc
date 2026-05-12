"use client";

import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

export default function VcDetailPage() {
  return (
    <div>
      <PageHeader breadcrumb="VC 발급 > VC 발급 요청 상세" title="VC 발급 요청 상세" />

            <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <p className="text-xs font-semibold text-slate-600 mb-3">설정값</p>
        <div className="space-y-2.5">
          {[
            { label: "VC ID", value: "vc:kyvc:2025:KYC-001" },
            { label: "Issuer", value: "did:kyvc:issuer:001" },
            { label: "Subject", value: "did:kyvc:holder:abc123" },
            { label: "Schema", value: "KYC-VC-Schema-v1.3" },
          ].map((item) => (
            <div key={item.label} className="flex items-center justify-between">
              <span className="text-xs text-slate-500">{item.label}</span>
              <span className="text-xs font-mono text-slate-700">{item.value}</span>
            </div>
          ))}
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-500">서명 상태</span>
            <StatusBadge status="성공" />
          </div>
        </div>
      </div>

      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
