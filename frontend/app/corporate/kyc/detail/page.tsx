"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ApiError,
  type KycDocument,
  type KycStatus,
  kyc as kycApi
} from "@/lib/api";

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

  const [status, setStatus] = useState<KycStatus | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!valid) return;
    setError(null);
    Promise.all([kycApi.status(kycId), kycApi.documents(kycId)])
      .then(([s, d]) => {
        setStatus(s);
        setDocuments(d.items ?? []);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [kycId, valid, refreshKey]);

  const onUpload = async (e: React.ChangeEvent<HTMLInputElement>, documentType: string) => {
    const file = e.target.files?.[0];
    if (!file || !valid) return;
    setError(null);
    try {
      await kycApi.uploadDocument(kycId, file, documentType);
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
      setStatus((prev) => ({ ...(prev ?? {}), status: res.status }));
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

  const beforeSubmit = status?.status === "DRAFT" || status?.status === "READY";

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
            <Badge variant="secondary">{status?.status ?? "..."}</Badge>
            {status?.aiReviewStatus ? (
              <Badge variant="outline">AI · {status.aiReviewStatus}</Badge>
            ) : null}
            {status?.vcStatus ? (
              <Badge variant="outline">VC · {status.vcStatus}</Badge>
            ) : null}
          </CardTitle>
        </CardHeader>
        {status?.nextAction ? (
          <CardContent className="text-sm text-muted-foreground">
            {status.nextAction}
          </CardContent>
        ) : null}
      </Card>

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
              {documents.map((d) => (
                <li
                  key={d.documentId}
                  className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
                >
                  <div className="flex flex-col">
                    <span className="font-medium">{d.documentType}</span>
                    <span className="font-mono text-xs text-muted-foreground">
                      {d.fileName ?? d.fileHash?.slice(0, 16)}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    {d.status ? <Badge variant="outline">{d.status}</Badge> : null}
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
          {beforeSubmit ? (
            <UploadField onPick={onUpload} />
          ) : null}
        </CardContent>
      </Card>
    </PageShell>
  );
}

function UploadField({
  onPick
}: {
  onPick: (e: React.ChangeEvent<HTMLInputElement>, documentType: string) => void;
}) {
  const [documentType, setDocumentType] = useState("CORP_REGISTRY");
  return (
    <div className="grid gap-2 rounded-md border border-dashed p-3">
      <div className="grid gap-2 md:grid-cols-[200px_1fr]">
        <select
          className="h-9 rounded-md border bg-background px-2 text-sm"
          value={documentType}
          onChange={(e) => setDocumentType(e.target.value)}
        >
          <option value="CORP_REGISTRY">등기사항전부증명서</option>
          <option value="BUSINESS_LICENSE">사업자등록증</option>
          <option value="SHAREHOLDERS">주주명부</option>
          <option value="ARTICLES">정관</option>
          <option value="POA">위임장</option>
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
