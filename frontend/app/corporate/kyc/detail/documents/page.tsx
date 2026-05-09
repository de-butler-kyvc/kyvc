"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState } from "react";

import { Icon } from "@/components/design/icons";
import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  ApiError,
  type Supplement,
  type SupplementDocument,
  kyc as kycApi
} from "@/lib/api";
import { formatFileSize } from "@/lib/kyc-flow";

const MAX_BYTES = 20 * 1024 * 1024;
const ACCEPT = ".pdf,.jpg,.jpeg,.png";

export default function CorporateKycSupplementUploadPage() {
  return (
    <Suspense>
      <SupplementUpload />
    </Suspense>
  );
}

type Slot = {
  documentTypeCode: string;
  label: string;
  uploaded?: SupplementDocument;
};

function SupplementUpload() {
  const router = useRouter();
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const supplementId = Number(params.get("supplementId"));
  const valid =
    Number.isFinite(kycId) && kycId > 0 && Number.isFinite(supplementId) && supplementId > 0;

  const [supplement, setSupplement] = useState<Supplement | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [comment, setComment] = useState("");

  const refresh = async () => {
    if (!valid) return;
    const s = await kycApi.supplementDetail(kycId, supplementId);
    setSupplement(s);
  };

  useEffect(() => {
    if (!valid) return;
    setError(null);
    refresh().catch((err: unknown) =>
      setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
    );
  }, [kycId, supplementId, valid]);

  if (!valid) {
    return (
      <PageShell title="보완 서류 재업로드" description="유효한 ID가 필요합니다." module="UWEB-017">
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            올바른 신청 ID(id)와 보완 요청 ID(supplementId)가 필요합니다.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const requestedCodes = supplement?.requestedDocumentTypeCodes ?? [];
  const uploadedDocs = supplement?.uploadedDocuments ?? [];
  const slots: Slot[] = (
    requestedCodes.length > 0 ? requestedCodes : uploadedDocs.map((d) => d.documentTypeCode ?? "OTHER")
  ).map((code) => ({
    documentTypeCode: code,
    label: documentTypeLabel(code),
    uploaded: uploadedDocs.find((d) => d.documentTypeCode === code)
  }));

  const allUploaded = slots.length > 0 && slots.every((s) => s.uploaded);

  const upload = async (slot: Slot, file: File) => {
    if (file.size > MAX_BYTES) {
      setError("파일은 20MB 이하만 업로드할 수 있습니다.");
      return;
    }
    setBusy(slot.documentTypeCode);
    setError(null);
    try {
      await kycApi.uploadSupplement(kycId, supplementId, file, slot.documentTypeCode);
      await refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setBusy(null);
    }
  };

  const onSubmit = async () => {
    if (!allUploaded) return;
    setSubmitting(true);
    setError(null);
    try {
      await kycApi.submitSupplement(kycId, supplementId, comment || undefined);
      router.push(`/corporate/kyc/detail?id=${kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
      setSubmitting(false);
    }
  };

  return (
    <PageShell
      title="보완 서류 재업로드"
      description="요청된 항목별로 새로운 파일을 업로드한 뒤 제출하세요."
      module="UWEB-017 · M-04"
    >
      {error ? (
        <Card>
          <CardContent className="text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      <div className="form-card">
        {slots.length === 0 ? (
          <p className="text-sm text-muted-foreground">요청된 항목이 없습니다.</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {slots.map((slot) => (
              <SlotRow
                key={slot.documentTypeCode}
                slot={slot}
                busy={busy === slot.documentTypeCode}
                onPick={(file) => upload(slot, file)}
              />
            ))}
          </div>
        )}

        <div style={{ marginTop: 20 }}>
          <label
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: "var(--text-secondary)",
              display: "block",
              marginBottom: 6
            }}
          >
            제출 메모 (선택)
          </label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="심사역에게 전달할 코멘트를 입력하세요."
            rows={3}
            style={{
              width: "100%",
              padding: "10px 12px",
              border: "1px solid var(--border)",
              borderRadius: "var(--radius-md)",
              background: "var(--surface)",
              color: "var(--text-primary)",
              fontSize: 13.5,
              fontFamily: "inherit",
              resize: "vertical"
            }}
          />
        </div>
      </div>

      <div className="form-actions right">
        <Button
          variant="ghost"
          onClick={() =>
            router.push(
              `/corporate/kyc/detail/supplement?id=${kycId}&supplementId=${supplementId}`
            )
          }
        >
          이전
        </Button>
        <Button size="lg" disabled={!allUploaded || submitting} onClick={onSubmit}>
          {submitting ? "제출 중..." : "보완 서류 제출"}
        </Button>
      </div>
    </PageShell>
  );
}

function SlotRow({
  slot,
  busy,
  onPick
}: {
  slot: Slot;
  busy: boolean;
  onPick: (file: File) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <div>
      <div
        style={{
          fontSize: 13,
          fontWeight: 600,
          color: "var(--text-secondary)",
          marginBottom: 8,
          display: "flex",
          alignItems: "center",
          gap: 6
        }}
      >
        {slot.label}
        <span style={{ color: "var(--danger)" }}>*</span>
      </div>
      {slot.uploaded ? (
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
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>
              {slot.uploaded.fileName ?? `${slot.label}.pdf`}
            </div>
            <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
              {formatFileSize(slot.uploaded.fileSize)}
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
          className="upload-tile"
          style={{ minHeight: 100 }}
          onClick={() => !busy && inputRef.current?.click()}
        >
          <Icon.Upload size={22} className="upload-tile-icon" />
          <div className="upload-tile-text">{busy ? "업로드 중..." : "클릭하거나 드래그"}</div>
          <div className="upload-tile-meta">PDF, JPG, PNG · 20MB</div>
        </div>
      )}
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) onPick(file);
          e.target.value = "";
        }}
      />
    </div>
  );
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
