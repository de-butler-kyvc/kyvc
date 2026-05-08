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
import { Label } from "@/components/ui/label";
import { SelectField } from "@/components/ui/select-field";
import { TextField } from "@/components/ui/text-field";
import { ApiError, corporate as corpApi } from "@/lib/api";

type AgentForm = {
  name: string;
  phone: string;
  email: string;
  relation: string;
  expiryDate: string;
  scope: string;
  memo: string;
};

const DEFAULTS: AgentForm = {
  name: "",
  phone: "",
  email: "",
  relation: "EMPLOYEE",
  expiryDate: "",
  scope: "KYC_VC",
  memo: ""
};

const RELATION_OPTIONS = [
  { value: "EMPLOYEE", label: "임직원" },
  { value: "LAWYER", label: "변호사" },
  { value: "LAW_FIRM", label: "법무법인" },
  { value: "ACCOUNTANT", label: "회계사" },
  { value: "FAMILY", label: "대표 가족" },
  { value: "OTHER", label: "기타" }
];

const SCOPE_OPTIONS = [
  { value: "KYC_SUBMIT", label: "KYC 신청" },
  { value: "KYC_VC", label: "KYC 신청 및 VC 수령" },
  { value: "DOC_UPLOAD", label: "서류 업로드" },
  { value: "VC_REVOKE", label: "VC 수령 및 폐기" },
  { value: "FULL", label: "전체 권한" }
];

export default function CorporateAgentsPage() {
  const router = useRouter();
  const [corp, setCorp] = useState<CorporateSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<AgentForm>({ defaultValues: DEFAULTS });

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const corporateType =
          (typeof window !== "undefined"
            ? window.localStorage.getItem("kyvc.corporateType")
            : null) ?? "";
        setCorp({
          corporateId: res.corporateId,
          corporateName: res.corporateName ?? "",
          businessNo: res.businessRegistrationNo ?? "",
          corporateNo: res.corporateRegistrationNo ?? "",
          representativeName: res.representativeName ?? "",
          corporateType
        });
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const onSubmit = handleSubmit(async (data) => {
    if (!corp?.corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    setError(null);
    try {
      await corpApi.updateAgent(corp.corporateId, {
        name: data.name,
        phone: data.phone,
        email: data.email,
        authorityScope: data.scope
      });
      router.push("/corporate/kyc/apply");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-6 px-9 py-8">
      <CorporateInfoCard summary={corp} />

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          대리인 정보 등록
        </h1>
        <p className="text-[13px] text-destructive">
          대리 신청 시 대리인 정보 및 위임 관계를 입력해주세요.
        </p>
      </div>

      <div className="flex items-start gap-2 rounded-lg border border-accent-border bg-accent px-4 py-3 text-[13px] text-accent-foreground">
        <span aria-hidden>ℹ</span>
        <span>위임장은 다음 단계 서류 업로드에서 첨부합니다.</span>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-7 px-7 py-7">
          <form onSubmit={onSubmit} className="flex flex-col gap-7" noValidate>
            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">대리인 정보</h2>
              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <TextField
                  label="대리인 성명"
                  required
                  placeholder="대리인 성명 입력"
                  error={errors.name?.message}
                  {...register("name", { required: "대리인 성명은 필수입니다" })}
                />
                <TextField
                  label="연락처"
                  required
                  placeholder="010-0000-0000"
                  error={errors.phone?.message}
                  {...register("phone", {
                    required: "연락처는 필수입니다",
                    pattern: { value: /^[0-9-]+$/, message: "숫자와 - 만 입력해 주세요" }
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
                <SelectField
                  label="위임 관계"
                  options={RELATION_OPTIONS}
                  {...register("relation")}
                />
              </div>
            </section>

            <div className="h-px w-full bg-border" />

            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">위임 정보</h2>
              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <TextField
                  label="위임 유효기간"
                  type="date"
                  placeholder="YYYY-MM-DD"
                  {...register("expiryDate")}
                />
                <SelectField
                  label="위임 범위"
                  options={SCOPE_OPTIONS}
                  {...register("scope")}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="memo" className="text-[13px] text-foreground">
                  위임 메모
                </Label>
                <textarea
                  id="memo"
                  rows={3}
                  placeholder="추가 메모 입력 (선택)"
                  className="flex w-full rounded-md border border-input bg-card px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                  {...register("memo")}
                />
              </div>
            </section>

            {error ? <p className="text-[12px] text-destructive">{error}</p> : null}

            <div className="flex items-center gap-2 pt-1">
              <Button type="submit" disabled={isSubmitting} className="rounded-[10px] px-5">
                저장하고 계속
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                className="rounded-[10px] px-5"
                onClick={() => router.push("/corporate/representative")}
              >
                이전으로
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
