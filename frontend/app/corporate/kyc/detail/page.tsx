"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  type KycDocument,
  type KycReviewSummaryResponse,
  type KycStatusResponse,
  type Supplement,
  kyc as kycApi
} from "@/lib/api";
import { DOCUMENT_LABELS, formatFileSize } from "@/lib/kyc-flow";

const PRE_SUBMIT_STATUSES = new Set(["DRAFT"]);

type StepKey = "received" | "ai" | "manual" | "result";

const TIMELINE: { key: StepKey; label: string }[] = [
  { key: "received", label: "접수 완료" },
  { key: "ai", label: "AI 심사" },
  { key: "manual", label: "운영자 검토" },
  { key: "result", label: "결과 확인" }
];

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "임시 저장",
  SUBMITTED: "제출 완료",
  AI_REVIEWING: "AI 심사 중",
  NEED_SUPPLEMENT: "보완 필요",
  MANUAL_REVIEW: "운영자 검토 중",
  APPROVED: "승인",
  REJECTED: "반려",
  VC_ISSUED: "인증 완료"
};

const STATUS_BADGE: Record<string, "default" | "secondary" | "warning" | "success" | "destructive"> = {
  DRAFT: "secondary",
  SUBMITTED: "default",
  AI_REVIEWING: "default",
  NEED_SUPPLEMENT: "warning",
  MANUAL_REVIEW: "default",
  APPROVED: "success",
  REJECTED: "destructive",
  VC_ISSUED: "success"
};

function formatConfidencePercent(value: number) {
  const percent = value >= 0 && value <= 1 ? value * 100 : value;
  return `${Math.round(percent)}%`;
}

export default function CorporateKycDetailPage() {
  return (
    <Suspense>
      <KycDetail />
    </Suspense>
  );
}

