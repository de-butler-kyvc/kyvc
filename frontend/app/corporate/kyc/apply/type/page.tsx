"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { ApiError, kyc as kycApi } from "@/lib/api";
import {
  getSelectableCorporateTypeCode,
  getStoredCorporateType,
  getSupportedCorporateTypeOptions,
  refreshCurrentKycStorage,
  setCurrentKycId,
  setStoredCorporateType
} from "@/lib/kyc-flow";
import { useCorporateProfile } from "@/lib/session-context";

const CORPORATE_TYPE_OPTIONS = getSupportedCorporateTypeOptions();

export default function KycApplyTypePage() {
  const router = useRouter();
  const { profile } = useCorporateProfile();
  const [selected, setSelected] = useState<string>("CORPORATION");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const storedType = getStoredCorporateType();
      const existingKycId = await refreshCurrentKycStorage(kycApi.current);
      if (existingKycId) {
        try {
          const detail = await kycApi.detail(existingKycId);
          if (cancelled) return;
          if (detail.corporateTypeCode) {
            const selectableType = getSelectableCorporateTypeCode(detail.corporateTypeCode);
            setSelected(selectableType);
            setStoredCorporateType(selectableType);
            return;
          }
        } catch {
          // 신청을 찾지 못하면 저장된 유형으로 폴백
        }
      }
      if (!cancelled) setSelected(storedType);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const onNext = async () => {
    if (!profile?.corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    const selectedOption = CORPORATE_TYPE_OPTIONS.find((option) => option.value === selected);
    if (!selectedOption || selectedOption.disabled) {
      setError("후속 확장 예정인 법인 유형은 아직 선택할 수 없습니다.");
      return;
    }
    setError(null);
    setBusy(true);
    try {
      setStoredCorporateType(selected);
      const existingId = await refreshCurrentKycStorage(kycApi.current);
      let kycId = existingId;
      if (!kycId) {
        const created = await kycApi.create(selected);
        kycId = created.kycId;
        setCurrentKycId(kycId);
      } else {
        // PUT /api/corporate/kyc/applications/{kycId}/corporate-type — changeCorporateType
        await kycApi.setCorporateType(kycId, selected);
      }
      router.push("/corporate/kyc/apply/docs");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "법인 유형 저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
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
        <ConfirmRow label="법인명" value={profile?.corporateName} />
        <ConfirmRow
          label="사업자등록번호"
          value={profile?.businessRegistrationNo}
          mono
        />
        <ConfirmRow label="대표자" value={profile?.representativeName} />
      </section>

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">법인 유형</div>
        </div>
        <div className="type-radio-list">
          {CORPORATE_TYPE_OPTIONS.map((option) => {
            const active = !option.disabled && selected === option.value;
            return (
              <button
                key={option.value}
                type="button"
                disabled={option.disabled}
                className={`type-radio-card ${active ? "selected" : ""} ${
                  option.disabled ? "cursor-not-allowed opacity-60" : ""
                }`}
                onClick={() => {
                  if (!option.disabled) setSelected(option.value);
                }}
              >
                <span className="type-radio-icon">
                  <TypeIcon value={option.value} />
                </span>
                <span className="type-radio-info">
                  <span className="type-radio-name">
                    {option.label}
                    {option.badge ? (
                      <span className="ml-2 rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
                        {option.badge}
                      </span>
                    ) : null}
                  </span>
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
  if (value === "NON_PROFIT") return <Icon.Shield />;
  if (value === "ASSOCIATION") return <Icon.Users />;
  return <Icon.Building />;
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
