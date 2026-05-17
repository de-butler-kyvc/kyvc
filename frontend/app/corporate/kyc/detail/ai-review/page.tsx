"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  ApiError,
  type KycAiReviewDetailResponse,
  type KycDocument,
  type KycReviewSummaryResponse,
  type Supplement,
  kyc as kycApi
} from "@/lib/api";
import { DOCUMENT_LABELS } from "@/lib/kyc-flow";

export default function CorporateKycAiReviewPage() {
  return (
    <Suspense>
      <AiReviewView />
    </Suspense>
  );
}

function formatConfidencePercent(value: number) {
  const percent = value >= 0 && value <= 1 ? value * 100 : value;
  return `${Math.round(percent)}%`;
}

function AiReviewView() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const valid = Number.isFinite(kycId) && kycId > 0;

  const [summary, setSummary] = useState<KycReviewSummaryResponse | null>(null);
  const [detail, setDetail] = useState<KycAiReviewDetailResponse | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [supplements, setSupplements] = useState<Supplement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!valid) return;
    setLoading(true);
    setError(null);
    Promise.all([
      kycApi.aiReviewSummary(kycId).catch(() => null),
      kycApi.aiReviewResult(kycId).catch(() => null),
      kycApi.documents(kycId).catch(() => []),
      kycApi.supplements(kycId).catch(() => ({ supplements: [] }))
    ])
      .then(([s, aiDetail, docs, sup]) => {
        setSummary(s);
        setDetail(aiDetail);
        setDocuments(docs);
        setSupplements(sup?.supplements ?? []);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      )
      .finally(() => setLoading(false));
  }, [kycId, valid]);

  if (!valid) {
    return (
      <PageShell
        title="AI 심사 결과"
        description="유효한 신청 ID가 필요합니다."
        module="UWEB-015"
        contentClassName="mx-auto flex w-full max-w-[920px] flex-col"
      >
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            올바른 신청 ID를 포함한 URL로 접근해 주세요.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const findings = summary?.findings ?? [];
  const detailItems = detail ? aiReviewItemsFromDetail(detail, documents) : [];
  const reviewItems = detailItems.length
    ? detailItems.filter((item) => !item.pass)
    : findings
        .filter((f) => f.result !== "PASS")
        .map((f) => ({
          code: f.findingType ?? "FINDING",
          label: f.message ?? findingTypeLabel(f.findingType),
          pass: false,
          kind: "reason" as const
        }));
  const activeSupplement = supplements.find((s) => s.supplementStatus === "REQUESTED");
  const supplementDocumentItems = uniqueStrings([
    ...((activeSupplement?.requestedDocumentTypeCodes ?? []).map(documentTypeLabel)),
    ...(detail?.reviewReasons ?? []).map(reasonDocumentLabel).filter((item): item is string => !!item)
  ]);
  const opinionItems = uniqueStrings([
    ...(detail?.reviewReasons ?? []).filter((reason) => !reasonDocumentLabel(reason)),
    ...reviewItems
      .filter((item) => item.kind === "document" || item.kind === "mismatch" || item.kind === "reason")
      .map((item) => item.label)
  ]);
  const additionalCheckItems = uniqueStrings(
    reviewItems
      .filter((item) => item.kind === "owner" || item.kind === "delegation")
      .map((item) => item.label)
  );
  const showSupplementCta = !!activeSupplement || reviewItems.length > 0 || !!detail?.supplementRequired;

  return (
    <PageShell
      title="AI 심사 결과"
      description="자동 심사 결과 요약과 후속 조치를 확인합니다."
      module="UWEB-015 · M-04"
      contentClassName="mx-auto flex w-full max-w-[920px] flex-col"
    >
      {error ? (
        <Card>
          <CardContent className="text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      {showSupplementCta ? (
        <div className="alert alert-warning" style={{ marginBottom: 16 }}>
          <span className="alert-icon">
            <Icon.Alert size={16} />
          </span>
          <span>보완 요청 사항이 있습니다. 기한 내 서류를 재제출해주세요.</span>
        </div>
      ) : null}

      <div className="dash-grid-2">
        <div className="form-card" style={{ marginTop: 0 }}>
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
            {detail?.summary ?? summary?.summaryMessage ?? "AI 심사가 진행되었습니다. 상세 내역을 확인해주세요."}
          </p>
          {opinionItems.length ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 14 }}>
              {opinionItems.map((item, i) => (
              <div
                key={`${item}-${i}`}
                style={{
                  display: "flex",
                  alignItems: "flex-start",
                  gap: 10,
                  padding: "10px 0",
                  borderTop: i === 0 ? "1px solid var(--divider)" : undefined
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
                <span style={{ fontSize: 13.5, lineHeight: 1.55 }}>{item}</span>
              </div>
              ))}
            </div>
          ) : !loading ? (
            <div className="text-sm text-muted-foreground" style={{ padding: "12px 0" }}>
              추가로 확인할 심사 의견이 없습니다.
            </div>
          ) : null}
          {supplementDocumentItems.length ? (
            <AiReviewItemGroup title="보완 필요 서류" items={supplementDocumentItems} />
          ) : null}
          {additionalCheckItems.length ? (
            <AiReviewItemGroup title="추가 확인 필요" items={additionalCheckItems} />
          ) : null}
          {(detail?.confidenceScore ?? summary?.confidenceScore) != null ? (
            <div className="text-sm text-muted-foreground" style={{ marginTop: 12 }}>
              신뢰도 {formatConfidencePercent(detail?.confidenceScore ?? summary?.confidenceScore ?? 0)}
            </div>
          ) : null}
        </div>

        <div className="form-card" style={{ marginTop: 0 }}>
          <div className="form-card-header">
            <div className="form-card-title">보완 요청 사항</div>
          </div>
          {activeSupplement ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {(activeSupplement.requestedDocumentTypeCodes ?? []).length > 0 ? (
                activeSupplement.requestedDocumentTypeCodes!.map((code, i) => (
                  <div
                    key={code + i}
                    style={{
                      background: "var(--warning-soft)",
                      border: "1px solid #FBE2BD",
                      borderRadius: "var(--radius-md)",
                      padding: "12px 14px"
                    }}
                  >
                    <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 6 }}>
                      {documentTypeLabel(code)}
                    </div>
                    <div
                      style={{
                        fontSize: 12.5,
                        color: "var(--text-secondary)",
                        lineHeight: 1.6
                      }}
                    >
                      {activeSupplement.requestReason ??
                        activeSupplement.message ??
                        "심사역의 요청에 따라 서류를 재제출해주세요."}
                    </div>
                  </div>
                ))
              ) : (
                <div
                  style={{
                    background: "var(--warning-soft)",
                    border: "1px solid #FBE2BD",
                    borderRadius: "var(--radius-md)",
                    padding: "12px 14px"
                  }}
                >
                  <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 6 }}>
                    {activeSupplement.title ?? "보완 요청"}
                  </div>
                  <div
                    style={{
                      fontSize: 12.5,
                      color: "var(--text-secondary)",
                      lineHeight: 1.6
                    }}
                  >
                    {activeSupplement.requestReason ??
                      activeSupplement.message ??
                      "심사역의 요청에 따라 서류를 재제출해주세요."}
                  </div>
                </div>
              )}
            </div>
          ) : detail?.reviewReasons?.length || reviewItems.length ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {(detail?.reviewReasons?.length ? detail.reviewReasons : reviewItems.map((item) => item.label)).map(
                (reason, index) => (
                  <div
                    key={`${reason}-${index}`}
                    style={{
                      background: "var(--warning-soft)",
                      border: "1px solid #FBE2BD",
                      borderRadius: "var(--radius-md)",
                      padding: "12px 14px"
                    }}
                  >
                    <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 6 }}>
                      확인 필요
                    </div>
                    <div
                      style={{
                        fontSize: 12.5,
                        color: "var(--text-secondary)",
                        lineHeight: 1.6
                      }}
                    >
                      {reason}
                    </div>
                  </div>
                )
              )}
            </div>
          ) : (
            <div className="text-sm text-muted-foreground" style={{ padding: "12px 0" }}>
              현재 보완 요청 사항이 없습니다.
            </div>
          )}

          <div style={{ marginTop: 16, display: "flex", flexDirection: "column", gap: 8 }}>
            {activeSupplement ? (
              <Button asChild>
                <Link
                  href={`/corporate/kyc/detail/supplement?id=${kycId}&supplementId=${activeSupplement.supplementId}`}
                >
                  보완 요청 상세
                </Link>
              </Button>
            ) : null}
            <Button asChild variant="ghost">
              <Link href={`/corporate/kyc/detail?id=${kycId}`}>← 진행 상태로 돌아가기</Link>
            </Button>
          </div>
        </div>
      </div>
    </PageShell>
  );
}

