"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { getManagers } from "@/lib/api/managers";

const roleBadge: Record<string, string> = {
  "심사자": "bg-blue-100 text-blue-600",
  "승인권자": "bg-purple-100 text-purple-600",
  "운영자": "bg-orange-100 text-orange-600",
  "조회자": "bg-slate-100 text-slate-500",
};

const statusBadge: Record<string, string> = {
  "정상": "bg-green-100 text-green-600",
  "잠금": "bg-orange-100 text-orange-600",
  "비활성": "bg-slate-100 text-slate-400",
};

const mfaBadge: Record<string, string> = {
  "설정됨": "bg-green-100 text-green-600",
  "미설정": "bg-red-100 text-red-500",
};

const defaultManagerForm = { id: "", name: "", role: "심사자", department: "" };

export default function ManagersPage() {
  const [managers, setManagers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("전체 역할");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [form, setForm] = useState(defaultManagerForm);
  const ITEMS_PER_PAGE = 3;

  const handleRegister = () => {
    if (!form.id || !form.name || !form.department) {
      alert("모든 항목을 입력해주세요.");
      return;
    }
    alert(`관리자가 등록되었습니다.\nID: ${form.id}`);
    setShowRegisterModal(false);
    setForm(defaultManagerForm);
  };

  const fetchManagers = async () => {
    setLoading(true);
    try {
      const data = await getManagers({
        search: searchTerm,
        role: roleFilter,
        status: statusFilter,
      });
      setManagers(data);
    } catch (error) {
      console.error("Failed to fetch managers:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchManagers();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchManagers();
  };

  const handleReset = () => {
    setSearchTerm("");
    setRoleFilter("전체 역할");
    setStatusFilter("전체 상태");
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchManagers();
  };

  const totalPages = Math.max(1, Math.ceil(managers.length / ITEMS_PER_PAGE));
  const paginatedList = managers.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);
  const allSelected = paginatedList.length > 0 && paginatedList.every((row: any) => selectedIds.has(row.id));
  const toggleAll = () => setSelectedIds(allSelected ? new Set() : new Set(paginatedList.map((row: any) => row.id)));
  const toggleRow = (id: string) => setSelectedIds(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">관리자 사용자 관리</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        {/* 필터 */}
        <div className="flex items-center gap-2 p-4 border-b border-slate-100">
          <input
            type="text"
            placeholder="사용자 ID / 성명"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-44 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 역할</option>
            <option>심사자</option>
            <option>승인권자</option>
            <option>운영자</option>
            <option>조회자</option>
          </select>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 상태</option>
            <option>정상</option>
            <option>잠금</option>
            <option>비활성</option>
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
          <button onClick={() => setShowRegisterModal(true)} className="ml-auto bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">+ 관리자 등록</button>
        </div>

        {/* 테이블 */}
        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 w-10"><input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" /></th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">사용자 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">성명</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">소속/역할</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">최근 접속일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">MFA 설정</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">관리</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${selectedIds.has(row.id) ? "bg-blue-50" : ""}`}>
                  <td className="px-4 py-3 w-10"><input type="checkbox" checked={selectedIds.has(row.id)} onChange={() => toggleRow(row.id)} className="accent-blue-600 cursor-pointer" /></td>
                  <td className="px-4 py-3 text-blue-600 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.name}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${roleBadge[row.role]}`}>{row.role}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status]}`}>{row.status}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{row.lastLogin}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${mfaBadge[row.mfa]}`}>{row.mfa}</span>
                  </td>
                  <td className="px-4 py-3">
                    <Link href={`/managers/${row.id}`} className="text-blue-600 hover:underline text-xs">수정</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {managers.length}건{selectedIds.size > 0 && <span className="ml-2 text-blue-600 font-medium">{selectedIds.size}개 선택됨</span>}</span>
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
            <h2 className="text-base font-semibold text-slate-800">관리자 등록</h2>
            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">사용자 ID</label>
                <input value={form.id} onChange={e => setForm(f => ({ ...f, id: e.target.value }))} placeholder="admin_xxx" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">성명</label>
                <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="홍길동" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">역할</label>
                <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
                  <option>심사자</option>
                  <option>승인권자</option>
                  <option>운영자</option>
                  <option>조회자</option>
                </select>
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">소속</label>
                <input value={form.department} onChange={e => setForm(f => ({ ...f, department: e.target.value }))} placeholder="심사팀" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => { setShowRegisterModal(false); setForm(defaultManagerForm); }} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleRegister} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">등록</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}