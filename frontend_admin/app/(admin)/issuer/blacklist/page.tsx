"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { getIssuerList, createIssuerBlacklist } from "@/lib/api/issuer";
import { issuerDetailPath } from "@/lib/navigation/admin-routes";
import type { IssuerItem } from "@/types/kyc";
import MfaModal from "@/components/MfaModal";

export default function IssuerBlacklistPage() {
  const [list, setList] = useState<IssuerItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [inputSearch, setInputSearch] = useState("");

  const [did, setDid] = useState("");
  const [issuerName, setIssuerName] = useState("");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [showMfa, setShowMfa] = useState(false);

  const fetchList = async (q?: string) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getIssuerList({ search: q, type: "블랙리스트" });
      setList(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchList(); }, []);

  const handleSave = () => {
    if (!did.trim() || !reason.trim()) { setSaveError("DID와 차단 사유를 입력해주세요."); return; }
    setSaveError(null);
    setShowMfa(true);
  };

  const handleMfaConfirm = async (mfaToken: string) => {
    setShowMfa(false);
    setSaving(true);
    try {
      await createIssuerBlacklist({ issuerDid: did, issuerName: issuerName || did, reasonCode: "FRAUD", reason, mfaToken });
      setDid(""); setIssuerName(""); setReason("");
      fetchList();
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : "등록에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · Issuer</p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 블랙리스트 관리</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="flex gap-4">
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex items-center gap-2 p-4 border-b border-slate-100">
            <input type="text" value={inputSearch} onChange={(e) => setInputSearch(e.target.value)} onKeyDown={(e) => e.key === "Enter" && fetchList(inputSearch)} placeholder="Issuer ID / DID" className="border border-slate-200 rounded px-3 py-1.5 text-sm w-52 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <button onClick={() => fetchList(inputSearch)} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">검색</button>
          </div>
          {loading ? (
            <div className="p-8 text-center text-slate-400 text-sm">로딩 중...</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 ID</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Issuer DID</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">적용 기간</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
                </tr>
              </thead>
              <tbody>
                {list.length === 0 ? (
                  <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-400 text-sm">데이터가 없습니다.</td></tr>
                ) : list.map((row) => (
                  <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                    <td className="px-4 py-3 text-blue-600 text-xs">{row.did}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{row.period}</td>
                    <td className="px-4 py-3"><span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-600">{row.status}</span></td>
                    <td className="px-4 py-3"><Link href={issuerDetailPath(row.id)} className="text-blue-600 hover:underline text-xs">상세 →</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="flex justify-end px-4 py-3">
            <span className="text-xs text-slate-400">총 {list.length}건</span>
          </div>
        </div>

        <div className="w-80 shrink-0 bg-white rounded-lg border border-slate-200">
          <div className="p-4 border-b border-slate-100">
            <p className="text-sm font-semibold text-slate-700">블랙리스트 등록</p>
          </div>
          <div className="p-4 space-y-3">
            {saveError && <p className="text-xs text-red-500">{saveError}</p>}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">Issuer DID</label>
              <input type="text" value={did} onChange={(e) => setDid(e.target.value)} placeholder="did:bad:issuer:999" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">Issuer 이름</label>
              <input type="text" value={issuerName} onChange={(e) => setIssuerName(e.target.value)} placeholder="발급기관명" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">정책 유형</label>
              <div className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-red-50 text-red-600 font-medium">블랙리스트</div>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">차단 사유</label>
              <input type="text" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="부정 발급 이력" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
            </div>
            <div className="flex gap-2 pt-2 border-t border-slate-100">
              <button onClick={() => { setDid(""); setIssuerName(""); setReason(""); }} className="flex-1 border border-slate-200 text-slate-600 py-2 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleSave} disabled={saving} className="flex-1 bg-red-600 text-white py-2 rounded text-sm hover:bg-red-700 disabled:opacity-60 transition-colors">{saving ? "저장 중..." : "저장"}</button>
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showMfa && (
        <MfaModal onConfirm={handleMfaConfirm} onClose={() => setShowMfa(false)} />
      )}
    </div>
  );
}
