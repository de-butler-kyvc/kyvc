"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  type KycApplicationSummaryResponse,
  kyc as kycApi
} from "@/lib/api";
import {
  DOCUMENT_LABELS,
  clearCurrentKycId,
  corporateTypeLabel,
  formatFileSize,
  refreshCurrentKycStorage
} from "@/lib/kyc-flow";

export default function KycApplyConfirmPage() {
  const router = useRouter();
  const [summary, setSummary] = useState<KycApplicationSummaryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    refreshCurrentKycStorage(kycApi.current).then((id) => {
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    if (cancelled) return;
    kycApi
      .summary(id)
      .then((res) => {
        if (!cancelled) setSummary(res);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
    });
    return () => {
      cancelled = true;
    };
  }, [router]);

  const onSubmit = async () => {
    setBusy(true);
    setError(null);
    try {
      const latestKycId = await refreshCurrentKycStorage(kycApi.current);
      if (!latestKycId) {
        router.push("/corporate/kyc/apply");
        return;
      }
      const res = await kycApi.submit(latestKycId);
      if (typeof window !== "undefined") {
        window.localStorage.setItem("kyvc.lastSubmittedKycId", String(res.kycId));
        if (res.submittedAt) {
          window.localStorage.setItem("kyvc.lastSubmittedAt", res.submittedAt);
        }
      }
      clearCurrentKycId();
      router.push(`/corporate/kyc/apply/complete?id=${res.kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const documents = summary?.documents ?? [];
  const submittable = summary?.submittable ?? false;
  const missing = summary?.missingItems ?? [];

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
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
        <ConfirmRow label="법인명" value={summary?.corporateName} />
        <ConfirmRow
          label="사업자등록번호"
          value={summary?.businessRegistrationNo}
          mono
        />
        <ConfirmRow
          label="법인등록번호"
          value={summary?.corporateRegistrationNo}
          mono
        />
        <ConfirmRow label="대표자" value={summary?.representativeName} />
        <ConfirmRow
          label="법인 유형"
          value={corporateTypeLabel(summary?.corporateTypeCode)}
        />
        {summary?.documentStoreOption ? (
          <ConfirmRow
            label="원본서류 보관"
            value={summary.documentStoreOption === "STORE" ? "보관" : "심사 후 삭제"}
          />
        ) : null}
      </section>

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">제출 서류</div>
        </div>
        <div className="table-scroll">
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
                    <td>
                      {DOCUMENT_LABELS[doc.documentTypeCode ?? ""] ??
                        doc.documentTypeCode ??
                        "-"}
                    </td>
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
        </div>
      </section>

      {missing.length > 0 ? (
        <div className="alert alert-warning mt-4">
          <Icon.Alert size={16} className="alert-icon" />
          <div>
            <div className="font-semibold">제출 전 확인이 필요합니다</div>
            <ul className="m-0 mt-1 list-disc pl-5 text-[12px]">
              {missing.map((m) => (
                <li key={m.code}>{m.message}</li>
              ))}
            </ul>
          </div>
        </div>
      ) : null}

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply/storage")}>
          이전
        </Button>
        <Button type="button" size="lg" disabled={busy || !submittable} onClick={onSubmit}>
          {busy ? "제출 중..." : submittable ? "KYC 신청 제출" : "제출 불가"}
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
  value?: string | null;
  mono?: boolean;
}) {
  return (
    <div className="kv-row">
      <div className="kv-key">{label}</div>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value || "-"}</div>
    </div>
  );
}
