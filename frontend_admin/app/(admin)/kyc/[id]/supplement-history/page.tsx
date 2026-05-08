"use client";
import { use, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

const submittedDocs = [
  { type: "주주명부 (재제출)", file: "shareholders_v2.pdf", date: "2025.05.05 14:20", hash: "일치", status: "정상" },
  { type: "동일인 확인서", file: "identity_confirm.pdf", date: "2025.05.05 14:20", hash: "일치", status: "정상" },
];

export default function SupplementHistoryPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [decision, setDecision] = useState("승인");
  const [reason, setReason] = useState("보완 서류 검토 완료. 주주명부 해시 일치 확인, 동일인 확인서로 대표자명 정정 근거 확인.");
  const [loading, setLoading] = useState(false);

  const handleSubmit = () => {
    setLoading(true);
    setTimeout(() => { router.push(`/kyc/${id}`); }, 600);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">보완 제출 내역 조회</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 요약 */}
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
          <h2 className="text-xs font-semibold text-slate-500">보완 제출 내역 조회</h2>
          {[
            { label: "신청번호", value: id },
            { label: "법인명", value: "주식회사 케이원" },
          ].map((item) => (
            <div key={item.label}>
              <p className="text-xs text-slate-400">{item.label}</p>
              <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
            </div>
          ))}
          <div>
            <p className="text-xs text-slate-400">상태</p>
            <span className="bg-blue-100 text-blue-600 text-xs px-2 py-0.5 rounded-full font-medium">보완서류 접수</span>
          </div>
          <div>
            <p className="text-xs text-slate-400">보완 요청일</p>
            <p className="text-slate-700 text-xs mt-0.5">2025.05.02</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">제출 기한</p>
            <p className="text-slate-700 text-xs mt-0.5 font-semibold">2025.05.12</p>
          </div>
        </div>

        {/* 우측 */}
        <div className="flex-1 space-y-4">
          {/* 원래 보완 요청 내용 */}
          <div className="bg-white rounded-lg border border-slate-200 p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-3">원래 보완 요청 내용</h2>
            <div className="bg-slate-50 rounded-lg border border-slate-200 px-4 py-3 text-sm text-slate-600">
              주주명부 파일 해시값 불일치. 원본 재업로드 요청. 대표자 동일인 확인서 정보 요청.
            </div>
          </div>

          {/* 보완 제출 내역 */}
          <div className="bg-white rounded-lg border border-slate-200 p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-3">보완 제출 내역</h2>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-4 py-2.5 text-slate-500 font-medium">문서 유형</th>
                  <th className="text-left px-4 py-2.5 text-slate-500 font-medium">파일명</th>
                  <th className="text-left px-4 py-2.5 text-slate-500 font-medium">제출일시</th>
                  <th className="text-left px-4 py-2.5 text-slate-500 font-medium">해시 검증</th>
                  <th className="text-left px-4 py-2.5 text-slate-500 font-medium">상태</th>
                  <th className="text-left px-4 py-2.5 text-slate-500 font-medium">미리보기</th>
                </tr>
              </thead>
              <tbody>
                {submittedDocs.map((doc) => (
                  <tr key={doc.file} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="px-4 py-2.5 text-slate-700">{doc.type}</td>
                    <td className="px-4 py-2.5 text-blue-600 text-xs">{doc.file}</td>
                    <td className="px-4 py-2.5 text-slate-400 text-xs">{doc.date}</td>
                    <td className="px-4 py-2.5">
                      <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded font-medium">{doc.hash}</span>
                    </td>
                    <td className="px-4 py-2.5">
                      <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">{doc.status}</span>
                    </td>
                    <td className="px-4 py-2.5">
                      <button className="text-xs text-blue-600 hover:underline">보기</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 재심사 판단 */}
          <div className="bg-white rounded-lg border border-slate-200 p-5 space-y-4">
            <h2 className="text-sm font-semibold text-slate-700">재심사 판단</h2>
            <div className="flex gap-4">
              {["승인", "반려", "추가 보완 요청"].map((opt) => (
                <label key={opt} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="decision"
                    value={opt}
                    checked={decision === opt}
                    onChange={() => setDecision(opt)}
                    className="accent-blue-600"
                  />
                  <span className="text-sm text-slate-700">{opt}</span>
                </label>
              ))}
            </div>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
              placeholder="판단 근거를 입력해주세요."
            />
            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-slate-400">처리자: 이심사 (admin2@kyvc.kr)</p>
              <div className="flex gap-2">
                <button
                  onClick={() => router.push(`/kyc/${id}/manual-review`)}
                  className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
                >
                  수동심사 처리로 →
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={loading || !reason.trim()}
                  className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
                >
                  {loading ? "처리 중..." : "처리 완료"}
                </button>
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