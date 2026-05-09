"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError, kyc as kycApi, type KycApplicationResponse, type KycDocument } from "@/lib/api";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "https://dev-api-kyvc.khuoo.synology.me";

export default function CorporateDocumentsPage() {
  const [application, setApplication] = useState<KycApplicationResponse | null>(null);
  const [docs, setDocs] = useState<KycDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    kycApi
      .current()
      .then(async (current) => {
        const documents = await kycApi.documents(current.kycId);
        if (!cancelled) {
          setApplication(current);
          setDocs(documents.filter((doc) => doc.uploadStatus !== "DELETED"));
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setDocs([]);
        else setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const preview = async (documentId: number) => {
    if (!application?.kycId) return;
    setError(null);
    try {
      const res = await kycApi.documentPreview(application.kycId, documentId);
      const url = res.previewUrl.startsWith("http") ? res.previewUrl : `${API_BASE}${res.previewUrl}`;
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "미리보기를 불러오지 못했습니다.");
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1040px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">제출서류 관리</h1>
          <p className="page-head-desc">현재 KYC 신청에 업로드된 제출서류를 확인합니다.</p>
        </div>
        <div className="page-head-actions">
          <Button asChild variant="ghost">
            <Link href="/corporate/documents/delete">삭제 요청</Link>
          </Button>
        </div>
      </div>

      {error ? (
        <div className="alert alert-warning mb-4">
          <Icon.Alert size={16} className="alert-icon" />
          <span>{error}</span>
        </div>
      ) : null}

      <table className="table">
        <thead>
          <tr>
            <th>파일명</th>
            <th>문서 유형</th>
            <th>KYC 신청번호</th>
            <th>상태</th>
            <th>크기</th>
            <th>업로드일</th>
            <th className="text-right">상세</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <Empty text="불러오는 중..." />
          ) : docs.length === 0 ? (
            <Empty text="제출된 서류가 없습니다." />
          ) : (
            docs.map((doc) => (
              <tr key={doc.documentId}>
                <td className="mono text-[13px]">{doc.fileName ?? `document-${doc.documentId}`}</td>
                <td>{doc.documentTypeCode ?? "-"}</td>
                <td className="mono text-[12.5px] text-muted-foreground">
                  {doc.kycId ? `KYC-${doc.kycId}` : application?.kycId ? `KYC-${application.kycId}` : "-"}
                </td>
                <td>
                  <Badge variant={doc.uploadStatus === "UPLOADED" ? "success" : "secondary"}>
                    {doc.uploadStatus === "UPLOADED" ? "업로드 완료" : doc.uploadStatus ?? "-"}
                  </Badge>
                </td>
                <td className="text-muted-foreground">{formatSize(doc.fileSize)}</td>
                <td className="text-muted-foreground">{formatDate(doc.uploadedAt)}</td>
                <td className="text-right">
                  <Button type="button" variant="ghost" size="sm" onClick={() => preview(doc.documentId)}>
                    <Icon.Eye size={14} />
                  </Button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <tr>
      <td colSpan={7} className="empty-state">
        {text}
      </td>
    </tr>
  );
}

function formatDate(value?: string | null) {
  return value ? value.slice(0, 10).replace(/-/g, ".") : "-";
}

function formatSize(value?: number) {
  if (!value) return "-";
  if (value < 1024 * 1024) return `${Math.max(1, Math.round(value / 1024))}KB`;
  return `${(value / 1024 / 1024).toFixed(1)}MB`;
}
