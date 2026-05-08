"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { ApiError, type KycDocument, kyc as kycApi } from "@/lib/api";
import {
  KYC_DOCUMENT_SLOTS,
  type KycDocumentSlot,
  compactHash,
  formatFileSize,
  getCurrentKycId
} from "@/lib/kyc-flow";

const MAX_BYTES = 20 * 1024 * 1024;
const ACCEPT = ".pdf,.jpg,.jpeg,.png";

export default function KycApplyUploadPage() {
  const router = useRouter();
  const [kycId, setKycId] = useState<number | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busySlot, setBusySlot] = useState<string | null>(null);

  useEffect(() => {
    const id = getCurrentKycId();
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    setKycId(id);
    kycApi
      .documents(id)
      .then((items) => setDocuments(items))
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  const refreshDocuments = async (id: number) => {
    const items = await kycApi.documents(id);
    setDocuments(items);
  };

  const upload = async (slot: KycDocumentSlot, file: File) => {
    if (!kycId) return;
    if (file.size > MAX_BYTES) {
      setError("파일은 20MB 이하만 업로드할 수 있습니다.");
      return;
    }
    setError(null);
    setBusySlot(slot.documentTypeCode);
    try {
      const existing = findDocument(documents, slot.documentTypeCode);
      if (existing) await kycApi.deleteDocument(kycId, existing.documentId);
      await kycApi.uploadDocument(kycId, file, slot.documentTypeCode);
      await refreshDocuments(kycId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setBusySlot(null);
    }
  };

  const remove = async (slot: KycDocumentSlot) => {
    if (!kycId) return;
    const existing = findDocument(documents, slot.documentTypeCode);
    if (!existing) return;
    setBusySlot(slot.documentTypeCode);
    setError(null);
    try {
      await kycApi.deleteDocument(kycId, existing.documentId);
      await refreshDocuments(kycId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "삭제에 실패했습니다.");
    } finally {
      setBusySlot(null);
    }
  };

  const requiredFilled = KYC_DOCUMENT_SLOTS.filter((slot) => slot.required).every((slot) =>
    documents.some((doc) => getDocumentType(doc) === slot.documentTypeCode)
  );

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col px-9 py-8">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 서류 업로드</h1>
          <p className="page-head-desc">
            PDF, JPG, PNG 파일 · 파일당 최대 20MB · 확장자/용량/손상파일 자동 검증
          </p>
        </div>
      </div>

      <StepIndicator current={4} />

      <div className="upload-grid">
        {KYC_DOCUMENT_SLOTS.map((slot) => (
          <UploadSlot
            key={slot.documentTypeCode}
            slot={slot}
            doc={findDocument(documents, slot.documentTypeCode)}
            busy={busySlot === slot.documentTypeCode}
            onPick={(file) => upload(slot, file)}
            onRemove={() => remove(slot)}
          />
        ))}
      </div>

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply/docs")}>
          이전
        </Button>
        <Button
          type="button"
          disabled={!requiredFilled}
          onClick={() => router.push("/corporate/kyc/apply/review")}
        >
          {requiredFilled ? "미리보기 확인" : "필수 서류 업로드 필요"}
        </Button>
      </div>
    </div>
  );
}

function UploadSlot({
  slot,
  doc,
  busy,
  onPick,
  onRemove
}: {
  slot: KycDocumentSlot;
  doc?: KycDocument;
  busy: boolean;
  onPick: (file: File) => void;
  onRemove: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [drag, setDrag] = useState(false);
  const filled = !!doc;

  const handleFiles = (files: FileList | null) => {
    if (!files || files.length === 0 || busy) return;
    onPick(files[0]);
  };

  return (
    <div
      className={`upload-tile ${filled ? "uploaded" : ""} ${drag ? "dragover" : ""}`}
      onClick={() => !filled && inputRef.current?.click()}
      onDragOver={(event) => {
        event.preventDefault();
        setDrag(true);
      }}
      onDragLeave={() => setDrag(false)}
      onDrop={(event) => {
        event.preventDefault();
        setDrag(false);
        handleFiles(event.dataTransfer.files);
      }}
    >
      <span className="upload-tile-label">
        {slot.label}
        {slot.required ? <span className="req-mark">*</span> : null}
      </span>

      {filled ? (
        <>
          <Icon.Check size={26} />
          <div className="upload-tile-text">{doc.fileName ?? `${slot.label}.pdf`}</div>
          <div className="upload-tile-meta">
            {formatFileSize(doc.fileSize)} · {compactHash(doc.documentHash ?? doc.fileHash)}
          </div>
          <div className="mt-2.5 flex gap-1.5">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={(event) => {
                event.stopPropagation();
                inputRef.current?.click();
              }}
            >
              교체
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={busy}
              aria-label={`${slot.label} 삭제`}
              onClick={(event) => {
                event.stopPropagation();
                onRemove();
              }}
            >
              <Icon.Trash />
            </Button>
          </div>
        </>
      ) : (
        <>
          <Icon.Upload />
          <div className="upload-tile-text">{busy ? "업로드 중..." : "클릭하거나 드래그"}</div>
          <div className="upload-tile-meta">{slot.hint ?? "PDF, JPG, PNG · 최대 20MB"}</div>
        </>
      )}

      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        className="hidden"
        onChange={(event) => {
          handleFiles(event.target.files);
          event.target.value = "";
        }}
      />
    </div>
  );
}

function getDocumentType(doc: KycDocument) {
  return doc.documentTypeCode ?? doc.documentType ?? "";
}

function findDocument(documents: KycDocument[], documentTypeCode: string) {
  return documents.find((doc) => getDocumentType(doc) === documentTypeCode);
}