function KycDetail() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const valid = Number.isFinite(kycId) && kycId > 0;

  const [status, setStatus] = useState<KycStatusResponse | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [aiReview, setAiReview] = useState<KycReviewSummaryResponse | null>(null);
  const [supplements, setSupplements] = useState<Supplement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!valid) return;
    setError(null);
    let cancelled = false;
    Promise.all([
      kycApi.status(kycId),
      kycApi.documents(kycId),
      kycApi.aiReviewSummary(kycId).catch(() => null),
      kycApi.supplements(kycId).catch(() => ({ supplements: [] }))
    ])
      .then(([s, d, review, supp]) => {
        if (cancelled) return;
        setStatus(s);
        setDocuments(d);
        setAiReview(review);
        setSupplements(supp.supplements ?? []);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
      });
    return () => {
      cancelled = true;
    };
  }, [kycId, valid]);

  const onSubmitApplication = async () => {
    if (!valid) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await kycApi.submit(kycId);
      setStatus((prev) => ({
        ...(prev ?? { kycId }),
        kycStatus: res.kycStatus ?? prev?.kycStatus
      }));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!valid) {
    return (
      <div className="mx-auto flex w-full max-w-[920px] flex-col">
        <div className="page-head">
          <div>
            <h1 className="page-head-title">KYC 진행상태 조회</h1>
            <p className="page-head-desc">유효한 신청 ID가 필요합니다.</p>
          </div>
        </div>
        <section className="form-card">
          <p className="text-sm text-muted-foreground">
            올바른 신청 ID(id)를 포함한 URL로 접근해 주세요.
          </p>
        </section>
      </div>
    );
  }

  const kycStatus = status?.kycStatus ?? "";
  const beforeSubmit = PRE_SUBMIT_STATUSES.has(kycStatus);
  const isApproved = kycStatus === "APPROVED" || kycStatus === "VC_ISSUED";
  const isRejected = kycStatus === "REJECTED";
  const needSupplement = kycStatus === "NEED_SUPPLEMENT";
  const pendingSupplements = supplements.filter(
    (s) => s.supplementStatus === "REQUESTED"
  );
  const visibleDocuments = documents.filter((d) => d.uploadStatus !== "DELETED");

  const stepStates = computeSteps(kycStatus);
  const progressPct = computeProgressPct(stepStates);

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 진행상태 조회</h1>
          <p className="page-head-desc">
            신청 KYC-{kycId}의 심사 진행 상태와 결과를 확인합니다.
          </p>
        </div>
      </div>

      {error ? (
        <div className="alert alert-warning" style={{ marginBottom: 16 }}>
          <span className="alert-icon">
            <Icon.Alert size={16} />
          </span>
          <span>{error}</span>
        </div>
      ) : null}

      {needSupplement || isRejected ? (
        <div className="alert alert-warning" style={{ marginBottom: 16 }}>
          <span className="alert-icon">
            <Icon.Alert size={16} />
          </span>
          <span>
            {needSupplement
              ? "보완 요청 사항이 있습니다. 기한 내 서류를 재제출해주세요."
              : "심사 결과를 확인해 주세요."}
          </span>
        </div>
      ) : null}

      {isApproved ? (
        <CompletionCard kycId={kycId} status={status} aiReview={aiReview} />
      ) : (
        <>
          <div className="dash-grid-2">
            <section className="form-card m-0">
              <div className="form-card-header">
                <div className="form-card-title">진행 상태</div>
              </div>
              <Timeline steps={stepStates} />
              <div
                style={{
                  marginTop: 20,
                  height: 8,
                  background: "var(--surface-2)",
                  borderRadius: 999,
                  overflow: "hidden",
                  border: "1px solid var(--border)"
                }}
              >
                <div
                  style={{
                    height: "100%",
                    width: `${progressPct}%`,
                    background: "var(--accent)",
                    borderRadius: 999,
                    transition: "width 0.5s ease"
                  }}
                />
              </div>
              <div
                style={{
                  fontSize: 12,
                  color: "var(--text-muted)",
                  marginTop: 6,
                  textAlign: "right"
                }}
              >
                {progressPct}%
              </div>
            </section>

            <section className="form-card m-0">
              <div className="form-card-header">
                <div className="form-card-title">신청 정보</div>
              </div>
              <InfoRow label="신청번호" value={`KYC-${kycId}`} mono />
              <InfoRow
                label="상태"
                value={
                  <Badge variant={STATUS_BADGE[kycStatus] ?? "secondary"}>
                    {STATUS_LABELS[kycStatus] ?? kycStatus ?? "-"}
                  </Badge>
                }
              />
              <InfoRow
                label="법인 유형"
                value={status?.corporateTypeCode ?? "-"}
              />
              <InfoRow
                label="제출 서류"
                value={`${visibleDocuments.length}건`}
              />
              <InfoRow
                label="VC 상태"
                value={
                  <Badge variant={kycStatus === "VC_ISSUED" ? "success" : "secondary"}>
                    {kycStatus === "VC_ISSUED" ? "발급 완료" : "발급 대기"}
                  </Badge>
                }
              />

              {beforeSubmit ? (
                <div style={{ marginTop: 16 }}>
                  <Button
                    type="button"
                    className="btn-block"
                    onClick={onSubmitApplication}
                    disabled={submitting}
                  >
                    {submitting ? "제출 중..." : "신청 제출"}
                  </Button>
                </div>
              ) : null}
            </section>
          </div>

          {aiReview&& !beforeSubmit ? (
            <AiReviewSummary
              review={aiReview}
              hasSupplements={pendingSupplements.length > 0}
              kycId={kycId}
              firstSupplementId={pendingSupplements[0]?.supplementId}
            />
          ) : null}

          {pendingSupplements.length > 0 ? (
            <PendingSupplements
              kycId={kycId}
              supplements={pendingSupplements}
            />
          ) : null}
        </>
      )}

      <DocumentList documents={visibleDocuments} />
    </div>
  );
}

