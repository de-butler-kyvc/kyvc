"use client";

import { getVcList, type VcListItem } from "@/lib/api/vc";
import { vcDetailPath } from "@/lib/navigation/admin-routes";
import Link from "next/link";
import { useEffect, useState } from "react";

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  폐기: "bg-red-100 text-red-600",
  만료: "bg-slate-100 text-slate-500",
  보류: "bg-orange-100 text-orange-600",
};

const ITEMS_PER_PAGE = 3;

export default function VcPage() {
  const [vcList, setVcList] = useState<VcListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const fetchVcList = async (filters = { search: searchTerm, status: statusFilter }) => {
    setLoading(true);
    setError("");
    try {
      const data = await getVcList(filters);
      setVcList(data);
    } catch (err) {
      setVcList([]);
      setError(err instanceof Error ? err.message : "VC 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVcList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchVcList({ search: searchTerm, status: statusFilter });
  };

  const handleReset = () => {
    const resetFilters = { search: "", status: "전체 상태" };
    setSearchTerm(resetFilters.search);
    setStatusFilter(resetFilters.status);
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchVcList(resetFilters);
  };

  const totalPages = Math.max(1, Math.ceil(vcList.length / ITEMS_PER_PAGE));
  const paginatedList = vcList.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);
  const allSelected = paginatedList.length > 0 && paginatedList.every((row) => selectedIds.has(row.id));

  const toggleAll = () => {
    setSelectedIds(allSelected ? new Set() : new Set(paginatedList.map((row) => row.id)));
  };

  const toggleRow = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">VC 발급 상태 조회</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 p-4 border-b border-slate-100 flex-wrap">
          <input
            type="text"
            placeholder="법인명 / Credential ID"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-48 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 상태</option>
            <option>활성</option>
            <option>폐기</option>
            <option>만료</option>
            <option>보류</option>
          </select>
          <button onClick={handleSearch} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">
            조회
          </button>
          <label className="flex items-center gap-1.5 text-xs text-slate-500 cursor-pointer select-none ml-1 pl-3 border-l border-slate-200">
            <input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" />
            목록 선택
          </label>
          <button onClick={handleReset} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">
            초기화
          </button>
        </div>

        {error && (
          <div className="mx-4 mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 w-10">
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" />
                </th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">VC ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">법인명</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Credential ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Credential Type</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">발급일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">만료일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr
                  key={row.id}
                  className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${selectedIds.has(row.id) ? "bg-blue-50" : ""}`}
                >
                  <td className="px-4 py-3 w-10">
                    <input
                      type="checkbox"
                      checked={selectedIds.has(row.id)}
                      onChange={() => toggleRow(row.id)}
                      className="accent-blue-600 cursor-pointer"
                    />
                  </td>
                  <td className="px-4 py-3 text-blue-600 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.corp}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs font-mono">{row.credId}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.type}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.issuedAt}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.expiresAt}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status] ?? "bg-slate-100 text-slate-500"}`}>
                      {row.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <Link href={vcDetailPath(row.id)} className="text-blue-600 hover:underline text-xs">
                      상세 보기
                    </Link>
                  </td>
                </tr>
              ))}
              {!paginatedList.length && (
                <tr>
                  <td colSpan={9} className="px-4 py-8 text-center text-slate-400">
                    조회된 VC가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">
            총 {vcList.length}건
            {selectedIds.size > 0 && <span className="ml-2 text-blue-600 font-medium">{selectedIds.size}개 선택됨</span>}
          </span>
          <div className="flex gap-1">
            <button
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              disabled={currentPage === 1}
              className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40"
            >
              {"<"}
            </button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`w-7 h-7 rounded text-xs ${currentPage === page ? "bg-blue-600 text-white" : "border border-slate-200 hover:bg-slate-50"}`}
              >
                {page}
              </button>
            ))}
            <button
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
              disabled={currentPage === totalPages}
              className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40"
            >
              {">"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
