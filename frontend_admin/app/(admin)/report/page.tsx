"use client";
import { useState, useEffect } from "react";
import { getOperationsReport, exportOperationsReport, type OperationsReport } from "@/lib/api/reports";

export default function ReportPage() {
  const [report, setReport] = useState<OperationsReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [from, setFrom] = useState("2025-03-01");
  const [to, setTo] = useState("2025-05-31");

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getOperationsReport({ from, to, granularity: "monthly" });
      setReport(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "리포트를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchReport(); }, []);

  const downloadReport = async (format: "csv" | "xlsx" | "pdf") => {
    try {
      const { blob, fileName } = await exportOperationsReport({
        from,
        to,
        granularity: "monthly",
        format,
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName || `report-${from}-${to}.${format}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : "리포트 내보내기에 실패했습니다.");
    }
  };

  const kycTotal = report?.kycTotal ?? 0;
  const kycApproved = report?.kycApproved ?? 0;
  const kycRejected = report?.kycRejected ?? 0;
  const vcIssued = report?.vcIssued ?? 0;
  const vpVerified = report?.vpVerified ?? 0;
  const vpFailed = report?.vpFailed ?? 0;
  const vpTotal = vpVerified + vpFailed;
  const autoApprovalRate = kycTotal > 0 ? ((kycApproved / kycTotal) * 100).toFixed(1) + "%" : "-";

  const cards = [
    { label: "KYC 처리 건수", value: kycTotal.toLocaleString(), color: "text-blue-600" },
    { label: "자동 승인율", value: autoApprovalRate, color: "text-green-600" },
    { label: "VC 발급 건수", value: vcIssued.toLocaleString(), color: "text-slate-700" },
    { label: "VP 검증 건수", value: vpTotal.toLocaleString(), color: "text-purple-600" },
  ];

  const kycStats = [
    { label: "승인", value: kycApproved, color: "bg-green-500", total: kycTotal || 1 },
    { label: "반려", value: kycRejected, color: "bg-red-500", total: kycTotal || 1 },
    { label: "기타", value: Math.max(0, kycTotal - kycApproved - kycRejected), color: "bg-blue-500", total: kycTotal || 1 },
  ];

  const vpStats = [
    { label: "검증 성공", value: vpVerified, color: "bg-green-500", total: vpTotal || 1 },
    { label: "검증 실패", value: vpFailed, color: "bg-red-500", total: vpTotal || 1 },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">운영 리포트</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="bg-white rounded-lg border border-slate-200 p-4 flex items-center gap-3">
        <div className="flex items-center gap-2">
          <label className="text-xs text-slate-500">시작</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none" />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-xs text-slate-500">종료</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none" />
        </div>
        <button onClick={fetchReport} disabled={loading} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60">{loading ? "조회 중..." : "조회"}</button>
        <div className="ml-auto flex gap-2">
          <button onClick={() => downloadReport("pdf")} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">PDF 내보내기</button>
          <button onClick={() => downloadReport("csv")} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">Excel 내보내기</button>
        </div>
      </div>

      {loading ? (
        <div className="p-8 text-center text-slate-400">로딩 중...</div>
      ) : (
        <>
          <div className="grid grid-cols-4 gap-4">
            {cards.map((card) => (
              <div key={card.label} className="bg-white rounded-lg border border-slate-200 p-5">
                <p className="text-xs text-slate-500">{card.label}</p>
                <p className={`text-3xl font-bold mt-1 ${card.color}`}>{card.value}</p>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white rounded-lg border border-slate-200 p-5">
              <h2 className="text-sm font-semibold text-slate-700 mb-4">KYC 상태별 분포</h2>
              <div className="space-y-3">
                {kycStats.map((item) => (
                  <div key={item.label} className="flex items-center gap-3">
                    <span className="text-xs text-slate-500 w-16 shrink-0">{item.label}</span>
                    <div className="flex-1 bg-slate-100 rounded-full h-4 overflow-hidden">
                      <div className={`h-4 rounded-full ${item.color}`} style={{ width: `${(item.value / item.total) * 100}%` }} />
                    </div>
                    <span className="text-xs text-slate-600 w-10 text-right shrink-0">{item.value}건</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="bg-white rounded-lg border border-slate-200 p-5">
              <h2 className="text-sm font-semibold text-slate-700 mb-4">VP 검증 현황</h2>
              <div className="space-y-3">
                {vpStats.map((item) => (
                  <div key={item.label} className="flex items-center gap-3">
                    <span className="text-xs text-slate-500 w-16 shrink-0">{item.label}</span>
                    <div className="flex-1 bg-slate-100 rounded-full h-4 overflow-hidden">
                      <div className={`h-4 rounded-full ${item.color}`} style={{ width: `${(item.value / item.total) * 100}%` }} />
                    </div>
                    <span className="text-xs text-slate-600 w-14 text-right shrink-0">{item.value.toLocaleString()}건</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </>
      )}

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
