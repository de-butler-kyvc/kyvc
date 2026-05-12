"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { Field, ToastStack, useToasts } from "@/components/design/primitives";
import { ApiError, corporate as corpApi } from "@/lib/api";
import { useDaumPostcode } from "@/lib/use-postcode";

type ProfileForm = {
  corporateName: string;
  businessNo: string;
  corporateNo: string;
  establishedDate: string;
  zonecode: string;
  baseAddress: string;
  detailAddress: string;
  phone: string;
  contactEmail: string;
  website: string;
};

const DEFAULTS: ProfileForm = {
  corporateName: "",
  businessNo: "",
  corporateNo: "",
  establishedDate: "",
  zonecode: "",
  baseAddress: "",
  detailAddress: "",
  phone: "",
  contactEmail: "",
  website: ""
};

const EXTRA_KEY = "kyvc.corporateExtras";

type ExtraFields = Pick<ProfileForm, "establishedDate" | "contactEmail" | "website">;

const extraKey = (corporateId?: number | null) =>
  corporateId ? `${EXTRA_KEY}.${corporateId}` : EXTRA_KEY;

const readExtras = (corporateId?: number | null): ExtraFields => {
  if (typeof window === "undefined")
    return { establishedDate: "", contactEmail: "", website: "" };
  try {
    const raw = window.localStorage.getItem(extraKey(corporateId));
    return raw
      ? (JSON.parse(raw) as ExtraFields)
      : { establishedDate: "", contactEmail: "", website: "" };
  } catch {
    return { establishedDate: "", contactEmail: "", website: "" };
  }
};

const writeExtras = (corporateId: number, extras: ExtraFields) => {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(extraKey(corporateId), JSON.stringify(extras));
  window.localStorage.removeItem(EXTRA_KEY);
};

const composeAddress = (zonecode: string, base: string, detail: string) => {
  const left = zonecode ? `(${zonecode}) ${base}` : base;
  return detail ? `${left} | ${detail}` : left;
};

const parseAddress = (raw: string | undefined) => {
  if (!raw) return { zonecode: "", baseAddress: "", detailAddress: "" };
  const m = raw.match(/^\((\d{5})\)\s*(.*?)\s*\|\s*(.*)$/);
  if (m) return { zonecode: m[1], baseAddress: m[2], detailAddress: m[3] };
  const m2 = raw.match(/^\((\d{5})\)\s*(.*)$/);
  if (m2) return { zonecode: m2[1], baseAddress: m2[2], detailAddress: "" };
  return { zonecode: "", baseAddress: raw, detailAddress: "" };
};

