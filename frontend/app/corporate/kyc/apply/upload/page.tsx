"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  type KycDocument,
  type RequiredDocument,
  kyc as kycApi
} from "@/lib/api";
import {
  KYC_DOCUMENT_SLOTS,
  compactHash,
  formatFileSize,
  getCurrentKycId
} from "@/lib/kyc-flow";

const ACCEPT = ".pdf,.jpg,.jpeg,.png";

const FALLBACK_SLOTS: RequiredDocument[] = KYC_DOCUMENT_SLOTS.map((slot) => ({
  documentTypeCode: slot.documentTypeCode,
  documentTypeName: slot.label,
  required: slot.required,
  uploaded: false,
  description: slot.hint,
  allowedExtensions: ["pdf", "jpg", "jpeg", "png"],
  maxFileSizeMb: 20
}));

export default function KycApplyUploadPage() {
  const router = useRouter();
  const [kycId, setKycId] = useState<number | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [slots, setSlots] = useState<RequiredDocument[]>(FALLBACK_SLOTS);
  const [error, setError] = useState<string | null>(null);
  const [busySlot, setBusySlot] = useState<string | null>(null);

  useEffect(() => {
    const id = getCurrentKycId();
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    setKycId(id);
    Promise.all([
      kycApi.requiredDocumentsByKyc(id).catch(() => FALLBACK_SLOTS),
      kycApi.documents(id)
    ])
      .then(([reqs, docs]) => {
        setSlots(reqs.length ? reqs : FALLBACK_SLOTS);
        setDocuments(docs);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  const refreshDocuments = async (id: number) => {
    const items = await kycApi.documents(id);
    setDocuments(items);
  };

  const upload = async (slot: RequiredDocument, file: File) => {
    if (!kycId) return;
    const maxBytes = (slot.maxFileSizeMb ?? 20) * 1024 * 1024;
    if (file.size > maxBytes) {
      setError(`파일은 ${slot.maxFileSizeMb ?? 20}MB 이하만 업로드할 수 있습니다.`);
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

  const remove = async (slot: RequiredDocument) => {
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

  const requiredFilled = slots
    .filter((slot) => slot.required)
    .every((slot) => !!findDocument(documents, slot.documentTypeCode));

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col">
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
        {slots.map((slot) => (
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
  slot: RequiredDocument;
  doc?: KycDocument;
  busy: boolean;
  onPick: (file: File) => void;
  onRemove: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [drag, setDrag] = useState(false);
  const filled = !!doc;
  const accept = slot.allowedExtensions?.length
    ? slot.allowedExtensions.map((ext) => `.${ext}`).join(",")
    : ACCEPT;
  const sizeHint = slot.maxFileSizeMb
    ? `최대 ${slot.maxFileSizeMb}MB`
    : "최대 20MB";

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
        {slot.documentTypeName}
        {slot.required ? <span className="req-mark">*</span> : null}
      </span>

      {filled ? (
        <>
          <Icon.Check size={26} />
          <div className="upload-tile-text">
            {doc.fileName ?? `${slot.documentTypeName}.pdf`}
          </div>
          <div className="upload-tile-meta">
            {formatFileSize(doc.fileSize)} · {compactHash(doc.documentHash)}
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
              aria-label={`${slot.documentTypeName} 삭제`}
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
          <div className="upload-tile-text">
            {busy ? "업로드 중..." : "클릭하거나 드래그"}
          </div>
          <div className="upload-tile-meta">
            {slot.description ?? `PDF, JPG, PNG · ${sizeHint}`}
          </div>
        </>
      )}

      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={(event) => {
          handleFiles(event.target.files);
          event.target.value = "";
        }}
      />
    </div>
  );
}

function findDocument(documents: KycDocument[], documentTypeCode: string) {
  return documents.find(
    (doc) =>
      doc.documentTypeCode === documentTypeCode &&
      (doc.uploadStatus?.toUpperCase() ?? "") !== "DELETED"
  );
}
