"use client";

import { useState } from "react";
import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";

interface PendingItem {
  item: string;
  content: string;
  requester: string;
  requestedAt: string;
  status: string;
}

const initialItems: PendingItem[] = [
  { item: "AI 모델",        content: "gpt-4o → gpt-4-turbo", requester: "admin_core", requestedAt: "2025.05.02", status: "대기" },
  { item: "신뢰도 임계치", content: "0.75 → 0.80",           requester: "admin_core", requestedAt: "2025.05.01", status: "승인" },
];

export default function ApprovalPage() {
  const [items, setItems] = useState<PendingItem[]>(initialItems);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const handleApprove = (index: number) => {
    if (!window.confirm(`"${items[index].item}" 변경을 승인하시겠습니까?`)) return;
    setItems((prev) =>
      prev.map((it, i) => (i === index ? { ...it, status: "승인" } : it))
    );
    showToast("승인 처리되었습니다.");
  };

  const handleReject = (index: number) => {
    if (!window.confirm(`"${items[index].item}" 변경을 반려하시겠습니까?`)) return;
    setItems((prev) =>
      prev.map((it, i) => (i === index ? { ...it, status: "반려" } : it))
    );
    showToast("반려 처리되었습니다.", "error");
  };

  return (
    <div className="relative">
      {/* 토스트 */}
      {toast && (
        <div
          className={`fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white transition-all ${
            toast.type === "success" ? "bg-emerald-600" : "bg-red-600"
          }`}
        >
          {toast.msg}
        </div>
      )}

      <PageHeader breadcrumb="설정 승인" title="코어 설정 변경 승인" />

            <div className="bg-white rounded-lg border border-slate-200 mb-4">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">설정값</p>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["변경 항목", "변경 내용", "요청자", "요청일", "이력", "처리"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {items.map((item, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs text-slate-700">{item.item}</td>
                <td className="px-5 py-3 text-xs font-mono text-slate-600">{item.content}</td>
                <td className="px-5 py-3 text-xs text-slate-500">{item.requester}</td>
                <td className="px-5 py-3 text-xs text-slate-500">{item.requestedAt}</td>
                <td className="px-5 py-3"><StatusBadge status={item.status} /></td>
                <td className="px-5 py-3">
                  {item.status === "대기" ? (
                    <div className="flex gap-1.5">
                      <button
                        onClick={() => handleApprove(i)}
                        className="bg-emerald-600 text-white text-[11px] px-2.5 py-1 rounded hover:bg-emerald-700 transition-colors"
                      >
                        승인
                      </button>
                      <button
                        onClick={() => handleReject(i)}
                        className="bg-red-100 text-red-600 text-[11px] px-2.5 py-1 rounded hover:bg-red-200 transition-colors"
                      >
                        반려
                      </button>
                    </div>
                  ) : (
                    <span className="text-xs text-slate-400">-</span>
                  )}
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
