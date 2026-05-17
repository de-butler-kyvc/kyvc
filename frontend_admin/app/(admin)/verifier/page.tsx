"use client";

import { createVerifier, getVerifierList } from "@/lib/api/verifier";
import { verifierDetailPath } from "@/lib/navigation/admin-routes";
import Link from "next/link";
import { useState, useEffect } from "react";

const typeBadge: Record<string, string> = {
  "코어 도입형": "bg-blue-100 text-blue-600",
  "SDK-only": "bg-purple-100 text-purple-600",
};

const statusBadge: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-600",
  PENDING: "bg-orange-100 text-orange-600",
  SUSPENDED: "bg-slate-100 text-slate-500",
  "활성": "bg-green-100 text-green-600",
  "심사중": "bg-orange-100 text-orange-600",
  "비활성": "bg-slate-100 text-slate-500",
};

export default function VerifierPage() {
  const [verifierList, setVerifierList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({
    name: "",
    businessNo: "",
    callbackUrl: "",
    managerEmail: "",
    managerName: "",
  });
  const ITEMS_PER_PAGE = 15;

  const fetchVerifierList = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getVerifierList({
        search: searchTerm,
        status: statusFilter,
      });
      setVerifierList(data);
    } catch (error) {
      setError(error instanceof Error ? error.message : "Verifier 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVerifierList();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchVerifierList();
  };

  const handleReset = () => {
    setSearchTerm("");
    setStatusFilter("전체 상태");
    setCurrentPage(1);
    setSelectedIds(new Set());
    fetchVerifierList();
  };

  const totalPages = Math.max(1, Math.ceil(verifierList.length / ITEMS_PER_PAGE));
  const paginatedList = verifierList.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);
  const allSelected = paginatedList.length > 0 && paginatedList.every((row: any) => selectedIds.has(row.id));
  const toggleAll = () => setSelectedIds(allSelected ? new Set() : new Set(paginatedList.map((row: any) => row.id)));
  const toggleRow = (id: string) => setSelectedIds(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });

  const handleCreate = async () => {
    if (!form.name.trim()) {
      setError("플랫폼명을 입력해주세요.");
      return;
    }
    setCreating(true);
    setError(null);
    try {
      await createVerifier({
        name: form.name.trim(),
        businessNo: form.businessNo.trim() || undefined,
        callbackUrl: form.callbackUrl.trim() || undefined,
        managerEmail: form.managerEmail.trim() || undefined,
        managerName: form.managerName.trim() || undefined,
      });
      setShowCreateModal(false);
      setForm({ name: "", businessNo: "", callbackUrl: "", managerEmail: "", managerName: "" });
      await fetchVerifierList();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Verifier 플랫폼 등록에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">증명서 관리자</p>
          <h1 className="text-xl font-bold text-slate-800">Verifier 플랫폼 목록</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        {error && <div className="m-4 mb-0 bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}
        <div className="flex items-center gap-2 p-4 border-b border-slate-100">
          <input
            type="text"
            placeholder="플랫폼명 / 도메인"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-44 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 상태</option>
            <option>활성</option>
            <option>심사중</option>
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
          <button onClick={() => setShowCreateModal(true)} className="ml-auto bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">+ 플랫폼 등록</button>
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 w-10"><input type="checkbox" checked={allSelected} onChange={toggleAll} className="accent-blue-600 cursor-pointer" /></th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">플랫폼 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">플랫폼명</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">도메인</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">연동 유형</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">허용 Credential</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">등록일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${selectedIds.has(row.id) ? "bg-blue-50" : ""}`}>
                  <td className="px-4 py-3 w-10"><input type="checkbox" checked={selectedIds.has(row.id)} onChange={() => toggleRow(row.id)} className="accent-blue-600 cursor-pointer" /></td>
                  <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.name}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.domain}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${typeBadge[row.type]}`}>{row.type}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.credential}</td>
                  <td className="px-4 py-3 text-slate-500">{row.regDate}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status]}`}>{row.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <Link href={verifierDetailPath(row.id)} className="text-blue-600 hover:underline text-xs">상세 →</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {verifierList.length}건{selectedIds.size > 0 && <span className="ml-2 text-blue-600 font-medium">{selectedIds.size}개 선택됨</span>}</span>
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
        <span>KYvC 증명서 관리자 · 증명서 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">Verifier 플랫폼 등록</h2>
            <div className="space-y-3">
              <div><label className="text-xs text-slate-500 mb-1 block">플랫폼명</label><input value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" /></div>
              <div><label className="text-xs text-slate-500 mb-1 block">사업자등록번호</label><input value={form.businessNo} onChange={(e) => setForm((f) => ({ ...f, businessNo: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" /></div>
              <div><label className="text-xs text-slate-500 mb-1 block">Callback URL</label><input value={form.callbackUrl} onChange={(e) => setForm((f) => ({ ...f, callbackUrl: e.target.value }))} placeholder="https://partner.example.com/callback" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" /></div>
              <div><label className="text-xs text-slate-500 mb-1 block">담당자 이메일</label><input type="email" value={form.managerEmail} onChange={(e) => setForm((f) => ({ ...f, managerEmail: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" /></div>
              <div><label className="text-xs text-slate-500 mb-1 block">담당자명</label><input value={form.managerName} onChange={(e) => setForm((f) => ({ ...f, managerName: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" /></div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowCreateModal(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleCreate} disabled={creating} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60">{creating ? "등록 중..." : "등록"}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
