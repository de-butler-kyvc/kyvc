"use client";

import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError, type RequiredDocument, kyc as kycApi } from "@/lib/api";
import {
  KYC_DOCUMENT_SLOTS,
  corporateTypeLabel,
  getStoredCorporateType,
  refreshCurrentKycStorage
} from "@/lib/kyc-flow";

export default function KycApplyDocsPage() {
  const router = useRouter();
  const [corporateType, setCorporateType] = useState("CORPORATION");
  const [docs, setDocs] = useState<RequiredDocument[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    refreshCurrentKycStorage(kycApi.current).then(() => {
    const stored = getStoredCorporateType();
    if (cancelled) return;
    setCorporateType(stored);
    kycApi
      .documentRequirements(stored)
      .then((items) => {
        if (!cancelled) setDocs(items);
      })
      .catch((err: unknown) => {
        setDocs(
          KYC_DOCUMENT_SLOTS.map((slot) => ({
            documentTypeCode: slot.documentTypeCode,
            documentTypeName: slot.label,
            required: slot.required,
            uploaded: false,
            description: slot.hint,
            allowedExtensions: ["pdf", "jpg", "jpeg", "png"],
            maxFileSizeMb: 20,
            groupCode: slot.groupCode,
            groupName: slot.groupName,
            minRequiredCount: slot.minRequiredCount,
            groupCandidate: slot.groupCandidate
          }))
        );
        setError(err instanceof ApiError ? err.message : "서류 기준 조회에 실패했습니다.");
      });
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const required = docs.filter((doc) => doc.required);
  const optional = docs.filter((doc) => !doc.required);

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">필수서류 안내</h1>
          <p className="page-head-desc">
            {corporateTypeLabel(corporateType)} KYC 심사에 필요한 서류 목록입니다.
          </p>
        </div>
      </div>

      <StepIndicator current={3} />

      <div className="req-doc-list">
        <section className="req-doc-card">
          <div className="req-doc-card-title">필수 서류 · {required.length}</div>
          {required.map((doc) => (
            <DocRow
              key={doc.documentTypeCode}
              name={doc.documentTypeName}
              badge={<Badge variant="destructive">필수</Badge>}
            />
          ))}
        </section>
        <section className="req-doc-card">
          <div className="req-doc-card-title">선택 서류 · {optional.length}</div>
          {optional.map((doc) => (
            <DocRow
              key={doc.documentTypeCode}
              name={doc.documentTypeName}
              badge={<Badge variant="default">{doc.description?.includes("대리") ? "대리 시" : "해당 시"}</Badge>}
            />
          ))}
          <div className="req-doc-note">
            선택 서류는 법인 유형, 대리 신청 여부, 심사 요청에 따라 제출 대상이 될 수 있습니다.
          </div>
        </section>
      </div>

      <div className="alert alert-info mt-4">
        <Icon.Info />
        <span>최근 3개월 이내 발급 서류를 권장하며, 파일당 최대 20MB까지 업로드할 수 있습니다.</span>
      </div>

      {error ? <p className="mt-4 text-[12px] text-warning">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply/type")}>
          이전
        </Button>
        <Button type="button" onClick={() => router.push("/corporate/kyc/apply/upload")}>
          서류 업로드 시작
        </Button>
      </div>
    </div>
  );
}

function DocRow({ name, badge }: { name: string; badge: ReactNode }) {
  return (
    <div className="req-doc-row">
      <span className="req-doc-name">{name}</span>
      {badge}
    </div>
  );
}
