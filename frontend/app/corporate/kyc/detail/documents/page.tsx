"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ApiError,
  type Supplement,
  kyc as kycApi
} from "@/lib/api";

const DEFAULT_DOC_OPTIONS = [
  { code: "CORPORATE_REGISTRATION", label: "등기사항전부증명서" },
  { code: "BUSINESS_REGISTRATION", label: "사업자등록증" },
  { code: "SHAREHOLDER_LIST", label: "주주명부" },
  { code: "ARTICLES_OF_INCORPORATION", label: "정관" },
  { code: "POWER_OF_ATTORNEY", label: "위임장" },
  { code: "OTHER", label: "기타" }
];

export default function CorporateKycDocumentsPage() {
  return (
    <Suspense>
      <Supplements />
    </Suspense>
  );
}

function Supplements() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const supplementId = Number(params.get("supplementId"));
  const valid =
    Number.isFinite(kycId) && kycId > 0 && Number.isFinite(supplementId) && supplementId > 0;

  const [detail, setDetail] = useState<Supplement | null>(null);
  const [documentTypeCode, setDocumentTypeCode] = useState<string>("");
  const [file, setFile] = useState<File | null>(null);
  const [comment, setComment] = useState("");
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    if (!valid) return;
    let cancelled = false;
    kycApi
      .supplementDetail(kycId, supplementId)
      .then((res) => {
        if (cancelled) return;
        setDetail(res);
        const first = res.requestedDocumentTypeCodes?.[0];
        if (first) setDocumentTypeCode(first);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
      });
    return () => {
      cancelled = true;
    };
  }, [kycId, supplementId, valid]);

  const reload = async () => {
    if (!valid) return;
    const res = await kycApi.supplementDetail(kycId, supplementId);
    setDetail(res);
  };

  const onUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!valid || !file || !documentTypeCode) return;
    setUploading(true);
    setError(null);
    setInfo(null);
    try {
      await kycApi.uploadSupplement(kycId, supplementId, file, documentTypeCode);
      setFile(null);
      setInfo("문서를 업로드했습니다.");
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  };

  const onSubmit = async () => {
    if (!valid) return;
    setSubmitting(true);
    setError(null);
    try {
      await kycApi.submitSupplement(kycId, supplementId, comment || undefined);
      setInfo("보완 제출이 접수되었습니다.");
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const docOptions = (detail?.requestedDocumentTypeCodes?.length
    ? detail.requestedDocumentTypeCodes.map((code) => ({
        code,
        label:
          DEFAULT_DOC_OPTIONS.find((o) => o.code === code)?.label ?? code
      }))
    : DEFAULT_DOC_OPTIONS);

  const completed =
    detail?.supplementStatus === "COMPLETED" ||
    detail?.supplementStatus === "SUBMITTED";

  return (
    <PageShell
      title={`서류 보완 — ${valid ? `KYC-${kycId}` : "(미지정)"}`}
      description="심사역의 보완 요청에 따라 서류를 추가 제출합니다."
      module="UWEB-017 · M-04"
    >
      {!valid ? (
        <Card>
          <CardContent className="p-6 text-sm text-muted-foreground">
            올바른 신청 ID(id)와 보완 요청 ID(supplementId)가 필요합니다.
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                {detail?.title ?? `보완요청 ${supplementId}`}
                {detail?.supplementStatus ? (
                  <Badge variant="outline">{detail.supplementStatus}</Badge>
                ) : null}
              </CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm">
              {detail?.message ? <p>{detail.message}</p> : null}
              {detail?.requestReason ? (
                <p className="text-muted-foreground">사유: {detail.requestReason}</p>
              ) : null}
              {detail?.dueAt ? (
                <p className="text-xs text-muted-foreground">
                  마감: {new Date(detail.dueAt).toLocaleString("ko-KR")}
                </p>
              ) : null}

              {detail?.uploadedDocuments?.length ? (
                <div>
                  <div className="mb-2 text-xs font-semibold uppercase text-muted-foreground">
                    제출된 보완 문서
                  </div>
                  <ul className="grid gap-1.5">
                    {detail.uploadedDocuments.map((d) => (
                      <li
                        key={d.supplementDocumentId}
                        className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
                      >
                        <span>
                          <span className="font-medium">
                            {docOptions.find((o) => o.code === d.documentTypeCode)?.label ??
                              d.documentTypeCode}
                          </span>
                          <span className="ml-2 font-mono text-xs text-muted-foreground">
                            {d.fileName}
                          </span>
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </CardContent>
          </Card>

          {!completed ? (
            <Card>
              <CardHeader>
                <CardTitle>보완 서류 업로드</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={onUpload} className="grid gap-3">
                  <div className="grid gap-2">
                    <Label>서류 유형</Label>
                    <select
                      className="h-9 rounded-md border bg-background px-2 text-sm"
                      value={documentTypeCode}
                      onChange={(e) => setDocumentTypeCode(e.target.value)}
                    >
                      <option value="">선택</option>
                      {docOptions.map((opt) => (
                        <option key={opt.code} value={opt.code}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="supplement-file">파일 선택</Label>
                    <Input
                      id="supplement-file"
                      type="file"
                      onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                    />
                  </div>
                  <div className="flex justify-end">
                    <Button
                      type="submit"
                      disabled={!file || !documentTypeCode || uploading}
                    >
                      {uploading ? "업로드 중..." : "업로드"}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          ) : null}

          {!completed ? (
            <Card>
              <CardHeader>
                <CardTitle>보완 제출</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-3">
                <Label htmlFor="supplement-comment">코멘트 (선택)</Label>
                <textarea
                  id="supplement-comment"
                  className="min-h-20 rounded-md border bg-background p-2 text-sm"
                  rows={3}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="심사역에게 전달할 메모를 작성하세요."
                />
                <div className="flex justify-end">
                  <Button
                    type="button"
                    onClick={onSubmit}
                    disabled={
                      submitting || !(detail?.uploadedDocuments?.length ?? 0)
                    }
                  >
                    {submitting ? "제출 중..." : "보완 제출"}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : null}

          {info ? <p className="text-sm text-muted-foreground">{info}</p> : null}
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </>
      )}
    </PageShell>
  );
}
