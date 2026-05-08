"use client";
import { useState } from "react";
import Link from "next/link";

const pending = [
  { id: "POL-004", did: "did:kyvc:issuer:003", type: "화이트리스트", credential: "KYC VC", scope: "비즈파트너 포털", period: "2025.05.01~2027.05.01", reqBy: "admin_lee", reqDate: "2025.05.01" },
  { id: "POL-005", did: "did:kyvc:issuer:004", type: "화이트리스트", credential: "위임권한 VC", scope: "파이낸셜 파트너스", period: "2025.05.03~2026.05.03", reqBy: "admin_kim", reqDate: "2025.05.03" },
];

export default function IssuerApprovalPage() {
  const [selected, setSelected] = useState<string | null>(null);
  const [comment, setComment] = useState("");
  const [done, setDone] = useState<string[]>([]);

  const handleAction = (id: string, action: "승인" | "반려") => {
    setDone([...done, id]);
    setSelected(null);
    setComment("");
  };

  const remaining = pending.filter((p) => !done.includes(p.id));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · Issuer</p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 신뢰정책 승인</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 승인 대기 목록 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-700">승인 대기 목록</h2>
            <span className="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full font-medium">{remaining.length}건 대기</span>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 text-slate-500 font-medium">정책 ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">Issuer DID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">유형</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">요청자</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">요청일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">처리</th>
              </tr>
            </thead>
            <tbody>
              {remaining.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400 text-sm">승인 대기 건이 없습니다.</td></tr>
              ) : remaining.map((row) => (
                <tr key={row.id} className={`border-b border-slate-50 hover:bg-slate-50 cursor-pointer ${selected === row.id ? "bg-blue-50" : ""}`} onClick={() => setSelected(row.id)}>
                  <td className="px-4 py-3 text-slate-700 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-blue-600 text-xs">{row.did}</td>
                  <td className="px-4 py-3"><span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded font-medium">{row.type}</span></td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{row.reqBy}</td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{row.reqDate}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1.5">
                      <button onClick={(e) => { e.stopPropagation(); handleAction(row.id, "승인"); }} className="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700">승인</button>
                      <button onClick={(e) => { e.stopPropagation(); handleAction(row.id, "반려"); }} className="border border-red-200 text-red-500 px-3 py-1 rounded text-xs hover:bg-red-50">반려</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* 우측 상세 */}
        {selected && (() => {
          const item = pending.find((p) => p.id === selected);
          if (!item) return null;
          return (
            <div className="w-72 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-4 h-fit">
              <h2 className="text-sm font-semibold text-slate-700">정책 상세</h2>
              <div className="space-y-2 text-xs">
                {[
                  { label: "정책 ID", value: item.id },
                  { label: "Issuer DID", value: item.did },
                  { label: "정책 유형", value: item.type },
                  { label: "Credential Type", value: item.credential },
                  { label: "적용 범위", value: item.scope },
                  { label: "적용 기간", value: item.period },
                  { label: "요청자", value: item.reqBy },
                ].map((i) => (
                  <div key={i.label}>
                    <p className="text-slate-400">{i.label}</p>
                    <p className="text-slate-700 font-medium mt-0.5 break-all">{i.value}</p>
                  </div>
                ))}
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-slate-500">승인/반려 코멘트 (선택)</label>
                <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} className="w-full border border-slate-200 rounded px-3 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none" placeholder="코멘트 입력..." />
              </div>
              <div className="flex gap-2">
                <button onClick={() => handleAction(item.id, "승인")} className="flex-1 bg-blue-600 text-white py-2 rounded text-sm hover:bg-blue-700">승인</button>
                <button onClick={() => handleAction(item.id, "반려")} className="flex-1 border border-red-200 text-red-500 py-2 rounded text-sm hover:bg-red-50">반려</button>
              </div>
            </div>
          );
        })()}
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}