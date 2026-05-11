"use client";

import { use, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  getAiReview,
  getAiReviewAgentAuthority,
  getAiReviewBeneficialOwners,
  getAiReviewMismatches,
  type AgentAuthority,
  type AiMismatch,
  type AiReviewResult,
  type BeneficialOwner,
} from "@/lib/api/kyc";

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
};

const MISMATCH_KO: Record<string, string> = {
  MATCH: "일치",
  MISMATCH: "불일치",
  NEEDS_REVIEW: "검토필요",
  NEED_REVIEW: "검토필요",
};

const AGENT_JUDGMENT_KO: Record<string, string> = {
  VALID: "유효",
  INVALID: "불일치",
  NEEDS_REVIEW: "검토필요",
  NEED_REVIEW: "검토필요",
  ...JUDGMENT_KO,
};

const judgmentBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  유효: "bg-green-100 text-green-600",
  일치: "bg-green-100 text-green-600",
  보완필요: "bg-orange-100 text-orange-600",
  검토필요: "bg-orange-100 text-orange-600",
  불충족: "bg-red-100 text-red-600",
  불일치: "bg-red-100 text-red-600",
  수동심사필요: "bg-red-100 text-red-600",
  심사중: "bg-blue-100 text-blue-600",
};

function toKo(map: Record<string, string>, value?: string) {
  if (!value) return "-";
  return map[value] ?? value;
}

function badgeClass(label: string) {
  return judgmentBadge[label] ?? "bg-slate-100 text-slate-500";
}

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

function formatPercent(value?: number) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  const normalized = value <= 1 ? value * 100 : value;
  return `${normalized.toFixed(1)}%`;
}

