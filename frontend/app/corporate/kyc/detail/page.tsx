"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import {
  StatusTimeline,
  formatDate,
  statusBadgeVariant,
  statusLabel
} from "@/components/kyc/status-timeline";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ApiError,
  type KycDocument,
  type KycReviewSummaryResponse,
  type KycStatusResponse,
  type Supplement,
  kyc as kycApi
} from "@/lib/api";

export default function CorporateKycDetailPage() {
  return (
    <Suspense>
      <KycStatusView />
    </Suspense>
  );
}

function KycStatusView() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const valid = Number.isFinite(kycId) && kycId > 0;

  const [status, setStatus] = useState<KycStatusResponse | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [aiReview, setAiReview] = useState<KycReviewSummaryResponse | null>(null);
  const [supplements, setSupplements] = useState<Supplement[]>([]);
  const [error, setError] = useState<string | null>(null);

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
      .then(([s, d, review, sup]) => {
        if (cancelled) return;
        setStatus(s);
        setDocuments(d.filter((doc) => doc.uploadStatus !== "DELETED"));
        setAiReview(review);
        setSupplements(sup?.supplements ?? []);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
      });
    return () => {
      cancelled = true;
    };
  }, [kycId, valid]);

  if (!valid) {
    return (
      <PageShell
        title="KYC 진행 상태"
        description="유효한 신청 ID가 필요합니다."
        module="UWEB-014"
      >
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            올바른 신청 ID를 포함한 URL로 접근해 주세요.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const applicationNo = `KYC-${String(kycId).padStart(7, "0")}`;
  const isApproved =
    status?.kycStatus === "APPROVED" || status?.kycStatus === "VC_ISSUED";
  const needsSupplement = status?.kycStatus === "NEED_SUPPLEMENT";
  const activeSupplement = supplements.find((s) => s.supplementStatus === "REQUESTED");
  const vcIssued = status?.kycStatus === "VC_ISSUED";

  return (
    <PageShell
      title="KYC 진행 상태"
      description="신청 진행 상황과 다음 단계를 확인합니다."
      module="UWEB-014 · M-04"
    >
      {error ? (
        <Card>
          <CardContent className="text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      {needsSupplement ? (
        <div className="alert alert-warning" style={{ marginBottom: 16 }}>
          <span className="alert-icon">⚠</span>
          <span>보완 요청 사항이 있습니다. 기한 내 서류를 재제출해주세요.</span>
        </div>
      ) : null}

      <div className="dash-grid-2">
        <div className="form-card" style={{ marginTop: 0 }}>
          <div className="form-card-header">
            <div className="form-card-title">진행 단계</div>
          </div>
          <StatusTimeline
            status={status?.kycStatus}
            submittedAt={status?.submittedAt}
            reviewedAt={aiReview?.reviewedAt}
          />
        </div>

        <div className="form-card" style={{ marginTop: 0 }}>
          <div className="form-card-header">
            <div className="form-card-title">신청 정보</div>
          </div>
          <div className="kv-row">
            <div className="kv-key">신청 번호</div>
            <div className="kv-val mono">{applicationNo}</div>
          </div>
          <div className="kv-row">
            <div className="kv-key">현재 상태</div>
            <div className="kv-val">
              <Badge variant={statusBadgeVariant(status?.kycStatus)}>
                {statusLabel(status?.kycStatus)}
              </Badge>
            </div>
          </div>
          <div className="kv-row">
            <div className="kv-key">제출 서류</div>
            <div className="kv-val">{documents.length}건</div>
          </div>
          <div className="kv-row">
            <div className="kv-key">AI 심사</div>
            <div className="kv-val">
              {aiReview?.aiReviewStatus ? (
                <Badge variant="outline">{aiReview.aiReviewStatus}</Badge>
              ) : (
                <Badge variant="secondary">대기</Badge>
              )}
            </div>
          </div>
          <div className="kv-row">
            <div className="kv-key">VC 상태</div>
            <div className="kv-val">
              {vcIssued ? (
                <Badge variant="success">발급 완료</Badge>
              ) : (
                <Badge variant="secondary">미발급</Badge>
              )}
            </div>
          </div>
          {status?.submittedAt ? (
            <div className="kv-row">
              <div className="kv-key">제출일</div>
              <div className="kv-val">{formatDate(status.submittedAt)}</div>
            </div>
          ) : null}

          <div style={{ marginTop: 16, display: "flex", flexDirection: "column", gap: 8 }}>
            {isApproved ? (
              <Button asChild variant="default">
                <Link href={`/corporate/kyc/detail/complete?id=${kycId}`}>
                  완료 화면 보기 →
                </Link>
              </Button>
            ) : null}
            {needsSupplement && activeSupplement ? (
              <Button asChild variant="default">
                <Link
                  href={`/corporate/kyc/detail/supplement?id=${kycId}&supplementId=${activeSupplement.supplementId}`}
                >
                  보완 요청 상세 →
                </Link>
              </Button>
            ) : null}
            <Button asChild variant="ghost">
              <Link href={`/corporate/kyc/detail/ai-review?id=${kycId}`}>
                AI 심사 결과 보기 →
              </Link>
            </Button>
          </div>
        </div>
      </div>

      {supplements.length > 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>보완 요청 이력</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="m-0 flex list-none flex-col gap-2 p-0">
              {supplements.map((s) => (
                <li
                  key={s.supplementId}
                  className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
                >
                  <div className="flex flex-col">
                    <span className="font-medium">{s.title ?? `보완 요청 #${s.supplementId}`}</span>
                    <span className="text-xs text-muted-foreground">
                      마감 {formatDate(s.dueAt)} · 요청 {formatDate(s.requestedAt)}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge
                      variant={
                        s.supplementStatus === "REQUESTED"
                          ? "warning"
                          : s.supplementStatus === "COMPLETED"
                            ? "success"
                            : "secondary"
                      }
                    >
                      {s.supplementStatus ?? "-"}
                    </Badge>
                    <Button asChild variant="ghost" size="sm">
                      <Link
                        href={`/corporate/kyc/detail/supplement?id=${kycId}&supplementId=${s.supplementId}`}
                      >
                        상세
                      </Link>
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      ) : null}
    </PageShell>
  );
}
