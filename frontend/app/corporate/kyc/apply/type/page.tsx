"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { ApiError, corporate as corpApi, kyc as kycApi } from "@/lib/api";
import {
  CORPORATE_TYPE_OPTIONS,
  getCurrentKycId,
  getStoredCorporateType
} from "@/lib/kyc-flow";

type CorporateIdentity = {
  corporateName: string;
  businessNo: string;
  representativeName: string;
};

export default function KycApplyTypePage() {
  const router = useRouter();
  const [corp, setCorp] = useState<CorporateIdentity | null>(null);
  const [selected, setSelected] = useState<string>("CORPORATION");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setSelected(getStoredCorporateType());
    corpApi
      .me()
      .then((res) => {
        setCorp({
          corporateName: res.corporateName ?? "",
          businessNo: res.businessRegistrationNo ?? "",
          representativeName: res.representativeName ?? ""
        });
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const onNext = async () => {
    setError(null);
    setBusy(true);
    try {
      const kycId = getCurrentKycId();
      if (!kycId) {
        router.push("/corporate/kyc/apply");
        return;
      }
      await kycApi.setCorporateType(kycId, selected);
      if (typeof window !== "undefined") {
        window.localStorage.setItem("kyvc.corporateType", selected);
      }
      router.push("/corporate/kyc/apply/docs");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "법인 유형 저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col px-9 py-8">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">법인 유형 선택</h1>
          <p className="page-head-desc">
            해당하는 법인 유형을 선택하세요. 유형에 따라 필요 서류가 달라집니다.
          </p>
        </div>
      </div>

      <StepIndicator current={2} />

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">기본 정보</div>
        </div>
        <ConfirmRow label="법인명" value={corp?.corporateName} />
        <ConfirmRow label="사업자등록번호" value={corp?.businessNo} mono />
        <ConfirmRow label="대표자" value={corp?.representativeName} />
      </section>

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">법인 유형</div>
        </div>
        <div className="type-radio-list">
          {CORPORATE_TYPE_OPTIONS.map((option) => {
            const active = selected === option.value;
            return (
              <button
                key={option.value}
                type="button"
                className={`type-radio-card ${active ? "selected" : ""}`}
                onClick={() => setSelected(option.value)}
              >
                <span className="type-radio-icon">
                  <TypeIcon value={option.value} />
                </span>
                <span className="type-radio-info">
                  <span className="type-radio-name">{option.label}</span>
                  <span className="type-radio-docs">{option.docs}</span>
                </span>
                <span className={`type-radio-dot ${active ? "selected" : ""}`} />
              </button>
            );
          })}
        </div>
      </section>

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply")}>
          이전
        </Button>
        <Button type="button" disabled={busy} onClick={onNext}>
          다음 →
        </Button>
      </div>
    </div>
  );
}

function TypeIcon({ value }: { value: string }) {
  if (value === "NONPROFIT") return <Icon.Shield />;
  if (value === "ASSOCIATION") return <Icon.Users />;
  return <Icon.Building />;
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
