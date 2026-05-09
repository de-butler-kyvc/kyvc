"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import {
  getAiReview,
  getAiReviewMismatches,
  getAiReviewBeneficialOwners,
  getAiReviewAgentAuthority,
  type AiReviewResult,
  type AiMismatch,
  type BeneficialOwner,
  type AgentAuthority,
} from "@/lib/api/kyc";

// ── 표시값 매핑 ──────────────────────────────────────────────

const JUDGMENT_KO: Record<string, string> = {
  NORMAL: "정상", NEEDS_SUPPLEMENT: "보완필요", UNSATISFACTORY: "불충족",
  NEEDS_MANUAL_REVIEW: "수동심사필요",
};
const MISMATCH_KO: Record<string, string> = {
  MATCH: "일치", MISMATCH: "불일치", NEEDS_REVIEW: "검토 필요",
};

function toKo(map: Record<string, string>, v: string) {
  return map[v] ?? v;
}

const judgmentBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  보완필요: "bg-orange-100 text-orange-600",
  불충족: "bg-red-100 text-red-600",
  수동심사필요: "bg-red-100 text-red-600",
};

const mismatchBadge: Record<string, string> = {
  일치: "bg-green-100 text-green-600",
  불일치: "bg-red-100 text-red-600",
  "검토 필요": "bg-orange-100 text-orange-600",
};

const ownerJudgeBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  "검토 필요": "bg-orange-100 text-orange-600",
  NORMAL: "bg-green-100 text-green-600",
  NEEDS_REVIEW: "bg-orange-100 text-orange-600",
};

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
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
    Promise.allSettled([
      getAiReview(id),
      getAiReviewMismatches(id),
      getAiReviewBeneficialOwners(id),
      getAiReviewAgentAuthority(id),
    ]).then(([r, m, o, a]) => {
      const errs: Record<string, string> = {};
      if (r.status === "fulfilled") setReview(r.value);
      else errs.review = r.reason instanceof Error ? r.reason.message : "AI 결과 로드 실패";
      if (m.status === "fulfilled") setMismatches(m.value);
      else errs.mismatches = m.reason instanceof Error ? m.reason.message : "불일치 항목 로드 실패";
      if (o.status === "fulfilled") setOwners(o.value);
      else errs.owners = o.reason instanceof Error ? o.reason.message : "실제소유자 로드 실패";
      if (a.status === "fulfilled") setAgents(a.value);
      else errs.agents = a.reason instanceof Error ? a.reason.message : "대리권 로드 실패";
      setErrors(errs);
    }).finally(() => setLoading(false));
  }, [id]);

  const judgmentKo = review ? toKo(JUDGMENT_KO, review.overallJudgment) : "-";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">AI 심사 결과 상세</h1>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-24 text-slate-500 text-sm">불러오는 중...</div>
      ) : (
        <div className="flex gap-4">
          {/* ── 좌측 요약 ── */}
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">AI 심사 결과</h2>
            {errors.review ? (
              <p className="text-xs text-red-500">{errors.review}</p>
            ) : review ? (
              <>
                <div>
                  <p className="text-xs text-slate-400">AI 판단</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${judgmentBadge[judgmentKo] ?? "bg-slate-100 text-slate-500"}`}>
                    {judgmentKo}
                  </span>
                </div>
                <div>
                  <p className="text-xs text-slate-400">신뢰도</p>
                  <p className="text-slate-700 font-bold text-lg">{review.confidenceScore}%</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">처리 모델</p>
                  <p className="text-slate-700 text-xs">{review.modelVersion ?? "-"}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">처리 시각</p>
                  <p className="text-slate-700 text-xs">{fmtDt(review.reviewedAt)}</p>
                </div>
              </>
            ) : (
              <p className="text-xs text-slate-400">데이터 없음</p>
            )}
          </div>

          {/* ── 우측 콘텐츠 ── */}
          <div className="flex-1 space-y-4">
            {/* 불일치 항목 테이블 */}
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
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">검토 항목</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">추출값</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">신뢰도</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">판단</th>
                    </tr>
                  </thead>
                  <tbody>
                    {mismatches.map((item, i) => {
                      const judgeKo = toKo(MISMATCH_KO, item.judgment);
                      const score = item.confidenceScore;
                      const scoreColor = score == null ? "text-slate-500"
                        : score >= 90 ? "text-green-600"
                        : score >= 70 ? "text-orange-500"
                        : "text-red-500";
                      return (
                        <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-5 py-3.5 text-slate-700 font-medium">{item.fieldName}</td>
                          <td className="px-5 py-3.5 text-slate-500">{item.extractedValue ?? "-"}</td>
                          <td className={`px-5 py-3.5 font-medium ${scoreColor}`}>
                            {score != null ? `${score}%` : "-"}
                          </td>
                          <td className="px-5 py-3.5">
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${mismatchBadge[judgeKo] ?? "bg-slate-100 text-slate-500"}`}>
                              {judgeKo}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {/* 실제소유자 */}
            <div className="bg-white rounded-lg border border-slate-200">
              <div className="px-5 py-4 border-b border-slate-100">
                <h2 className="text-sm font-semibold text-slate-700">실제소유자 분석</h2>
              </div>
              {errors.owners ? (
                <p className="px-5 py-4 text-sm text-red-500">{errors.owners}</p>
              ) : owners.length === 0 ? (
                <p className="px-5 py-4 text-sm text-slate-400">실제소유자 데이터가 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">소유자명</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">주주 비율</th>
                      <th className="text-left px-5 py-3 text-slate-500 font-medium">판단</th>
                    </tr>
                  </thead>
                  <tbody>
                    {owners.map((o, i) => (
                      <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-5 py-3.5 text-slate-700 font-medium">{o.ownerName}</td>
                        <td className="px-5 py-3.5 text-slate-500">
                          {o.shareRatio != null ? `${o.shareRatio}%` : "-"}
                        </td>
                        <td className="px-5 py-3.5">
                          {o.judgment ? (
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ownerJudgeBadge[o.judgment] ?? "bg-slate-100 text-slate-500"}`}>
                              {toKo(JUDGMENT_KO, o.judgment)}
                            </span>
                          ) : "-"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            {/* 대리권 */}
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
                    {agents.map((a, i) => (
                      <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-5 py-3.5 text-slate-700 font-medium">{a.agentName}</td>
                        <td className="px-5 py-3.5 text-slate-500">{a.authorityType ?? "-"}</td>
                        <td className="px-5 py-3.5 text-slate-400 text-xs">
                          {a.validFrom ? `${fmtDt(a.validFrom)} ~ ${fmtDt(a.validTo)}` : "-"}
                        </td>
                        <td className="px-5 py-3.5">
                          {a.judgment ? (
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ownerJudgeBadge[a.judgment] ?? "bg-green-100 text-green-600"}`}>
                              {toKo({ VALID: "정상", INVALID: "불일치", NEEDS_REVIEW: "검토 필요", ...JUDGMENT_KO }, a.judgment)}
                            </span>
                          ) : "-"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            {/* AI 판단 근거 */}
            {review?.summaryReason && (
              <div className="bg-white rounded-lg border border-slate-200 p-5">
                <h2 className="text-sm font-semibold text-slate-700 mb-3">AI 판단 근거</h2>
                <p className="text-sm text-slate-600 leading-relaxed bg-slate-50 rounded-lg p-4 border border-slate-100">
                  {review.summaryReason}
                </p>
              </div>
            )}

            <div className="flex justify-end">
              <Link href={`/kyc/${id}`} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
                ← 신청 상세로 돌아가기
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
