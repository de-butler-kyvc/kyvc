"use client";

import { useState } from "react";

const verifiers = ["파이낸셜 파트너스", "비즈파트너 포털"];

const keyStatusBadge: Record<string, string> = {
  "활성": "bg-green-100 text-green-600",
  "만료 임박": "bg-orange-100 text-orange-600",
  "폐기": "bg-slate-100 text-slate-400 line-through",
};

const keyData: Record<string, { type: string; value: string; issued: string; expires: string; status: string }[]> = {
  "파이낸셜 파트너스": [
    { type: "API Key", value: "kyvc_live_************************ef3a", issued: "2025.01.15", expires: "2026.01.15", status: "활성" },
    { type: "Client ID", value: "client_fp_****8b2c", issued: "2025.01.15", expires: "-", status: "활성" },
    { type: "OAuth2 Secret", value: "secret_************************7d1f", issued: "2025.01.15", expires: "2025.07.15", status: "만료 임박" },
    { type: "mTLS 인증서", value: "CN=financial-partners.co.kr, ****", issued: "2025.01.15", expires: "2026.01.15", status: "활성" },
  ],
  "비즈파트너 포털": [
    { type: "API Key", value: "kyvc_live_************************bz01", issued: "2025.03.10", expires: "2026.03.10", status: "활성" },
    { type: "Client ID", value: "client_bz_****3a1d", issued: "2025.03.10", expires: "-", status: "활성" },
  ],
};

export default function SdkPage() {
  const [selected, setSelected] = useState("파이낸셜 파트너스");
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [keyType, setKeyType] = useState("API Key");
  const keys = keyData[selected];

  const handleIssue = () => {
    alert(`${selected}에 ${keyType}가 발급되었습니다.`);
    setShowIssueModal(false);
    setKeyType("API Key");
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">SDK 연동키 관리</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200 flex">
        {/* 좌측 Verifier 선택 */}
        <div className="w-48 border-r border-slate-200 p-4 shrink-0">
          <p className="text-xs text-slate-400 mb-3 font-medium">Verifier 선택</p>
          <div className="space-y-1">
            {verifiers.map((v) => (
              <button
                key={v}
                onClick={() => setSelected(v)}
                className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${
                  v === selected
                    ? "bg-blue-600 text-white font-medium"
                    : "text-slate-600 hover:bg-slate-50"
                }`}
              >
                {v}
              </button>
            ))}
          </div>
        </div>

        {/* 우측 키 목록 */}
        <div className="flex-1 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-slate-700">{selected} — 연동키</h2>
            <button onClick={() => setShowIssueModal(true)} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">+ 신규 키 발급</button>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 text-slate-500 font-medium">키 유형</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">키 값 (일부 마스킹)</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">발급일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">만료일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">관리</th>
              </tr>
            </thead>
            <tbody>
              {keys.map((row) => (
                <tr key={row.type} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3 text-slate-700 font-medium">{row.type}</td>
                  <td className="px-4 py-3 text-slate-500 text-xs font-mono">{row.value}</td>
                  <td className="px-4 py-3 text-slate-500">{row.issued}</td>
                  <td className="px-4 py-3 text-slate-500">{row.expires}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${keyStatusBadge[row.status]}`}>{row.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <button className="text-red-500 hover:text-red-700 text-xs font-medium">폐기</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showIssueModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">신규 키 발급</h2>
            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">Verifier</label>
                <p className="text-sm font-medium text-slate-700">{selected}</p>
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">키 유형</label>
                <select value={keyType} onChange={e => setKeyType(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
                  <option>API Key</option>
                  <option>Client ID</option>
                  <option>OAuth2 Secret</option>
                  <option>mTLS 인증서</option>
                </select>
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowIssueModal(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleIssue} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">발급</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}