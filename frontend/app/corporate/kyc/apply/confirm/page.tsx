"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  type CorporateProfile,
  type KycDocument,
  corporate as corpApi,
  kyc as kycApi
} from "@/lib/api";
import {
  DOCUMENT_LABELS,
  corporateTypeLabel,
  formatFileSize,
  getCurrentKycId,
  getStoredCorporateType
} from "@/lib/kyc-flow";

export default function KycApplyConfirmPage() {
  const router = useRouter();
  const [kycId, setKycId] = useState<number | null>(null);
  const [corporate, setCorporate] = useState<CorporateProfile | null>(null);
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const id = getCurrentKycId();
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    setKycId(id);
    Promise.all([corpApi.me(), kycApi.documents(id)])
      .then(([corp, docs]) => {
        setCorporate(corp);
        setDocuments(docs);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  const onSubmit = async () => {
    if (!kycId) return;
    setBusy(true);
    setError(null);
    try {
      const res = await kycApi.submit(kycId);
      if (typeof window !== "undefined") {
        window.localStorage.setItem("kyvc.lastSubmittedKycId", String(res.kycId));
        window.localStorage.setItem("kyvc.lastSubmittedAt", res.submittedAt);
        window.localStorage.removeItem("kyvc.currentKycId");
      }
      router.push(`/corporate/kyc/apply/complete?id=${res.kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col px-9 py-8">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 신청 내용 확인</h1>
          <p className="page-head-desc">제출 전 법인 정보와 업로드 서류를 최종 확인해주세요.</p>
        </div>
      </div>

      <StepIndicator current={5} />

      <div className="alert alert-info mb-4">
        <span className="alert-icon">
          <Icon.Info size={16} />
        </span>
        <span>제출 후에는 심사가 시작되며, 일부 정보와 서류는 직접 수정할 수 없습니다.</span>
      </div>

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">법인 정보</div>
        </div>
        <ConfirmRow label="법인명" value={corporate?.corporateName} />
        <ConfirmRow label="사업자등록번호" value={corporate?.businessRegistrationNo} mono />
        <ConfirmRow label="법인등록번호" value={corporate?.corporateRegistrationNo ?? undefined} mono />
        <ConfirmRow label="대표자" value={corporate?.representativeName} />
        <ConfirmRow label="법인 유형" value={corporateTypeLabel(getStoredCorporateType())} />
      </section>

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">제출 서류</div>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>파일명</th>
              <th>서류 유형</th>
              <th>크기</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {documents.length === 0 ? (
              <tr>
                <td colSpan={4} className="text-center text-muted-foreground">
                  업로드된 서류가 없습니다.
                </td>
              </tr>
            ) : (
              documents.map((doc) => (
                <tr key={doc.documentId}>
                  <td className="mono">{doc.fileName ?? "-"}</td>
                  <td>{DOCUMENT_LABELS[getDocumentType(doc)] ?? getDocumentType(doc)}</td>
                  <td>{formatFileSize(doc.fileSize)}</td>
                  <td>
                    <Badge variant="success">
                      <Icon.Check size={11} /> 완료
                    </Badge>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply/storage")}>
          이전
        </Button>
        <Button type="button" size="lg" disabled={busy} onClick={onSubmit}>
          {busy ? "제출 중..." : "KYC 신청 제출"}
        </Button>
      </div>
    </div>
  );
}

function ConfirmRow({
  label,
  value,
  mono = false
}: {
  label: string;
  value?: string;
  mono?: boolean;
}) {
  return (
    <div className="kv-row">
      <div className="kv-key">{label}</div>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value || "-"}</div>
    </div>
  );
}

function getDocumentType(doc: KycDocument) {
  return doc.documentTypeCode ?? doc.documentType ?? "";
}
