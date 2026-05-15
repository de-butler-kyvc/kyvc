"use client";

import { use, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  getAiReview,
  getAiReviewAgentAuthority,
  getAiReviewBeneficialOwners,
  getKycCorporate,
  getAiReviewMismatches,
  type AgentAuthority,
  type AiMismatch,
  type AiReviewResult,
  type BackendKycCorporate,
  type BeneficialOwner,
} from "@/lib/api/kyc";
import { kycDetailPath } from "@/lib/navigation/admin-routes";

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

const ASSESSMENT_STATUS_KO: Record<string, string> = {
  NORMAL: "정상",
  SUPPLEMENT_REQUIRED: "보완필요",
  MANUAL_REVIEW_REQUIRED: "수동심사필요",
  REJECTED: "반려",
};

const CHECK_STATUS_KO: Record<string, string> = {
  PASS: "일치",
  MATCH: "일치",
  FAILED: "불일치",
  FAIL: "불일치",
  MISMATCH: "불일치",
  WARNING: "검토필요",
  REVIEW_REQUIRED: "검토필요",
};

const SEVERITY_KO: Record<string, string> = {
  INFO: "낮음",
  LOW: "낮음",
  MEDIUM: "중간",
  HIGH: "높음",
  CRITICAL: "긴급",
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

function formatPercent(value?: number | string | null) {
  if (value === null || value === undefined) return "-";
  const numeric = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(numeric)) return "-";
  const normalized = numeric <= 1 ? numeric * 100 : numeric;
  return `${Math.round(normalized)}%`;
}

type DetailRecord = Record<string, unknown>;

type AssessmentIssue = {
  code: string;
  message: string;
  evidenceRefs: string[];
};

type CrossDocumentCheck = {
  checkCode: string;
  status: string;
  severity?: string;
  message: string;
  confidence?: number;
  evidenceRefs: string[];
};

type AssessmentDetail = {
  assessmentId?: string;
  assessmentStatus?: string;
  confidenceScore?: number;
  message?: string;
  createdAt?: string;
  supplementRequests: AssessmentIssue[];
  manualReviewReasons: AssessmentIssue[];
  crossDocumentChecks: CrossDocumentCheck[];
};

type ClaimSection = {
  title: string;
  rows: Array<{ label: string; value: string }>;
};

