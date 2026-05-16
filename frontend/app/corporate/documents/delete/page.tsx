"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError, kyc as kycApi, type KycApplicationResponse, type KycDocument } from "@/lib/api";

export default function CorporateDocumentDeletePage() {
  const [application, setApplication] = useState<KycApplicationResponse | null>(null);
  const [docs, setDocs] = useState<KycDocument[]>([]);
  const [checked, setChecked] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
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

  const canDelete = application?.kycStatus === "DRAFT";
  const checkedSet = useMemo(() => new Set(checked), [checked]);

  const toggle = (id: number) => {
    setChecked((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));
    setMessage(null);
  };

  const requestDelete = async () => {
    if (!application?.kycId || checked.length === 0) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await Promise.all(checked.map((documentId) => kycApi.deleteDocument(application.kycId, documentId)));
      setDocs((current) => current.filter((doc) => !checked.includes(doc.documentId)));
      setChecked([]);
      setMessage("삭제 요청이 접수되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "삭제 요청에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">원본서류 삭제 요청</h1>
          <p className="page-head-desc">제출 전 상태의 KYC 서류를 선택해 삭제합니다.</p>
        </div>
      </div>

      <div className="alert alert-warning mb-4">
        <Icon.Alert size={16} className="alert-icon" />
        <span>DRAFT 상태의 KYC 문서만 삭제할 수 있습니다. 제출 이후 원본서류 삭제는 별도 보관 정책을 따릅니다.</span>
      </div>

      <section className="form-card">
        <table className="table">
          <thead>
            <tr>
              <th style={{ width: 40 }} />
              <th>파일명</th>
              <th>문서 유형</th>
              <th>KYC 신청번호</th>
              <th>크기</th>
              <th>삭제 가능</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <Empty text="불러오는 중..." />
            ) : docs.length === 0 ? (
              <Empty text="삭제할 서류가 없습니다." />
            ) : (
              docs.map((doc) => (
                <tr key={doc.documentId} className={!canDelete ? "opacity-50" : ""}>
                  <td>
                    <input
                      type="checkbox"
                      checked={checkedSet.has(doc.documentId)}
                      disabled={!canDelete}
                      onChange={() => toggle(doc.documentId)}
                      className="size-[16px] accent-[var(--accent)]"
                      aria-label={`${doc.fileName ?? doc.documentId} 선택`}
                    />
                  </td>
                  <td className="mono text-[13px]">{doc.fileName ?? `document-${doc.documentId}`}</td>
                  <td>{doc.documentTypeCode ?? "-"}</td>
                  <td className="mono text-[12.5px] text-muted-foreground">
                    {doc.kycId ? `KYC-${doc.kycId}` : application?.kycId ? `KYC-${application.kycId}` : "-"}
                  </td>
                  <td className="text-muted-foreground">{formatSize(doc.fileSize)}</td>
                  <td>
                    <Badge variant={canDelete ? "success" : "secondary"}>{canDelete ? "가능" : "진행중"}</Badge>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>

      {error || message ? (
        <p className="mt-4 text-[12px]">
          {message ? (
            <span className="text-success">{message}</span>
          ) : (
            <span className="text-destructive">{error}</span>
          )}
        </p>
      ) : null}

      <div className="form-actions right">
        <Button asChild variant="ghost">
          <Link href="/corporate/documents">취소</Link>
        </Button>
        <Button type="button" disabled={saving || checked.length === 0 || !canDelete} onClick={requestDelete}>
          삭제 요청 ({checked.length})
        </Button>
      </div>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <tr>
      <td colSpan={6} className="empty-state">
        {text}
      </td>
    </tr>
  );
}

function formatSize(value?: number) {
  if (!value) return "-";
  if (value < 1024 * 1024) return `${Math.max(1, Math.round(value / 1024))}KB`;
  return `${(value / 1024 / 1024).toFixed(1)}MB`;
}
