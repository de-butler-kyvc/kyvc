"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import {
  CorporateInfoCard,
  type CorporateSummary
} from "@/components/corporate/info-card";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import {
  ApiError,
  type Representative,
  corporate as corpApi
} from "@/lib/api";

type RepresentativeForm = Representative & {
  nationality: string;
  isAgent: boolean;
};

const DEFAULTS: RepresentativeForm = {
  name: "",
  birthDate: "",
  phone: "",
  email: "",
  nationality: "대한민국",
  isAgent: false
};

export default function CorporateRepresentativePage() {
  const router = useRouter();
  const [corp, setCorp] = useState<CorporateSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting }
  } = useForm<RepresentativeForm>({ defaultValues: DEFAULTS });

  const isAgent = watch("isAgent");

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
              businessType?: string;
            }
          | undefined;
        const corporateType =
          (typeof window !== "undefined"
            ? window.localStorage.getItem("kyvc.corporateType")
            : null) ?? "";
        setCorp({
          corporateId: c?.corporateId,
          corporateName: c?.corporateName ?? "",
          businessNo: c?.businessNo ?? "",
          corporateNo: c?.corporateNo ?? "",
          representativeName: res.representative?.name ?? "",
          corporateType
        });
        const next: RepresentativeForm = { ...DEFAULTS };
        if (res.representative) {
          next.name = res.representative.name ?? "";
          next.birthDate = res.representative.birthDate ?? "";
          next.phone = res.representative.phone ?? "";
          next.email = res.representative.email ?? "";
        }
        reset(next);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [reset]);

  const persist = async (data: RepresentativeForm) => {
    setError(null);
    setMessage(null);
    if (!corp?.corporateId) return;
    await corpApi.updateRepresentative(corp.corporateId, {
      name: data.name,
      birthDate: data.birthDate,
      phone: data.phone,
      email: data.email
    });
  };

  const onSaveDraft = handleSubmit(async (data) => {
    try {
      await persist(data);
      setMessage("임시 저장되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  const onContinue = handleSubmit(async (data) => {
    try {
      await persist(data);
      router.push(data.isAgent ? "/corporate/agents" : "/corporate/kyc/apply");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-6 px-9 py-8">
      <CorporateInfoCard summary={corp} />

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          대표자 정보 등록
        </h1>
        <p className="text-[13px] text-destructive">
          KYC 심사에 활용될 대표자 정보를 입력해주세요.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-7 px-7 py-7">
          <form onSubmit={onContinue} className="flex flex-col gap-7" noValidate>
            <CorporateConfirm summary={corp} />

            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">대표자 정보</h2>
              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-3">
                <TextField
                  label="대표자명"
                  required
                  placeholder="대표자명 입력"
                  error={errors.name?.message}
                  {...register("name", { required: "대표자명은 필수입니다" })}
                />
                <TextField
                  label="생년월일"
                  type="date"
                  placeholder="YYYY-MM-DD"
                  error={errors.birthDate?.message}
                  {...register("birthDate")}
                />
                <TextField label="국적" placeholder="대한민국" {...register("nationality")} />
                <TextField
                  label="연락처"
                  placeholder="010-0000-0000"
                  error={errors.phone?.message}
                  {...register("phone", {
                    pattern: { value: /^[0-9-]*$/, message: "숫자와 - 만 입력해 주세요" }
                  })}
                />
                <TextField
                  label="이메일"
                  type="email"
                  placeholder="이메일 입력"
                  error={errors.email?.message}
                  {...register("email", {
                    pattern: { value: /^$|\S+@\S+\.\S+/, message: "이메일 형식이 올바르지 않습니다" }
                  })}
                />
              </div>
            </section>

            <button
              type="button"
              onClick={() => setValue("isAgent", !isAgent, { shouldDirty: true })}
              className="flex items-center justify-between rounded-lg border border-border bg-card px-5 py-3.5 text-left transition-colors hover:bg-secondary/60"
            >
              <div className="flex flex-col gap-0.5">
                <span className="text-[14px] font-semibold text-foreground">
                  대리인이 신청합니다
                </span>
                <span className="text-[12px] text-muted-foreground">
                  대표자 대신 대리인이 KYC를 진행하는 경우
                </span>
              </div>
              <span
                aria-hidden
                className={`flex h-6 w-11 shrink-0 items-center rounded-full p-0.5 transition-colors ${
                  isAgent ? "bg-primary" : "bg-border"
                }`}
              >
                <span
                  className={`size-5 rounded-full bg-white shadow-sm transition-transform ${
                    isAgent ? "translate-x-5" : ""
                  }`}
                />
              </span>
            </button>
            <input type="hidden" {...register("isAgent")} />

            {error || message ? (
              <p className="text-[12px]">
                {message ? (
                  <span className="text-success">{message}</span>
                ) : (
                  <span className="text-destructive">{error}</span>
                )}
              </p>
            ) : null}

            <div className="flex items-center gap-2 pt-1">
              <Button type="submit" disabled={isSubmitting} className="rounded-[10px] px-5">
                저장하고 계속
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                className="rounded-[10px] px-5"
                onClick={onSaveDraft}
              >
                임시 저장
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function CorporateConfirm({ summary }: { summary: CorporateSummary | null }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-[15px] font-bold text-foreground">법인 확인</h2>
      <div className="overflow-hidden rounded-lg border border-border">
        <ConfirmRow label="법인명" value={summary?.corporateName} />
        <ConfirmRow label="사업자등록번호" value={summary?.businessNo} />
        <ConfirmRow label="법인 유형" value={summary?.corporateType} />
      </div>
    </section>
  );
}

function ConfirmRow({ label, value }: { label: string; value?: string }) {
  return (
    <div className="grid grid-cols-[160px_1fr] border-b border-row-border last:border-0">
      <div className="bg-secondary px-4 py-3 text-[13px] text-muted-foreground">
        {label}
      </div>
      <div className="px-4 py-3 text-[13px] font-medium text-foreground">
        {value || "-"}
      </div>
    </div>
  );
}
