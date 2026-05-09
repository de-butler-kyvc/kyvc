"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ApiError,
  type KycDocument,
  type KycReviewSummaryResponse,
  type KycStatusResponse,
  type Supplement,
  kyc as kycApi
} from "@/lib/api";
import { DOCUMENT_LABELS } from "@/lib/kyc-flow";

const PRE_SUBMIT_STATUSES = new Set(["DRAFT", "READY"]);

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
  const [refreshKey, setRefreshKey] = useState(0);

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
  }, [kycId, valid, refreshKey]);

  const onUpload = async (
    e: React.ChangeEvent<HTMLInputElement>,
    documentTypeCode: string
  ) => {
    const file = e.target.files?.[0];
    if (!file || !valid) return;
    setError(null);
    try {
      await kycApi.uploadDocument(kycId, file, documentTypeCode);
      setRefreshKey((k) => k + 1);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      e.target.value = "";
    }
  };

  const onDelete = async (documentId: number) => {
    if (!valid) return;
    setError(null);
    try {
      await kycApi.deleteDocument(kycId, documentId);
      setRefreshKey((k) => k + 1);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "삭제에 실패했습니다.");
    }
  };

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
      <PageShell title="KYC 신청 (미지정)" description="유효한 신청 ID가 필요합니다." module="UWEB-014">
        <Card>
          <CardContent className="p-6 text-sm text-muted-foreground">
            올바른 신청 ID를 포함한 URL로 접근해 주세요.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const beforeSubmit = PRE_SUBMIT_STATUSES.has(status?.kycStatus ?? "");
  const pendingSupplements = supplements.filter(
    (s) => s.supplementStatus !== "COMPLETED" && s.supplementStatus !== "SUBMITTED"
  );

  return (
    <PageShell
      title={`KYC 신청 #${kycId}`}
      description="진행 상태와 제출 서류를 확인합니다."
      module="UWEB-009/010/013/014 · M-03 / M-04 / M-05"
    >
      {error ? (
        <Card>
          <CardContent className="p-4 text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardDescription>현재 상태</CardDescription>
          <CardTitle className="flex flex-wrap items-center gap-2">
            <Badge variant="secondary">{status?.kycStatus ?? "..."}</Badge>
            {aiReview?.aiReviewStatus ? (
              <Badge variant="outline">AI · {aiReview.aiReviewStatus}</Badge>
            ) : null}
            {status?.corporateTypeCode ? (
              <Badge variant="outline">유형 · {status.corporateTypeCode}</Badge>
            ) : null}
          </CardTitle>
        </CardHeader>
        {aiReview?.summaryMessage ? (
          <CardContent className="text-sm text-muted-foreground">
            {aiReview.summaryMessage}
          </CardContent>
        ) : null}
      </Card>

      {pendingSupplements.length > 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>보완 요청 {pendingSupplements.length}건</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {pendingSupplements.map((s) => (
              <div
                key={s.supplementId}
                className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium">{s.title ?? `보완요청 ${s.supplementId}`}</span>
                  <span className="text-xs text-muted-foreground">{s.message ?? s.requestReason ?? ""}</span>
                </div>
                <Button asChild variant="ghost" size="sm">
                  <Link
                    href={`/corporate/kyc/detail/documents?id=${kycId}&supplementId=${s.supplementId}`}
                  >
                    보완 제출
                  </Link>
                </Button>
              </div>
            ))}
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>제출 서류</CardTitle>
          {beforeSubmit ? (
            <Button onClick={onSubmitApplication} disabled={submitting}>
              {submitting ? "제출 중..." : "신청 제출"}
            </Button>
          ) : null}
        </CardHeader>
        <CardContent className="grid gap-3">
          {documents.length === 0 ? (
            <p className="text-sm text-muted-foreground">업로드된 서류가 없습니다.</p>
          ) : (
            <ul className="grid gap-2">
              {documents.filter(v => v.uploadStatus != 'DELETED').map((d) => (
                <li
                  key={d.documentId}
                  className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
                >
                  <div className="flex flex-col">
                    <span className="font-medium">
                      {DOCUMENT_LABELS[d.documentTypeCode ?? ""] ?? d.documentTypeCode ?? "-"}
                    </span>
                    <span className="font-mono text-xs text-muted-foreground">
                      {d.fileName ?? d.documentHash?.slice(0, 16) ?? "-"}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    {d.uploadStatus ? (
                      <Badge variant="outline">{d.uploadStatus}</Badge>
                    ) : null}
                    {beforeSubmit ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => onDelete(d.documentId)}
                      >
                        삭제
                      </Button>
                    ) : null}
                  </div>
                </li>
              ))}
            </ul>
          )}
          {beforeSubmit ? <UploadField onPick={onUpload} /> : null}
        </CardContent>
      </Card>
    </PageShell>
  );
}

function UploadField({
  onPick
}: {
  onPick: (
    e: React.ChangeEvent<HTMLInputElement>,
    documentTypeCode: string
  ) => void;
}) {
  const [documentType, setDocumentType] = useState("CORPORATE_REGISTRATION");
  return (
    <div className="grid gap-2 rounded-md border border-dashed p-3">
      <div className="grid gap-2 md:grid-cols-[200px_1fr]">
        <select
          className="h-9 rounded-md border bg-background px-2 text-sm"
          value={documentType}
          onChange={(e) => setDocumentType(e.target.value)}
        >
          <option value="CORPORATE_REGISTRATION">등기사항전부증명서</option>
          <option value="BUSINESS_REGISTRATION">사업자등록증</option>
          <option value="SHAREHOLDER_LIST">주주명부</option>
          <option value="ARTICLES_OF_INCORPORATION">정관</option>
          <option value="POWER_OF_ATTORNEY">위임장</option>
          <option value="OTHER">기타</option>
        </select>
        <input
          type="file"
          className="text-sm"
          onChange={(e) => onPick(e, documentType)}
        />
      </div>
    </div>
  );
}
