"use client";
import { useState } from "react";
import Link from "next/link";

const hashBadge = "bg-green-100 text-green-600";
const hashFailBadge = "bg-red-100 text-red-600";
const storageBadge: Record<string, string> = {
  보관중: "bg-blue-100 text-blue-600",
  "검토 필요": "bg-orange-100 text-orange-600",
  미보관: "bg-slate-100 text-slate-500",
};

const mockDocs = [
  { id: "DOC-00124", kyc: "KYC-2025-0502-001", corp: "주식회사 케이원", type: "사업자등록증", file: "biz_reg_kone.pdf", date: "2025.05.02 09:12", hash: "SHA-256 일치", storage: "보관중" },
  { id: "DOC-00125", kyc: "KYC-2025-0502-001", corp: "주식회사 케이원", type: "등기사항전부증명서", file: "corp_reg_kone.pdf", date: "2025.05.02 09:12", hash: "SHA-256 일치", storage: "보관중" },
  { id: "DOC-00126", kyc: "KYC-2025-0502-001", corp: "주식회사 케이원", type: "주주명부", file: "shareholders.pdf", date: "2025.05.02 09:12", hash: "해시 불일치", storage: "검토 필요" },
  { id: "DOC-00127", kyc: "KYC-2025-0502-002", corp: "(주)테크비전", type: "사업자등록증", file: "bizreg_tech.pdf", date: "2025.05.02 09:30", hash: "SHA-256 일치", storage: "미보관" },
  { id: "DOC-00128", kyc: "KYC-2025-0502-002", corp: "(주)테크비전", type: "위임장", file: "poa_techvision.pdf", date: "2025.05.02 09:30", hash: "SHA-256 일치", storage: "미보관" },
];

type DocItem = typeof mockDocs[number];

export default function DocumentsPage() {
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("전체 문서 유형");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [storageFilter, setStorageFilter] = useState("보관 여부 전체");
  const [previewDoc, setPreviewDoc] = useState<DocItem | null>(null);

  const filtered = mockDocs.filter((d) => {
    const matchSearch = !search || d.corp.includes(search) || d.id.includes(search) || d.kyc.includes(search);
    const matchType = typeFilter === "전체 문서 유형" || d.type === typeFilter;
    const matchStatus = statusFilter === "전체 상태" || d.hash.includes(statusFilter === "일치" ? "일치" : statusFilter === "불일치" ? "불일치" : "");
    const matchStorage = storageFilter === "보관 여부 전체" || d.storage === storageFilter;
    return matchSearch && matchType && matchStatus && matchStorage;
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · KYC 신청</p>
          <h1 className="text-xl font-bold text-slate-800">제출서류 목록</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        {/* 필터 */}
        <div className="flex items-center gap-2 p-4 border-b border-slate-100 flex-wrap">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="법인명 / 문서 유형"
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-44 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
            <option>전체 문서 유형</option>
            <option>사업자등록증</option>
            <option>등기사항전부증명서</option>
            <option>주주명부</option>
            <option>위임장</option>
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
            <option>전체 상태</option>
            <option>일치</option>
            <option>불일치</option>
          </select>
          <select value={storageFilter} onChange={(e) => setStorageFilter(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
            <option>보관 여부 전체</option>
            <option>보관중</option>
            <option>검토 필요</option>
            <option>미보관</option>
          </select>
          <button onClick={() => { setSearch(""); setTypeFilter("전체 문서 유형"); setStatusFilter("전체 상태"); setStorageFilter("보관 여부 전체"); }} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">초기화</button>
          <button className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">검색</button>
        </div>

        {/* 테이블 */}
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50">
              <th className="text-left px-4 py-3 text-slate-500 font-medium">문서 ID</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">신청번호</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">법인명</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">문서 유형</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">파일명</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">업로드일시</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">해시 검증</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">보관 상태</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">미리보기</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((doc) => (
              <tr key={doc.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                <td className="px-4 py-3 text-slate-600 font-medium text-xs">{doc.id}</td>
                <td className="px-4 py-3">
                  <Link href={`/kyc/${doc.kyc}`} className="text-blue-600 hover:underline text-xs">{doc.kyc}</Link>
                </td>
                <td className="px-4 py-3 text-slate-700">{doc.corp}</td>
                <td className="px-4 py-3 text-slate-600">{doc.type}</td>
                <td className="px-4 py-3 text-slate-500 text-xs">{doc.file}</td>
                <td className="px-4 py-3 text-slate-400 text-xs">{doc.date}</td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${doc.hash.includes("불일치") ? hashFailBadge : hashBadge}`}>
                    {doc.hash}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${storageBadge[doc.storage]}`}>
                    {doc.storage}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <button onClick={() => setPreviewDoc(doc)} className="text-blue-600 hover:underline text-xs">보기 →</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* 페이지네이션 */}
        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {filtered.length}건</span>
          <div className="flex gap-1">
            <button className="w-7 h-7 rounded bg-blue-600 text-white text-xs">1</button>
            <button className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50">2</button>
            <button className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50">›</button>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {previewDoc && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-lg p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-slate-800">문서 미리보기</h2>
              <button onClick={() => setPreviewDoc(null)} className="text-slate-400 hover:text-slate-600 text-lg leading-none">✕</button>
            </div>
            <div className="space-y-2 text-sm">
              {[
                { label: "문서 ID", value: previewDoc.id },
                { label: "신청번호", value: previewDoc.kyc },
                { label: "법인명", value: previewDoc.corp },
                { label: "문서 유형", value: previewDoc.type },
                { label: "파일명", value: previewDoc.file },
                { label: "업로드일시", value: previewDoc.date },
                { label: "해시 검증", value: previewDoc.hash },
                { label: "보관 상태", value: previewDoc.storage },
              ].map(item => (
                <div key={item.label} className="flex gap-3 py-1.5 border-b border-slate-50 last:border-0">
                  <span className="text-slate-400 w-24 shrink-0 text-xs">{item.label}</span>
                  <span className="text-slate-700 font-medium text-xs">{item.value}</span>
                </div>
              ))}
            </div>
            <div className="bg-slate-100 rounded-lg h-40 flex items-center justify-center text-slate-400 text-sm">
              PDF 뷰어 영역 (API 연결 후 실제 파일 표시)
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setPreviewDoc(null)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">닫기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}