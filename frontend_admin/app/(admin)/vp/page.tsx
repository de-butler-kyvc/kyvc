"use client";

import { getVpList } from "@/lib/api/vp";
import { useState, useEffect } from "react";

const resultBadge: Record<string, string> = {
  "성공": "bg-green-100 text-green-600",
  "실패": "bg-red-100 text-red-600",
  "만료": "bg-orange-100 text-orange-600",
};

export default function VpPage() {
  const [vpList, setVpList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [resultFilter, setResultFilter] = useState("전체 검증 결과");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const ITEMS_PER_PAGE = 3;

  const fetchVpList = async () => {
    setLoading(true);
    try {
      const data = await getVpList({
        search: searchTerm,
        result: resultFilter,
      });
      setVpList(data);
    } catch (error) {
      console.error("Failed to fetch VP list:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVpList();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchVpList();
  };

  const handleReset = () => {
    setSearchTerm("");
    setResultFilter("전체 검증 결과");
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchVpList();
  };

  const totalPages = Math.max(1, Math.ceil(vpList.length / ITEMS_PER_PAGE));
  const paginatedList = vpList.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);
  const allSelected = paginatedList.length > 0 && paginatedList.every((row: any) => selectedIds.has(row.id));
  const toggleAll = () => setSelectedIds(allSelected ? new Set() : new Set(paginatedList.map((row: any) => row.id)));
  const toggleRow = (id: string) => setSelectedIds(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">VP 검증 이력 조회</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 p-4 border-b border-slate-100 flex-wrap">
          <input
            type="text"
            placeholder="법인명 / Verifier명"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-44 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={resultFilter}
            onChange={(e) => setResultFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 검증 결과</option>
            <option>성공</option>
            <option>실패</option>
            <option>만료</option>
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
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 w-10"><input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" /></th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">검증 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">법인명</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Verifier</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">제출 목적</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">사용 VC</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">검증 결과</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">실패 사유</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">검증 일시</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${selectedIds.has(row.id) ? "bg-blue-50" : ""}`}>
                  <td className="px-4 py-3 w-10"><input type="checkbox" checked={selectedIds.has(row.id)} onChange={() => toggleRow(row.id)} className="accent-blue-600 cursor-pointer" /></td>
                  <td className="px-4 py-3 text-blue-600 font-medium text-xs">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.corp}</td>
                  <td className="px-4 py-3 text-slate-500">{row.verifier}</td>
                  <td className="px-4 py-3 text-slate-500">{row.purpose}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.vc}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${resultBadge[row.result]}`}>{row.result}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{row.reason}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.date}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {vpList.length}건{selectedIds.size > 0 && <span className="ml-2 text-blue-600 font-medium">{selectedIds.size}개 선택됨</span>}</span>
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