function Timeline({
  steps
}: {
  steps: { key: StepKey; label: string; done: boolean; active: boolean; meta?: string }[];
}) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 0 }}>
      {steps.map((s, i) => (
        <div
          key={s.key}
          style={{
            display: "flex",
            gap: 16,
            alignItems: "flex-start",
            paddingBottom: i < steps.length - 1 ? 24 : 0,
            position: "relative"
          }}
        >
          {i < steps.length - 1 && (
            <div
              style={{
                position: "absolute",
                left: 13,
                top: 28,
                width: 2,
                height: "calc(100% - 4px)",
                background: s.done ? "var(--success)" : "var(--border)"
              }}
            />
          )}
          <div
            style={{
              width: 28,
              height: 28,
              borderRadius: "50%",
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 12,
              fontWeight: 700,
              zIndex: 1,
              background: s.done
                ? "var(--success)"
                : s.active
                  ? "var(--accent)"
                  : "var(--surface-2)",
              color: s.done || s.active ? "#fff" : "var(--text-muted)",
              border: `2px solid ${
                s.done
                  ? "var(--success)"
                  : s.active
                    ? "var(--accent)"
                    : "var(--border)"
              }`
            }}
          >
            {s.done ? <Icon.Check size={13} /> : i + 1}
          </div>
          <div style={{ paddingTop: 4 }}>
            <div
              style={{
                fontSize: 14,
                fontWeight: s.active ? 700 : 500,
                color: s.active
                  ? "var(--text-primary)"
                  : s.done
                    ? "var(--text-secondary)"
                    : "var(--text-muted)"
              }}
            >
              {s.label}
            </div>
            {s.meta ? (
              <div
                style={{
                  fontSize: 12,
                  color: s.active ? "var(--accent)" : "var(--text-muted)",
                  marginTop: 2
                }}
              >
                {s.meta}
              </div>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  );
}

function AiReviewSummary({
  review,
  hasSupplements,
  kycId,
  firstSupplementId
}: {
  review: KycReviewSummaryResponse;
  hasSupplements: boolean;
  kycId: number;
  firstSupplementId?: number;
}) {
  const findings = review.findings ?? [];
  const passItems = findings.filter((f) => (f.result ?? "").toUpperCase() === "PASS");
  const reviewItems = findings.filter((f) => (f.result ?? "").toUpperCase() !== "PASS");
  const fallbackPass: { label: string }[] =
    passItems.length === 0
      ? [
          { label: "서류 진위 검증" },
          { label: "OCR 텍스트 추출" },
          { label: "신분증 유효성" }
        ]
      : passItems.map((f) => ({ label: f.findingType ?? "검증 항목" }));

  return (
    <div className="dash-grid-2" style={{ marginTop: 16 }}>
      <section className="form-card m-0">
        <div className="form-card-header">
          <div className="form-card-title">AI 심사 통과 항목</div>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 0 }}>
          {fallbackPass.map((item, i) => (
            <div
              key={i}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "10px 0",
                borderBottom: "1px solid var(--divider)"
              }}
            >
              <div
                style={{
                  width: 24,
                  height: 24,
                  borderRadius: "50%",
                  background: "var(--success-soft)",
                  color: "var(--success)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  flexShrink: 0
                }}
              >
                <Icon.Check size={13} />
              </div>
              <span style={{ fontSize: 13.5 }}>{item.label}</span>
              <Badge variant="success" style={{ marginLeft: "auto" }}>
                통과
              </Badge>
            </div>
          ))}
          {reviewItems.map((f, i) => (
            <div
              key={`r-${i}`}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "10px 0",
                borderBottom:
                  i < reviewItems.length - 1
                    ? "1px solid var(--divider)"
                    : "none"
              }}
            >
              <div
                style={{
                  width: 24,
                  height: 24,
                  borderRadius: "50%",
                  background: "var(--warning-soft)",
                  color: "var(--warning)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  flexShrink: 0
                }}
              >
                <Icon.Alert size={13} />
              </div>
              <span style={{ fontSize: 13.5 }}>
                {f.message ?? "검토 필요"}
              </span>
              <Badge variant="warning" style={{ marginLeft: "auto" }}>
                보완
              </Badge>
            </div>
          ))}
        </div>
      </section>

      <section className="form-card m-0">
        <div className="form-card-header">
          <div className="form-card-title">심사 의견</div>
        </div>
        <p
          style={{
            fontSize: 13,
            color: "var(--text-secondary)",
            lineHeight: 1.6,
            margin: 0
          }}
        >
          {review.summaryMessage ?? "AI 심사가 진행되었습니다. 상세 내역을 확인해주세요."}
        </p>
        {review.confidenceScore != null ? (
          <div style={{ marginTop: 12 }}>
            <InfoRow
              label="신뢰도"
              value={formatConfidencePercent(review.confidenceScore ?? 0)}
            />
          </div>
        ) : null}
        {hasSupplements && firstSupplementId ? (
          <div
            style={{
              marginTop: 16,
              display: "flex",
              flexDirection: "column",
              gap: 8
            }}
          >
            <Button asChild className="btn-block">
              <Link
                href={`/corporate/kyc/detail/documents?id=${kycId}&supplementId=${firstSupplementId}`}
              >
                보완 서류 제출
              </Link>
            </Button>
          </div>
        ) : null}
      </section>
    </div>
  );
}

