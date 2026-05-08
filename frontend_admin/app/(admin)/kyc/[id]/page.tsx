"use client";
import { use, useState } from "react";
import Link from "next/link";
import { X, FileText, Download } from "lucide-react";

const tabs = ["법인정보", "제출서류", "AI 결과", "심사 이력", "VC 발급"];

const corpInfo = [
  { label: "법인명", value: "주식회사 케이원" },
  { label: "사업자등록번호", value: "123-45-67890" },
  { label: "법인등록번호", value: "110111-0000000" },
  { label: "법인 유형", value: "주식회사" },
  { label: "대표자명", value: "김민준" },
  { label: "설립일", value: "2018.03.15" },
  { label: "주소", value: "서울특별시 강남구 테헤란로 123" },
  { label: "업종", value: "소프트웨어 개발 및 공급" },
];

const docs = [
  { name: "사업자등록증", file: "biz_reg_kone.pdf", size: "234KB", date: "2025.05.02 09:12" },
  { name: "등기사항전부증명서", file: "corp_reg_kone.pdf", size: "891KB", date: "2025.05.02 09:12" },
  { name: "주주명부", file: "shareholders.pdf", size: "156KB", date: "2025.05.02 09:12" },
];

const reviewHistory = [
  { date: "2025.05.02 10:30", action: "수동심사 대기", actor: "시스템", status: "bg-blue-100 text-blue-600" },
  { date: "2025.05.02 09:18", action: "AI 보완필요 판정", actor: "KYvC-AI v2.1", status: "bg-orange-100 text-orange-600" },
  { date: "2025.05.02 09:14", action: "접수 완료", actor: "시스템", status: "bg-green-100 text-green-600" },
];

const timeline = [
  { color: "bg-blue-500", label: "수동심사 대기", date: "2025.05.02 10:30" },
  { color: "bg-orange-400", label: "AI 보완필요 판정", date: "2025.05.02 09:18" },
  { color: "bg-green-500", label: "접수 완료", date: "2025.05.02 09:14" },
];

type DocItem = { name: string; file: string; size: string; date: string };

