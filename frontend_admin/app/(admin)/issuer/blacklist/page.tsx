"use client";
import { useState } from "react";
import Link from "next/link";

const mockList = [
  { id: "POL-003", did: "did:bad:issuer:999", credential: "전체", scope: "플랫폼 전체", period: "2025.04.01~", reason: "부정 발급 이력", status: "차단" },
];

export default function IssuerBlacklistPage() {
  const [search, setSearch] = useState("");
  const [did, setDid] = useState("");
  const [credential, setCredential] = useState("전체");
  const [scope, setScope] = useState("플랫폼 전체");
  const [startDate, setStartDate] = useState("2025-01-01");
  const [endDate, setEndDate] = useState("2026-12-31");
  const [saved, setSaved] = useState(false);

  const filtered = mockList.filter((r) => !search || r.did.includes(search) || r.id.includes(search));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · Issuer</p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 블랙리스트 관리</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 목록 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex items-center gap-2 p-4 border-b border-slate-100">
            <input type="text" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Issuer ID / DID" className="border border-slate-200 rounded px-3 py-1.5 text-sm w-52 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <button className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">검색</button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Issuer DID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">차단 사유</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">적용 기간</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((row) => (
                <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50">
                  <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-blue-600 text-xs">{row.did}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.reason}</td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{row.period}</td>
                  <td className="px-4 py-3"><span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-600">{row.status}</span></td>
                  <td className="px-4 py-3"><Link href={`/issuer/${row.id}`} className="text-blue-600 hover:underline text-xs">상세 →</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex justify-end px-4 py-3">
            <span className="text-xs text-slate-400">총 {filtered.length}건</span>
          </div>
        </div>

        {/* 우측 등록 폼 */}
        <div className="w-80 shrink-0 bg-white rounded-lg border border-slate-200">
          <div className="p-4 border-b border-slate-100">
            <p className="text-sm font-semibold text-slate-700">블랙리스트 등록</p>
          </div>
          <div className="p-4 space-y-3">
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">Issuer ID / DID</label>
              <input type="text" value={did} onChange={(e) => setDid(e.target.value)} placeholder="did:bad:issuer:999" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">정책 유형</label>
              <div className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-red-50 text-red-600 font-medium">블랙리스트</div>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">Credential Type</label>
              <select value={credential} onChange={(e) => setCredential(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                <option>전체</option><option>KYC VC</option><option>위임권한 VC</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">적용 범위</label>
              <select value={scope} onChange={(e) => setScope(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                <option>플랫폼 전체</option><option>파이낸셜 파트너스</option><option>비즈파트너 포털</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">적용 기간</label>
              <div className="flex items-center gap-1.5">
                <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="flex-1 border border-slate-200 rounded px-2 py-1.5 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
                <span className="text-slate-400 text-xs">~</span>
                <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="flex-1 border border-slate-200 rounded px-2 py-1.5 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
            </div>
            <div className="flex gap-2 pt-2 border-t border-slate-100">
              <button onClick={() => setDid("")} className="flex-1 border border-slate-200 text-slate-600 py-2 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={() => { setSaved(true); setTimeout(() => setSaved(false), 2000); }} className="flex-1 bg-red-600 text-white py-2 rounded text-sm hover:bg-red-700 transition-colors">{saved ? "저장됨 ✓" : "저장"}</button>
            </div>
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