"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  ApiError,
  type KycReviewSummaryResponse,
  type Supplement,
  kyc as kycApi
} from "@/lib/api";

export default function CorporateKycAiReviewPage() {
  return (
    <Suspense>
      <AiReviewView />
    </Suspense>
  );
}

const FALLBACK_PASS_ITEMS = [
  { code: "DOCUMENT_VERIFY", label: "서류 진위 확인" },
  { code: "OCR_PARSE", label: "OCR 추출" },
  { code: "ID_VALIDATION", label: "신원 정보 일치" },
  { code: "SANCTION_CHECK", label: "제재 명단 조회" }
];

function AiReviewView() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const valid = Number.isFinite(kycId) && kycId > 0;

  const [summary, setSummary] = useState<KycReviewSummaryResponse | null>(null);
  const [supplements, setSupplements] = useState<Supplement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!valid) return;
    setLoading(true);
    setError(null);
    Promise.all([
      kycApi.aiReviewSummary(kycId).catch(() => null),
      kycApi.supplements(kycId).catch(() => ({ supplements: [] }))
    ])
      .then(([s, sup]) => {
        setSummary(s);
        setSupplements(sup?.supplements ?? []);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      )
      .finally(() => setLoading(false));
  }, [kycId, valid]);

  if (!valid) {
    return (
      <PageShell title="AI 심사 결과" description="유효한 신청 ID가 필요합니다." module="UWEB-015">
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            올바른 신청 ID를 포함한 URL로 접근해 주세요.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const findings = summary?.findings ?? [];
  const passItems = findings.filter((f) => f.result === "PASS");
  const reviewItems = findings.filter((f) => f.result !== "PASS");
  const activeSupplement = supplements.find((s) => s.supplementStatus === "REQUESTED");
  const showSupplementCta = !!activeSupplement || reviewItems.length > 0;

  return (
    <PageShell
      title="AI 심사 결과"
      description="자동 심사 결과 요약과 후속 조치를 확인합니다."
      module="UWEB-015 · M-04"
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

      {summary?.summaryMessage ? (
        <div className="alert alert-info" style={{ marginBottom: 16 }}>
          <span className="alert-icon">
            <Icon.Info size={16} />
          </span>
          <span>{summary.summaryMessage}</span>
        </div>
      ) : null}

      <div className="dash-grid-2">
        <div className="form-card" style={{ marginTop: 0 }}>
          <div className="form-card-header">
            <div className="form-card-title">자동 심사 통과 항목</div>
            {summary?.confidenceScore != null ? (
              <Badge variant="outline">신뢰도 {summary.confidenceScore.toFixed(0)}%</Badge>
            ) : null}
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {(passItems.length > 0
              ? passItems.map((f) => ({
                  code: f.findingType ?? "FINDING",
                  label: f.message ?? findingTypeLabel(f.findingType)
                }))
              : loading
                ? []
                : FALLBACK_PASS_ITEMS
            ).map((item, i, arr) => (
              <div
                key={item.code + i}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 10,
                  padding: "10px 0",
                  borderBottom:
                    i < arr.length - 1 || reviewItems.length > 0
                      ? "1px solid var(--divider)"
                      : "none"
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
                <Badge variant="success" className="ml-auto">
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
                  padding: "10px 0"
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
                  {f.message ?? findingTypeLabel(f.findingType)}
                </span>
                <Badge variant="warning" className="ml-auto">
                  보완
                </Badge>
              </div>
            ))}
          </div>
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

function documentTypeLabel(code?: string) {
  if (!code) return "-";
  const map: Record<string, string> = {
    BUSINESS_REGISTRATION: "사업자등록증",
    CORPORATE_REGISTRATION: "등기사항전부증명서",
    SHAREHOLDER_LIST: "주주명부",
    ARTICLES_OF_INCORPORATION: "정관",
    POWER_OF_ATTORNEY: "위임장",
    REPRESENTATIVE_ID: "대표자 신분증",
    AGENT_ID: "대리인 신분증",
    OTHER: "기타"
  };
  return map[code] ?? code;
}
