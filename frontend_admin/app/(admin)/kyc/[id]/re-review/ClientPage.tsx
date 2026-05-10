"use client";
import { use, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getAiReview, retryAiReview, type AiReviewResult } from "@/lib/api/kyc";

const JUDGMENT_KO: Record<string, string> = {
  NORMAL: "정상", NEEDS_SUPPLEMENT: "보완필요", UNSATISFACTORY: "불충족",
  NEEDS_MANUAL_REVIEW: "수동심사필요",
};

const judgmentBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  보완필요: "bg-orange-100 text-orange-600",
  불충족: "bg-red-100 text-red-600",
  수동심사필요: "bg-red-100 text-red-600",
};

export default function ReReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [reason, setReason] = useState("");
  const [priority, setPriority] = useState("일반");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [aiReview, setAiReview] = useState<AiReviewResult | null>(null);
  const [reviewLoading, setReviewLoading] = useState(true);

  useEffect(() => {
    getAiReview(id)
      .then(setAiReview)
      .catch(() => {/* 요약 로드 실패는 무시하고 폼은 표시 */})
      .finally(() => setReviewLoading(false));
  }, [id]);

  const handleSubmit = async () => {
    if (!reason.trim()) return;
    setLoading(true);
    setError(null);
    try {
      await retryAiReview(id, { reason: `[${priority}] ${reason}` });
      router.push(`/kyc/${id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "재심사 요청 중 오류가 발생했습니다.");
      setLoading(false);
    }
  };

  const judgmentKo = aiReview
    ? (JUDGMENT_KO[aiReview.overallJudgment] ?? aiReview.overallJudgment)
    : null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">재심사 요청</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 요약 */}
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
          <h2 className="text-xs font-semibold text-slate-500">재심사 요청</h2>
          <div>
            <p className="text-xs text-slate-400">신청번호</p>
            <p className="text-slate-700 text-xs font-medium mt-0.5">{id}</p>
          </div>
          {aiReview && judgmentKo && (
            <>
              <div>
                <p className="text-xs text-slate-400">AI 판단</p>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${judgmentBadge[judgmentKo] ?? "bg-slate-100 text-slate-500"}`}>
                  {judgmentKo} ({aiReview.confidenceScore}%)
                </span>
              </div>
              {aiReview.modelVersion && (
                <div>
                  <p className="text-xs text-slate-400">처리 모델</p>
                  <p className="text-slate-700 text-xs mt-0.5">{aiReview.modelVersion}</p>
                </div>
              )}
            </>
          )}
        </div>

        {/* 우측 폼 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-5">
          {/* 기존 심사 결과 요약 */}
          <div>
            <h2 className="text-sm font-semibold text-slate-700 mb-3">기존 심사 결과 요약</h2>
            {reviewLoading ? (
              <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 text-sm text-slate-400">
                불러오는 중...
              </div>
            ) : aiReview ? (
              <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 grid grid-cols-3 gap-4 text-sm">
                <div>
                  <p className="text-xs text-slate-400">AI 판단</p>
                  {judgmentKo && (
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${judgmentBadge[judgmentKo] ?? "bg-slate-100 text-slate-500"}`}>
                      {judgmentKo}
                    </span>
                  )}
                </div>
                <div>
                  <p className="text-xs text-slate-400">신뢰도</p>
                  <p className="font-bold text-slate-700">{aiReview.confidenceScore}%</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">주요 사유</p>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    {aiReview.summaryReason ?? "-"}
                  </p>
                </div>
              </div>
            ) : (
              <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 text-sm text-slate-400">
                AI 심사 결과를 불러오지 못했습니다.
              </div>
            )}
          </div>

          {/* 재심사 우선순위 */}
          <div>
            <h2 className="text-sm font-semibold text-slate-700 mb-3">재심사 우선순위</h2>
            <div className="flex gap-4">
              {["긴급", "높음", "일반"].map((opt) => (
                <label key={opt} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="priority"
                    value={opt}
                    checked={priority === opt}
                    onChange={() => setPriority(opt)}
                    className="accent-blue-600"
                  />
                  <span className="text-sm text-slate-700">{opt}</span>
                </label>
              ))}
            </div>
          </div>

          {/* 재심사 요청 사유 */}
          <div>
            <h2 className="text-sm font-semibold text-slate-700 mb-3">
              재심사 요청 사유 <span className="text-red-500">*</span>
            </h2>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={4}
              placeholder="재심사가 필요한 사유를 입력해주세요."
              className="w-full border border-slate-200 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
            />
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          )}

          {/* 버튼 */}
          <div className="flex items-center justify-between pt-2 border-t border-slate-100">
            <p className="text-xs text-slate-400">요청자: 김심사 (admin@kyvc.kr)</p>
            <div className="flex gap-2">
              <Link href={`/kyc/${id}`} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
                취소
              </Link>
              <button
                onClick={handleSubmit}
                disabled={loading || !reason.trim()}
                className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
              >
                {loading ? "처리 중..." : "재심사 요청"}
              </button>
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