function findingTypeLabel(code?: string) {
  if (!code) return "-";
  const map: Record<string, string> = {
    SUMMARY: "요약",
    DOCUMENT_VERIFY: "서류 진위 확인",
    OCR_PARSE: "OCR 추출",
    ID_VALIDATION: "신원 정보 일치",
    SANCTION_CHECK: "제재 명단 조회",
    CONSISTENCY_CHECK: "정보 일관성 확인",
    MANUAL_REVIEW_REASON: "수동 심사 사유"
  };
  return map[code] ?? code;
}

type AiReviewDisplayItem = {
  code: string;
  label: string;
  pass: boolean;
  kind: "document" | "mismatch" | "owner" | "delegation" | "reason";
};

function aiReviewItemsFromDetail(detail: KycAiReviewDetailResponse, documents: KycDocument[]): AiReviewDisplayItem[] {
  const items: AiReviewDisplayItem[] = [];
  detail.documentResults.forEach((result, index) => {
    const name = documentResultLabel(result, documents, index);
    const pass = isPassResult(result.resultCode) || isPassMessage(result.message);
    if (pass || !result.message) return;
    items.push({
      code: `document-${result.documentId ?? result.documentTypeCode ?? index}`,
      label: result.message ? `${name} - ${result.message}` : name,
      pass,
      kind: "document"
    });
  });
  detail.mismatchResults.forEach((result, index) => {
    const pass = isPassResult(result.severityCode) || isPassMessage(result.message);
    items.push({
      code: `mismatch-${result.fieldName ?? index}`,
      label: result.message ? `${mismatchLabel(result, pass)} - ${result.message}` : mismatchFallback(result),
      pass,
      kind: "mismatch"
    });
  });
  detail.beneficialOwnerResults.forEach((result, index) => {
    const name = result.ownerName ? `실소유자 확인(${result.ownerName})` : "실소유자 확인";
    items.push({
      code: `owner-${result.ownerName ?? index}`,
      label: result.message ? `${name} - ${result.message}` : name,
      pass: isPassResult(result.resultCode),
      kind: "owner"
    });
  });
  if (detail.delegationResult) {
    items.push({
      code: "delegation",
      label: detail.delegationResult.message
        ? `위임권한 확인 - ${detail.delegationResult.message}`
        : "위임권한 확인",
      pass: isPassResult(detail.delegationResult.resultCode),
      kind: "delegation"
    });
  }
  if (!items.some((item) => !item.pass)) {
    detail.reviewReasons.forEach((reason, index) => {
      items.push({ code: `reason-${index}`, label: reason, pass: false, kind: "reason" });
    });
  }
  return items;
}

