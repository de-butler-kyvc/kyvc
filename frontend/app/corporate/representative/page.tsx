"use client";

import { useRouter } from "next/navigation";
import { type ChangeEvent, useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";

import { type CorporateSummary } from "@/components/corporate/info-card";
import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";
import {
  ApiError,
  type RepresentativeResponse,
  corporate as corpApi
} from "@/lib/api";

type FileMeta = {
  name: string;
  size: string;
  file?: File;
  uploaded?: boolean;
};

type RepresentativeFormValues = {
  name: string;
  birthDate: string;
  nationality: string;
  phone: string;
  email: string;
  idDoc: FileMeta | null;
};

const NATIONALITIES = [
  { value: "KR", label: "대한민국" },
  { value: "US", label: "United States" },
  { value: "JP", label: "日本" },
  { value: "CN", label: "中国" },
  { value: "OTHER", label: "기타" }
];

const DEFAULT_FORM: RepresentativeFormValues = {
  name: "",
  birthDate: "",
  nationality: "KR",
  phone: "",
  email: "",
  idDoc: null
};

const formatSize = (size: number) => {
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))}KB`;
  return `${(size / 1024 / 1024).toFixed(1)}MB`;
};

export default function CorporateRepresentativePage() {
  const router = useRouter();
  const [corp, setCorp] = useState<CorporateSummary | null>(null);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting }
  } = useForm<RepresentativeFormValues>({ defaultValues: DEFAULT_FORM });

  useEffect(() => {
    corpApi
      .me()
      .then(async (res) => {
        const corporateType =
          (typeof window !== "undefined"
            ? window.localStorage.getItem("kyvc.corporateType")
            : null) ?? "";
        const summary: CorporateSummary = {
          corporateId: res.corporateId,
          corporateName: res.corporateName ?? "",
          businessNo: res.businessRegistrationNo ?? "",
          corporateNo: res.corporateRegistrationNo ?? "",
          representativeName: res.representativeName ?? "",
          corporateType
        };
        setCorp(summary);

        let values: RepresentativeFormValues;
        try {
          const saved = await corpApi.representatives(res.corporateId);
          values = representativeDefaultsFromApi(saved, res);
        } catch {
          values = representativeDefaultsFromApi([], res);
        }
        reset(values);
      })
      .catch((err: unknown) =>
        setFetchError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [reset]);

  const onFileChange = (
    onChange: (value: FileMeta | null) => void,
    event: ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;
    onChange({ name: file.name, size: formatSize(file.size), file });
    event.target.value = "";
  };

  const persist = async (data: RepresentativeFormValues) => {
    setSubmitError(null);
    setMessage(null);

    if (!corp?.corporateId) {
      setSubmitError("법인 기본정보를 먼저 등록하세요.");
      return false;
    }

    try {
      await corpApi.updateRepresentative(corp.corporateId, {
        name: data.name,
        birthDate: data.birthDate,
        nationalityCode: data.nationality,
        phone: data.phone,
        email: data.email,
        identityFile: data.idDoc?.file
      });
      setMessage("저장되었습니다.");
      return true;
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
      return false;
    }
  };

  const onContinue = handleSubmit(async (data) => {
    const ok = await persist(data);
    if (ok) router.push("/corporate/agents");
  });

  const bannerError = fetchError ?? submitError;

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">대표자 정보 등록</h1>
          <p className="page-head-desc">
            법인 KYC 심사에 활용될 대표자 식별정보와 신분증 사본을 등록해주세요.
          </p>
        </div>
      </div>

      <form onSubmit={onContinue} noValidate>
        <section className="form-card">
          <div className="form-card-header">
            <div className="flex items-center gap-2">
              <div className="form-card-title">대표자</div>
              <Badge variant="default">주 대표</Badge>
            </div>
          </div>

          <div className="form-grid">
            <TextField
              label="대표자명"
              required
              placeholder="대표자명 입력"
              error={errors.name?.message}
              {...register("name", { required: "대표자명은 필수입니다." })}
            />
            <TextField
              label="생년월일"
              required
              type="date"
              error={errors.birthDate?.message}
              {...register("birthDate", { required: "생년월일은 필수입니다." })}
            />
            <label className="field">
              <span className="field-label">
                국적 <span style={{ color: "var(--danger)" }}>*</span>
              </span>
              <select className="input" {...register("nationality", { required: true })}>
                {NATIONALITIES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <TextField
              label="연락처"
              required
              placeholder="010-0000-0000"
              error={errors.phone?.message}
              {...register("phone", {
                required: "연락처는 필수입니다.",
                pattern: {
                  value: /^[0-9-]+$/,
                  message: "연락처는 숫자와 - 만 입력해 주세요."
                }
              })}
            />
            <TextField
              label="이메일"
              type="email"
              containerClassName="col-span-2"
              placeholder="이메일 입력"
              error={errors.email?.message}
              {...register("email", {
                validate: (v) =>
                  !v || /\S+@\S+\.\S+/.test(v) || "이메일 형식이 올바르지 않습니다."
              })}
            />

            <Controller
              name="idDoc"
              control={control}
              rules={{
                validate: (v) => v != null || "신분증 사본은 필수입니다."
              }}
              render={({ field, fieldState }) => (
                <div className="field col-span-2">
                  <span className="field-label">
                    신분증 사본 <span style={{ color: "var(--danger)" }}>*</span>
                  </span>
                  <span className="field-help">
                    주민등록증, 운전면허증, 여권 등 대표자 신분확인 문서
                  </span>
                  {fieldState.error ? (
                    <span className="field-error">{fieldState.error.message}</span>
                  ) : null}
                  {field.value ? (
                    <div className="file-pill">
                      <Icon.File />
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-[13px] font-semibold">
                          {field.value.name}
                        </div>
                        <div className="text-[12px] text-muted-foreground">
                          {field.value.size}
                          {field.value.uploaded ? " · 등록됨" : ""}
                        </div>
                      </div>
                      <label className="btn btn-ghost btn-sm">
                        교체
                        <input
                          type="file"
                          className="hidden"
                          accept=".pdf,.jpg,.jpeg,.png"
                          onChange={(event) => onFileChange(field.onChange, event)}
                        />
                      </label>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        aria-label="신분증 파일 삭제"
                        onClick={() => field.onChange(null)}
                      >
                        <Icon.Trash />
                      </Button>
                    </div>
                  ) : (
                    <label className="upload-tile min-h-[100px]">
                      <Icon.Upload className="upload-tile-icon" />
                      <span className="upload-tile-text">신분증 사본 업로드</span>
                      <span className="upload-tile-meta">PDF, JPG, PNG · 최대 10MB</span>
                      <input
                        type="file"
                        className="hidden"
                        accept=".pdf,.jpg,.jpeg,.png"
                        onChange={(event) => onFileChange(field.onChange, event)}
                      />
                    </label>
                  )}
                </div>
              )}
            />
          </div>
        </section>

        {bannerError || message ? (
          <p className="mt-4 text-[12px]">
            {message ? (
              <span className="text-success">{message}</span>
            ) : (
              <span className="text-destructive">{bannerError}</span>
            )}
          </p>
        ) : null}

        <div className="form-actions right">
          <Button type="submit" disabled={isSubmitting}>
            저장
          </Button>
        </div>
      </form>
    </div>
  );
}

function representativeDefaultsFromApi(
  responses: RepresentativeResponse[],
  fallback: {
    representativeName?: string | null;
    representativePhone?: string | null;
    representativeEmail?: string | null;
  }
): RepresentativeFormValues {
  if (responses.length === 0) {
    return {
      ...DEFAULT_FORM,
      name: fallback.representativeName ?? "",
      phone: fallback.representativePhone ?? "",
      email: fallback.representativeEmail ?? ""
    };
  }

  const item = responses[0];
  const hasIdentityDocument = item.identityDocumentId != null;
  return {
    ...DEFAULT_FORM,
    name: item.name ?? "",
    birthDate: item.birthDate ?? "",
    nationality: item.nationalityCode ?? "KR",
    phone: item.phoneNumber ?? "",
    email: item.email ?? "",
    idDoc: hasIdentityDocument
      ? { name: "등록된 신분증 사본", size: "서버 보관", uploaded: true }
      : null
  };
}
