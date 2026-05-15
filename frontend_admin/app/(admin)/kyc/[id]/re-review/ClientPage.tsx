"use client";

import { use, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  formatConfidence,
  getAiReview,
  getKycDetail,
  retryAiReview,
  type AiReviewResult,
  type BackendKycDetail,
} from "@/lib/api/kyc";
import { kycDetailPath } from "@/lib/navigation/admin-routes";

const RETRYABLE_STATUSES = new Set(["SUBMITTED", "MANUAL_REVIEW", "NEED_SUPPLEMENT"]);

const STATUS_KO: Record<string, string> = {
  SUBMITTED: "제출완료",
  MANUAL_REVIEW: "수동심사필요",
  NEED_SUPPLEMENT: "보완필요",
  APPROVED: "승인완료",
  REJECTED: "반려",
  AI_REVIEWING: "AI 심사중",
  REVIEWING: "심사중",
  DRAFT: "작성중",
};

const JUDGMENT_KO: Record<string, string> = {
  NORMAL: "정상",
  PASS: "정상",
  QUEUED: "심사중",
  PROCESSING: "심사중",
  LOW_CONFIDENCE: "수동심사필요",
  NEEDS_SUPPLEMENT: "보완필요",
  NEED_SUPPLEMENT: "보완필요",
  UNSATISFACTORY: "불충족",
  FAIL: "불충족",
  FAILED: "불충족",
  NEEDS_MANUAL_REVIEW: "수동심사필요",
  NEED_MANUAL_REVIEW: "수동심사필요",
  MANUAL_APPROVAL_REQUIRED: "수동심사필요",
};

const badge: Record<string, string> = {
  제출완료: "bg-blue-100 text-blue-600",
  수동심사필요: "bg-red-100 text-red-600",
  보완필요: "bg-orange-100 text-orange-600",
  승인완료: "bg-green-100 text-green-600",
  반려: "bg-slate-100 text-slate-500",
  정상: "bg-green-100 text-green-600",
  심사중: "bg-blue-100 text-blue-600",
  불충족: "bg-red-100 text-red-600",
};

function toKo(map: Record<string, string>, value?: string) {
  if (!value) return "-";
  return map[value] ?? value;
}

export default function ReReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();

  const [reason, setReason] = useState("");
  const [priority, setPriority] = useState("일반");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [detail, setDetail] = useState<BackendKycDetail | null>(null);
  const [aiReview, setAiReview] = useState<AiReviewResult | null>(null);
  const [pageLoading, setPageLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    setPageLoading(true);
    setPageError(null);

    Promise.allSettled([getKycDetail(id), getAiReview(id)])
      .then(([detailResult, reviewResult]) => {
        if (!alive) return;

        if (detailResult.status === "fulfilled") setDetail(detailResult.value);
        else setPageError(detailResult.reason instanceof Error ? detailResult.reason.message : "KYC 신청 정보를 불러오지 못했습니다.");

        if (reviewResult.status === "fulfilled") setAiReview(reviewResult.value);
      })
      .finally(() => {
        if (alive) setPageLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [id]);

  const summary = useMemo(() => {
    const rawStatus = detail?.kycStatus ?? detail?.status ?? "";
    const statusKo = toKo(STATUS_KO, rawStatus);
    const judgmentKo = toKo(JUDGMENT_KO, aiReview?.overallJudgment);
    return {
      rawStatus,
      statusKo,
      judgmentKo,
      canRetry: RETRYABLE_STATUSES.has(rawStatus),
      confidence: formatConfidence(aiReview?.confidenceScore),
      reason: aiReview?.summaryReason ?? detail?.aiReviewSummary ?? "-",
    };
  }, [aiReview, detail]);

  const handleSubmit = async () => {
    if (!reason.trim() || !summary.canRetry) return;
    setLoading(true);
    setError(null);
    try {
      await retryAiReview(id, { reason: `[${priority}] ${reason.trim()}` });
      router.push(kycDetailPath(id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "재심사 요청 중 오류가 발생했습니다.");
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드어드민 · <Link href={kycDetailPath(id)} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">재심사 요청</h1>
        </div>
      </div>

      {pageLoading ? (
        <div className="flex items-center justify-center py-24 text-sm text-slate-500">불러오는 중...</div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">재심사 요청</h2>
            <div>
              <p className="text-xs text-slate-400">신청번호</p>
              <p className="text-slate-700 text-xs font-medium mt-0.5">{id}</p>
            </div>
            <div>
              <p className="text-xs text-slate-400">현재 상태</p>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${badge[summary.statusKo] ?? "bg-slate-100 text-slate-500"}`}>
                {summary.statusKo}
              </span>
            </div>
            {aiReview && (
              <>
                <div>
                  <p className="text-xs text-slate-400">AI 판단</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${badge[summary.judgmentKo] ?? "bg-slate-100 text-slate-500"}`}>
                    {summary.judgmentKo} ({summary.confidence})
                  </span>
                </div>
              </>
            )}
            {pageError && <p className="text-xs text-red-500">{pageError}</p>}
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-5">
            {!summary.canRetry && (
              <div className="rounded-lg border border-orange-200 bg-orange-50 px-4 py-3 text-sm text-orange-700">
                현재 상태에서는 AI 재심사를 요청할 수 없습니다. 재심사는 제출완료, 수동심사필요, 보완필요 상태에서만 가능합니다.
              </div>
            )}

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">기존 심사 결과 요약</h2>
              {aiReview ? (
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 grid grid-cols-3 gap-4 text-sm">
                  <div>
                    <p className="text-xs text-slate-400">AI 판단</p>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${badge[summary.judgmentKo] ?? "bg-slate-100 text-slate-500"}`}>
                      {summary.judgmentKo}
                    </span>
                  </div>
                  <div>
                    <p className="text-xs text-slate-400">신뢰도</p>
                    <p className="font-bold text-slate-700">{summary.confidence}</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-400">주요 사유</p>
                    <p className="text-xs text-slate-600 leading-relaxed">{summary.reason}</p>
                  </div>
                </div>
              ) : (
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 text-sm text-slate-400">
                  AI 심사 결과를 불러오지 못했습니다.
                </div>
              )}
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">재심사 우선순위</h2>
              <div className="flex gap-4">
                {["긴급", "높음", "일반"].map((option) => (
                  <label key={option} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="priority"
                      value={option}
                      checked={priority === option}
                      onChange={() => setPriority(option)}
                      className="accent-blue-600"
                      disabled={!summary.canRetry}
                    />
                    <span className="text-sm text-slate-700">{option}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">
                재심사 요청 사유 <span className="text-red-500">*</span>
              </h2>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={4}
                placeholder="재심사가 필요한 사유를 입력해주세요."
                disabled={!summary.canRetry}
                className="w-full border border-slate-200 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none disabled:bg-slate-50 disabled:text-slate-400"
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
                {error}
              </div>
            )}

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-slate-400">요청자: 관리자</p>
              <div className="flex gap-2">
                <Link href={kycDetailPath(id)} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
                  취소
                </Link>
                <button
                  onClick={handleSubmit}
                  disabled={loading || !reason.trim() || !summary.canRetry}
                  className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
                >
                  {loading ? "처리 중..." : "재심사 요청"}
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
