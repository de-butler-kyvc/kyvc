"use client";
import { use, useState } from "react";
import Link from "next/link";
import { approveKycManualReview, rejectKycManualReview } from "@/lib/api/kyc";

const decisionColors: Record<string, string> = {
  승인: "bg-green-100 text-green-700",
  반려: "bg-red-100 text-red-700",
  보완요청: "bg-orange-100 text-orange-700",
  재심사: "bg-blue-100 text-blue-700",
};

export default function ManualReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [reason, setReason] = useState("대표자명 오기재 확인 후 수동 승인 처리. 등기부상 성명은 '김민주'이나 실질 대표자 확인 완료.");
  const [decision, setDecision] = useState("승인");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleComplete = async () => {
    if (!reason.trim()) return;
    setLoading(true);
    setError(null);
    try {
      if (decision === "승인") {
        await approveKycManualReview(id, { reviewComment: reason });
      } else if (decision === "반려") {
        await rejectKycManualReview(id, { rejectReason: reason });
      }
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "처리 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href="/kyc" className="hover:underline">KYC 신청</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">수동심사 처리</h1>
        </div>
      </div>

      {success ? (
        <div className="flex items-center justify-center py-20">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-12 text-center max-w-md w-full">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-5">
              <span className="text-green-600 text-3xl leading-none">✓</span>
            </div>
            <h2 className="text-lg font-bold text-slate-800 mb-2">심사 처리 완료</h2>
            <div className="flex justify-center mb-3">
              <span className={`text-sm px-3 py-1 rounded-full font-medium ${decisionColors[decision]}`}>
                {decision}
              </span>
            </div>
            <p className="text-sm text-slate-500 mb-1">
              KYC 신청 <span className="font-medium text-slate-700">{id}</span>에 대한
            </p>
            <p className="text-sm text-slate-500 mb-8">수동심사가 정상적으로 처리되었습니다.</p>
            <div className="flex gap-3 justify-center">
              <Link
                href="/kyc"
                className="border border-slate-200 text-slate-600 px-5 py-2.5 rounded-lg text-sm hover:bg-slate-50 inline-block transition-colors"
              >
                목록으로
              </Link>
              <Link
                href={`/kyc/${id}`}
                className="bg-blue-600 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 inline-block transition-colors"
              >
                신청 상세로
              </Link>
            </div>
          </div>
        </div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">수동심사 처리</h2>
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
              <span className="bg-red-100 text-red-600 text-xs px-2 py-0.5 rounded-full font-medium">수동심사필요</span>
            </div>
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-6">
            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">AI 판단 결과</h2>
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-4 grid grid-cols-3 gap-4">
                <div>
                  <p className="text-xs text-slate-400">AI 판단</p>
                  <span className="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full font-medium">보완필요</span>
                </div>
                <div>
                  <p className="text-xs text-slate-400">신뢰도</p>
                  <p className="text-slate-700 font-bold">72.4%</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">주요 사유</p>
                  <p className="text-slate-600 text-xs">대표자명 불일치 (등기 vs 사업자)</p>
                </div>
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">
                심사 결정 <span className="text-red-500">*</span>
              </h2>
              <div className="flex gap-4">
                {["승인", "반려", "보완요청", "재심사"].map((option) => (
                  <label key={option} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="decision"
                      value={option}
                      checked={decision === option}
                      onChange={() => setDecision(option)}
                      className="accent-blue-600"
                    />
                    <span className="text-sm text-slate-700">{option}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">
                판단 사유 <span className="text-red-500">*</span>
              </h2>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={4}
                className="w-full border border-slate-200 rounded-lg px-4 py-3 text-sm text-slate-700 focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
                placeholder="수동 판단 근거를 입력해주세요."
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
                {error}
              </div>
            )}

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-slate-400">처리자: 김심사 (admin@kyvc.kr)</p>
              <div className="flex gap-2">
                <Link
                  href={`/kyc/${id}`}
                  className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
                >
                  임시저장
                </Link>
                <button
                  onClick={handleComplete}
                  disabled={loading || !reason.trim()}
                  className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
                >
                  {loading ? "처리 중..." : "처리 완료"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
