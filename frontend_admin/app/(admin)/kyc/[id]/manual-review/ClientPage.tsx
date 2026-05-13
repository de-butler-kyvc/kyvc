"use client";

import { use, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import MfaModal from "@/components/MfaModal";
import {
  approveKycManualReview,
  getAiReview,
  getKycDetail,
  rejectKycManualReview,
  type AiReviewResult,
  type BackendKycDetail,
} from "@/lib/api/kyc";
import {
  kycDetailPath,
  kycReReviewPath,
  kycSupplementRequestPath,
} from "@/lib/navigation/admin-routes";

const DECISION_APPROVE = "승인";
const DECISION_REJECT = "반려";
const DECISION_SUPPLEMENT = "보완요청";
const DECISION_RE_REVIEW = "재심사";
const REVIEWABLE_STATUS = "MANUAL_REVIEW";

const decisionColors: Record<string, string> = {
  [DECISION_APPROVE]: "bg-green-100 text-green-700",
  [DECISION_REJECT]: "bg-red-100 text-red-700",
  [DECISION_SUPPLEMENT]: "bg-orange-100 text-orange-700",
  [DECISION_RE_REVIEW]: "bg-blue-100 text-blue-700",
};

const statusBadge: Record<string, string> = {
  수동심사필요: "bg-red-100 text-red-600",
  보완필요: "bg-orange-100 text-orange-600",
  심사중: "bg-blue-100 text-blue-600",
  정상: "bg-green-100 text-green-600",
  불충족: "bg-slate-100 text-slate-500",
};

const aiBadge: Record<string, string> = {
  수동심사필요: "bg-red-100 text-red-600",
  보완필요: "bg-orange-100 text-orange-600",
  심사중: "bg-blue-100 text-blue-600",
  정상: "bg-green-100 text-green-600",
  불충족: "bg-red-100 text-red-600",
};

const STATUS_KO: Record<string, string> = {
  NEEDS_MANUAL_REVIEW: "수동심사필요",
  NEED_MANUAL_REVIEW: "수동심사필요",
  MANUAL_REVIEW: "수동심사필요",
  NEEDS_SUPPLEMENT: "보완필요",
  NEED_SUPPLEMENT: "보완필요",
  SUPPLEMENT_REQUESTED: "보완필요",
  REVIEWING: "심사중",
  AI_REVIEWING: "심사중",
  SUBMITTED: "심사중",
  DRAFT: "심사중",
  NORMAL: "정상",
  APPROVED: "정상",
  UNSATISFACTORY: "불충족",
  REJECTED: "불충족",
};

const AI_KO: Record<string, string> = {
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
};

function toKo(map: Record<string, string>, value?: string) {
  if (!value) return "-";
  return map[value] ?? value;
}

function formatConfidence(value?: number) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  const normalized = value <= 1 ? value * 100 : value;
  return `${normalized.toFixed(1)}%`;
}

function getCorporateName(detail: BackendKycDetail | null) {
  return detail?.corporateName ?? detail?.corporationName ?? "-";
}

function getStatus(detail: BackendKycDetail | null) {
  return toKo(STATUS_KO, detail?.kycStatus ?? detail?.status);
}

function getAiJudgment(detail: BackendKycDetail | null, review: AiReviewResult | null) {
  const value =
    review?.overallJudgment ??
    detail?.aiJudgment ??
    detail?.aiReviewResult ??
    detail?.aiReviewResultCode ??
    detail?.aiReviewStatus;
  return toKo(AI_KO, value);
}

function getAiReason(detail: BackendKycDetail | null, review: AiReviewResult | null) {
  return review?.summaryReason ?? review?.manualReviewReason ?? detail?.aiReviewSummary ?? "-";
}

