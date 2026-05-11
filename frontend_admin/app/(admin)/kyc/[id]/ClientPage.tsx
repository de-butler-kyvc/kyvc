"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import { X, FileText, Download, ExternalLink } from "lucide-react";
import MfaModal from "@/components/MfaModal";
import {
  getKycDetail,
  getKycCorporate,
  getKycDocuments,
  getKycDocumentPreview,
  issueKycCredential,
  type BackendKycDetail,
  type BackendKycCorporate,
  type KycSubmittedDocument,
} from "@/lib/api/kyc";
import { getCredentials, type KycCredential } from "@/lib/api/credentials";

// ── 배지 색상 ─────────────────────────────────────────────────

const STATUS_BADGE: Record<string, string> = {
  수동심사필요: "bg-red-100 text-red-600",
  보완필요: "bg-orange-100 text-orange-600",
  심사중: "bg-blue-100 text-blue-600",
  정상: "bg-green-100 text-green-600",
  불충족: "bg-slate-100 text-slate-500",
};

const AI_BADGE: Record<string, string> = {
  보완필요: "bg-orange-100 text-orange-600",
  불충족: "bg-red-100 text-red-600",
  정상: "bg-green-100 text-green-600",
};

const CRED_STATUS_BADGE: Record<string, string> = {
  ISSUED: "bg-green-100 text-green-600",
  ACTIVE: "bg-green-100 text-green-600",
  REVOKED: "bg-red-100 text-red-600",
  EXPIRED: "bg-slate-100 text-slate-500",
  활성: "bg-green-100 text-green-600",
  취소: "bg-red-100 text-red-600",
  만료: "bg-slate-100 text-slate-500",
};

const HISTORY_TYPE_COLOR: Record<string, string> = {
  AI: "bg-blue-500",
  MANUAL: "bg-purple-500",
  수동: "bg-purple-500",
  SYSTEM: "bg-green-500",
  시스템: "bg-green-500",
};

const STATUS_KO: Record<string, string> = {
  NEEDS_MANUAL_REVIEW: "수동심사필요",
  NEED_MANUAL_REVIEW: "수동심사필요",
  MANUAL_REVIEW: "수동심사필요",
  NEEDS_SUPPLEMENT: "보완필요",
  NEED_SUPPLEMENT: "보완필요",
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
const CHANNEL_KO: Record<string, string> = { WEB: "웹", FINANCIAL: "금융사" };

function toKo(map: Record<string, string>, v?: string) {
  if (!v) return "-";
  return map[v] ?? v;
}

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

function fmtDate(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 10).replaceAll("-", ".");
}

const tabs = ["법인정보", "제출서류", "AI 결과", "심사 이력", "VC 발급"];

