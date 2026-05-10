"use client";

import { getAuditLogs } from "@/lib/api/audit";
import { useState, useEffect } from "react";

const resultBadge: Record<string, string> = {
  "성공": "bg-green-100 text-green-600",
  "권한 오류": "bg-red-100 text-red-600",
};

export default function AuditLogPage() {
  const [logs, setLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [actionFilter, setActionFilter] = useState("전체 액션 유형");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const ITEMS_PER_PAGE = 3;

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const data = await getAuditLogs({
        search: searchTerm,
        action: actionFilter,
      });
      setLogs(data);
    } catch (error) {
      console.error("Failed to fetch audit logs:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchLogs();
  };

  const handleReset = () => {
    setSearchTerm("");
    setActionFilter("전체 액션 유형");
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchLogs();
  };

  const totalPages = Math.max(1, Math.ceil(logs.length / ITEMS_PER_PAGE));
  const paginatedList = logs.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);
  const allSelected = paginatedList.length > 0 && paginatedList.every((row: any) => selectedIds.has(row.id));
  const toggleAll = () => setSelectedIds(allSelected ? new Set() : new Set(paginatedList.map((row: any) => row.id)));
  const toggleRow = (id: string) => setSelectedIds(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });

  const handleExportCsv = () => {
    const target = selectedIds.size > 0 ? logs.filter(r => selectedIds.has(r.id)) : logs;
    const header = ["로그ID", "일시", "처리자", "액션유형", "대상", "변경내용", "IP", "결과"];
    const rows = target.map(r => [r.id, r.date, r.actor, r.action, r.target, r.content, r.ip, r.result]);
    const csv = [header, ...rows].map(r => r.map(v => `"${v}"`).join(",")).join("\n");
    const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `audit-log-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">백엔드 감사로그 조회</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        {/* 필터 */}
        <div className="flex items-center gap-2 p-4 border-b border-slate-100 flex-wrap">
          <input
            type="text"
            placeholder="사용자 ID / 법인명"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-44 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 액션 유형</option>
            <option>수동심사</option>
            <option>정책 변경</option>
            <option>VC 발급</option>
            <option>보완요청</option>
            <option>KYC 조회</option>
          </select>
          <button
            onClick={handleSearch}
            className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700"
          >
            검색
          </button>
          <button
            onClick={handleReset}
            className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50"
          >
            초기화
          </button>
          <label className="flex items-center gap-1.5 text-xs text-slate-500 cursor-pointer select-none ml-1 pl-3 border-l border-slate-200">
            <input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" />
            전체 선택
          </label>
          <button onClick={handleExportCsv} className="ml-auto border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">CSV 내보내기</button>
        </div>

        {/* 테이블 */}
        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 w-10"><input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" /></th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">로그 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">일시</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">처리자</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">액션 유형</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">대상</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">변경 내용</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">IP</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">결과</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${selectedIds.has(row.id) ? "bg-blue-50" : ""}`}>
                  <td className="px-4 py-3 w-10"><input type="checkbox" checked={selectedIds.has(row.id)} onChange={() => toggleRow(row.id)} className="accent-blue-600 cursor-pointer" /></td>
                  <td className="px-4 py-3 text-slate-600 font-medium text-xs">{row.id}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.date}</td>
                  <td className="px-4 py-3 text-slate-700">{row.actor}</td>
                  <td className="px-4 py-3 text-slate-500">{row.action}</td>
                  <td className="px-4 py-3 text-blue-600 text-xs">{row.target}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.content}</td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{row.ip}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${resultBadge[row.result] ?? "bg-slate-100 text-slate-500"}`}>{row.result}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {logs.length}건{selectedIds.size > 0 && <span className="ml-2 text-blue-600 font-medium">{selectedIds.size}개 선택됨</span>}</span>
          <div className="flex gap-1">
            <button onClick={() => setCurrentPage(p => Math.max(1, p - 1))} disabled={currentPage === 1} className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40">‹</button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
              <button key={page} onClick={() => setCurrentPage(page)} className={`w-7 h-7 rounded text-xs ${currentPage === page ? "bg-blue-600 text-white" : "border border-slate-200 hover:bg-slate-50"}`}>{page}</button>
            ))}
            <button onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))} disabled={currentPage === totalPages} className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40">›</button>
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