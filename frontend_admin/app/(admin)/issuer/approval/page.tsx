"use client";
import { useState, useEffect } from "react";
import { getIssuerList, approveIssuerPolicy, rejectIssuerPolicy } from "@/lib/api/issuer";
import type { IssuerItem } from "@/types/kyc";

export default function IssuerApprovalPage() {
  const [list, setList] = useState<IssuerItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [comment, setComment] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const fetchList = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getIssuerList({ status: "심사중" });
      setList(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchList(); }, []);

  const handleAction = async (id: string, action: "승인" | "반려") => {
    setActionLoading(true);
    setError(null);
    try {
      const policyId = parseInt(id.replace(/\D/g, ""), 10) || 0;
      if (action === "승인") {
        await approveIssuerPolicy(policyId, { comment });
      } else {
        await rejectIssuerPolicy(policyId, { rejectReason: comment || "반려" });
      }
      setSelected(null);
      setComment("");
      fetchList();
    } catch (err) {
      setError(err instanceof Error ? err.message : `${action}에 실패했습니다.`);
    } finally {
      setActionLoading(false);
    }
  };

  const selectedItem = list.find((p) => p.id === selected);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">증명서 관리자 · Issuer</p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 신뢰정책 승인</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="flex gap-4">
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-700">승인 대기 목록</h2>
            <span className="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full font-medium">{list.length}건 대기</span>
          </div>
          {loading ? (
            <div className="p-8 text-center text-slate-400 text-sm">로딩 중...</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 ID</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Issuer DID</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">유형</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">기간</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">처리</th>
                </tr>
              </thead>
              <tbody>
                {list.length === 0 ? (
                  <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-400 text-sm">승인 대기 건이 없습니다.</td></tr>
                ) : list.map((row) => (
                  <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 cursor-pointer ${selected === row.id ? "bg-blue-50" : ""}`} onClick={() => setSelected(row.id)}>
                    <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                    <td className="px-4 py-3 text-blue-600 text-xs">{row.did}</td>
                    <td className="px-4 py-3"><span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded font-medium">{row.type}</span></td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{row.period}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1.5">
                        <button onClick={(e) => { e.stopPropagation(); handleAction(row.id, "승인"); }} disabled={actionLoading} className="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 disabled:opacity-60">승인</button>
                        <button onClick={(e) => { e.stopPropagation(); handleAction(row.id, "반려"); }} disabled={actionLoading} className="border border-red-200 text-red-500 px-3 py-1 rounded text-xs hover:bg-red-50 disabled:opacity-60">반려</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {selected && selectedItem && (
          <div className="w-72 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-4 h-fit">
            <h2 className="text-sm font-semibold text-slate-700">정책 상세</h2>
            <div className="space-y-2 text-xs">
              {[
                { label: "정책 ID", value: selectedItem.id },
                { label: "Issuer DID", value: selectedItem.did },
                { label: "정책 유형", value: selectedItem.type },
                { label: "Credential Type", value: selectedItem.credential },
                { label: "적용 기간", value: selectedItem.period },
              ].map((i) => (
                <div key={i.label}>
                  <p className="text-slate-400">{i.label}</p>
                  <p className="text-slate-700 font-medium mt-0.5 break-all">{i.value}</p>
                </div>
              ))}
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">승인/반려 코멘트 (선택)</label>
              <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} className="w-full border border-slate-200 rounded px-3 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none" placeholder="코멘트 입력..." />
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleAction(selectedItem.id, "승인")} disabled={actionLoading} className="flex-1 bg-blue-600 text-white py-2 rounded text-sm hover:bg-blue-700 disabled:opacity-60">승인</button>
              <button onClick={() => handleAction(selectedItem.id, "반려")} disabled={actionLoading} className="flex-1 border border-red-200 text-red-500 py-2 rounded text-sm hover:bg-red-50 disabled:opacity-60">반려</button>
            </div>
          </div>
        )}
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC 증명서 관리자 · 증명서 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