export default function CorporateProfilePage() {
  const router = useRouter();
  const openPostcode = useDaumPostcode();
  const { toasts, addToast } = useToasts();
  const [loading, setLoading] = useState(true);
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors, isDirty, isSubmitting }
  } = useForm<ProfileForm>({ defaultValues: DEFAULTS });

  useEffect(() => {
    let cancelled = false;
    corpApi
      .me()
      .then((res) => {
        if (cancelled) return;
        const extras = readExtras(res?.corporateId);
        const addr = parseAddress(res?.address ?? "");
        reset({
          corporateName: res?.corporateName ?? "",
          businessNo: res?.businessRegistrationNo ?? "",
          corporateNo: res?.corporateRegistrationNo ?? "",
          zonecode: addr.zonecode,
          baseAddress: addr.baseAddress,
          detailAddress: addr.detailAddress,
          phone: res?.representativePhone ?? "",
          contactEmail: res?.representativeEmail ?? "",
          website: extras.website ?? "",
          establishedDate: extras.establishedDate ?? ""
        });
        setCorporateId(res?.corporateId ?? null);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [reset]);

  const onSearchAddress = () =>
    openPostcode((data) => {
      setValue("zonecode", data.zonecode, { shouldDirty: true });
      setValue("baseAddress", data.roadAddress || data.address, { shouldDirty: true });
    });

  const persist = async (data: ProfileForm) => {
    setError(null);
    const composedAddress = composeAddress(data.zonecode, data.baseAddress, data.detailAddress);
    const payload = {
      corporateName: data.corporateName,
      representativeName: 'Test',
      businessRegistrationNo: data.businessNo,
      corporateRegistrationNo: data.corporateNo,
      address: composedAddress,
      businessType: "",
      
    };
    const id = corporateId
      ? (await corpApi.updateBasicInfo(corporateId, payload), corporateId)
      : (await corpApi.create(payload)).corporateId;
    setCorporateId(id);
    writeExtras(id, {
      establishedDate: data.establishedDate,
      contactEmail: data.contactEmail,
      website: data.website
    });
    return id;
  };

  const onSave = handleSubmit(async (data) => {
    if (!isDirty) {
      addToast({ kind: "", title: "변경된 내용이 없습니다" });
      return;
    }
    try {
      await persist(data);
      reset(data);
      addToast({ kind: "success", title: "변경 사항이 저장되었습니다" });
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : "저장에 실패했습니다.";
      setError(msg);
      addToast({ kind: "danger", title: "저장 실패", desc: msg });
    }
  });

  if (loading) {
    return (
      <div style={{ padding: 24, color: "var(--text-muted)", fontSize: 13 }}>
        불러오는 중...
      </div>
    );
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-head-title">법인 기본정보 등록</h1>
          <p className="page-head-desc">
            KYC 심사 시 사용되는 법인 기본정보를 입력해주세요.
          </p>
        </div>
      </div>

      <form onSubmit={onSave} noValidate>
        <div className="form-card">
          <div className="form-card-header">
            <div className="form-card-title">법인 식별정보</div>
            <div className="form-card-meta">필수</div>
          </div>
          <div className="form-grid">
            <Field label="법인명" required error={errors.corporateName?.message}>
              <input
                className={`input${errors.corporateName ? " error" : ""}`}
                placeholder="예) 주식회사 케이원"
                {...register("corporateName", { required: "법인명은 필수입니다" })}
              />
            </Field>
            <Field label="사업자등록번호" required error={errors.businessNo?.message}>
              <input
                className={`input${errors.businessNo ? " error" : ""}`}
                placeholder="000-00-00000"
                {...register("businessNo", {
                  required: "사업자등록번호는 필수입니다",
                  pattern: {
                    value: /^\d{3}-?\d{2}-?\d{5}$/,
                    message: "형식이 올바르지 않습니다 (000-00-00000)"
                  }
                })}
              />
            </Field>
            <Field label="법인등록번호" required error={errors.corporateNo?.message}>
              <input
                className={`input${errors.corporateNo ? " error" : ""}`}
                placeholder="000000-0000000"
                {...register("corporateNo", {
                  required: "법인등록번호는 필수입니다",
                  pattern: {
                    value: /^\d{6}-?\d{7}$/,
                    message: "형식이 올바르지 않습니다 (000000-0000000)"
                  }
                })}
              />
            </Field>
            <Field label="설립일" required>
              <input className="input" type="date" {...register("establishedDate")} />
            </Field>
          </div>
        </div>

        <div className="form-card">
          <div className="form-card-header">
            <div className="form-card-title">소재지</div>
            <div className="form-card-meta">필수</div>
          </div>
          <div className="form-grid">
            <Field
              label="주소"
              required
              className="col-span-2"
              error={errors.baseAddress?.message}
            >
              <div className="form-row">
                <input
                  className="input"
                  style={{ maxWidth: 130, flex: "0 0 auto" }}
                  placeholder="00000"
                  readOnly
                  {...register("zonecode")}
                />
                <input
                  className={`input${errors.baseAddress ? " error" : ""}`}
                  placeholder="주소를 검색하세요"
                  readOnly
                  {...register("baseAddress", { required: "주소를 검색해 주세요" })}
                />
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={onSearchAddress}
                  style={{ flex: "0 0 auto" }}
                >
                  주소 검색
                </button>
              </div>
            </Field>
            <Field label="상세주소" className="col-span-2">
              <input
                className="input"
                placeholder="상세주소 입력"
                {...register("detailAddress")}
              />
            </Field>
          </div>
        </div>

        <div className="form-card">
          <div className="form-card-header">
            <div className="form-card-title">연락처</div>
            <div className="form-card-meta">선택</div>
          </div>
          <div className="form-grid">
            <Field label="대표 전화" error={errors.phone?.message}>
              <input
                className={`input${errors.phone ? " error" : ""}`}
                placeholder="02-1234-5678"
                {...register("phone", {
                  pattern: { value: /^[0-9-]+$/, message: "숫자와 - 만 입력해 주세요" }
                })}
              />
            </Field>
            <Field label="대표 이메일" error={errors.contactEmail?.message}>
              <input
                className={`input${errors.contactEmail ? " error" : ""}`}
                type="email"
                placeholder="contact@company.com"
                {...register("contactEmail", {
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$|^$/,
                    message: "이메일 형식이 올바르지 않습니다"
                  }
                })}
              />
            </Field>
            <Field label="웹사이트" className="col-span-2">
              <input
                className="input"
                placeholder="https://company.com"
                {...register("website")}
              />
            </Field>
          </div>
        </div>

        {error ? (
          <div className="alert alert-warning" style={{ marginTop: 16 }}>
            <span>{error}</span>
          </div>
        ) : null}

        <div className="form-actions right">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => router.push("/corporate")}
          >
            취소
          </button>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            변경 저장
          </button>
        </div>
      </form>

      <ToastStack toasts={toasts} />
    </>
  );
}
