"use client";
import { useState, useEffect } from "react";
import { getVerifierList, getVerifierUsageStats, type Verifier, type VerifierUsageStats } from "@/lib/api/verifier";

const statusBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  미사용: "bg-slate-100 text-slate-400",
  오류: "bg-red-100 text-red-600",
};

interface UsageRow {
  verifier: string;
  totalRequests: number;
  successCount: number;
  failCount: number;
  lastUsedAt: string;
  status: string;
}

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

export default function SdkUsagePage() {
  const [rows, setRows] = useState<UsageRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const verifiers = await getVerifierList();
        const stats = await Promise.allSettled(
          verifiers.map((v) => getVerifierUsageStats(v.verifierId).then((s) => ({ v, s })))
        );
        const result: UsageRow[] = stats.map((r, i) => {
          const v = verifiers[i];
          if (r.status === "fulfilled") {
            const s = r.value.s;
            const total = s.totalRequests ?? 0;
            const success = s.successCount ?? 0;
            const fail = s.failCount ?? 0;
            return {
              verifier: v.name,
              totalRequests: total,
              successCount: success,
              failCount: fail,
              lastUsedAt: fmtDt(s.lastUsedAt),
              status: total === 0 ? "미사용" : fail > success * 0.1 ? "오류" : "정상",
            };
          }
          return { verifier: v.name, totalRequests: 0, successCount: 0, failCount: 0, lastUsedAt: "-", status: "미사용" };
        });
        setRows(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : "데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const totalCalls = rows.reduce((s, r) => s + r.totalRequests, 0);
  const totalSuccess = rows.reduce((s, r) => s + r.successCount, 0);
  const totalFail = rows.reduce((s, r) => s + r.failCount, 0);
  const successRate = totalCalls > 0 ? ((totalSuccess / totalCalls) * 100).toFixed(1) + "%" : "-";

  if (loading) return <div className="p-8 text-center text-slate-400">로딩 중...</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · SDK 관리</p>
          <h1 className="text-xl font-bold text-slate-800">SDK 사용 현황 조회</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="grid grid-cols-4 gap-4">
        {[
          { label: "전체 호출", value: totalCalls.toLocaleString(), color: "text-slate-700" },
          { label: "성공", value: totalSuccess.toLocaleString(), color: "text-green-600" },
          { label: "실패", value: totalFail.toLocaleString(), color: "text-red-500" },
          { label: "성공률", value: successRate, color: "text-blue-600" },
        ].map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs text-slate-400">{card.label}</p>
            <p className={`text-2xl font-bold mt-1 ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">Verifier별 SDK 호출 현황</h2>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50">
              <th className="text-left px-4 py-3 text-slate-500 font-medium">Verifier</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">전체 호출</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">성공</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">실패</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">마지막 호출</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400 text-sm">데이터가 없습니다.</td></tr>
            ) : rows.map((row, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                <td className="px-4 py-3 text-slate-700 font-medium">{row.verifier}</td>
                <td className="px-4 py-3 text-slate-700 font-semibold">{row.totalRequests.toLocaleString()}</td>
                <td className="px-4 py-3 text-green-600">{row.successCount.toLocaleString()}</td>
                <td className={`px-4 py-3 font-medium ${row.failCount > 0 ? "text-red-500" : "text-slate-400"}`}>{row.failCount}</td>
                <td className="px-4 py-3 text-slate-400 text-xs">{row.lastUsedAt}</td>
                <td className="px-4 py-3"><span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status] ?? "bg-slate-100 text-slate-500"}`}>{row.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