function isRecord(value: unknown): value is DetailRecord {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseDetailJson(raw?: string): DetailRecord | null {
  if (!raw?.trim()) return null;
  try {
    const parsed = JSON.parse(raw) as unknown;
    return isRecord(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function asRecord(source: DetailRecord | null | undefined, key: string) {
  const value = source?.[key];
  return isRecord(value) ? value : null;
}

function asRecordArray(source: DetailRecord | null | undefined, key: string) {
  const value = source?.[key];
  return Array.isArray(value) ? value.filter(isRecord) : [];
}

function textValue(source: DetailRecord | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = source?.[key];
    if (typeof value === "string" && value.trim()) return value.trim();
    if (typeof value === "number" || typeof value === "boolean") return String(value);
  }
  return undefined;
}

function numberValue(source: DetailRecord | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = source?.[key];
    if (typeof value === "number" && Number.isFinite(value)) return value;
    if (typeof value === "string" && value.trim()) {
      const parsed = Number(value);
      if (Number.isFinite(parsed)) return parsed;
    }
  }
  return undefined;
}

function ynValue(source: DetailRecord | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = source?.[key];
    if (typeof value === "boolean") return value ? "Y" : "N";
    if (typeof value === "string") {
      if (["Y", "N"].includes(value)) return value;
      if (value.toLowerCase() === "true") return "Y";
      if (value.toLowerCase() === "false") return "N";
    }
  }
  return undefined;
}

function hasText(value?: string) {
  return !!value?.trim() && value !== "-";
}

function uniqueOwners(owners: BeneficialOwner[]) {
  const seen = new Set<string>();
  return owners.filter((owner) => {
    const key = `${owner.ownerName}:${owner.shareRatio ?? owner.ownershipRatio ?? ""}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function normalizeOwner(source: DetailRecord): BeneficialOwner | null {
  const ownerName = textValue(source, "ownerName", "name");
  const shareRatio = numberValue(source, "shareRatio", "ownershipRatio", "ownershipPercent", "ownershipPercentage", "ratio");
  const beneficialOwnerYn = ynValue(source, "beneficialOwnerYn", "isBeneficialOwner", "beneficialOwner") ?? "Y";
  if (!ownerName && shareRatio == null) return null;
  return {
    ownerName: ownerName ?? "-",
    ownershipRatio: shareRatio,
    shareRatio,
    beneficialOwnerYn,
    controlTypeCode: textValue(source, "controlTypeCode", "holderType", "controlType"),
    judgementReason: textValue(source, "judgementReason", "basis", "reason"),
    judgment: textValue(source, "judgment", "status", "result") ?? (beneficialOwnerYn === "Y" ? "NORMAL" : "NEEDS_REVIEW"),
  };
}

function extractOwnersFromDetail(raw?: string) {
  const detail = parseDetailJson(raw);
  const claims = asRecord(detail, "claims");
  const assessment = asRecord(detail, "assessment");
  const extracted = asRecord(assessment, "extractedFields") ?? asRecord(detail, "extractedFields") ?? asRecord(claims, "extractedFields");
  const beneficialOwnership = asRecord(assessment, "beneficialOwnership") ?? asRecord(detail, "beneficialOwnership") ?? asRecord(claims, "beneficialOwnership");
  const rows = [
    ...asRecordArray(claims, "beneficialOwners"),
    ...asRecordArray(detail, "beneficialOwners"),
    ...asRecordArray(beneficialOwnership, "owners"),
    ...asRecordArray(extracted, "beneficialOwners"),
  ];
  return uniqueOwners(rows.map(normalizeOwner).filter((owner): owner is BeneficialOwner => owner !== null));
}

function hasOwnerData(owner: BeneficialOwner) {
  return hasText(owner.ownerName) ||
    typeof owner.shareRatio === "number" ||
    typeof owner.ownershipRatio === "number";
}

function authorityScopeFrom(source: DetailRecord | null | undefined) {
  const direct = textValue(source, "authorityType", "authorityScope", "scope", "authorityText");
  if (direct) return direct;

  const authority = asRecord(source, "authority") ?? source;
  const scopes = [
    ynValue(authority, "kycApplication") === "Y" ? "KYC 신청" : null,
    ynValue(authority, "documentSubmission") === "Y" ? "서류 제출" : null,
    ynValue(authority, "vcReceipt") === "Y" ? "VC 수령" : null,
  ].filter((scope): scope is string => scope !== null);
  return scopes.length > 0 ? scopes.join(", ") : undefined;
}

function judgmentFrom(source: DetailRecord | null | undefined, authorityValidYn?: string) {
  const raw = textValue(source, "judgment", "result", "status")?.toUpperCase();
  if (raw === "AUTHORIZED" || raw === "VALID" || raw === "PASS" || raw === "NORMAL") return "VALID";
  if (raw === "NOT_AUTHORIZED" || raw === "INVALID" || raw === "FAIL" || raw === "FAILED") return "INVALID";
  if (authorityValidYn === "Y") return "VALID";
  if (authorityValidYn === "N") return "INVALID";
  return "NEEDS_REVIEW";
}

function normalizeAgent(source: DetailRecord): AgentAuthority | null {
  const authority = asRecord(source, "authority");
  const authorityFlags = ["kycApplication", "documentSubmission", "vcReceipt"]
    .map((key) => ynValue(authority ?? source, key))
    .filter((value): value is string => !!value);
  const derivedAuthorityValidYn = authorityFlags.length > 0
    ? authorityFlags.every((value) => value === "Y") ? "Y" : "N"
    : undefined;
  const authorityValidYn = ynValue(source, "authorityValidYn", "authorityValid", "validAuthority")
    ?? derivedAuthorityValidYn;
  const agentName = textValue(source, "agentName", "name", "delegateName");
  const authorityType = authorityScopeFrom(source);
  const validFrom = textValue(source, "validFrom", "from");
  const validTo = textValue(source, "validTo", "validUntil", "until");
  const hasData = agentName || authorityType || validFrom || validTo || authorityValidYn;
  if (!hasData) return null;
  return {
    agentName: agentName ?? "-",
    authorityScope: authorityType,
    authorityType,
    signatureVerifiedYn: ynValue(source, "signatureVerifiedYn", "signatureVerified", "hasSignature"),
    sealVerifiedYn: ynValue(source, "sealVerifiedYn", "sealVerified", "hasSeal"),
    authorityValidYn,
    confidenceScore: numberValue(source, "confidenceScore", "confidence"),
    judgementReason: textValue(source, "judgementReason", "reason", "basis"),
    validFrom,
    validTo,
    judgment: judgmentFrom(source, authorityValidYn),
  };
}

function extractAgentsFromDetail(raw?: string) {
  const detail = parseDetailJson(raw);
  const claims = asRecord(detail, "claims");
  const assessment = asRecord(detail, "assessment");
  const extracted = asRecord(assessment, "extractedFields") ?? asRecord(detail, "extractedFields") ?? asRecord(claims, "extractedFields");
  const delegate = asRecord(claims, "delegate") ?? asRecord(extracted, "delegate") ?? asRecord(detail, "delegate");
  const delegation = asRecord(claims, "delegation") ?? asRecord(extracted, "delegation") ?? asRecord(assessment, "delegation") ?? asRecord(detail, "delegation");
  const combined = delegate || delegation ? normalizeAgent({ ...(delegation ?? {}), ...(delegate ?? {}) }) : null;
  const rows = [
    ...(combined ? [combined] : []),
    ...asRecordArray(claims, "agentAuthorities").map(normalizeAgent),
    ...asRecordArray(detail, "agentAuthorities").map(normalizeAgent),
  ].filter((agent): agent is AgentAuthority => agent !== null);
  const single = normalizeAgent(asRecord(claims, "agentAuthority") ?? asRecord(detail, "agentAuthority") ?? {});
  return single ? [single, ...rows] : rows;
}

function hasAgentData(agent: AgentAuthority) {
  return hasText(agent.agentName) ||
    hasText(agent.authorityType) ||
    hasText(agent.authorityScope) ||
    hasText(agent.validFrom) ||
    hasText(agent.validTo) ||
    !!agent.authorityValidYn;
}

function mergeAgentFallback(agents: AgentAuthority[], corporate: BackendKycCorporate | null) {
  const corporateName = corporate?.agentName?.trim();
  const corporateScope = corporate?.agentAuthorityScope?.trim();
  const hasCorporateAgent = !!corporateName || !!corporateScope;
  const meaningfulAgents = agents.filter(hasAgentData);

  if (meaningfulAgents.length > 0) {
    return meaningfulAgents.map((agent, index) => index === 0
      ? {
          ...agent,
          agentName: hasText(agent.agentName) ? agent.agentName : corporateName ?? "-",
          authorityScope: agent.authorityScope ?? agent.authorityType ?? corporateScope,
          authorityType: agent.authorityType ?? agent.authorityScope ?? corporateScope,
        }
      : agent
    );
  }

  return hasCorporateAgent
    ? [{
        agentName: corporateName ?? "-",
        authorityScope: corporateScope,
        authorityType: corporateScope,
        judgment: "NEEDS_REVIEW",
    }]
    : [];
}

function stringListValue(source: DetailRecord | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = source?.[key];
    if (Array.isArray(value)) {
      return value
        .map((item) => typeof item === "string" || typeof item === "number" ? String(item) : undefined)
        .filter((item): item is string => !!item?.trim());
    }
    if (typeof value === "string" && value.trim()) return [value.trim()];
  }
  return [];
}

function normalizeIssue(source: DetailRecord): AssessmentIssue | null {
  const code = textValue(source, "code", "issueCode", "reasonCode");
  const message = textValue(source, "message", "reason", "summary");
  const evidenceRefs = stringListValue(source, "evidenceRefs", "evidence_refs", "refs");
  if (!code && !message && evidenceRefs.length === 0) return null;
  return {
    code: code ?? "-",
    message: message ?? "-",
    evidenceRefs,
  };
}

function normalizeCrossCheck(source: DetailRecord): CrossDocumentCheck | null {
  const checkCode = textValue(source, "checkCode", "code", "type");
  const status = textValue(source, "status", "result", "judgment");
  const message = textValue(source, "message", "reason", "summary");
  const evidenceRefs = stringListValue(source, "evidenceRefs", "evidence_refs", "refs");
  if (!checkCode && !status && !message && evidenceRefs.length === 0) return null;
  return {
    checkCode: checkCode ?? "-",
    status: status ?? "-",
    severity: textValue(source, "severity", "level"),
    message: message ?? "-",
    confidence: numberValue(source, "confidence", "confidenceScore"),
    evidenceRefs,
  };
}

function extractAssessmentDetail(raw?: string): AssessmentDetail | null {
  const parsed = parseDetailJson(raw);
  const assessment = asRecord(parsed, "assessment") ?? parsed;
  if (!assessment) return null;

  const supplementRequests = asRecordArray(assessment, "supplementRequests")
    .map(normalizeIssue)
    .filter((issue): issue is AssessmentIssue => issue !== null);
  const manualReviewReasons = asRecordArray(assessment, "manualReviewReasons")
    .map(normalizeIssue)
    .filter((issue): issue is AssessmentIssue => issue !== null);
  const crossDocumentChecks = asRecordArray(assessment, "crossDocumentChecks")
    .map(normalizeCrossCheck)
    .filter((check): check is CrossDocumentCheck => check !== null);

  const detail = {
    assessmentId: textValue(assessment, "assessmentId"),
    assessmentStatus: textValue(assessment, "status", "assessmentStatus"),
    confidenceScore: numberValue(assessment, "overallConfidence", "confidenceScore", "confidence"),
    message: textValue(assessment, "message", "summary"),
    createdAt: textValue(assessment, "createdAt"),
    supplementRequests,
    manualReviewReasons,
    crossDocumentChecks,
  };
  const hasData = detail.assessmentId || detail.assessmentStatus || detail.confidenceScore != null ||
    detail.message || detail.createdAt || supplementRequests.length > 0 ||
    manualReviewReasons.length > 0 || crossDocumentChecks.length > 0;
  return hasData ? detail : null;
}

const CLAIM_LABELS: Record<string, string> = {
  jurisdiction: "관할",
  assuranceLevel: "보증 수준",
  type: "유형",
  name: "이름",
  registrationNumber: "등록번호",
  nonProfit: "비영리 여부",
  purposeCheckRequired: "목적 확인 필요",
  birthDate: "생년월일",
  nationality: "국적",
  englishName: "영문명",
  ownershipPercent: "지분율",
  basis: "판단 기준",
  contact: "연락처",
  address: "주소",
  identityDigest: "신원 식별 해시",
  identityDigestAlgorithm: "해시 알고리즘",
  identityDigestVersion: "해시 버전",
  kycApplication: "KYC 신청 권한",
  documentSubmission: "서류 제출 권한",
  vcReceipt: "VC 수령 권한",
  validFrom: "유효 시작",
  validUntil: "유효 종료",
  status: "상태",
};

function claimLabel(key: string) {
  return CLAIM_LABELS[key] ?? key;
}

function claimValue(value: unknown) {
  if (value === null || value === undefined) return undefined;
  if (typeof value === "boolean") return value ? "Y" : "N";
  if (typeof value === "number") return String(value);
  if (typeof value === "string") return value.trim() || undefined;
  return undefined;
}

function claimRows(source: DetailRecord | null | undefined) {
  if (!source) return [];
  return Object.entries(source)
    .filter(([key]) => key !== "coreRequestId")
    .map(([key, value]) => ({ label: claimLabel(key), value: claimValue(value) }))
    .filter((row): row is { label: string; value: string } => !!row.value);
}

function extractClaimSections(raw?: string): ClaimSection[] {
  const detail = parseDetailJson(raw);
  const claims = asRecord(detail, "claims");
  if (!claims) return [];

  const sections: ClaimSection[] = [];
  [
    ["KYC", asRecord(claims, "kyc")],
    ["법인", asRecord(claims, "legalEntity")],
    ["대표자", asRecord(claims, "representative")],
    ["대리인", asRecord(claims, "delegate")],
    ["위임권한", asRecord(claims, "delegation")],
  ].forEach(([title, source]) => {
    const rows = claimRows(source as DetailRecord | null);
    if (rows.length > 0) sections.push({ title: title as string, rows });
  });

  asRecordArray(claims, "beneficialOwners").forEach((owner, index) => {
    const rows = claimRows(owner);
    if (rows.length > 0) sections.push({ title: `실소유자 ${index + 1}`, rows });
  });
  return sections;
}

function EvidenceRefs({ refs }: { refs: string[] }) {
  if (refs.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-1.5 pt-2">
      {refs.map((ref) => (
        <span key={ref} className="rounded bg-slate-100 px-2 py-0.5 text-[11px] text-slate-500">{ref}</span>
      ))}
    </div>
  );
}

function InfoGrid({ items }: { items: Array<{ label: string; value?: string }> }) {
  const rows = items.filter((item) => hasText(item.value));
  if (rows.length === 0) return null;
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
      {rows.map((item) => (
        <div key={item.label} className="rounded border border-slate-100 bg-slate-50 px-3 py-2">
          <p className="text-[11px] text-slate-400">{item.label}</p>
          <p className="text-sm text-slate-700 break-words">{item.value}</p>
        </div>
      ))}
    </div>
  );
}

function IssueList({ title, issues }: { title: string; issues: AssessmentIssue[] }) {
  if (issues.length === 0) return null;
  return (
    <div>
      <p className="text-xs font-semibold text-slate-500 mb-2">{title}</p>
      <div className="space-y-2">
        {issues.map((issue, index) => (
          <div key={`${issue.code}-${index}`} className="rounded border border-slate-100 bg-white px-3 py-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">{issue.code}</span>
              <p className="text-sm text-slate-700">{issue.message}</p>
            </div>
            <EvidenceRefs refs={issue.evidenceRefs} />
          </div>
        ))}
      </div>
    </div>
  );
}

function CrossCheckList({ checks }: { checks: CrossDocumentCheck[] }) {
  if (checks.length === 0) return null;
  return (
    <div>
      <p className="text-xs font-semibold text-slate-500 mb-2">문서 간 검증</p>
      <div className="space-y-2">
        {checks.map((check, index) => {
          const status = toKo(CHECK_STATUS_KO, check.status);
          return (
            <div key={`${check.checkCode}-${index}`} className="rounded border border-slate-100 bg-white px-3 py-3">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600">{check.checkCode}</span>
                <span className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${badgeClass(status)}`}>{status}</span>
                {check.severity && <span className="text-[11px] text-slate-400">{toKo(SEVERITY_KO, check.severity)}</span>}
                {check.confidence != null && <span className="text-[11px] text-slate-400">{formatPercent(check.confidence)}</span>}
              </div>
              <p className="mt-2 text-sm text-slate-700">{check.message}</p>
              <EvidenceRefs refs={check.evidenceRefs} />
            </div>
          );
        })}
      </div>
    </div>
  );
}

