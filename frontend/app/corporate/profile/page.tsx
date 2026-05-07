"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { SelectField } from "@/components/ui/select-field";
import { TextField } from "@/components/ui/text-field";
import { ApiError, corporate as corpApi } from "@/lib/api";
import { useDaumPostcode } from "@/lib/use-postcode";

type ProfileForm = {
  corporateName: string;
  corporateType: string;
  businessNo: string;
  corporateNo: string;
  representativeName: string;
  zonecode: string;
  baseAddress: string;
  detailAddress: string;
  industry: string;
  phone: string;
};

type Mode = "loading" | "view" | "register";

const DEFAULTS: ProfileForm = {
  corporateName: "",
  corporateType: "",
  businessNo: "",
  corporateNo: "",
  representativeName: "",
  zonecode: "",
  baseAddress: "",
  detailAddress: "",
  industry: "",
  phone: ""
};

const CORP_TYPE_OPTIONS = [
  { value: "주식회사", label: "주식회사" },
  { value: "유한회사", label: "유한회사" },
  { value: "재단법인", label: "재단법인" },
  { value: "사단법인", label: "사단법인" },
  { value: "외국법인 지점", label: "외국법인 지점" }
];

export default function CorporateProfilePage() {
  const router = useRouter();
  const openPostcode = useDaumPostcode();
  const [mode, setMode] = useState<Mode>("loading");
  const [profile, setProfile] = useState<ProfileForm | null>(null);
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting }
  } = useForm<ProfileForm>({ defaultValues: DEFAULTS });

  const zonecode = watch("zonecode");
  const baseAddress = watch("baseAddress");

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
              address?: string;
              businessType?: string;
            }
          | undefined;
        const next: ProfileForm = { ...DEFAULTS };
        if (c) {
          next.corporateName = c.corporateName ?? "";
          next.businessNo = c.businessNo ?? "";
          next.corporateNo = c.corporateNo ?? "";
          next.industry = c.businessType ?? "";
          if (c.corporateId) setCorporateId(c.corporateId);
          const m = (c.address ?? "").match(/^\((\d{5})\)\s*(.*?)\s*\|\s*(.*)$/);
          if (m) {
            next.zonecode = m[1];
            next.baseAddress = m[2];
            next.detailAddress = m[3];
          } else {
            next.baseAddress = c.address ?? "";
          }
        }
        if (res.representative) {
          next.representativeName = res.representative.name ?? "";
          next.phone = res.representative.phone ?? "";
        }
        if (typeof window !== "undefined") {
          next.corporateType = window.localStorage.getItem("kyvc.corporateType") ?? "";
        }
        reset(next);
        if (c?.corporateId) {
          setProfile(next);
          setMode("view");
        } else {
          setMode("register");
        }
      })
      .catch((err: unknown) => {
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
        setMode("register");
      });
  }, [reset]);

  const onSearchAddress = () =>
    openPostcode((data) => {
      setValue("zonecode", data.zonecode, { shouldDirty: true });
      setValue("baseAddress", data.roadAddress || data.address, { shouldDirty: true });
    });

  const persist = async (data: ProfileForm) => {
    setError(null);
    setMessage(null);
    const composedAddress = data.zonecode
      ? `(${data.zonecode}) ${data.baseAddress} | ${data.detailAddress}`
      : data.baseAddress;
    const payload = {
      corporateName: data.corporateName,
      businessNo: data.businessNo,
      corporateNo: data.corporateNo,
      address: composedAddress,
      businessType: data.industry
    };
    const id = corporateId
      ? (await corpApi.updateBasicInfo(corporateId, payload), corporateId)
      : (await corpApi.create(payload)).corporateId;
    setCorporateId(id);
    if (typeof window !== "undefined") {
      window.localStorage.setItem("kyvc.corporateType", data.corporateType);
    }
    if (data.representativeName.trim() || data.phone.trim()) {
      await corpApi
        .updateRepresentative(id, {
          name: data.representativeName,
          birthDate: "",
          phone: data.phone,
          email: ""
        })
        .catch(() => undefined);
    }
    setProfile(data);
    return id;
  };

  const onSaveDraft = handleSubmit(async (data) => {
    try {
      await persist(data);
      setMode("view");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  const onContinue = handleSubmit(async (data) => {
    try {
      await persist(data);
      router.push("/corporate/representative");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  if (mode === "loading") {
    return (
      <div className="mx-auto w-full max-w-[920px] px-9 py-8 text-[13px] text-muted-foreground">
        불러오는 중...
      </div>
    );
  }

  if (mode === "view" && profile) {
    return <ProfileView profile={profile} />;
  }

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-5 px-9 py-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          법인 기본정보 등록
        </h1>
        <p className="text-[13px] text-destructive">
          KYC 심사에 활용될 법인 정보를 입력해주세요. * 표시 항목은 필수입니다.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-7 px-7 py-7">
          <form onSubmit={onContinue} className="flex flex-col gap-7" noValidate>
            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">법인 식별정보</h2>
              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <TextField
                  label="법인명"
                  required
                  placeholder="법인명 입력"
                  error={errors.corporateName?.message}
                  {...register("corporateName", { required: "법인명은 필수입니다" })}
                />
                <SelectField
                  label="법인 유형"
                  required
                  placeholder="선택"
                  options={CORP_TYPE_OPTIONS}
                  error={errors.corporateType?.message}
                  {...register("corporateType", { required: "법인 유형을 선택하세요" })}
                />
                <TextField
                  label="사업자등록번호"
                  required
                  placeholder="000-00-00000"
                  error={errors.businessNo?.message}
                  {...register("businessNo", {
                    required: "사업자등록번호는 필수입니다",
                    pattern: { value: /^\d{3}-?\d{2}-?\d{5}$/, message: "형식이 올바르지 않습니다 (000-00-00000)" }
                  })}
                />
                <TextField
                  label="법인등록번호"
                  placeholder="법인등록번호 입력"
                  error={errors.corporateNo?.message}
                  {...register("corporateNo", {
                    pattern: { value: /^\d{6}-?\d{7}$/, message: "형식이 올바르지 않습니다 (000000-0000000)" }
                  })}
                />
                <TextField
                  label="대표자명"
                  required
                  placeholder="대표자명 입력"
                  error={errors.representativeName?.message}
                  {...register("representativeName", { required: "대표자명은 필수입니다" })}
                />
              </div>
            </section>

            <div className="h-px w-full bg-border" />

            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">주소 및 업종</h2>

              <TextField
                label="본점 주소"
                required
                readOnly
                placeholder="주소를 검색하세요"
                value={[zonecode && `(${zonecode})`, baseAddress].filter(Boolean).join(" ")}
                onChange={() => undefined}
                error={errors.baseAddress?.message}
                endAdornment={
                  <Button type="button" variant="outline" onClick={onSearchAddress}>
                    주소 검색
                  </Button>
                }
              />
              <input type="hidden" {...register("zonecode")} />
              <input
                type="hidden"
                {...register("baseAddress", { required: "주소를 검색해 주세요" })}
              />

              <TextField
                label="상세 주소"
                placeholder="상세 주소 입력"
                {...register("detailAddress")}
              />

              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <TextField label="업종" placeholder="업종 입력" {...register("industry")} />
                <TextField
                  label="연락처"
                  placeholder="대표 연락처"
                  error={errors.phone?.message}
                  {...register("phone", {
                    pattern: { value: /^[0-9-]+$/, message: "숫자와 - 만 입력해 주세요" }
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

function ProfileView({ profile }: { profile: ProfileForm }) {
  const fullAddress = [profile.zonecode && `(${profile.zonecode})`, profile.baseAddress]
    .filter(Boolean)
    .join(" ");
  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-5 px-9 py-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          법인 기본정보
        </h1>
        <p className="text-[13px] text-muted-foreground">
          등록된 법인 기본정보입니다.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-7 px-7 py-7">
          <section className="flex flex-col gap-4">
            <h2 className="text-[15px] font-bold text-foreground">법인 식별정보</h2>
            <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
              <ViewField label="법인명" value={profile.corporateName} />
              <ViewField label="법인 유형" value={profile.corporateType} />
              <ViewField label="사업자등록번호" value={profile.businessNo} />
              <ViewField label="법인등록번호" value={profile.corporateNo} />
              <ViewField label="대표자명" value={profile.representativeName} />
            </div>
          </section>

          <div className="h-px w-full bg-border" />

          <section className="flex flex-col gap-4">
            <h2 className="text-[15px] font-bold text-foreground">주소 및 업종</h2>
            <ViewField label="본점 주소" value={fullAddress} />
            <ViewField label="상세 주소" value={profile.detailAddress} />
            <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
              <ViewField label="업종" value={profile.industry} />
              <ViewField label="연락처" value={profile.phone} />
            </div>
          </section>
        </CardContent>
      </Card>
    </div>
  );
}

function ViewField({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-[13px] text-muted-foreground">{label}</span>
      <div className="flex h-9 items-center rounded-md border border-border bg-secondary px-3 text-[13px] text-foreground">
        {value || "-"}
      </div>
    </div>
  );
}
