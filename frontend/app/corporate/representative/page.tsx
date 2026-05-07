"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import {
  ApiError,
  type Representative,
  corporate as corpApi
} from "@/lib/api";

const DEFAULTS: Representative = { name: "", birthDate: "", phone: "", email: "" };

export default function CorporateRepresentativePage() {
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting }
  } = useForm<Representative>({ defaultValues: DEFAULTS });

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as { corporateId?: number } | undefined;
        if (c?.corporateId) setCorporateId(c.corporateId);
        if (res.representative) reset({ ...DEFAULTS, ...res.representative });
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [reset]);

  const onSave = handleSubmit(async (data) => {
    if (!corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    setError(null);
    setMessage(null);
    try {
      await corpApi.updateRepresentative(corporateId, data);
      setMessage("저장되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-5 px-9 py-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          대표자 정보
        </h1>
        <p className="text-[13px] text-muted-foreground">
          등기부등본 상의 대표자 정보를 입력합니다. KYC 심사에 활용됩니다.
        </p>
      </div>

      <Card>
        <CardContent className="px-7 py-7">
          <form onSubmit={onSave} className="flex flex-col gap-7" noValidate>
            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">대표자</h2>
              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <TextField
                  label="이름"
                  required
                  placeholder="이름 입력"
                  error={errors.name?.message}
                  {...register("name", { required: "이름은 필수입니다" })}
                />
                <TextField
                  label="생년월일"
                  required
                  type="date"
                  error={errors.birthDate?.message}
                  {...register("birthDate", { required: "생년월일은 필수입니다" })}
                />
                <TextField
                  label="휴대폰"
                  required
                  placeholder="010-0000-0000"
                  error={errors.phone?.message}
                  {...register("phone", {
                    required: "휴대폰 번호는 필수입니다",
                    pattern: { value: /^[0-9-]+$/, message: "숫자와 - 만 입력해 주세요" }
                  })}
                />
                <TextField
                  label="이메일"
                  required
                  type="email"
                  placeholder="email@example.com"
                  error={errors.email?.message}
                  {...register("email", {
                    required: "이메일은 필수입니다",
                    pattern: { value: /\S+@\S+\.\S+/, message: "이메일 형식이 올바르지 않습니다" }
                  })}
                />
              </div>
            </section>

            {error || message ? (
              <p className="text-[12px]">
                {message ? (
                  <span className="text-success">{message}</span>
                ) : (
                  <span className="text-destructive">{error}</span>
                )}
              </p>
            ) : null}

            <div className="flex items-center gap-2">
              <Button type="submit" disabled={isSubmitting} className="rounded-[10px] px-5">
                저장
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
