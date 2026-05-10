"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import { getReviewHistories, type ReviewHistory } from "@/lib/api/kyc";

const ACTION_TYPE_KO: Record<string, string> = {
  AI: "AI", MANUAL: "수동", SYSTEM: "시스템",
};

const typeBadge: Record<string, string> = {
  AI: "bg-blue-100 text-blue-600",
  수동: "bg-purple-100 text-purple-600",
  시스템: "bg-slate-100 text-slate-500",
};

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

export default function ReviewHistoryPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [history, setHistory] = useState<ReviewHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getReviewHistories(id)
      .then(setHistory)
      .catch((err) => setError(err instanceof Error ? err.message : "데이터를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [id]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">심사 이력 조회</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 요약 */}
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
          <h2 className="text-xs font-semibold text-slate-500">심사 이력 조회</h2>
          {[
            { label: "신청번호", value: id },
          ].map((item) => (
            <div key={item.label}>
              <p className="text-xs text-slate-400">{item.label}</p>
              <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
            </div>
          ))}
          <div>
            <p className="text-xs text-slate-400">총 이력</p>
            <p className="text-slate-700 text-xs font-semibold mt-0.5">
              {loading ? "…" : `${history.length}건`}
            </p>
          </div>
        </div>

        {/* 우측 이력 테이블 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">전체 심사 이력</h2>
          </div>

          {loading ? (
            <div className="px-5 py-10 text-center text-slate-500 text-sm">불러오는 중...</div>
          ) : error ? (
            <div className="px-5 py-4 text-sm text-red-500">{error}</div>
          ) : history.length === 0 ? (
            <div className="px-5 py-10 text-center text-slate-400 text-sm">심사 이력이 없습니다.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">일시</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">처리 내용</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">처리자</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">유형</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">상세</th>
                </tr>
              </thead>
              <tbody>
                {history.map((row, i) => {
                  const typeKo = ACTION_TYPE_KO[row.actionType] ?? row.actionType;
                  return (
                    <tr key={row.historyId ?? i} className="border-b border-slate-50 hover:bg-slate-50">
                      <td className="px-5 py-3.5 text-slate-400 text-xs whitespace-nowrap">
                        {fmtDt(row.actionDate)}
                      </td>
                      <td className="px-5 py-3.5 text-slate-700 font-medium">{row.actionContent}</td>
                      <td className="px-5 py-3.5 text-slate-500 text-xs">{row.actorName ?? "-"}</td>
                      <td className="px-5 py-3.5">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${typeBadge[typeKo] ?? "bg-slate-100 text-slate-500"}`}>
                          {typeKo}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-slate-400 text-xs">{row.detail ?? "-"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          <div className="flex items-center justify-between px-5 py-3">
            <span className="text-xs text-slate-400">
              {loading ? "" : `총 ${history.length}건`}
            </span>
            <Link href={`/kyc/${id}`} className="text-xs text-blue-600 hover:underline">
              ← 신청 상세로
            </Link>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