function formatDetailJson(raw?: string) {
  if (!raw?.trim()) return "";
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

export default function AiResultPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [review, setReview] = useState<AiReviewResult | null>(null);
  const [mismatches, setMismatches] = useState<AiMismatch[]>([]);
  const [owners, setOwners] = useState<BeneficialOwner[]>([]);
  const [agents, setAgents] = useState<AgentAuthority[]>([]);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setErrors({});

    Promise.allSettled([
      getAiReview(id),
      getAiReviewMismatches(id),
      getAiReviewBeneficialOwners(id),
      getAiReviewAgentAuthority(id),
    ])
      .then(([reviewResult, mismatchResult, ownerResult, agentResult]) => {
        if (!alive) return;

        const nextErrors: Record<string, string> = {};
        if (reviewResult.status === "fulfilled") setReview(reviewResult.value);
        else nextErrors.review = reviewResult.reason instanceof Error ? reviewResult.reason.message : "AI 결과 로드 실패";

        if (mismatchResult.status === "fulfilled") setMismatches(mismatchResult.value);
        else nextErrors.mismatches = mismatchResult.reason instanceof Error ? mismatchResult.reason.message : "불일치 항목 로드 실패";

        if (ownerResult.status === "fulfilled") setOwners(ownerResult.value);
        else nextErrors.owners = ownerResult.reason instanceof Error ? ownerResult.reason.message : "실소유자 로드 실패";

        if (agentResult.status === "fulfilled") setAgents(agentResult.value);
        else nextErrors.agents = agentResult.reason instanceof Error ? agentResult.reason.message : "대리권 로드 실패";

        setErrors(nextErrors);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [id]);

  const summary = useMemo(() => {
    const judgment = toKo(JUDGMENT_KO, review?.overallJudgment);
    return {
      judgment,
      confidence: formatPercent(review?.confidenceScore),
      modelVersion: review?.modelVersion ?? "-",
      reviewedAt: fmtDt(review?.reviewedAt),
      summaryReason: review?.summaryReason,
      manualReviewReason: review?.manualReviewReason,
      detailJson: formatDetailJson(review?.detailJson),
    };
  }, [review]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">AI 심사 결과 상세</h1>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-24 text-slate-500 text-sm">불러오는 중...</div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">AI 심사 결과</h2>
            {errors.review ? (
              <p className="text-xs text-red-500">{errors.review}</p>
            ) : review ? (
              <>
                <div>
                  <p className="text-xs text-slate-400">AI 판단</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${badgeClass(summary.judgment)}`}>
                    {summary.judgment}
                  </span>
                </div>
                <div>
                  <p className="text-xs text-slate-400">신뢰도</p>
                  <p className="text-slate-700 font-bold text-lg">{summary.confidence}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">처리 모델</p>
                  <p className="text-slate-700 text-xs">{summary.modelVersion}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">처리 시각</p>
                  <p className="text-slate-700 text-xs">{summary.reviewedAt}</p>
                </div>
              </>
            ) : (
              <p className="text-xs text-slate-400">데이터 없음</p>
            )}
          </div>

          <div className="flex-1 space-y-4">
            <div className="bg-white rounded-lg border border-slate-200">
              <div className="px-5 py-4 border-b border-slate-100">
                <h2 className="text-sm font-semibold text-slate-700">AI 불일치 항목</h2>
              </div>
              {errors.mismatches ? (
                <p className="px-5 py-4 text-sm text-red-500">{errors.mismatches}</p>
              ) : mismatches.length === 0 ? (
                <p className="px-5 py-4 text-sm text-slate-400">불일치 항목이 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">검증 항목</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">추출값</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">신뢰도</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">판단</th>
                    </tr>
                  </thead>
                  <tbody>
                    {mismatches.map((item, index) => {
                      const judgment = toKo(MISMATCH_KO, item.judgment);
                      const score = item.confidenceScore;
                      const scoreColor = score == null ? "text-slate-500" : score >= 90 ? "text-green-600" : score >= 70 ? "text-orange-500" : "text-red-500";
                      return (
                        <tr key={`${item.fieldName}-${index}`} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-5 py-3.5 text-slate-700 font-medium">{item.fieldName}</td>
                          <td className="px-5 py-3.5 text-slate-500">{item.extractedValue ?? "-"}</td>
                          <td className={`px-5 py-3.5 font-medium ${scoreColor}`}>{formatPercent(score)}</td>
                          <td className="px-5 py-3.5">
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${badgeClass(judgment)}`}>{judgment}</span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            <div className="bg-white rounded-lg border border-slate-200">
              <div className="px-5 py-4 border-b border-slate-100">
                <h2 className="text-sm font-semibold text-slate-700">실소유자 분석</h2>
              </div>
              {errors.owners ? (
                <p className="px-5 py-4 text-sm text-red-500">{errors.owners}</p>
              ) : owners.length === 0 ? (
                <p className="px-5 py-4 text-sm text-slate-400">실소유자 데이터가 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">소유자명</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">지분율</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">판단</th>
                    </tr>
                  </thead>
                  <tbody>
                    {owners.map((owner, index) => {
                      const judgment = toKo(JUDGMENT_KO, owner.judgment);
                      return (
                        <tr key={`${owner.ownerName}-${index}`} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-5 py-3.5 text-slate-700 font-medium">{owner.ownerName}</td>
                          <td className="px-5 py-3.5 text-slate-500">{formatPercent(owner.shareRatio)}</td>
                          <td className="px-5 py-3.5">
                            {owner.judgment ? (
                              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${badgeClass(judgment)}`}>{judgment}</span>
                            ) : "-"}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            <div className="bg-white rounded-lg border border-slate-200">
              <div className="px-5 py-4 border-b border-slate-100">
                <h2 className="text-sm font-semibold text-slate-700">대리권 분석</h2>
              </div>
              {errors.agents ? (
                <p className="px-5 py-4 text-sm text-red-500">{errors.agents}</p>
              ) : agents.length === 0 ? (
                <p className="px-5 py-4 text-sm text-slate-400">대리권 데이터가 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">대리인명</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">권한 유형</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">유효 기간</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">판단</th>
                    </tr>
                  </thead>
                  <tbody>
                    {agents.map((agent, index) => {
                      const judgment = toKo(AGENT_JUDGMENT_KO, agent.judgment);
                      return (
                        <tr key={`${agent.agentName}-${index}`} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-5 py-3.5 text-slate-700 font-medium">{agent.agentName}</td>
                          <td className="px-5 py-3.5 text-slate-500">{agent.authorityType ?? "-"}</td>
                          <td className="px-5 py-3.5 text-slate-400 text-xs">
                            {agent.validFrom ? `${fmtDt(agent.validFrom)} ~ ${fmtDt(agent.validTo)}` : "-"}
                          </td>
                          <td className="px-5 py-3.5">
                            {agent.judgment ? (
                              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${badgeClass(judgment)}`}>{judgment}</span>
                            ) : "-"}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {(summary.summaryReason || summary.manualReviewReason || summary.detailJson) && (
              <div className="bg-white rounded-lg border border-slate-200 p-5 space-y-4">
                <h2 className="text-sm font-semibold text-slate-700">AI 판단 근거</h2>
                {summary.summaryReason && (
                  <div>
                    <p className="text-xs text-slate-400 mb-1">요약 사유</p>
                    <p className="text-sm text-slate-600 leading-relaxed bg-slate-50 rounded-lg p-4 border border-slate-100">{summary.summaryReason}</p>
                  </div>
                )}
                {summary.manualReviewReason && (
                  <div>
                    <p className="text-xs text-slate-400 mb-1">수동심사 사유</p>
                    <p className="text-sm text-slate-600 leading-relaxed bg-slate-50 rounded-lg p-4 border border-slate-100">{summary.manualReviewReason}</p>
                  </div>
                )}
                {summary.detailJson && (
                  <div>
                    <p className="text-xs text-slate-400 mb-1">상세 원문</p>
                    <pre className="max-h-72 overflow-auto whitespace-pre-wrap rounded-lg border border-slate-100 bg-slate-950 p-4 text-xs leading-relaxed text-slate-100">
                      {summary.detailJson}
                    </pre>
                  </div>
                )}
              </div>
            )}

            <div className="flex justify-end">
              <Link href={`/kyc/${id}`} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
                신청 상세로 돌아가기
              </Link>
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