function ClaimSectionList({ sections }: { sections: ClaimSection[] }) {
  if (sections.length === 0) return null;
  return (
    <div>
      <p className="text-xs font-semibold text-slate-500 mb-2">Claims 판단 데이터</p>
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-3">
        {sections.map((section) => (
          <div key={section.title} className="rounded border border-slate-100 bg-white px-3 py-3">
            <p className="text-xs font-semibold text-slate-600 mb-2">{section.title}</p>
            <div className="space-y-1.5">
              {section.rows.map((row) => (
                <div key={`${section.title}-${row.label}`} className="flex gap-3 text-xs">
                  <span className="w-24 shrink-0 text-slate-400">{row.label}</span>
                  <span className="min-w-0 text-slate-700 break-words">{row.value}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
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
      getKycCorporate(id),
    ])
      .then(([reviewResult, mismatchResult, ownerResult, agentResult, corporateResult]) => {
        if (!alive) return;

        const nextErrors: Record<string, string> = {};
        const reviewValue = reviewResult.status === "fulfilled" ? reviewResult.value : null;
        const corporateValue = corporateResult.status === "fulfilled" ? corporateResult.value : null;

        if (reviewResult.status === "fulfilled") setReview(reviewResult.value);
        else nextErrors.review = reviewResult.reason instanceof Error ? reviewResult.reason.message : "AI 결과 로드 실패";

        if (mismatchResult.status === "fulfilled") setMismatches(mismatchResult.value);
        else nextErrors.mismatches = mismatchResult.reason instanceof Error ? mismatchResult.reason.message : "불일치 항목 로드 실패";

        const detailOwners = uniqueOwners([
          ...extractOwnersFromDetail(reviewValue?.detailJson),
          ...extractOwnersFromDetail(reviewValue?.coreAiAssessmentJson),
        ]);
        if (ownerResult.status === "fulfilled") {
          setOwners(ownerResult.value.some(hasOwnerData) ? ownerResult.value : detailOwners);
        } else if (detailOwners.length > 0) {
          setOwners(detailOwners);
        } else nextErrors.owners = ownerResult.reason instanceof Error ? ownerResult.reason.message : "실소유자 로드 실패";

        const detailAgents = [
          ...extractAgentsFromDetail(reviewValue?.detailJson),
          ...extractAgentsFromDetail(reviewValue?.coreAiAssessmentJson),
        ];
        if (agentResult.status === "fulfilled") {
          const sourceAgents = agentResult.value.some(hasAgentData) ? agentResult.value : detailAgents;
          const mergedAgents = mergeAgentFallback(
            sourceAgents,
            corporateValue
          );
          setAgents(mergedAgents.length > 0 ? mergedAgents : mergeAgentFallback(detailAgents, corporateValue));
        } else {
          const fallbackAgents = mergeAgentFallback(detailAgents, corporateValue);
          if (fallbackAgents.length > 0) setAgents(fallbackAgents);
          else nextErrors.agents = agentResult.reason instanceof Error ? agentResult.reason.message : "대리권 로드 실패";
        }

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
      reviewedAt: fmtDt(review?.reviewedAt),
      summaryReason: review?.summaryReason,
      manualReviewReason: review?.manualReviewReason,
    };
  }, [review]);

  const assessmentDetail = useMemo(
    () => extractAssessmentDetail(review?.coreAiAssessmentJson || review?.detailJson),
    [review?.coreAiAssessmentJson, review?.detailJson]
  );
  const claimSections = useMemo(
    () => extractClaimSections(review?.detailJson),
    [review?.detailJson]
  );
  const hasGrounds = !!summary.summaryReason || !!summary.manualReviewReason || !!assessmentDetail || claimSections.length > 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드어드민 · <Link href={kycDetailPath(id)} className="hover:underline">KYC 신청 상세</Link>
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

            {hasGrounds && (
              <div className="bg-white rounded-lg border border-slate-200 p-5 space-y-4">
                <h2 className="text-sm font-semibold text-slate-700">AI 판단 근거</h2>
                {assessmentDetail && (
                  <div className="space-y-3">
                    <p className="text-xs font-semibold text-slate-500">Core 판정 정보</p>
                    <InfoGrid
                      items={[
                        { label: "Assessment ID", value: assessmentDetail.assessmentId },
                        {
                          label: "Assessment Status",
                          value: assessmentDetail.assessmentStatus
                            ? toKo(ASSESSMENT_STATUS_KO, assessmentDetail.assessmentStatus)
                            : undefined,
                        },
                        { label: "Confidence Score", value: formatPercent(assessmentDetail.confidenceScore) },
                        { label: "Message", value: assessmentDetail.message },
                        { label: "Created At", value: fmtDt(assessmentDetail.createdAt) },
                      ]}
                    />
                  </div>
                )}
                {assessmentDetail && (
                  <>
                    <IssueList title="보완 요청" issues={assessmentDetail.supplementRequests} />
                    <IssueList title="수동심사 사유" issues={assessmentDetail.manualReviewReasons} />
                    <CrossCheckList checks={assessmentDetail.crossDocumentChecks} />
                  </>
                )}
                <ClaimSectionList sections={claimSections} />
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
              </div>
            )}

            <div className="flex justify-end">
              <Link href={kycDetailPath(id)} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
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
