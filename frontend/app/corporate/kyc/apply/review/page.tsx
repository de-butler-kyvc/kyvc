"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError, type KycDocument, kyc as kycApi } from "@/lib/api";
import {
  DOCUMENT_LABELS,
  compactHash,
  formatFileSize,
  getCurrentKycId
} from "@/lib/kyc-flow";

export default function KycApplyReviewPage() {
  const router = useRouter();
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const id = getCurrentKycId();
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    kycApi
      .documents(id)
      .then((items) => setDocuments(items))
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col px-9 py-8">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">업로드 서류 미리보기</h1>
          <p className="page-head-desc">업로드된 서류 목록과 해시를 확인하세요.</p>
        </div>
      </div>

      <StepIndicator current={4} />

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">업로드된 서류 {documents.length}건</div>
          <div className="form-card-meta flex items-center gap-1">
            <Icon.Lock size={13} /> SHA-256 해시 검증완료
          </div>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>파일명</th>
              <th>서류 유형</th>
              <th>크기</th>
              <th>해시</th>
              <th>상태</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {documents.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center text-muted-foreground">
                  업로드된 서류가 없습니다.
                </td>
              </tr>
            ) : (
              documents.map((doc) => (
                <tr key={doc.documentId}>
                  <td>
                    <span className="inline-flex items-center gap-1.5">
                      <Icon.File size={14} /> {doc.fileName ?? "-"}
                    </span>
                  </td>
                  <td>{DOCUMENT_LABELS[getDocumentType(doc)] ?? getDocumentType(doc)}</td>
                  <td className="mono">{formatFileSize(doc.fileSize)}</td>
                  <td className="mono text-subtle-foreground">
                    {compactHash(doc.documentHash ?? doc.fileHash)}
                  </td>
                  <td>
                    <Badge variant="success">
                      <Icon.Check size={11} /> 완료
                    </Badge>
                  </td>
                  <td className="text-right">
                    <button
                      type="button"
                      className="link inline-flex items-center gap-1"
                      onClick={() => router.push("/corporate/kyc/apply/upload")}
                    >
                      <Icon.Eye size={14} /> 미리보기
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply/upload")}>
          이전
        </Button>
        <Button type="button" onClick={() => router.push("/corporate/kyc/apply/storage")}>
          저장 옵션 선택
        </Button>
      </div>
    </div>
  );
}

function getDocumentType(doc: KycDocument) {
  return doc.documentTypeCode ?? doc.documentType ?? "";
}
