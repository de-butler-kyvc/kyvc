"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { getIssuerList, type IssuerPolicyDetail } from "@/lib/api/issuer";
import type { IssuerItem } from "@/types/kyc";

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  심사중: "bg-orange-100 text-orange-600",
  차단: "bg-red-100 text-red-600",
};

export default function IssuerWhitelistPage() {
  const [list, setList] = useState<IssuerItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [inputSearch, setInputSearch] = useState("");

  const fetchList = async (q?: string) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getIssuerList({ search: q, type: "화이트리스트" });
      setList(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchList(); }, []);

  const handleSearch = () => { setSearch(inputSearch); fetchList(inputSearch); };
  const handleReset = () => { setInputSearch(""); setSearch(""); fetchList(); };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · Issuer</p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 화이트리스트 관리</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 p-4 border-b border-slate-100">
          <input type="text" value={inputSearch} onChange={(e) => setInputSearch(e.target.value)} onKeyDown={(e) => e.key === "Enter" && handleSearch()} placeholder="Issuer ID / DID" className="border border-slate-200 rounded px-3 py-1.5 text-sm w-52 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <button onClick={handleSearch} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">검색</button>
          <button onClick={handleReset} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">초기화</button>
          <Link href="/issuer/new?type=whitelist" className="ml-auto bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">+ 화이트리스트 등록</Link>
        </div>
        {loading ? (
          <div className="p-8 text-center text-slate-400 text-sm">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Issuer DID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Credential Type</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">적용 범위</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">적용 기간</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {list.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400 text-sm">데이터가 없습니다.</td></tr>
              ) : list.map((row) => (
                <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50">
                  <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-blue-600 text-xs">{row.did}</td>
                  <td className="px-4 py-3 text-slate-500">{row.credential}</td>
                  <td className="px-4 py-3 text-slate-500">{row.scope}</td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{row.period}</td>
                  <td className="px-4 py-3"><span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status] ?? "bg-slate-100 text-slate-600"}`}>{row.status}</span></td>
                  <td className="px-4 py-3"><Link href={`/issuer/${row.id}`} className="text-blue-600 hover:underline text-xs">상세 →</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {list.length}건</span>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