function PendingSupplements({
  kycId,
  supplements
}: {
  kycId: number;
  supplements: Supplement[];
}) {
  return (
    <section className="form-card" style={{ marginTop: 16 }}>
      <div className="form-card-header">
        <div>
          <div className="form-card-title">보완 요청 {supplements.length}건</div>
          <div className="form-card-meta">
            아래 항목을 확인 후 기한 내 재제출해주세요.
          </div>
        </div>
        <Badge variant="warning">
          <Icon.Alert size={11} /> 보완 필요
        </Badge>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
        {supplements.map((s) => (
          <div
            key={s.supplementId}
            style={{
              border: "1px solid var(--border)",
              borderRadius: "var(--radius-md)",
              padding: "14px 16px"
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                marginBottom: 6
              }}
            >
              <div
                style={{
                  width: 20,
                  height: 20,
                  borderRadius: "50%",
                  background: "var(--danger-soft)",
                  color: "var(--danger)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  flexShrink: 0
                }}
              >
                <Icon.Alert size={11} />
              </div>
              <span style={{ fontSize: 13.5, fontWeight: 700 }}>
                {s.title ?? `보완요청 #${s.supplementId}`}
              </span>
              {s.dueAt ? (
                <span
                  style={{
                    marginLeft: "auto",
                    fontSize: 12,
                    color: "var(--danger)",
                    fontWeight: 600
                  }}
                >
                  마감 {formatDate(s.dueAt)}
                </span>
              ) : null}
            </div>
            <div
              style={{
                fontSize: 13,
                color: "var(--text-secondary)",
                lineHeight: 1.6,
                paddingLeft: 28
              }}
            >
              {s.message ?? s.requestReason ?? "추가 자료 제출이 필요합니다."}
            </div>
            <div
              style={{
                marginTop: 12,
                paddingLeft: 28,
                display: "flex",
                gap: 8
              }}
            >
              <Button asChild size="sm">
                <Link
                  href={`/corporate/kyc/detail/documents?id=${kycId}&supplementId=${s.supplementId}`}
                >
                  보완 제출하기
                </Link>
              </Button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function DocumentList({ documents }: { documents: KycDocument[] }) {
  if (documents.length === 0) {
    return (
      <section className="form-card" style={{ marginTop: 16 }}>
        <div className="form-card-header">
          <div className="form-card-title">제출 서류</div>
        </div>
        <p className="text-sm text-muted-foreground">업로드된 서류가 없습니다.</p>
      </section>
    );
  }
  return (
    <section className="form-card" style={{ marginTop: 16 }}>
      <div className="form-card-header">
        <div className="form-card-title">제출 서류 {documents.length}건</div>
      </div>
      <ul style={{ display: "flex", flexDirection: "column", gap: 8, margin: 0, padding: 0, listStyle: "none" }}>
        {documents.map((d) => (
          <li
            key={d.documentId}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 12,
              padding: "12px 14px",
              border: "1px solid var(--border)",
              borderRadius: "var(--radius-md)"
            }}
          >
            <Icon.File size={18} style={{ color: "var(--text-muted)" }} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600 }}>
                {DOCUMENT_LABELS[d.documentTypeCode ?? ""] ?? d.documentTypeCode ?? "-"}
              </div>
              <div
                style={{
                  fontSize: 12,
                  color: "var(--text-muted)",
                  whiteSpace: "nowrap",
                  overflow: "hidden",
                  textOverflow: "ellipsis"
                }}
              >
                {d.fileName ?? "-"} · {formatFileSize(d.fileSize)}
              </div>
            </div>
            {d.uploadStatus ? (
              <Badge variant={d.uploadStatus === "UPLOADED" ? "success" : "secondary"}>
                {d.uploadStatus === "UPLOADED" ? "완료" : d.uploadStatus}
              </Badge>
            ) : null}
          </li>
        ))}
      </ul>
    </section>
  );
}

function CompletionCard({
  kycId,
  status,
  aiReview
}: {
  kycId: number;
  status: KycStatusResponse | null;
  aiReview: KycReviewSummaryResponse | null;
}) {
  const isVcIssued = status?.kycStatus === "VC_ISSUED";
  return (
    <section className="form-card" style={{ maxWidth: 560 }}>
      <div style={{ textAlign: "center", padding: "24px 0 20px" }}>
        <div
          style={{
            width: 64,
            height: 64,
            borderRadius: "50%",
            background: "var(--success-soft)",
            color: "var(--success)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            margin: "0 auto 16px"
          }}
        >
          <Icon.Check size={30} />
        </div>
        <div
          style={{
            fontSize: 20,
            fontWeight: 800,
            letterSpacing: "-0.02em",
            marginBottom: 6
          }}
        >
          KYC 인증이 완료되었습니다
        </div>
        <div style={{ fontSize: 13.5, color: "var(--text-secondary)" }}>
          {isVcIssued
            ? "VC 발급이 완료되어 인증서를 활용할 수 있습니다."
            : "심사가 승인되었습니다. VC 발급을 진행해 주세요."}
        </div>
      </div>
      <div style={{ borderTop: "1px solid var(--divider)", paddingTop: 20 }}>
        <InfoRow label="신청번호" value={`KYC-${kycId}`} mono />
        <InfoRow
          label="상태"
          value={
            <Badge variant="success">
              {STATUS_LABELS[status?.kycStatus ?? ""] ?? "인증 완료"}
            </Badge>
          }
        />
        {aiReview?.aiReviewResult ? (
          <InfoRow
            label="심사 결과"
            value={<Badge variant="default">{aiReview.aiReviewResult}</Badge>}
          />
        ) : null}
        {status?.submittedAt ? (
          <InfoRow label="접수일시" value={formatDateTime(status.submittedAt)} />
        ) : null}
        <InfoRow
          label="VC 상태"
          value={
            <Badge variant={isVcIssued ? "success" : "secondary"}>
              {isVcIssued ? "발급 완료" : "발급 대기"}
            </Badge>
          }
        />
      </div>
    </section>
  );
}

function InfoRow({
  label,
  value,
  mono = false
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <div className="kv-row">
      <div className="kv-key">{label}</div>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value ?? "-"}</div>
    </div>
  );
}

