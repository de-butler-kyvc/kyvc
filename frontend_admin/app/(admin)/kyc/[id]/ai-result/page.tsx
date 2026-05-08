"use client";
import { use } from "react";
import Link from "next/link";

const aiItems = [
  { label: "법인명 일치", value: "주식회사 케이원", confidence: "98.2%", result: "일치" },
  { label: "사업자등록번호", value: "123-45-67890", confidence: "99.1%", result: "일치" },
  { label: "대표자명", value: "김민준 / 김민주 불일치", confidence: "61.3%", result: "불일치" },
  { label: "주소", value: "서울 강남구 테헤란로 123", confidence: "88.7%", result: "일치" },
  { label: "실제소유자 주주 비율", value: "25.3% (김민준)", confidence: "77.0%", result: "검토 필요" },
];

const resultBadge: Record<string, string> = {
  "일치": "bg-green-100 text-green-600",
  "불일치": "bg-red-100 text-red-600",
  "검토 필요": "bg-orange-100 text-orange-600",
};

export default function AiResultPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · KYC 신청</p>
          <h1 className="text-xl font-bold text-slate-800">AI 심사 결과 상세</h1>
        </div>
      </div>

      <div className="flex gap-4">
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
          <h2 className="text-xs font-semibold text-slate-500">AI 심사 결과 상세</h2>
          {[{ label: "AI 판단", value: "보완필요", badge: "bg-orange-100 text-orange-600" }].map((item) => (
            <div key={item.label}>
              <p className="text-xs text-slate-400">{item.label}</p>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${item.badge}`}>{item.value}</span>
            </div>
          ))}
          <div>
            <p className="text-xs text-slate-400">신뢰도</p>
            <p className="text-slate-700 font-bold text-lg">72.4%</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">처리 모델</p>
            <p className="text-slate-700 text-xs">KYvC-AI v2.1</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">처리 시각</p>
            <p className="text-slate-700 text-xs">2025.05.02 09:18</p>
          </div>
        </div>

        <div className="flex-1 space-y-4">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="px-5 py-4 border-b border-slate-100">
              <h2 className="text-sm font-semibold text-slate-700">AI 추출 결과</h2>
            </div>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">검토 항목</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">추출값</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">신뢰도</th>
                  <th className="text-left px-5 py-3 text-slate-500 font-medium">판단</th>
                </tr>
              </thead>
              <tbody>
                {aiItems.map((item) => (
                  <tr key={item.label} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="px-5 py-3.5 text-slate-700 font-medium">{item.label}</td>
                    <td className="px-5 py-3.5 text-slate-500">{item.value}</td>
                    <td className={`px-5 py-3.5 font-medium ${parseFloat(item.confidence) >= 90 ? "text-green-600" : parseFloat(item.confidence) >= 70 ? "text-orange-500" : "text-red-500"}`}>
                      {item.confidence}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${resultBadge[item.result]}`}>{item.result}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="bg-white rounded-lg border border-slate-200 p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-3">AI 판단 근거</h2>
            <p className="text-sm text-slate-600 leading-relaxed bg-slate-50 rounded-lg p-4 border border-slate-100">
              [보완필요 사유] 등기사항전부증명서의 대표자명(김민주)이 사업자등록증(김민준)과 상이합니다. 해당 항목에 대해 재확인 또는 재제출이 필요합니다. 실제소유자 판단의 경우 주주명부 상 25.3%로 기준(25%) 초과이나 소수점 근접으로 수동 확인을 권고합니다.
            </p>
          </div>

          <div className="flex justify-end">
            <Link href={`/kyc/${id}`} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">← 신청 상세로 돌아가기</Link>
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