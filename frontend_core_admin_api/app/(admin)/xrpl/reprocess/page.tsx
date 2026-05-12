"use client";

import { useState } from "react";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

interface ReprocessRow {
  txId: string;
  reason: string;
  retryCount: number;
  lastTried: string;
  processing: boolean;
}

const initialRows: ReprocessRow[] = [
  { txId: "B7E2A1F4C3D8...", reason: "응답 타임아웃", retryCount: 2, lastTried: "2025.05.02 16:28", processing: false },
];

export default function XrplReprocessPage() {
  const [rows, setRows] = useState<ReprocessRow[]>(initialRows);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const handleReprocess = async (index: number) => {
    if (!window.confirm(`트랜잭션 ${rows[index].txId}을 재처리하시겠습니까?`)) return;

    setRows((prev) => prev.map((r, i) => i === index ? { ...r, processing: true } : r));
    try {
      // TODO: await fetch(`/api/xrpl/reprocess/${rows[index].txId}`, { method: 'POST' })
      await new Promise((r) => setTimeout(r, 800));
      const now = new Date().toLocaleString("ko-KR", { hour12: false }).replace(/\./g, ".").slice(0, 16);
      setRows((prev) =>
        prev.map((r, i) =>
          i === index ? { ...r, processing: false, retryCount: r.retryCount + 1, lastTried: now } : r
        )
      );
      showToast("재처리 요청이 전송되었습니다.");
    } catch {
      setRows((prev) => prev.map((r, i) => i === index ? { ...r, processing: false } : r));
      showToast("재처리 요청에 실패했습니다.", "error");
    }
  };

  return (
    <div className="relative">
      {toast && (
        <div className={`fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white transition-all ${toast.type === "success" ? "bg-emerald-600" : "bg-red-600"}`}>
          {toast.msg}
        </div>
      )}

      <PageHeader breadcrumb="XRPL > 재처리 관리" title="XRPL 재처리 관리" />

      <div className="bg-white rounded-lg border border-slate-200 p-4 mb-4">
        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-xs text-slate-500 mb-1">XRPL 네트워크 · Select</label>
            <select className="w-full border border-slate-300 rounded-md px-2 py-1.5 text-xs focus:outline-none">
              <option>testnet</option><option>devnet</option><option>mainnet</option>
            </select>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">노드 상태 · Badge</label>
            <div className="mt-1.5"><StatusBadge status="지연" /></div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">트랜잭션 ID</label>
            <p className="text-xs font-mono text-slate-600 mt-1.5">B7E2A1F4C3D8...</p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["트랜잭션 ID", "실패 사유", "재처리 횟수", "마지막 시도", "처리"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs font-mono text-slate-600">{row.txId}</td>
                <td className="px-5 py-3 text-xs text-slate-700">{row.reason}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{row.retryCount}회</td>
                <td className="px-5 py-3 text-xs text-slate-500">{row.lastTried}</td>
                <td className="px-5 py-3">
                  <button
                    onClick={() => handleReprocess(i)}
                    disabled={row.processing}
                    className="bg-blue-600 text-white text-xs px-3 py-1 rounded hover:bg-blue-700 transition-colors disabled:opacity-50"
                  >
                    {row.processing ? "처리 중..." : "재처리"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