function computeSteps(kycStatus: string) {
  const order: Record<string, number> = {
    DRAFT: 0,
    SUBMITTED: 1,
    AI_REVIEWING: 1,
    NEED_SUPPLEMENT: 2,
    MANUAL_REVIEW: 2,
    APPROVED: 3,
    REJECTED: 3,
    VC_ISSUED: 3
  };
  const cur = order[kycStatus] ?? 0;

  return TIMELINE.map((s, i) => {
    const done = i < cur;
    const active = i === cur && kycStatus !== "DRAFT";
    return {
      ...s,
      done,
      active,
      meta: stepMeta(s.key, kycStatus, active)
    };
  });
}

function stepMeta(key: StepKey, kycStatus: string, active: boolean) {
  if (!active) return undefined;
  if (key === "ai" && kycStatus === "AI_REVIEWING") return "진행 중...";
  if (key === "manual" && kycStatus === "MANUAL_REVIEW") return "진행 중...";
  if (key === "manual" && kycStatus === "NEED_SUPPLEMENT") return "보완 필요";
  if (key === "result") {
    if (kycStatus === "APPROVED") return "승인";
    if (kycStatus === "REJECTED") return "반려";
    if (kycStatus === "VC_ISSUED") return "VC 발급 완료";
  }
  return undefined;
}

function computeProgressPct(steps: { done: boolean; active: boolean }[]) {
  const total = steps.length;
  const completed = steps.filter((s) => s.done).length;
  const activeBonus = steps.some((s) => s.active) ? 0.5 : 0;
  const pct = ((completed + activeBonus) / total) * 100;
  return Math.round(pct);
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  });
}
