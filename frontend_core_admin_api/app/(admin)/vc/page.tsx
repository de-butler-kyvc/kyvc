"use client";

import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

const vcStatus = [
  { label: "VC 생성", value: "정상", ok: true },
  { label: "전자서명 (XRPL)", value: "정상", ok: true },
  { label: "XRPL 기록", value: "지연 142ms", ok: false },
  { label: "모바일 전달", value: "정상", ok: true },
  { label: "오늘 발급 건수", value: "168건", plain: true },
];

export default function VcPage() {
  return (
    <div>
      <PageHeader breadcrumb="VC 발급" title="VC 발급 코어 상태" />

      <div className="mb-2">
        <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
          <p className="text-xs font-semibold text-slate-600 mb-3">설정값</p>
          <div className="space-y-2.5">
            {vcStatus.map((s) => (
              <div key={s.label} className="flex items-center justify-between">
                <span className="text-xs text-slate-500">{s.label}</span>
                {s.plain ? (
                  <span className="text-xs font-semibold text-slate-700">{s.value}</span>
                ) : (
                  <StatusBadge status={s.ok ? "정상" : "지연"} />
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
