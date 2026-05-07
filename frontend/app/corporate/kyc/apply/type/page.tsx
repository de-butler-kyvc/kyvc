"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import {
  CorporateInfoCard,
  type CorporateSummary
} from "@/components/corporate/info-card";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { ApiError, corporate as corpApi, kyc as kycApi } from "@/lib/api";

const OPTIONS = [
  {
    value: "STOCK",
    label: "주식회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 주주명부"
  },
  {
    value: "LIMITED",
    label: "유한회사",
    docs: "사업자등록증 · 등기사항전부증명서 · 출자자명부"
  },
  {
    value: "NONPROFIT",
    label: "비영리법인",
    docs: "정관 · 설립허가증 · 등기사항전부증명서"
  },
  {
    value: "GROUP",
    label: "조합·단체",
    docs: "규약 · 고유번호증 · 대표자 확인서류"
  },
  {
    value: "FOREIGN",
    label: "외국기업",
    docs: "국내 사업자등록증 · 등기 · 본국 설립서류"
  }
];

export default function KycApplyTypePage() {
  const router = useRouter();
  const [corp, setCorp] = useState<CorporateSummary | null>(null);
  const [selected, setSelected] = useState<string>("STOCK");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as
          | {
              corporateId?: number;
              corporateName?: string;
              businessNo?: string;
              corporateNo?: string;
            }
          | undefined;
        const stored =
          typeof window !== "undefined"
            ? window.localStorage.getItem("kyvc.corporateType")
            : null;
        setCorp({
          corporateId: c?.corporateId,
          corporateName: c?.corporateName ?? "",
          businessNo: c?.businessNo ?? "",
          corporateNo: c?.corporateNo ?? "",
          representativeName: res.representative?.name ?? "",
          corporateType:
            OPTIONS.find((o) => o.value === stored)?.label ?? stored ?? ""
        });
        if (stored && OPTIONS.some((o) => o.value === stored)) setSelected(stored);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const onNext = async () => {
    setError(null);
    setBusy(true);
    try {
      const kycId =
        typeof window !== "undefined"
          ? Number(window.localStorage.getItem("kyvc.currentKycId"))
          : 0;
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
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-6 px-9 py-8">
      <StepIndicator current={2} />
      <CorporateInfoCard summary={corp} />

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          법인 유형 선택
        </h1>
        <p className="text-[13px] text-destructive">
          해당하는 법인 유형을 선택하세요. 유형에 따라 필요 서류가 달라집니다.
        </p>
      </div>

      <div className="flex flex-col gap-3">
        {OPTIONS.map((o) => {
          const on = selected === o.value;
          return (
            <button
              key={o.value}
              type="button"
              onClick={() => setSelected(o.value)}
              className={`flex items-start gap-3 rounded-lg border px-5 py-4 text-left transition-colors ${
                on
                  ? "border-primary bg-accent/40"
                  : "border-border bg-card hover:bg-secondary/50"
              }`}
            >
              <span
                className={`mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border-2 ${
                  on ? "border-primary" : "border-border"
                }`}
              >
                {on ? <span className="size-2 rounded-full bg-primary" /> : null}
              </span>
              <div className="flex flex-col gap-0.5">
                <span className="text-[14px] font-bold text-foreground">{o.label}</span>
                <span className="text-[12px] text-muted-foreground">{o.docs}</span>
              </div>
            </button>
          );
        })}
      </div>

      {error ? <p className="text-[12px] text-destructive">{error}</p> : null}

      <div className="flex items-center gap-2">
        <Button onClick={onNext} disabled={busy} className="rounded-[10px] px-5">
          다음 →
        </Button>
        <Button
          variant="outline"
          className="rounded-[10px] px-5"
          onClick={() => router.push("/corporate/kyc/apply")}
        >
          이전
        </Button>
      </div>
    </div>
  );
}
