"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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

export default function CorporateKycApplyPage() {
  const router = useRouter();
  const [info, setInfo] = useState<CorporateBasicInfo>({
    corporateName: "",
    businessNo: "",
    corporateNo: "",
    address: "",
    businessType: ""
  });
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [corporateType, setCorporateType] = useState(CORPORATE_TYPES[0].value);
  const [requirements, setRequirements] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as (CorporateBasicInfo & { corporateId?: number }) | undefined;
        if (!c) return;
        setInfo({
          corporateName: c.corporateName ?? "",
          businessNo: c.businessNo ?? "",
          corporateNo: c.corporateNo ?? "",
          address: c.address ?? "",
          businessType: c.businessType ?? ""
        });
        if (c.corporateId) setCorporateId(c.corporateId);
      })
      .catch(() => {
        /* 신규 사용자 */
      });
  }, []);

  useEffect(() => {
    kycApi
      .documentRequirements(corporateType)
      .then((r) => setRequirements(r.requiredDocuments ?? []))
      .catch(() => setRequirements([]));
  }, [corporateType]);

  const update = (key: keyof CorporateBasicInfo, value: string) =>
    setInfo((prev) => ({ ...prev, [key]: value }));

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const id = corporateId
        ? (await corpApi.updateBasicInfo(corporateId, info), corporateId)
        : (await corpApi.create(info)).corporateId;
      const created = await kycApi.create({
        corporateId: id,
        applicationType: "ONLINE",
        corporateType
      });
      router.push(`/corporate/kyc/detail?id=${created.kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "신청 생성 중 오류가 발생했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageShell
      title="KYC 신청"
      description="법인 정보와 유형을 입력하면 신청이 생성되고 서류 업로드 단계로 이동합니다."
      module="UWEB-006~008 · M-02 / M-03"
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle>법인 기본 정보</CardTitle>
            <CardDescription>등기부등본·사업자등록증과 일치하도록 입력하세요.</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <Field id="corp-name" label="법인명" value={info.corporateName} onChange={(v) => update("corporateName", v)} />
            <Field id="corp-no" label="법인등록번호" value={info.corporateNo} onChange={(v) => update("corporateNo", v)} placeholder="000000-0000000" />
            <Field id="biz-no" label="사업자등록번호" value={info.businessNo} onChange={(v) => update("businessNo", v)} placeholder="000-00-00000" />
            <Field id="biz-type" label="업종" value={info.businessType} onChange={(v) => update("businessType", v)} />
            <div className="md:col-span-2">
              <Field id="address" label="주소" value={info.address} onChange={(v) => update("address", v)} />
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
                  onClick={() => setCorporateType(t.value)}
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
          <Button type="submit" disabled={submitting}>
            {submitting ? "처리 중..." : "신청 생성 및 다음 단계"}
          </Button>
        </div>
      </form>
    </PageShell>
  );
}

function Field({
  id,
  label,
  value,
  onChange,
  placeholder
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}) {
  return (
    <div className="grid gap-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
    </div>
  );
}
