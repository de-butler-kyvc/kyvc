"use client";
import { useState } from "react";

const templates = [
  { id: "TPL-001", event: "KYC 신청 접수", channel: "이메일", subject: "[KYvC] KYC 신청이 접수되었습니다", active: true },
  { id: "TPL-002", event: "보완요청 발송", channel: "이메일", subject: "[KYvC] 서류 보완 요청 안내", active: true },
  { id: "TPL-003", event: "심사 완료 (승인)", channel: "이메일", subject: "[KYvC] KYC 심사가 완료되었습니다", active: true },
  { id: "TPL-004", event: "심사 완료 (반려)", channel: "이메일", subject: "[KYvC] KYC 심사 반려 안내", active: true },
  { id: "TPL-005", event: "VC 발급 완료", channel: "이메일", subject: "[KYvC] Verifiable Credential이 발급되었습니다", active: true },
  { id: "TPL-006", event: "계정 잠금", channel: "이메일", subject: "[KYvC] 계정이 잠겼습니다", active: true },
  { id: "TPL-007", event: "비밀번호 재설정", channel: "이메일", subject: "[KYvC] 비밀번호 재설정 링크", active: true },
  { id: "TPL-008", event: "VC 만료 임박", channel: "이메일", subject: "[KYvC] VC 만료 30일 전 안내", active: false },
];

export default function NotificationsPage() {
  const [selected, setSelected] = useState<string | null>(null);
  const [activeMap, setActiveMap] = useState<Record<string, boolean>>(
    Object.fromEntries(templates.map((t) => [t.id, t.active]))
  );
  const [editSubject, setEditSubject] = useState("");
  const [editBody, setEditBody] = useState("안녕하세요, {{법인명}} 담당자님.\n\n{{내용}}\n\nKYvC 드림.");
  const [saved, setSaved] = useState(false);

  const selectedTpl = templates.find((t) => t.id === selected);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · 설정</p>
          <h1 className="text-xl font-bold text-slate-800">알림 템플릿 관리</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 목록 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">알림 템플릿 목록</h2>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 text-slate-500 font-medium">ID</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">이벤트</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">채널</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">제목</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">활성</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">편집</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((tpl) => (
                <tr key={tpl.id} className={`border-b border-slate-50 hover:bg-slate-50 cursor-pointer ${selected === tpl.id ? "bg-blue-50" : ""}`} onClick={() => { setSelected(tpl.id); setEditSubject(tpl.subject); }}>
                  <td className="px-4 py-3 text-slate-400 text-xs">{tpl.id}</td>
                  <td className="px-4 py-3 text-slate-700 font-medium">{tpl.event}</td>
                  <td className="px-4 py-3">
                    <span className="bg-blue-100 text-blue-600 text-xs px-2 py-0.5 rounded">{tpl.channel}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-500 text-xs">{tpl.subject}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={(e) => { e.stopPropagation(); setActiveMap({ ...activeMap, [tpl.id]: !activeMap[tpl.id] }); }}
                      className={`px-2 py-0.5 rounded-full text-xs font-medium transition-colors ${activeMap[tpl.id] ? "bg-green-100 text-green-600" : "bg-slate-100 text-slate-400"}`}
                    >
                      {activeMap[tpl.id] ? "활성" : "비활성"}
                    </button>
                  </td>
                  <td className="px-4 py-3">
                    <button onClick={(e) => { e.stopPropagation(); setSelected(tpl.id); setEditSubject(tpl.subject); }} className="text-xs text-blue-600 hover:underline">편집</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* 우측 편집 */}
        {selected && selectedTpl && (
          <div className="w-80 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-4 h-fit">
            <h2 className="text-sm font-semibold text-slate-700">템플릿 편집</h2>
            <div>
              <p className="text-xs text-slate-400 mb-1">이벤트</p>
              <p className="text-sm text-slate-700 font-medium">{selectedTpl.event}</p>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">제목</label>
              <input type="text" value={editSubject} onChange={(e) => setEditSubject(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">본문</label>
              <textarea value={editBody} onChange={(e) => setEditBody(e.target.value)} rows={6} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none" />
              <p className="text-xs text-slate-400">변수: {`{{법인명}}, {{내용}}, {{링크}}`}</p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => setSelected(null)} className="flex-1 border border-slate-200 text-slate-600 py-2 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={() => { setSaved(true); setTimeout(() => setSaved(false), 2000); }} className="flex-1 bg-blue-600 text-white py-2 rounded text-sm hover:bg-blue-700">{saved ? "저장됨 ✓" : "저장"}</button>
            </div>
          </div>
        )}
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}