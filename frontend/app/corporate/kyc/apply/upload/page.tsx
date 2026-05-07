"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { ApiError, type KycDocument, kyc as kycApi } from "@/lib/api";

type Slot = {
  documentType: string;
  label: string;
  required: boolean;
  hint?: string;
};

const SLOTS: Slot[] = [
  { documentType: "BUSINESS_LICENSE", label: "사업자등록증", required: true },
  { documentType: "CORP_REGISTRY", label: "등기사항전부증명서", required: true },
  { documentType: "SHAREHOLDERS", label: "주주명부", required: true },
  { documentType: "ARTICLES", label: "정관", required: false, hint: "해당 시 제출" },
  { documentType: "POA", label: "위임장", required: false, hint: "대리 신청 시 제출" }
];

const MAX_BYTES = 20 * 1024 * 1024;
const ACCEPT = ".pdf,.jpg,.jpeg,.png";

export default function KycApplyUploadPage() {
  const router = useRouter();
  const [kycId, setKycId] = useState<number | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busySlot, setBusySlot] = useState<string | null>(null);

  useEffect(() => {
    const id =
      typeof window !== "undefined"
        ? Number(window.localStorage.getItem("kyvc.currentKycId"))
        : 0;
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    setKycId(id);
    kycApi
      .documents(id)
      .then((r) => setDocuments(r.items ?? []))
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  const upload = async (slot: Slot, file: File) => {
    if (!kycId) return;
    if (file.size > MAX_BYTES) {
      setError("파일은 20MB 이하만 업로드할 수 있습니다.");
      return;
    }
    setError(null);
    setBusySlot(slot.documentType);
    try {
      const existing = documents.find((d) => d.documentType === slot.documentType);
      if (existing) await kycApi.deleteDocument(kycId, existing.documentId);
      await kycApi.uploadDocument(kycId, file, slot.documentType);
      const next = await kycApi.documents(kycId);
      setDocuments(next.items ?? []);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setBusySlot(null);
    }
  };

  const requiredFilled = SLOTS.filter((s) => s.required).every((s) =>
    documents.some((d) => d.documentType === s.documentType)
  );

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-6 px-9 py-8">
      <StepIndicator current={4} />

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          KYC 서류 업로드
        </h1>
        <p className="text-[13px] text-destructive">
          PDF, JPG, PNG 파일 · 파일당 최대 20MB · 확장자/용량/손상파일 자동 검증
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {SLOTS.slice(0, 3).map((s) => (
          <UploadSlot
            key={s.documentType}
            slot={s}
            doc={documents.find((d) => d.documentType === s.documentType)}
            busy={busySlot === s.documentType}
            onPick={(file) => upload(s, file)}
          />
        ))}
      </div>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {SLOTS.slice(3).map((s) => (
          <UploadSlot
            key={s.documentType}
            slot={s}
            doc={documents.find((d) => d.documentType === s.documentType)}
            busy={busySlot === s.documentType}
            onPick={(file) => upload(s, file)}
          />
        ))}
      </div>

      {error ? <p className="text-[12px] text-destructive">{error}</p> : null}

      <div className="flex items-center gap-2 pt-2">
        <Button
          onClick={() => router.push("/corporate/kyc/apply/review")}
          disabled={!requiredFilled}
          className="rounded-[10px] px-5"
        >
          다음 — 미리보기 확인 →
        </Button>
        <Button
          variant="outline"
          className="rounded-[10px] px-5"
          onClick={() => router.push("/corporate/kyc/apply/docs")}
        >
          임시 저장
        </Button>
      </div>
    </div>
  );
}

function UploadSlot({
  slot,
  doc,
  busy,
  onPick
}: {
  slot: Slot;
  doc?: KycDocument;
  busy: boolean;
  onPick: (file: File) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [drag, setDrag] = useState(false);
  const filled = !!doc;

  const handleFiles = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    onPick(files[0]);
  };

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-1 text-[13px] text-muted-foreground">
        <span>{slot.label}</span>
        {slot.required ? (
          <span className="text-destructive">*</span>
        ) : (
          <span className="text-subtle-foreground">(선택)</span>
        )}
      </div>
      <div
        onClick={() => !busy && inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setDrag(true);
        }}
        onDragLeave={() => setDrag(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDrag(false);
          if (!busy) handleFiles(e.dataTransfer.files);
        }}
        className={`flex min-h-[140px] cursor-pointer flex-col items-center justify-center gap-1.5 rounded-lg border-2 border-dashed px-4 py-5 text-center transition-colors ${
          filled
            ? "border-primary bg-accent/40"
            : drag
              ? "border-primary bg-secondary"
              : "border-border bg-secondary/40 hover:bg-secondary"
        }`}
      >
        {filled ? (
          <>
            <span aria-hidden className="text-2xl">📄</span>
            <span className="break-all text-[13px] font-semibold text-primary">
              {doc?.fileName ?? `${slot.label}.pdf`}
            </span>
            <span className="text-[11px] text-muted-foreground">
              해시 생성됨 ✓
            </span>
            <span className="rounded-full bg-success/10 px-2 py-0.5 text-[11px] font-semibold text-success">
              {busy ? "업로드 중..." : "업로드 완료"}
            </span>
          </>
        ) : (
          <>
            <span aria-hidden className="text-2xl">📁</span>
            <span className="text-[13px] font-semibold text-foreground">
              파일을 드래그하거나 클릭
            </span>
            <span className="text-[11px] text-muted-foreground">
              {slot.hint ?? "PDF, JPG, PNG · 최대 20MB"}
            </span>
          </>
        )}
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT}
          className="hidden"
          onChange={(e) => {
            handleFiles(e.target.files);
            e.target.value = "";
          }}
        />
      </div>
    </div>
  );
}
