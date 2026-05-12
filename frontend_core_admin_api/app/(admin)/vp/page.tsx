"use client";

import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

const vpStatus = [
  { label: "VP 형식 검증", ok: true },
  { label: "VC 서명 검증", ok: true },
  { label: "상태 조회", ok: true },
  { label: "Issuer 확인", ok: true },
];

export default function VpPage() {
  return (
    <div>
      <PageHeader breadcrumb="VP 검증" title="VP 검증 코어 상태" />

      <div className="mb-2">
        <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
          <p className="text-xs font-semibold text-slate-600 mb-3">설정값</p>
          <div className="space-y-2.5">
            {vpStatus.map((s) => (
              <div key={s.label} className="flex items-center justify-between">
                <span className="text-xs text-slate-500">{s.label}</span>
                <StatusBadge status="정상" />
              </div>
            ))}
            <div className="flex items-center justify-between">
              <span className="text-xs text-slate-500">오늘 검증 건수</span>
              <span className="text-xs font-semibold text-slate-700">3,847건</span>
            </div>
          </div>
        </div>
      </div>

      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