function isPassResult(result?: string | null) {
  const normalized = (result ?? "").toUpperCase();
  return normalized === "PASS" || normalized === "PASSED" || normalized === "OK" || normalized === "VALID" || normalized === "MATCH";
}

function isPassMessage(message?: string | null) {
  const normalized = (message ?? "").toLowerCase();
  return (
    normalized.includes(" passed") ||
    normalized.endsWith("passed.") ||
    normalized.includes("consistent") ||
    normalized.includes("match")
  );
}

function documentResultLabel(
  result: KycAiReviewDetailResponse["documentResults"][number],
  documents: KycDocument[],
  index: number
) {
  if (result.documentTypeName) return result.documentTypeName;
  const matched = documents.find((document) => document.documentId === result.documentId);
  if (matched?.documentTypeCode) return documentTypeLabel(matched.documentTypeCode);
  if (result.documentTypeCode) return documentTypeLabel(result.documentTypeCode);
  if (result.documentId) return `제출서류 ${result.documentId}`;
  return `제출서류 ${index + 1}`;
}

function mismatchLabel(
  result: KycAiReviewDetailResponse["mismatchResults"][number],
  pass: boolean
) {
  const prefix = pass ? "문서 간 교차검증" : "문서 간 확인";
  return result.fieldName ? `${prefix}(${result.fieldName})` : prefix;
}

function mismatchFallback(result: KycAiReviewDetailResponse["mismatchResults"][number]) {
  const source = documentTypeLabel(result.sourceDocumentTypeCode);
  const target = documentTypeLabel(result.targetDocumentTypeCode);
  const pass = isPassResult(result.severityCode) || isPassMessage(result.message);
  const label = mismatchLabel(result, pass);
  if (source && target) return `${label} - ${source}와 ${target}의 정보 확인이 필요합니다.`;
  return label;
}

function documentTypeLabel(code?: string | null) {
  if (!code) return "-";
  if (DOCUMENT_LABELS[code]) return DOCUMENT_LABELS[code];
  const map: Record<string, string> = {
    BUSINESS_REGISTRATION: "사업자등록증",
    CORPORATE_REGISTRATION: "등기사항전부증명서",
    CORPORATE_SEAL_CERTIFICATE: "법인인감증명서",
    SHAREHOLDER_LIST: "주주명부",
    ARTICLES_OF_INCORPORATION: "정관",
    POWER_OF_ATTORNEY: "위임장",
    REPRESENTATIVE_ID: "대표자 신분증",
    AGENT_ID: "대리인 신분증",
    OTHER: "기타"
  };
  return map[code] ?? code;
}

function reasonDocumentLabel(reason: string) {
  const normalized = reason.trim();
  const candidates = [
    "SHAREHOLDER_LIST",
    "CORPORATE_SEAL_CERTIFICATE",
    "CORPORATE_REGISTRATION",
    "BUSINESS_REGISTRATION",
    "주주명부",
    "법인인감증명서",
    "등기사항전부증명서",
    "사업자등록증"
  ];
  if (!candidates.includes(normalized)) return null;
  return documentTypeLabel(normalized);
}

function uniqueStrings(values: string[]) {
  return Array.from(new Set(values.map((value) => value.trim()).filter(Boolean)));
}

function AiReviewItemGroup({ title, items }: { title: string; items: string[] }) {
  return (
    <div style={{ marginTop: 16 }}>
      <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>{title}</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
        {items.map((item) => (
          <span
            key={item}
            style={{
              border: "1px solid var(--border)",
              borderRadius: "var(--radius-sm)",
              padding: "6px 10px",
              fontSize: 12.5,
              color: "var(--text-secondary)",
              background: "var(--surface-2)"
            }}
          >
            {item}
          </span>
        ))}
      </div>
    </div>
  );
}
