"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import {
  ApiError,
  type CorporateBasicInfo,
  corporate as corpApi,
  kyc as kycApi
} from "@/lib/api";

const CORPORATE_TYPES = [
  { value: "STOCK", label: "주식회사" },
  { value: "LIMITED", label: "유한회사" },
  { value: "FOUNDATION", label: "재단/사단법인" },
  { value: "FOREIGN", label: "외국법인 지점" }
];

type ApplyForm = CorporateBasicInfo & { corporateType: string };

const DEFAULTS: ApplyForm = {
  corporateName: "",
  businessNo: "",
  corporateNo: "",
  address: "",
  businessType: "",
  corporateType: CORPORATE_TYPES[0].value
};

export default function CorporateKycApplyPage() {
  const router = useRouter();
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [requirements, setRequirements] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting }
  } = useForm<ApplyForm>({ defaultValues: DEFAULTS });

  const corporateType = watch("corporateType");

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as
          | (CorporateBasicInfo & { corporateId?: number })
          | undefined;
        if (!c) return;
        if (c.corporateId) setCorporateId(c.corporateId);
        const stored =
          typeof window !== "undefined"
            ? window.localStorage.getItem("kyvc.corporateType")
            : null;
        const figmaToCode = (label: string) =>
          CORPORATE_TYPES.find((t) => t.label.startsWith(label.replace("법인", "")))?.value;
        reset({
          corporateName: c.corporateName ?? "",
          businessNo: c.businessNo ?? "",
          corporateNo: c.corporateNo ?? "",
          address: c.address ?? "",
          businessType: c.businessType ?? "",
          corporateType: (stored && figmaToCode(stored)) || DEFAULTS.corporateType
        });
      })
      .catch(() => undefined);
  }, [reset]);

  useEffect(() => {
    kycApi
      .documentRequirements(corporateType)
      .then((r) => setRequirements(r.requiredDocuments ?? []))
      .catch(() => setRequirements([]));
  }, [corporateType]);

  const onSubmit = handleSubmit(async (data) => {
    setError(null);
    try {
      const basic: CorporateBasicInfo = {
        corporateName: data.corporateName,
        businessNo: data.businessNo,
        corporateNo: data.corporateNo,
        address: data.address,
        businessType: data.businessType
      };
      const id = corporateId
        ? (await corpApi.updateBasicInfo(corporateId, basic), corporateId)
        : (await corpApi.create(basic)).corporateId;
      const created = await kycApi.create({
        corporateId: id,
        applicationType: "ONLINE",
        corporateType: data.corporateType
      });
      router.push(`/corporate/kyc/detail?id=${created.kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "신청 생성 중 오류가 발생했습니다.");
    }
  });

  return (
    <PageShell
      title="KYC 신청"
      description="법인 정보와 유형을 입력하면 신청이 생성되고 서류 업로드 단계로 이동합니다."
      module="UWEB-006~008 · M-02 / M-03"
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-6" noValidate>
        <Card>
          <CardHeader>
            <CardTitle>법인 기본 정보</CardTitle>
            <CardDescription>등기부등본·사업자등록증과 일치하도록 입력하세요.</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <TextField
              label="법인명"
              required
              error={errors.corporateName?.message}
              {...register("corporateName", { required: "법인명은 필수입니다" })}
            />
            <TextField
              label="법인등록번호"
              placeholder="000000-0000000"
              error={errors.corporateNo?.message}
              {...register("corporateNo")}
            />
            <TextField
              label="사업자등록번호"
              required
              placeholder="000-00-00000"
              error={errors.businessNo?.message}
              {...register("businessNo", { required: "사업자등록번호는 필수입니다" })}
            />
            <TextField label="업종" {...register("businessType")} />
            <div className="md:col-span-2">
              <TextField
                label="주소"
                error={errors.address?.message}
                {...register("address")}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>법인 유형</CardTitle>
            <CardDescription>유형에 따라 필수 서류가 달라집니다.</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3">
            <div className="flex flex-wrap gap-2">
              {CORPORATE_TYPES.map((t) => (
                <Button
                  key={t.value}
                  type="button"
                  variant={t.value === corporateType ? "default" : "outline"}
                  size="sm"
                  onClick={() => setValue("corporateType", t.value, { shouldDirty: true })}
                >
                  {t.label}
                </Button>
              ))}
            </div>
            <ul className="grid gap-1 text-sm text-muted-foreground">
              {requirements.length === 0 ? (
                <li>· 필수서류 정보를 불러오는 중입니다.</li>
              ) : (
                requirements.map((r) => <li key={r}>· {r}</li>)
              )}
            </ul>
          </CardContent>
        </Card>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        <div className="flex justify-end gap-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "처리 중..." : "신청 생성 및 다음 단계"}
          </Button>
        </div>
      </form>
    </PageShell>
  );
}