export default function ManualReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();

  const [detail, setDetail] = useState<BackendKycDetail | null>(null);
  const [review, setReview] = useState<AiReviewResult | null>(null);
  const [dataLoading, setDataLoading] = useState(true);
  const [dataError, setDataError] = useState<string | null>(null);

  const [reason, setReason] = useState("");
  const [decision, setDecision] = useState(DECISION_APPROVE);
  const [processing, setProcessing] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showMfa, setShowMfa] = useState(false);

  useEffect(() => {
    let alive = true;
    setDataLoading(true);
    setDataError(null);

    Promise.allSettled([getKycDetail(id), getAiReview(id)])
      .then(([detailResult, reviewResult]) => {
        if (!alive) return;

        if (detailResult.status === "fulfilled") {
          setDetail(detailResult.value);
        } else {
          setDataError(detailResult.reason instanceof Error ? detailResult.reason.message : "KYC 신청 정보를 불러오지 못했습니다.");
        }

        if (reviewResult.status === "fulfilled") {
          setReview(reviewResult.value);
          setReason((current) => current || getAiReason(detailResult.status === "fulfilled" ? detailResult.value : null, reviewResult.value));
        } else if (detailResult.status === "fulfilled") {
          setReason((current) => current || getAiReason(detailResult.value, null));
        }
      })
      .finally(() => {
        if (alive) setDataLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [id]);

  const summary = useMemo(() => {
    const rawStatus = detail?.kycStatus ?? detail?.status ?? "";
    const aiJudgment = getAiJudgment(detail, review);
    return {
      applicationId: String(detail?.applicationId ?? detail?.kycId ?? detail?.id ?? id),
      corporateName: getCorporateName(detail),
      status: getStatus(detail),
      canManualReview: rawStatus === REVIEWABLE_STATUS,
      aiJudgment,
      confidence: formatConfidence(review?.confidenceScore ?? detail?.aiConfidenceScore),
      reason: getAiReason(detail, review),
      statusClass: statusBadge[getStatus(detail)] ?? "bg-slate-100 text-slate-500",
      aiClass: aiBadge[aiJudgment] ?? "bg-slate-100 text-slate-500",
    };
  }, [detail, id, review]);

  const handleComplete = async () => {
    if (!reason.trim()) return;
    if (decision === DECISION_SUPPLEMENT) {
      router.push(kycSupplementRequestPath(id));
      return;
    }
    if (decision === DECISION_RE_REVIEW) {
      router.push(kycReReviewPath(id));
      return;
    }
    if (!summary.canManualReview) {
      setError("수동심사는 KYC 상태가 수동심사필요인 건만 승인 또는 반려할 수 있습니다. 현재 상태를 확인해 주세요.");
      return;
    }
    setShowMfa(true);
  };

  const handleMfaConfirm = async (mfaToken: string) => {
    setProcessing(true);
    setError(null);
    try {
      if (decision === DECISION_APPROVE) {
        await approveKycManualReview(id, { mfaToken, comment: reason });
      } else if (decision === DECISION_REJECT) {
        await rejectKycManualReview(id, {
          mfaToken,
          rejectReasonCode: "INVALID_DOCUMENT",
          comment: reason,
        });
      }
      setShowMfa(false);
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "처리 중 오류가 발생했습니다.");
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드어드민 · <Link href="/kyc" className="hover:underline">KYC 신청</Link>
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
              KYC 신청 <span className="font-medium text-slate-700">{summary.applicationId}</span>에 대해
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
                href={kycDetailPath(id)}
                className="bg-blue-600 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 inline-block transition-colors"
              >
                신청 상세로
              </Link>
            </div>
          </div>
        </div>
      ) : dataLoading ? (
        <div className="flex items-center justify-center py-24 text-sm text-slate-500">불러오는 중...</div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">수동심사 처리</h2>
            {[
              { label: "신청번호", value: summary.applicationId },
              { label: "법인명", value: summary.corporateName },
            ].map((item) => (
              <div key={item.label}>
                <p className="text-xs text-slate-400">{item.label}</p>
                <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
              </div>
            ))}
            <div>
              <p className="text-xs text-slate-400">KYC 상태</p>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${summary.statusClass}`}>{summary.status}</span>
            </div>
            {dataError && <p className="text-xs text-red-500">{dataError}</p>}
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-6">
            {!summary.canManualReview && (
              <div className="rounded-lg border border-orange-200 bg-orange-50 px-4 py-3 text-sm text-orange-700 space-y-1">
                <p className="font-medium">현재 KYC 상태에서는 승인/반려를 진행할 수 없습니다.</p>
                <p>수동심사는 상태가 수동심사필요인 건만 처리할 수 있으며, AI 판단 결과는 참고 정보로 표시됩니다.</p>
              </div>
            )}

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">AI 판단 결과</h2>
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-4 grid grid-cols-3 gap-4">
                <div>
                  <p className="text-xs text-slate-400">AI 판단(참고)</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${summary.aiClass}`}>{summary.aiJudgment}</span>
                </div>
                <div>
                  <p className="text-xs text-slate-400">신뢰도</p>
                  <p className="text-slate-700 font-bold">{summary.confidence}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">주요 사유</p>
                  <p className="text-slate-600 text-xs">{summary.reason}</p>
                </div>
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">
                심사 결정 <span className="text-red-500">*</span>
              </h2>
              <div className="flex gap-4">
                {[DECISION_APPROVE, DECISION_REJECT, DECISION_SUPPLEMENT, DECISION_RE_REVIEW].map((option) => (
                  <label key={option} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="decision"
                      value={option}
                      checked={decision === option}
                      onChange={() => setDecision(option)}
                      className="accent-blue-600"
                      disabled={!summary.canManualReview && option !== DECISION_RE_REVIEW}
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
              <p className="text-xs text-slate-400">처리자: 관리자</p>
              <div className="flex gap-2">
                <Link
                  href={kycDetailPath(id)}
                  className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
                >
                  취소
                </Link>
                <button
                  onClick={handleComplete}
                  disabled={processing || !reason.trim() || (!summary.canManualReview && decision !== DECISION_RE_REVIEW)}
                  className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
                >
                  {processing ? "처리 중..." : "처리 완료"}
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

      {showMfa && (
        <MfaModal
          purpose={decision === DECISION_APPROVE ? "KYC_APPROVE" : "KYC_REJECT"}
          onConfirm={handleMfaConfirm}
          onClose={() => setShowMfa(false)}
        />
      )}
    </div>
  );
}
