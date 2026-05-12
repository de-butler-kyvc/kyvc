"use client";

interface PageHeaderProps {
  breadcrumb: string;
  title: string;
  pageId?: string;
  actions?: React.ReactNode;
}

export function PageHeader({ breadcrumb, title, pageId, actions }: PageHeaderProps) {
  return (
    <div className="mb-4">
      <div className="flex items-center justify-between mb-1">
        <p className="text-xs text-slate-400">
          코어 어드민 &gt; <span className="text-slate-600">{breadcrumb}</span>
        </p>
        {pageId && <p className="text-xs text-slate-400">{pageId}</p>}
      </div>
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-800">{title}</h1>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
    </div>
  );
}

interface StatusBadgeProps {
  status: "정상" | "지연" | "장애" | "성공" | "실패" | "보류" | "활성" | "비활성" | "승인" | "검토중" | "대기" | "통과" | "호환" | "미호환" | "만료 임박" | "가능" | "지원" | string;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const colorMap: Record<string, string> = {
    정상: "bg-emerald-100 text-emerald-700",
    성공: "bg-emerald-100 text-emerald-700",
    활성: "bg-emerald-100 text-emerald-700",
    승인: "bg-emerald-100 text-emerald-700",
    통과: "bg-emerald-100 text-emerald-700",
    호환: "bg-emerald-100 text-emerald-700",
    가능: "bg-emerald-100 text-emerald-700",
    지원: "bg-emerald-100 text-emerald-700",
    "자동 승인": "bg-emerald-100 text-emerald-700",
    "자동 거절": "bg-red-100 text-red-700",
    "수동 심사": "bg-yellow-100 text-yellow-700",
    지연: "bg-yellow-100 text-yellow-700",
    보류: "bg-yellow-100 text-yellow-700",
    검토중: "bg-yellow-100 text-yellow-700",
    대기: "bg-yellow-100 text-yellow-700",
    "만료 임박": "bg-yellow-100 text-yellow-700",
    장애: "bg-red-100 text-red-700",
    실패: "bg-red-100 text-red-700",
    비활성: "bg-slate-100 text-slate-500",
    미호환: "bg-red-100 text-red-700",
  };
  const cls = colorMap[status] ?? "bg-slate-100 text-slate-500";
  return (
    <span className={`text-[11px] font-semibold px-2 py-0.5 rounded ${cls}`}>
      {status}
    </span>
  );
}

export function ModuleBadges({ modules = ["AI", "VC", "VP", "XRPL", "SDK"] }: { modules?: string[] }) {
  const colorMap: Record<string, string> = {
    AI: "bg-blue-100 text-blue-700",
    VC: "bg-emerald-100 text-emerald-700",
    VP: "bg-purple-100 text-purple-700",
    XRPL: "bg-yellow-100 text-yellow-700",
    SDK: "bg-cyan-100 text-cyan-700",
  };
  return (
    <div className="flex items-center gap-1.5 mb-3">
      {modules.map((m) => (
        <span key={m} className={`text-[11px] font-semibold px-2 py-0.5 rounded ${colorMap[m] ?? "bg-slate-100 text-slate-500"}`}>
          {m}
        </span>
      ))}
    </div>
  );
}

interface ChangeHistoryRow {
  date: string;
  item: string;
  content: string;
  by: string;
  status: string;
}

export function ChangeHistoryTable({ rows }: { rows: ChangeHistoryRow[] }) {
  return (
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
          {rows.map((row, i) => (
            <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
              <td className="px-5 py-3 text-xs text-slate-500">{row.date}</td>
              <td className="px-5 py-3 text-xs text-slate-700">{row.item}</td>
              <td className="px-5 py-3 text-xs text-slate-600 font-mono">{row.content}</td>
              <td className="px-5 py-3 text-xs text-slate-500">{row.by}</td>
              <td className="px-5 py-3"><StatusBadge status={row.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const defaultChangeRows: ChangeHistoryRow[] = [
  { date: "2025.05.02 14:30", item: "신뢰도 임계치", content: "0.75 → 0.80", by: "admin_core", status: "승인" },
  { date: "2025.05.01 10:00", item: "프롬프트 버전", content: "v2.1 → v2.2", by: "admin_core", status: "승인" },
  { date: "2025.04.28 09:15", item: "XRPL 네트워크", content: "testnet → mainnet", by: "admin_core", status: "검토중" },
];

export { defaultChangeRows };
export type { ChangeHistoryRow };
