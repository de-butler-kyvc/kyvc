"use client";
import { use, useState } from "react";
import Link from "next/link";

const mockIssuerMap: Record<string, {
  id: string; did: string; type: string; credential: string;
  scope: string; startDate: string; endDate: string; status: string;
  regDate: string; regBy: string;
}> = {
  "POL-001": { id: "POL-001", did: "did:kyvc:issuer:001", type: "화이트리스트", credential: "KYC VC", scope: "플랫폼 전체", startDate: "2025-01-01", endDate: "2026-12-31", status: "활성", regDate: "2025.01.01", regBy: "admin_park" },
  "POL-002": { id: "POL-002", did: "did:kyvc:issuer:002", type: "화이트리스트", credential: "위임권한 VC", scope: "파이낸셜 파트너스", startDate: "2025-03-01", endDate: "2026-03-01", status: "활성", regDate: "2025.03.01", regBy: "admin_park" },
  "POL-003": { id: "POL-003", did: "did:bad:issuer:999", type: "블랙리스트", credential: "전체", scope: "플랫폼 전체", startDate: "2025-04-01", endDate: "2026-12-31", status: "차단", regDate: "2025.04.01", regBy: "admin_kim" },
  "POL-004": { id: "POL-004", did: "did:kyvc:issuer:003", type: "화이트리스트", credential: "KYC VC", scope: "비즈파트너 포털", startDate: "2025-05-01", endDate: "2027-05-01", status: "심사중", regDate: "2025.05.01", regBy: "admin_lee" },
};

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  차단: "bg-red-100 text-red-600",
  심사중: "bg-orange-100 text-orange-600",
};

const typeBadge: Record<string, string> = {
  화이트리스트: "bg-green-100 text-green-600",
  블랙리스트: "bg-red-100 text-red-600",
};

const credentialOptions = ["KYC VC", "위임권한 VC", "전체"];
const scopeOptions = ["플랫폼 전체", "파이낸셜 파트너스", "비즈파트너 포털"];

export default function IssuerDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const raw = mockIssuerMap[id] ?? mockIssuerMap["POL-001"];

  const [did, setDid] = useState(raw.did);
  const [type, setType] = useState(raw.type);
  const [credential, setCredential] = useState(raw.credential);
  const [scope, setScope] = useState(raw.scope);
  const [startDate, setStartDate] = useState(raw.startDate);
  const [endDate, setEndDate] = useState(raw.endDate);
  const [saved, setSaved] = useState(false);

  const handleSave = () => { setSaved(true); setTimeout(() => setSaved(false), 3000); };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · <Link href="/issuer" className="hover:underline">Issuer 신뢰정책 목록</Link></p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 신뢰정책 상세</h1>
        </div>
      </div>

      <div className="flex gap-4">
        <div className="w-52 shrink-0">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs font-semibold text-slate-500 mb-3">{raw.id} · Issuer 신뢰정책 상세</p>
            <div className="space-y-3 text-xs">
              <div><p className="text-slate-400">Issuer ID</p><p className="text-slate-700 font-medium mt-0.5 break-all">{did}</p></div>
              <div><p className="text-slate-400">정책 유형</p><span className={`inline-block mt-0.5 px-2 py-0.5 rounded text-xs font-medium ${typeBadge[type]}`}>{type}</span></div>
              <div><p className="text-slate-400">상태</p><span className={`inline-block mt-0.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[raw.status]}`}>{raw.status}</span></div>
              <div><p className="text-slate-400">등록일</p><p className="text-slate-700 mt-0.5">{raw.regDate}</p></div>
              <div><p className="text-slate-400">등록자</p><p className="text-slate-700 mt-0.5">{raw.regBy}</p></div>
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="p-4 border-b border-slate-100"><p className="text-sm font-semibold text-slate-700">정책 정보 입력</p></div>
            <div className="p-5 space-y-4">
              {saved && (
                <div className="flex items-center gap-2 bg-green-50 border border-green-200 rounded-lg px-4 py-3 text-green-700 text-sm font-medium">
                  <span>✓</span> 변경사항이 저장되었습니다.
                </div>
              )}
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">Issuer ID / DID</label>
                <input type="text" value={did} onChange={(e) => setDid(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">정책 유형</label>
                <select value={type} onChange={(e) => setType(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  <option>화이트리스트</option>
                  <option>블랙리스트</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">Credential Type</label>
                <select value={credential} onChange={(e) => setCredential(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  {credentialOptions.map((opt) => <option key={opt}>{opt}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">적용 범위</label>
                <select value={scope} onChange={(e) => setScope(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  {scopeOptions.map((opt) => <option key={opt}>{opt}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">적용 기간</label>
                <div className="flex items-center gap-2">
                  <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
                  <span className="text-slate-400 text-sm">~</span>
                  <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Link href="/issuer" className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">취소</Link>
                <button onClick={handleSave} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">수정</button>
              </div>
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