export default function KycDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [activeTab, setActiveTab] = useState("법인정보");
  const [previewDoc, setPreviewDoc] = useState<DocItem | null>(null);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href="/kyc" className="hover:underline">KYC 신청</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">KYC 신청 상세</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 요약 카드 */}
        <div className="w-60 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <h2 className="text-xs font-semibold text-slate-500 mb-3">KYC 신청 정보</h2>
            <div className="space-y-2.5">
              {[
                { label: "신청번호", value: id },
                { label: "법인명", value: "주식회사 케이원" },
                { label: "사업자번호", value: "123-45-67890" },
                { label: "신청일시", value: "2025.05.02 09:14" },
                { label: "신청 채널", value: "웹" },
              ].map((item) => (
                <div key={item.label}>
                  <p className="text-xs text-slate-400">{item.label}</p>
                  <p className="text-slate-700 font-medium text-xs mt-0.5">{item.value}</p>
                </div>
              ))}
              <div>
                <p className="text-xs text-slate-400">상태</p>
                <span className="bg-red-100 text-red-600 text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block">수동심사필요</span>
              </div>
              <div>
                <p className="text-xs text-slate-400">AI 판단</p>
                <span className="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block">보완필요</span>
              </div>
            </div>
          </div>

          {/* 타임라인 */}
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <h2 className="text-xs font-semibold text-slate-500 mb-3">처리 이력</h2>
            <div className="space-y-3">
              {timeline.map((item, i) => (
                <div key={i} className="flex items-start gap-2">
                  <div className={`w-2.5 h-2.5 rounded-full mt-0.5 shrink-0 ${item.color}`} />
                  <div>
                    <p className="text-xs font-medium text-slate-700">{item.label}</p>
                    <p className="text-xs text-slate-400">{item.date}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 액션 버튼 */}
          <div className="space-y-2">
            <Link
              href={`/kyc/${id}/manual-review`}
              className="block w-full bg-blue-600 text-white text-center py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors"
            >
              수동심사 처리 →
            </Link>
            <Link
              href={`/kyc/${id}/ai-result`}
              className="block w-full border border-slate-200 text-slate-600 text-center py-2.5 rounded-lg text-sm hover:bg-slate-50 transition-colors"
            >
              AI 결과 상세 보기
            </Link>
          </div>
        </div>

        {/* 우측 탭 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex border-b border-slate-200">
            {tabs.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-5 py-3 text-sm font-medium transition-colors border-b-2 -mb-px ${
                  activeTab === tab
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-slate-500 hover:text-slate-700"
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          <div className="p-6">

            {/* 법인정보 */}
            {activeTab === "법인정보" && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-4">법인 기본정보</h3>
                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="w-full text-sm">
                    <tbody>
                      {corpInfo.map((item) => (
                        <tr key={item.label} className="border-b border-slate-100 last:border-0">
                          <td className="px-5 py-3.5 text-slate-500 bg-slate-50 w-36 font-medium">{item.label}</td>
                          <td className="px-5 py-3.5 text-slate-700">{item.value}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* 제출서류 */}
            {activeTab === "제출서류" && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-4">제출서류 목록</h3>
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">서류명</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">파일명</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">용량</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">접수일시</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">미리보기</th>
                    </tr>
                  </thead>
                  <tbody>
                    {docs.map((doc) => (
                      <tr key={doc.file} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-4 py-3 text-slate-700 font-medium">{doc.name}</td>
                        <td className="px-4 py-3 text-blue-600 text-xs">{doc.file}</td>
                        <td className="px-4 py-3 text-slate-400 text-xs">{doc.size}</td>
                        <td className="px-4 py-3 text-slate-400 text-xs">{doc.date}</td>
                        <td className="px-4 py-3">
                          <button
                            onClick={() => setPreviewDoc(doc)}
                            className="text-xs text-blue-600 hover:underline"
                          >미리보기</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* AI 결과 */}
            {activeTab === "AI 결과" && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-700">AI 심사 결과 요약</h3>
                  <Link href={`/kyc/${id}/ai-result`} className="text-xs text-blue-600 hover:underline">
                    상세 보기 →
                  </Link>
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                    <p className="text-xs text-slate-400 mb-1">AI 판단</p>
                    <span className="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full font-medium">보완필요</span>
                  </div>
                  <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                    <p className="text-xs text-slate-400 mb-1">신뢰도</p>
                    <p className="text-xl font-bold text-slate-700">72.4%</p>
                  </div>
                  <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                    <p className="text-xs text-slate-400 mb-1">처리 모델</p>
                    <p className="text-sm font-medium text-slate-700">KYvC-AI v2.1</p>
                  </div>
                </div>
                <div className="bg-orange-50 border border-orange-200 rounded-lg p-4 text-sm text-orange-800">
                  <p className="font-medium mb-1">보완필요 사유</p>
                  <p className="text-xs leading-relaxed">등기사항전부증명서의 대표자명(김민주)이 사업자등록증(김민준)과 상이합니다. 재확인 또는 재제출이 필요합니다.</p>
                </div>
                <div className="flex justify-end">
                  <Link href={`/kyc/${id}/ai-result`} className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 transition-colors">
                    AI 결과 상세 →
                  </Link>
                </div>
              </div>
            )}

            {/* 심사 이력 */}
            {activeTab === "심사 이력" && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-4">심사 이력</h3>
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">일시</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">처리 내용</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">처리자</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reviewHistory.map((row, i) => (
                      <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-4 py-3 text-slate-400 text-xs">{row.date}</td>
                        <td className="px-4 py-3 text-slate-700">{row.action}</td>
                        <td className="px-4 py-3 text-slate-500 text-xs">{row.actor}</td>
                        <td className="px-4 py-3">
                          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${row.status}`}>
                            {row.action.includes("완료") ? "완료" : row.action.includes("판정") ? "판정" : "대기"}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* VC 발급 */}
            {activeTab === "VC 발급" && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-slate-700">VC 발급 상태</h3>
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-5 text-center">
                  <p className="text-sm text-slate-500 mb-1">현재 심사 상태</p>
                  <span className="bg-red-100 text-red-600 text-sm px-3 py-1 rounded-full font-medium">수동심사 필요 — VC 발급 대기 중</span>
                  <p className="text-xs text-slate-400 mt-3">수동심사 완료 후 VC 발급이 가능합니다.</p>
                </div>
                <div className="flex justify-end">
                  <Link href={`/kyc/${id}/manual-review`} className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 transition-colors">
                    수동심사 처리 →
                  </Link>
                </div>
              </div>
            )}

          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {/* 파일 미리보기 모달 */}
      {previewDoc && (
        <div
          className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-6"
          onClick={() => setPreviewDoc(null)}
        >
          <div
            className="bg-white rounded-xl shadow-xl w-full max-w-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-5 py-4 border-b border-slate-200">
              <div>
                <p className="font-semibold text-slate-800 text-sm">{previewDoc.name}</p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {previewDoc.file} · {previewDoc.size} · {previewDoc.date}
                </p>
              </div>
              <button
                onClick={() => setPreviewDoc(null)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded hover:bg-slate-100"
              >
                <X size={18} />
              </button>
            </div>
            <div className="p-6">
              <div className="bg-slate-100 rounded-lg h-80 flex flex-col items-center justify-center text-slate-400 border border-slate-200">
                <FileText size={48} className="text-slate-300 mb-3" />
                <p className="text-sm font-medium text-slate-600">{previewDoc.file}</p>
                <p className="text-xs mt-1 text-slate-400">PDF 미리보기</p>
                <p className="text-xs text-slate-300 mt-4">실제 환경에서는 PDF 뷰어가 표시됩니다</p>
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 pb-5">
              <button
                onClick={() => setPreviewDoc(null)}
                className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
              >
                닫기
              </button>
              <button className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 flex items-center gap-1.5">
                <Download size={14} />
                다운로드
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}