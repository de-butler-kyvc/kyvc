"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError, kyc as kycApi } from "@/lib/api";

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

  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [doneId, setDoneId] = useState<number | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!valid || !file) return;
    setUploading(true);
    setError(null);
    try {
      const res = await kycApi.uploadSupplement(kycId, supplementId, file);
      setDoneId(res.documentId);
      setFile(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  };

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
        <Card>
          <CardHeader>
            <CardTitle>보완 서류 업로드</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="supplement-file">파일 선택</Label>
                <Input
                  id="supplement-file"
                  type="file"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                />
              </div>
              {error ? <p className="text-sm text-destructive">{error}</p> : null}
              {doneId ? (
                <p className="text-sm text-muted-foreground">
                  업로드 완료 (문서 ID: {doneId})
                </p>
              ) : null}
              <div className="flex justify-end">
                <Button type="submit" disabled={!file || uploading}>
                  {uploading ? "업로드 중..." : "업로드"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </PageShell>
  );
}
