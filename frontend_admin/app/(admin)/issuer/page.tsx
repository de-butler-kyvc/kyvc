"use client";

import { getIssuerList } from "@/lib/api/issuer";
import Link from "next/link";
import { useState, useEffect } from "react";

const policyBadge: Record<string, string> = {
  "화이트리스트": "bg-green-100 text-green-600",
  "블랙리스트": "bg-red-100 text-red-600",
};

const statusBadge: Record<string, string> = {
  "활성": "bg-green-100 text-green-600",
  "차단": "bg-red-100 text-red-600",
  "심사중": "bg-orange-100 text-orange-600",
};

const defaultForm = { did: "", credential: "", type: "화이트리스트", scope: "", startDate: "", endDate: "" };

export default function IssuerPage() {
  const [issuerList, setIssuerList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [typeFilter, setTypeFilter] = useState("전체 정책 유형");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [form, setForm] = useState(defaultForm);
  const ITEMS_PER_PAGE = 3;

  const handleRegister = () => {
    if (!form.did || !form.credential || !form.scope || !form.startDate || !form.endDate) {
      alert("모든 항목을 입력해주세요.");
      return;
    }
    alert(`신뢰정책이 등록되었습니다.\nDID: ${form.did}`);
    setShowRegisterModal(false);
    setForm(defaultForm);
  };

  const fetchIssuerList = async () => {
    setLoading(true);
    try {
      const data = await getIssuerList({
        search: searchTerm,
        type: typeFilter,
        status: statusFilter,
      });
      setIssuerList(data);
    } catch (error) {
      console.error("Failed to fetch issuer list:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIssuerList();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchIssuerList();
  };

  const handleReset = () => {
    setSearchTerm("");
    setTypeFilter("전체 정책 유형");
    setStatusFilter("전체 상태");
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchIssuerList();
  };

  const totalPages = Math.max(1, Math.ceil(issuerList.length / ITEMS_PER_PAGE));
  const paginatedList = issuerList.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);
  const allSelected = paginatedList.length > 0 && paginatedList.every((row: any) => selectedIds.has(row.id));
  const toggleAll = () => setSelectedIds(allSelected ? new Set() : new Set(paginatedList.map((row: any) => row.id)));
  const toggleRow = (id: string) => setSelectedIds(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 신뢰정책 목록</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 p-4 border-b border-slate-100">
          <input
            type="text"
            placeholder="Issuer ID / DID"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-48 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 정책 유형</option>
            <option>화이트리스트</option>
            <option>블랙리스트</option>
          </select>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 상태</option>
            <option>활성</option>
            <option>차단</option>
            <option>심사중</option>
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
          <button onClick={() => setShowRegisterModal(true)} className="ml-auto bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">+ 신뢰정책 등록</button>
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 w-10"><input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" /></th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Issuer ID / DID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 유형</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Credential Type</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">적용 범위</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">적용 기간</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${selectedIds.has(row.id) ? "bg-blue-50" : ""}`}>
                  <td className="px-4 py-3 w-10"><input type="checkbox" checked={selectedIds.has(row.id)} onChange={() => toggleRow(row.id)} className="accent-blue-600 cursor-pointer" /></td>
                  <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-blue-600 text-xs">{row.did}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${policyBadge[row.type]}`}>{row.type}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{row.credential}</td>
                  <td className="px-4 py-3 text-slate-500">{row.scope}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.period}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status]}`}>{row.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <Link href={`/issuer/${row.id}`} className="text-blue-600 hover:underline text-xs">상세 →</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {issuerList.length}건{selectedIds.size > 0 && <span className="ml-2 text-blue-600 font-medium">{selectedIds.size}개 선택됨</span>}</span>
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

      {showRegisterModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">신뢰정책 등록</h2>
            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">Issuer DID</label>
                <input value={form.did} onChange={e => setForm(f => ({ ...f, did: e.target.value }))} placeholder="did:kyvc:issuer:xxx" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">Credential Type</label>
                <input value={form.credential} onChange={e => setForm(f => ({ ...f, credential: e.target.value }))} placeholder="KYC VC" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">정책 유형</label>
                <select value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
                  <option>화이트리스트</option>
                  <option>블랙리스트</option>
                </select>
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">적용 범위</label>
                <input value={form.scope} onChange={e => setForm(f => ({ ...f, scope: e.target.value }))} placeholder="플랫폼 전체" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div className="flex gap-2">
                <div className="flex-1">
                  <label className="text-xs text-slate-500 mb-1 block">시작일</label>
                  <input type="date" value={form.startDate} onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                </div>
                <div className="flex-1">
                  <label className="text-xs text-slate-500 mb-1 block">종료일</label>
                  <input type="date" value={form.endDate} onChange={e => setForm(f => ({ ...f, endDate: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => { setShowRegisterModal(false); setForm(defaultForm); }} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleRegister} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">등록</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}