"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Field } from "@/components/design/primitives";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  type Supplement,
  type SupplementDocument,
  kyc as kycApi
} from "@/lib/api";
import { DOCUMENT_LABELS, formatFileSize } from "@/lib/kyc-flow";

const DEFAULT_DOC_OPTIONS = [
  { code: "CORPORATE_REGISTRATION", label: "등기사항전부증명서" },
  { code: "BUSINESS_REGISTRATION", label: "사업자등록증" },
  { code: "SHAREHOLDER_LIST", label: "주주명부" },
  { code: "ARTICLES_OF_INCORPORATION", label: "정관" },
  { code: "POWER_OF_ATTORNEY", label: "위임장" },
  { code: "OTHER", label: "기타" }
];

const ACCEPT = ".pdf,.jpg,.jpeg,.png";

export default function CorporateKycSupplementPage() {
  return (
    <Suspense>
      <Supplements />
    </Suspense>
  );
}

function Supplements() {
  const router = useRouter();
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const supplementId = Number(params.get("supplementId"));
  const valid =
    Number.isFinite(kycId) && kycId > 0 && Number.isFinite(supplementId) && supplementId > 0;

  const [detail, setDetail] = useState<Supplement | null>(null);
  const [comment, setComment] = useState("");
  const [busyDocCode, setBusyDocCode] = useState<string | null>(null);
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

  const docCodes =
    detail?.requestedDocumentTypeCodes?.length
      ? detail.requestedDocumentTypeCodes
      : DEFAULT_DOC_OPTIONS.map((o) => o.code);

  const docLabel = (code: string) =>
    DOCUMENT_LABELS[code] ??
    DEFAULT_DOC_OPTIONS.find((o) => o.code === code)?.label ??
    code;

  const findUploaded = (code: string): SupplementDocument | undefined =>
    detail?.uploadedDocuments?.find((d) => d.documentTypeCode === code);

  const onUpload = async (code: string, file: File) => {
    if (!valid) return;
    if (file.size > 20 * 1024 * 1024) {
      setError("파일은 20MB 이하만 업로드할 수 있습니다.");
      return;
    }
    setBusyDocCode(code);
    setError(null);
    setInfo(null);
    try {
      await kycApi.uploadSupplement(kycId, supplementId, file, code);
      setInfo("문서를 업로드했습니다.");
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setBusyDocCode(null);
    }
  };

  const onSubmit = async () => {
    if (!valid) return;
    setSubmitting(true);
    setError(null);
    try {
      await kycApi.submitSupplement(kycId, supplementId, comment || undefined);
      setInfo("보완 제출이 접수되었습니다.");
      router.push(`/corporate/kyc/detail?id=${kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (!valid) {
    return (
      <div className="mx-auto flex w-full max-w-[1180px] flex-col px-9 py-8">
        <div className="page-head">
          <div>
            <h1 className="page-head-title">보완 서류 제출</h1>
            <p className="page-head-desc">유효한 신청 ID와 보완 요청 ID가 필요합니다.</p>
          </div>
        </div>
        <section className="form-card">
          <p className="text-sm text-muted-foreground">
            올바른 신청 ID(id)와 보완 요청 ID(supplementId)가 필요합니다.
          </p>
        </section>
      </div>
    );
  }

  const completed =
    detail?.supplementStatus === "COMPLETED" ||
    detail?.supplementStatus === "SUBMITTED";
  const allUploaded = docCodes.every((code) => !!findUploaded(code));

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col px-9 py-8">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">보완 서류 제출</h1>
          <p className="page-head-desc">
            요청된 항목을 다시 업로드한 뒤 제출해주세요. 제출 후에는 수정할 수 없습니다.
          </p>
        </div>
      </div>

      <section className="form-card">
        <div className="form-card-header">
          <div>
            <div className="form-card-title">
              {detail?.title ?? `보완요청 #${supplementId}`}
            </div>
            <div className="form-card-meta">KYC-{kycId}</div>
          </div>
          {completed ? (
            <Badge variant="success">
              <Icon.Check size={11} /> 제출 완료
            </Badge>
          ) : (
            <Badge variant="warning">
              <Icon.Alert size={11} /> 보완 필요
            </Badge>
          )}
        </div>

        {detail?.dueAt ? (
          <div className="kv-row">
            <div className="kv-key">제출 기한</div>
            <div
              className="kv-val"
              style={{ color: "var(--danger)", fontWeight: 600 }}
            >
              {formatDate(detail.dueAt)}
            </div>
          </div>
        ) : null}
        {detail?.requestedAt ? (
          <div className="kv-row">
            <div className="kv-key">요청 일시</div>
            <div className="kv-val">{formatDateTime(detail.requestedAt)}</div>
          </div>
        ) : null}
        {detail?.message ? (
          <div className="kv-row">
            <div className="kv-key">요청 메시지</div>
            <div className="kv-val">{detail.message}</div>
          </div>
        ) : null}
        {detail?.requestReason ? (
          <div className="kv-row">
            <div className="kv-key">요청 사유</div>
            <div className="kv-val">{detail.requestReason}</div>
          </div>
        ) : null}

        <div style={{ marginTop: 16 }}>
          <div
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: "var(--text-secondary)",
              marginBottom: 12
            }}
          >
            보완 항목 {docCodes.length}건
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {docCodes.map((code) => (
              <div
                key={code}
                style={{
                  border: "1px solid var(--border)",
                  borderRadius: "var(--radius-md)",
                  padding: "14px 16px"
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    marginBottom: 6
                  }}
                >
                  <div
                    style={{
                      width: 20,
                      height: 20,
                      borderRadius: "50%",
                      background: findUploaded(code)
                        ? "var(--success-soft)"
                        : "var(--danger-soft)",
                      color: findUploaded(code) ? "var(--success)" : "var(--danger)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0
                    }}
                  >
                    {findUploaded(code) ? (
                      <Icon.Check size={11} />
                    ) : (
                      <Icon.Alert size={11} />
                    )}
                  </div>
                  <span style={{ fontSize: 13.5, fontWeight: 700 }}>
                    {docLabel(code)}
                  </span>
                </div>
                <div
                  style={{
                    fontSize: 13,
                    color: "var(--text-secondary)",
                    lineHeight: 1.6,
                    paddingLeft: 28
                  }}
                >
                  요청 항목입니다. 최신본을 PDF, JPG, PNG로 제출해주세요.
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {!completed ? (
        <section className="form-card">
          <div className="form-card-header">
            <div className="form-card-title">서류 업로드</div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {docCodes.map((code) => (
              <UploadField
                key={code}
                label={docLabel(code)}
                accept={ACCEPT}
                file={findUploaded(code)}
                busy={busyDocCode === code}
                onPick={(file) => onUpload(code, file)}
              />
            ))}
          </div>
          <div style={{ marginTop: 16 }}>
            <Field label="추가 메모 (선택)">
              <textarea
                className="input"
                style={{ height: "auto", minHeight: 96, padding: 12 }}
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="심사역에게 전달할 메모를 작성하세요."
              />
            </Field>
          </div>
        </section>
      ) : null}

      {detail?.uploadedDocuments?.length ? (
        <section className="form-card">
          <div className="form-card-header">
            <div className="form-card-title">
              제출된 보완 문서 {detail.uploadedDocuments.length}건
            </div>
          </div>
          <ul
            style={{
              display: "flex",
              flexDirection: "column",
              gap: 8,
              margin: 0,
              padding: 0,
              listStyle: "none"
            }}
          >
            {detail.uploadedDocuments.map((d) => (
              <li
                key={d.supplementDocumentId}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "12px 14px",
                  border: "1px solid var(--border)",
                  borderRadius: "var(--radius-md)"
                }}
              >
                <Icon.File size={18} style={{ color: "var(--text-muted)" }} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>
                    {docLabel(d.documentTypeCode ?? "")}
                  </div>
                  <div
                    style={{
                      fontSize: 12,
                      color: "var(--text-muted)",
                      whiteSpace: "nowrap",
                      overflow: "hidden",
                      textOverflow: "ellipsis"
                    }}
                  >
                    {d.fileName ?? "-"} · {formatFileSize(d.fileSize)}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {info ? (
        <p className="mt-3 text-[13px] text-muted-foreground">{info}</p>
      ) : null}
      {error ? (
        <p className="mt-3 text-[13px] text-destructive">{error}</p>
      ) : null}

      <div className="form-actions right">
        <Button
          type="button"
          variant="ghost"
          onClick={() => router.push(`/corporate/kyc/detail?id=${kycId}`)}
        >
          이전
        </Button>
        {!completed ? (
          <Button
            type="button"
            size="lg"
            disabled={submitting || !allUploaded}
            onClick={onSubmit}
          >
            {submitting ? "제출 중..." : "보완 제출"}
          </Button>
        ) : null}
      </div>
    </div>
  );
}

function UploadField({
  label,
  accept,
  file,
  busy,
  onPick
}: {
  label: string;
  accept: string;
  file?: SupplementDocument;
  busy: boolean;
  onPick: (file: File) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [drag, setDrag] = useState(false);
  return (
    <div className="field">
      <span className="field-label">
        {label}
        <span style={{ color: "var(--danger)" }}> *</span>
      </span>
      {file ? (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            padding: "10px 14px",
            background: "var(--accent-soft)",
            border: "1px solid var(--accent)",
            borderRadius: "var(--radius-md)"
          }}
        >
          <Icon.File size={18} style={{ color: "var(--accent)" }} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div
              style={{
                fontSize: 13,
                fontWeight: 600,
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis"
              }}
            >
              {file.fileName ?? "-"}
            </div>
            <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
              {formatFileSize(file.fileSize)}
            </div>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={busy}
            onClick={() => inputRef.current?.click()}
          >
            교체
          </Button>
        </div>
      ) : (
        <div
          className={`upload-tile${drag ? " dragover" : ""}`}
          style={{ minHeight: 100 }}
          onClick={() => !busy && inputRef.current?.click()}
          onDragOver={(event) => {
            event.preventDefault();
            setDrag(true);
          }}
          onDragLeave={() => setDrag(false)}
          onDrop={(event) => {
            event.preventDefault();
            setDrag(false);
            const f = event.dataTransfer.files?.[0];
            if (f && !busy) onPick(f);
          }}
        >
          <Icon.Upload size={22} className="upload-tile-icon" />
          <div className="upload-tile-text">
            {busy ? "업로드 중..." : "클릭하거나 드래그하여 파일 선택"}
          </div>
          <div className="upload-tile-meta">PDF, JPG, PNG · 최대 20MB</div>
        </div>
      )}
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={(event) => {
          const f = event.target.files?.[0];
          event.target.value = "";
          if (f) onPick(f);
        }}
      />
    </div>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  });
}