export default function KycDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  // ── 신청 상세 ────────────────────────────────────────────────
  const [detail, setDetail] = useState<BackendKycDetail | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detailLoading, setDetailLoading] = useState(true);

  // ── 법인 정보 ────────────────────────────────────────────────
  const [corporate, setCorporate] = useState<BackendKycCorporate | null>(null);
  const [corpError, setCorpError] = useState<string | null>(null);
  const [corpLoading, setCorpLoading] = useState(false);
  const [corpLoaded, setCorpLoaded] = useState(false);

  // ── 제출 서류 ────────────────────────────────────────────────
  const [documents, setDocuments] = useState<KycSubmittedDocument[]>([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const [docsError, setDocsError] = useState<string | null>(null);
  const [docsLoaded, setDocsLoaded] = useState(false);

  // ── 문서 미리보기 ────────────────────────────────────────────
  const [previewDoc, setPreviewDoc] = useState<KycSubmittedDocument | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  // ── VC 발급 ──────────────────────────────────────────────────
  const [credentials, setCredentials] = useState<KycCredential[]>([]);
  const [credsLoading, setCredsLoading] = useState(false);
  const [credsError, setCredsError] = useState<string | null>(null);
  const [credsLoaded, setCredsLoaded] = useState(false);
  const [issueLoading, setIssueLoading] = useState(false);
  const [issueSuccess, setIssueSuccess] = useState(false);
  const [issueError, setIssueError] = useState<string | null>(null);
  const [showIssueMfa, setShowIssueMfa] = useState(false);

  // ── UI 상태 ──────────────────────────────────────────────────
  const [activeTab, setActiveTab] = useState("법인정보");

  // ── 초기 로드: 신청 상세 ─────────────────────────────────────
  useEffect(() => {
    getKycDetail(id)
      .then(setDetail)
      .catch((err) => setDetailError(err instanceof Error ? err.message : "데이터를 불러오지 못했습니다."))
      .finally(() => setDetailLoading(false));
  }, [id]);

  // ── 법인 정보 (1회) ──────────────────────────────────────────
  useEffect(() => {
    if (activeTab !== "법인정보" || corpLoaded) return;
    setCorpLoading(true);
    getKycCorporate(id)
      .then((data) => { setCorporate(data); setCorpLoaded(true); })
      .catch((err) => setCorpError(err instanceof Error ? err.message : "법인 정보를 불러오지 못했습니다."))
      .finally(() => setCorpLoading(false));
  }, [activeTab, id, corpLoaded]);

  // ── 제출 서류 (1회) ──────────────────────────────────────────
  useEffect(() => {
    if (activeTab !== "제출서류" || docsLoaded) return;
    setDocsLoading(true);
    getKycDocuments(id)
      .then((data) => { setDocuments(data); setDocsLoaded(true); })
      .catch((err) => setDocsError(err instanceof Error ? err.message : "서류 목록을 불러오지 못했습니다."))
      .finally(() => setDocsLoading(false));
  }, [activeTab, id, docsLoaded]);

  // ── VC 발급 이력 ─────────────────────────────────────────────
  useEffect(() => {
    if (activeTab !== "VC 발급" || credsLoaded) return;
    setCredsLoading(true);
    getCredentials({ applicationId: id })
      .then((data) => { setCredentials(data); setCredsLoaded(true); })
      .catch((err) => setCredsError(err instanceof Error ? err.message : "발급 이력을 불러오지 못했습니다."))
      .finally(() => setCredsLoading(false));
  }, [activeTab, id, credsLoaded]);

  // ── 문서 미리보기 핸들러 ─────────────────────────────────────
  const handlePreview = async (doc: KycSubmittedDocument) => {
    setPreviewDoc(doc);
    setPreviewUrl(null);
    setPreviewLoading(true);
    try {
      const data = await getKycDocumentPreview(id, doc.documentId);
      setPreviewUrl(data.previewUrl ?? null);
    } catch {
      // API 미지원 시 플레이스홀더 표시
    } finally {
      setPreviewLoading(false);
    }
  };

  const closePreview = () => {
    setPreviewDoc(null);
    setPreviewUrl(null);
  };

  // ── VC 발급 핸들러 ───────────────────────────────────────────
  const handleIssueCredential = async (mfaToken: string) => {
    setIssueLoading(true);
    setIssueError(null);
    try {
      await issueKycCredential(id, { mfaToken, comment: "KYC 승인 완료에 따른 VC 발급" });
      setIssueSuccess(true);
      setCredsLoaded(false);
      setShowIssueMfa(false);
    } catch (err) {
      setIssueError(err instanceof Error ? err.message : "VC 발급 중 오류가 발생했습니다.");
    } finally {
      setIssueLoading(false);
    }
  };

  // ── 파생 표시값 ──────────────────────────────────────────────
  const statusKo = detail ? toKo(STATUS_KO, detail.kycStatus ?? detail.status) : "-";
  const aiKo = detail ? toKo(AI_KO, detail.aiJudgment ?? detail.aiReviewResult ?? detail.aiReviewResultCode) : "-";
  const channelKo = detail ? toKo(CHANNEL_KO, detail.channel) : "-";
  const histories = detail?.recentHistories ?? [];
  const canManualReview = (detail?.kycStatus ?? detail?.status) === "MANUAL_REVIEW";

  const corpFields = corporate
    ? [
        { label: "법인명", value: corporate.corporationName ?? corporate.corporateName ?? "-" },
        { label: "사업자등록번호", value: corporate.businessRegistrationNumber ?? corporate.businessRegistrationNo ?? "-" },
        { label: "법인등록번호", value: corporate.corporateRegistrationNumber ?? "-" },
        { label: "법인 유형", value: corporate.corporationType ?? corporate.corporateType ?? "-" },
        { label: "대표자명", value: corporate.representativeName ?? "-" },
        { label: "설립일", value: fmtDate(corporate.establishedDate) },
        { label: "주소", value: corporate.address ?? "-" },
        { label: "업종", value: corporate.businessType ?? "-" },
      ]
    : [];

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

      {detailError && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
          {detailError}
        </div>
      )}

      <div className="flex gap-4">
        {/* ── 좌측 요약 카드 ── */}
        <div className="w-60 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <h2 className="text-xs font-semibold text-slate-500 mb-3">KYC 신청 정보</h2>
            {detailLoading ? (
              <p className="text-xs text-slate-400">불러오는 중...</p>
            ) : (
              <div className="space-y-2.5">
                {[
                  { label: "신청번호", value: detail?.applicationId ?? detail?.kycId ?? id },
                  { label: "법인명", value: detail?.corporationName ?? detail?.corporateName ?? "-" },
                  { label: "사업자번호", value: detail?.businessRegistrationNumber ?? detail?.businessRegistrationNo ?? "-" },
                  { label: "신청일시", value: fmtDt(detail?.applicationDate ?? detail?.submittedAt) },
                  { label: "신청 채널", value: channelKo },
                ].map((item) => (
                  <div key={item.label}>
                    <p className="text-xs text-slate-400">{item.label}</p>
                    <p className="text-slate-700 font-medium text-xs mt-0.5">{item.value}</p>
                  </div>
                ))}
                <div>
                  <p className="text-xs text-slate-400">KYC 상태</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block ${STATUS_BADGE[statusKo] ?? "bg-slate-100 text-slate-500"}`}>
                    {statusKo}
                  </span>
                </div>
                <div>
                  <p className="text-xs text-slate-400">AI 판단(참고)</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block ${AI_BADGE[aiKo] ?? "bg-slate-100 text-slate-500"}`}>
                    {aiKo}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* 타임라인 */}
          {histories.length > 0 && (
            <div className="bg-white rounded-lg border border-slate-200 p-4">
              <h2 className="text-xs font-semibold text-slate-500 mb-3">처리 이력</h2>
              <div className="space-y-3">
                {histories.slice(0, 3).map((item, i) => (
                  <div key={i} className="flex items-start gap-2">
                    <div className={`w-2.5 h-2.5 rounded-full mt-0.5 shrink-0 ${HISTORY_TYPE_COLOR[item.actionType ?? ""] ?? "bg-slate-400"}`} />
                    <div>
                      <p className="text-xs font-medium text-slate-700">{item.actionContent}</p>
                      <p className="text-xs text-slate-400">{fmtDt(item.actionDate)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 액션 버튼 */}
          <div className="space-y-2">
            {canManualReview ? (
              <Link
                href={`/kyc/${id}/manual-review`}
                className="block w-full bg-blue-600 text-white text-center py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors"
              >
                수동심사 처리 →
              </Link>
            ) : (
              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-left text-xs text-slate-500 space-y-1">
                <p className="font-medium text-slate-600">수동심사는 현재 KYC 상태가 수동심사필요인 건만 진행할 수 있습니다.</p>
                <p>현재 상태: <span className="font-semibold text-slate-700">{toKo(STATUS_KO, detail?.kycStatus ?? detail?.status)}</span> · 처리 조건: 수동심사필요</p>
              </div>
            )}
            <Link
              href={`/kyc/${id}/ai-result`}
              className="block w-full border border-slate-200 text-slate-600 text-center py-2.5 rounded-lg text-sm hover:bg-slate-50 transition-colors"
            >
              AI 결과 상세 보기
            </Link>
          </div>
        </div>

        {/* ── 우측 탭 ── */}
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
                {corpLoading ? (
                  <p className="text-sm text-slate-400 py-6 text-center">불러오는 중...</p>
                ) : corpError ? (
                  <p className="text-sm text-red-500">{corpError}</p>
                ) : corpFields.length === 0 ? (
                  <p className="text-sm text-slate-400 py-6 text-center">법인 정보가 없습니다.</p>
                ) : (
                  <div className="border border-slate-200 rounded-lg overflow-hidden">
                    <table className="w-full text-sm">
                      <tbody>
                        {corpFields.map((item) => (
                          <tr key={item.label} className="border-b border-slate-100 last:border-0">
                            <td className="px-5 py-3.5 text-slate-500 bg-slate-50 w-36 font-medium">{item.label}</td>
                            <td className="px-5 py-3.5 text-slate-700">{item.value}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {/* 제출서류 */}
            {activeTab === "제출서류" && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-4">제출서류 목록</h3>
                {docsLoading ? (
                  <p className="text-sm text-slate-400 py-6 text-center">불러오는 중...</p>
                ) : docsError ? (
                  <p className="text-sm text-red-500 py-4">{docsError}</p>
                ) : documents.length === 0 ? (
                  <p className="text-sm text-slate-400 py-6 text-center">제출된 서류가 없습니다.</p>
                ) : (
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
                      {documents.map((doc) => (
                        <tr key={doc.documentId} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-4 py-3 text-slate-700 font-medium">{doc.documentName}</td>
                          <td className="px-4 py-3 text-blue-600 text-xs">{doc.fileName}</td>
                          <td className="px-4 py-3 text-slate-400 text-xs">{doc.fileSize ?? "-"}</td>
                          <td className="px-4 py-3 text-slate-400 text-xs">{fmtDt(doc.uploadedAt)}</td>
                          <td className="px-4 py-3">
                            <button
                              onClick={() => handlePreview(doc)}
                              className="text-xs text-blue-600 hover:underline"
                            >
                              미리보기
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
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
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${AI_BADGE[aiKo] ?? "bg-slate-100 text-slate-500"}`}>
                      {aiKo}
                    </span>
                  </div>
                  <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                    <p className="text-xs text-slate-400 mb-1">처리 모델</p>
                    <p className="text-sm font-medium text-slate-700">{detail?.aiConfidenceScore != null ? `${detail.aiConfidenceScore}%` : "-"}</p>
                  </div>
                  <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                    <p className="text-xs text-slate-400 mb-1">상세 분석</p>
                    <Link href={`/kyc/${id}/ai-result`} className="text-xs text-blue-600 hover:underline">
                      AI 결과 상세 →
                    </Link>
                  </div>
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
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-700">최근 심사 이력</h3>
                  <Link href={`/kyc/${id}/review-history`} className="text-xs text-blue-600 hover:underline">
                    전체 이력 보기 →
                  </Link>
                </div>
                {histories.length === 0 ? (
                  <p className="text-sm text-slate-400 py-4 text-center">심사 이력이 없습니다.</p>
                ) : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 bg-slate-50">
                        <th className="text-left px-4 py-3 text-slate-500 font-medium">일시</th>
                        <th className="text-left px-4 py-3 text-slate-500 font-medium">처리 내용</th>
                      </tr>
                    </thead>
                    <tbody>
                      {histories.map((row, i) => (
                        <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-4 py-3 text-slate-400 text-xs whitespace-nowrap">
                            {fmtDt(row.actionDate)}
                          </td>
                          <td className="px-4 py-3 text-slate-700">{row.actionContent}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                <div className="flex justify-end pt-1">
                  <Link href={`/kyc/${id}/review-history`} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
                    전체 심사 이력 →
                  </Link>
                </div>
              </div>
            )}

            {/* VC 발급 */}
            {activeTab === "VC 발급" && (
              <div className="space-y-5">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-700">VC 발급 관리</h3>
                </div>

                {/* 발급 가능 여부 */}
                <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-400 mb-1">현재 심사 상태</p>
                    <span className={`text-sm px-3 py-1 rounded-full font-medium ${STATUS_BADGE[statusKo] ?? "bg-slate-100 text-slate-500"}`}>
                      {statusKo === "정상" ? "심사 완료 — VC 발급 가능" : `${statusKo} — VC 발급 대기 중`}
                    </span>
                  </div>
                  {statusKo === "정상" && !issueSuccess && (
                    <button
                      onClick={() => setShowIssueMfa(true)}
                      disabled={issueLoading}
                      className="bg-blue-600 text-white px-5 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
                    >
                      {issueLoading ? "발급 중..." : "VC 발급"}
                    </button>
                  )}
                </div>

                {issueSuccess && (
                  <div className="bg-green-50 border border-green-200 rounded-lg px-4 py-3 text-sm text-green-700">
                    VC가 성공적으로 발급되었습니다.
                  </div>
                )}
                {issueError && (
                  <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
                    {issueError}
                  </div>
                )}

                {/* 발급 이력 */}
                <div>
                  <h4 className="text-xs font-semibold text-slate-500 mb-3">발급 이력</h4>
                  {credsLoading ? (
                    <p className="text-sm text-slate-400 py-4 text-center">불러오는 중...</p>
                  ) : credsError ? (
                    <p className="text-sm text-red-500">{credsError}</p>
                  ) : credentials.length === 0 ? (
                    <p className="text-sm text-slate-400 py-4 text-center">발급된 VC가 없습니다.</p>
                  ) : (
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-slate-100 bg-slate-50">
                          <th className="text-left px-4 py-3 text-slate-500 font-medium">Credential ID</th>
                          <th className="text-left px-4 py-3 text-slate-500 font-medium">유형</th>
                          <th className="text-left px-4 py-3 text-slate-500 font-medium">발급일시</th>
                          <th className="text-left px-4 py-3 text-slate-500 font-medium">만료일</th>
                          <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                        </tr>
                      </thead>
                      <tbody>
                        {credentials.map((cred) => (
                          <tr key={cred.credentialId} className="border-b border-slate-50 hover:bg-slate-50">
                            <td className="px-4 py-3 text-xs text-slate-600 font-mono">{cred.credentialId}</td>
                            <td className="px-4 py-3 text-slate-600 text-xs">{cred.credentialType ?? "-"}</td>
                            <td className="px-4 py-3 text-slate-400 text-xs">{fmtDt(cred.issuedAt)}</td>
                            <td className="px-4 py-3 text-slate-400 text-xs">{fmtDate(cred.expiresAt)}</td>
                            <td className="px-4 py-3">
                              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${CRED_STATUS_BADGE[cred.status ?? ""] ?? "bg-slate-100 text-slate-500"}`}>
                                {cred.status ?? "-"}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>

                {canManualReview && (
                  <div className="flex justify-end">
                    <Link href={`/kyc/${id}/manual-review`} className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 transition-colors">
                      수동심사 처리 →
                    </Link>
                  </div>
                )}
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
          onClick={closePreview}
        >
          <div
            className="bg-white rounded-xl shadow-xl w-full max-w-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-5 py-4 border-b border-slate-200">
              <div>
                <p className="font-semibold text-slate-800 text-sm">{previewDoc.documentName}</p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {previewDoc.fileName}
                  {previewDoc.fileSize ? ` · ${previewDoc.fileSize}` : ""}
                  {previewDoc.uploadedAt ? ` · ${fmtDt(previewDoc.uploadedAt)}` : ""}
                </p>
              </div>
              <button
                onClick={closePreview}
                className="text-slate-400 hover:text-slate-600 p-1 rounded hover:bg-slate-100"
              >
                <X size={18} />
              </button>
            </div>
            <div className="p-6">
              {previewLoading ? (
                <div className="bg-slate-100 rounded-lg h-80 flex items-center justify-center text-slate-400 border border-slate-200">
                  <p className="text-sm">미리보기 불러오는 중...</p>
                </div>
              ) : previewUrl ? (
                <iframe
                  src={previewUrl}
                  className="w-full h-80 rounded-lg border border-slate-200"
                  title={previewDoc.documentName}
                />
              ) : (
                <div className="bg-slate-100 rounded-lg h-80 flex flex-col items-center justify-center text-slate-400 border border-slate-200">
                  <FileText size={48} className="text-slate-300 mb-3" />
                  <p className="text-sm font-medium text-slate-600">{previewDoc.fileName}</p>
                  <p className="text-xs mt-1 text-slate-400">PDF 미리보기</p>
                  <p className="text-xs text-slate-300 mt-4">실제 환경에서는 PDF 뷰어가 표시됩니다</p>
                </div>
              )}
            </div>
            <div className="flex justify-end gap-2 px-6 pb-5">
              <button
                onClick={closePreview}
                className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
              >
                닫기
              </button>
              {previewUrl && (
                <a
                  href={previewUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50 flex items-center gap-1.5"
                >
                  <ExternalLink size={14} />
                  새 탭으로 열기
                </a>
              )}
              <button className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 flex items-center gap-1.5">
                <Download size={14} />
                다운로드
              </button>
            </div>
          </div>
        </div>
      )}

      {showIssueMfa && (
        <MfaModal
          purpose="VC_ISSUE"
          onConfirm={handleIssueCredential}
          onClose={() => setShowIssueMfa(false)}
        />
      )}
    </div>
  );
}
