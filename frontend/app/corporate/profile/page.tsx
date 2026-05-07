"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  ApiError,
  type CorporateBasicInfo,
  corporate as corpApi
} from "@/lib/api";
import { useDaumPostcode } from "@/lib/use-postcode";

const EMPTY: CorporateBasicInfo = {
  corporateName: "",
  businessNo: "",
  corporateNo: "",
  address: "",
  businessType: ""
};

const CORP_TYPES = ["주식회사", "유한회사", "재단법인", "사단법인", "외국법인 지점"];

export default function CorporateProfilePage() {
  const router = useRouter();
  const [info, setInfo] = useState<CorporateBasicInfo>(EMPTY);
  const [corporateType, setCorporateType] = useState("");
  const [industry, setIndustry] = useState("");
  const [zonecode, setZonecode] = useState("");
  const [baseAddress, setBaseAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [representativeName, setRepresentativeName] = useState("");
  const [phone, setPhone] = useState("");
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const openPostcode = useDaumPostcode();

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as
          | (CorporateBasicInfo & { corporateId?: number })
          | undefined;
        if (c) {
          setInfo({
            corporateName: c.corporateName ?? "",
            businessNo: c.businessNo ?? "",
            corporateNo: c.corporateNo ?? "",
            address: c.address ?? "",
            businessType: c.businessType ?? ""
          });
          setIndustry(c.businessType ?? "");
          if (c.corporateId) setCorporateId(c.corporateId);
          const stored =
            typeof window !== "undefined"
              ? window.localStorage.getItem("kyvc.corporateType")
              : null;
          if (stored) setCorporateType(stored);
          const m = (c.address ?? "").match(/^\((\d{5})\)\s*(.*?)\s*\|\s*(.*)$/);
          if (m) {
            setZonecode(m[1]);
            setBaseAddress(m[2]);
            setDetailAddress(m[3]);
          } else {
            setBaseAddress(c.address ?? "");
          }
        }
        if (res.representative) {
          setRepresentativeName(res.representative.name ?? "");
          setPhone(res.representative.phone ?? "");
        }
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const update = (k: keyof CorporateBasicInfo, v: string) =>
    setInfo((prev) => ({ ...prev, [k]: v }));

  const onSearchAddress = () =>
    openPostcode((data) => {
      setZonecode(data.zonecode);
      setBaseAddress(data.roadAddress || data.address);
    });

  const persist = async () => {
    setError(null);
    setMessage(null);
    const composedAddress = zonecode
      ? `(${zonecode}) ${baseAddress} | ${detailAddress}`
      : baseAddress;
    const payload: CorporateBasicInfo = {
      ...info,
      address: composedAddress,
      businessType: industry
    };
    const id = corporateId
      ? (await corpApi.updateBasicInfo(corporateId, payload), corporateId)
      : (await corpApi.create(payload)).corporateId;
    setCorporateId(id);
    setInfo(payload);
    if (typeof window !== "undefined") {
      window.localStorage.setItem("kyvc.corporateType", corporateType);
    }
    if (representativeName.trim() || phone.trim()) {
      await corpApi
        .updateRepresentative(id, {
          name: representativeName,
          birthDate: "",
          phone,
          email: ""
        })
        .catch(() => {
          /* 대표자 부분 저장 실패는 메인 흐름을 막지 않음 */
        });
    }
    return id;
  };

  const onSaveDraft = async () => {
    setBusy(true);
    try {
      await persist();
      setMessage("임시 저장되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await persist();
      router.push("/corporate/representative");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

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
          <form onSubmit={onSubmit} className="flex flex-col gap-7">
            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">법인 식별정보</h2>
              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <Field label="법인명" required>
                  <Input
                    value={info.corporateName}
                    onChange={(e) => update("corporateName", e.target.value)}
                    placeholder="법인명 입력"
                  />
                </Field>
                <Field label="법인 유형" required>
                  <select
                    className="flex h-9 w-full rounded-md border border-input bg-card px-3 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                    value={corporateType}
                    onChange={(e) => setCorporateType(e.target.value)}
                  >
                    <option value="">선택</option>
                    {CORP_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="사업자등록번호" required>
                  <Input
                    value={info.businessNo}
                    onChange={(e) => update("businessNo", e.target.value)}
                    placeholder="000-00-00000"
                  />
                </Field>
                <Field label="법인등록번호">
                  <Input
                    value={info.corporateNo}
                    onChange={(e) => update("corporateNo", e.target.value)}
                    placeholder="법인등록번호 입력"
                  />
                </Field>
                <Field label="대표자명" required>
                  <Input
                    value={representativeName}
                    onChange={(e) => setRepresentativeName(e.target.value)}
                    placeholder="대표자명 입력"
                  />
                </Field>
              </div>
            </section>

            <div className="h-px w-full bg-border" />

            <section className="flex flex-col gap-4">
              <h2 className="text-[15px] font-bold text-foreground">주소 및 업종</h2>

              <Field label="본점 주소" required>
                <div className="flex gap-2">
                  <Input
                    className="flex-1"
                    value={[zonecode && `(${zonecode})`, baseAddress]
                      .filter(Boolean)
                      .join(" ")}
                    placeholder="주소를 검색하세요"
                    readOnly
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="default"
                    onClick={onSearchAddress}
                  >
                    주소 검색
                  </Button>
                </div>
              </Field>

              <Field label="상세 주소">
                <Input
                  value={detailAddress}
                  onChange={(e) => setDetailAddress(e.target.value)}
                  placeholder="상세 주소 입력"
                />
              </Field>

              <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
                <Field label="업종">
                  <Input
                    value={industry}
                    onChange={(e) => setIndustry(e.target.value)}
                    placeholder="업종 입력"
                  />
                </Field>
                <Field label="연락처">
                  <Input
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="대표 연락처"
                  />
                </Field>
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
              <Button type="submit" disabled={busy} className="rounded-[10px] px-5">
                저장하고 계속
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={busy}
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

function Field({
  label,
  required,
  children
}: {
  label: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-[13px] text-foreground">
        {label}
        {required ? <span className="ml-0.5 text-destructive">*</span> : null}
      </label>
      {children}
    </div>
  );
}
