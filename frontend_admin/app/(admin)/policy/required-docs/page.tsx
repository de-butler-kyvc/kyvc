"use client";
import { useState } from "react";

const corpTypes = ["주식회사", "유한회사", "합자회사", "합명회사", "사단법인", "재단법인", "조합", "외국기업"];

type Doc = { name: string; required: boolean; note: string };

const initialDocsByType: Record<string, Doc[]> = {
  주식회사: [
    { name: "사업자등록증", required: true, note: "최근 3개월 이내" },
    { name: "등기사항전부증명서", required: true, note: "최근 3개월 이내" },
    { name: "주주명부", required: true, note: "최신본" },
    { name: "위임장", required: false, note: "대리인 방문 시" },
  ],
  유한회사: [
    { name: "사업자등록증", required: true, note: "최근 3개월 이내" },
    { name: "등기사항전부증명서", required: true, note: "최근 3개월 이내" },
    { name: "출자자명부", required: true, note: "최신본" },
    { name: "위임장", required: false, note: "대리인 방문 시" },
  ],
  합자회사: [
    { name: "사업자등록증", required: true, note: "최근 3개월 이내" },
    { name: "등기사항전부증명서", required: true, note: "최근 3개월 이내" },
    { name: "사원명부", required: true, note: "최신본" },
  ],
  합명회사: [
    { name: "사업자등록증", required: true, note: "최근 3개월 이내" },
    { name: "등기사항전부증명서", required: true, note: "최근 3개월 이내" },
    { name: "사원명부", required: true, note: "최신본" },
  ],
  사단법인: [
    { name: "사업자등록증", required: true, note: "" },
    { name: "등기사항전부증명서", required: true, note: "" },
    { name: "사원명부", required: true, note: "" },
    { name: "정관", required: true, note: "설립목적 확인용" },
  ],
  재단법인: [
    { name: "사업자등록증", required: true, note: "" },
    { name: "등기사항전부증명서", required: true, note: "" },
    { name: "정관", required: true, note: "설립목적 확인용" },
  ],
  조합: [
    { name: "사업자등록증", required: true, note: "" },
    { name: "등기사항전부증명서 또는 정관", required: true, note: "" },
    { name: "조합원명부", required: true, note: "" },
    { name: "정관·운영회칙·규약 중 1개", required: true, note: "" },
  ],
  외국기업: [
    { name: "투자등록증 등 실명확인증표", required: true, note: "" },
    { name: "법인 등기사항전부증명서", required: true, note: "" },
    { name: "주주명부", required: true, note: "" },
    { name: "정관 (비영리법인)", required: false, note: "비영리법인인 경우" },
  ],
};

export default function RequiredDocsPage() {
  const [selectedType, setSelectedType] = useState("주식회사");
  const [editing, setEditing] = useState(false);
  const [docsByType, setDocsByType] = useState(initialDocsByType);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newDoc, setNewDoc] = useState<Doc>({ name: "", required: true, note: "" });
  const docs = docsByType[selectedType] ?? [];

  const handleAddDoc = () => {
    if (!newDoc.name.trim()) {
      alert("서류명을 입력해주세요.");
      return;
    }
    setDocsByType(prev => ({
      ...prev,
      [selectedType]: [...(prev[selectedType] ?? []), newDoc],
    }));
    setNewDoc({ name: "", required: true, note: "" });
    setShowAddModal(false);
  };

  const handleDeleteDoc = (index: number) => {
    setDocsByType(prev => ({
      ...prev,
      [selectedType]: prev[selectedType].filter((_, i) => i !== index),
    }));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · 정책/규칙</p>
          <h1 className="text-xl font-bold text-slate-800">법인 유형별 필수서류 관리</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 법인 유형 선택 */}
        <div className="w-44 shrink-0 bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50">
            <p className="text-xs font-semibold text-slate-500">법인 유형</p>
          </div>
          {corpTypes.map((type) => (
            <button
              key={type}
              onClick={() => { setSelectedType(type); setEditing(false); }}
              className={`w-full text-left px-4 py-2.5 text-sm transition-colors border-b border-slate-50 ${
                selectedType === type
                  ? "bg-blue-50 text-blue-600 font-medium"
                  : "text-slate-600 hover:bg-slate-50"
              }`}
            >
              {type}
            </button>
          ))}
        </div>

        {/* 우측 서류 목록 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">{selectedType} · 필수서류 목록</h2>
            <button
              onClick={() => setEditing(!editing)}
              className={`px-4 py-1.5 rounded text-sm transition-colors ${editing ? "bg-slate-100 text-slate-600" : "bg-blue-600 text-white hover:bg-blue-700"}`}
            >
              {editing ? "취소" : "편집"}
            </button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-5 py-3 text-slate-500 font-medium">서류명</th>
                <th className="text-left px-5 py-3 text-slate-500 font-medium">필수 여부</th>
                <th className="text-left px-5 py-3 text-slate-500 font-medium">비고</th>
                {editing && <th className="text-left px-5 py-3 text-slate-500 font-medium">관리</th>}
              </tr>
            </thead>
            <tbody>
              {docs.map((doc, i) => (
                <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                  <td className="px-5 py-3 text-slate-700 font-medium">{doc.name}</td>
                  <td className="px-5 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${doc.required ? "bg-blue-100 text-blue-600" : "bg-slate-100 text-slate-500"}`}>
                      {doc.required ? "필수" : "선택"}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-slate-400 text-xs">{doc.note}</td>
                  {editing && (
                    <td className="px-5 py-3">
                      <button onClick={() => handleDeleteDoc(i)} className="text-xs text-red-400 hover:text-red-600">삭제</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
          {editing && (
            <div className="px-5 py-3 border-t border-slate-100">
              <button onClick={() => setShowAddModal(true)} className="text-xs text-blue-600 border border-blue-200 px-3 py-1.5 rounded hover:bg-blue-50">+ 서류 추가</button>
            </div>
          )}
          {editing && (
            <div className="flex justify-end gap-2 px-5 py-3 border-t border-slate-100">
              <button onClick={() => setEditing(false)} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">저장</button>
            </div>
          )}
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showAddModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">서류 추가 — {selectedType}</h2>
            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">서류명</label>
                <input value={newDoc.name} onChange={e => setNewDoc(d => ({ ...d, name: e.target.value }))} placeholder="사업자등록증" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">필수 여부</label>
                <select value={newDoc.required ? "필수" : "선택"} onChange={e => setNewDoc(d => ({ ...d, required: e.target.value === "필수" }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
                  <option>필수</option>
                  <option>선택</option>
                </select>
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">비고</label>
                <input value={newDoc.note} onChange={e => setNewDoc(d => ({ ...d, note: e.target.value }))} placeholder="최근 3개월 이내" className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => { setShowAddModal(false); setNewDoc({ name: "", required: true, note: "" }); }} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleAddDoc} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">추가</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}