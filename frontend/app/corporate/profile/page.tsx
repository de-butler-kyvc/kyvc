"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
  const [info, setInfo] = useState<CorporateBasicInfo>(EMPTY);
  const [zonecode, setZonecode] = useState("");
  const [baseAddress, setBaseAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const openPostcode = useDaumPostcode();

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as
          | (CorporateBasicInfo & { corporateId?: number })
          | undefined;
        if (!c) return;
        setInfo({
          corporateName: c.corporateName ?? "",
          businessNo: c.businessNo ?? "",
          corporateNo: c.corporateNo ?? "",
          address: c.address ?? "",
          businessType: c.businessType ?? ""
        });
        if (c.corporateId) setCorporateId(c.corporateId);
        const m = (c.address ?? "").match(/^\((\d{5})\)\s*(.*?)\s*\|\s*(.*)$/);
        if (m) {
          setZonecode(m[1]);
          setBaseAddress(m[2]);
          setDetailAddress(m[3]);
        } else {
          setBaseAddress(c.address ?? "");
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
      setDetailAddress("");
    });

  const onSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    setSaving(true);
    const composedAddress = zonecode
      ? `(${zonecode}) ${baseAddress} | ${detailAddress}`
      : baseAddress;
    const payload = { ...info, address: composedAddress };
    try {
      if (corporateId) {
        await corpApi.updateBasicInfo(corporateId, payload);
      } else {
        const res = await corpApi.create(payload);
        setCorporateId(res.corporateId);
      }
      setInfo(payload);
      setMessage("저장되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-4 px-9 py-7">
      <div className="flex flex-col gap-1">
        <div className="font-mono text-[10px] uppercase tracking-[0.6px] text-subtle-foreground">
          UWEB-002 · M-02
        </div>
        <h1 className="text-[20px] font-bold tracking-[-0.4px] text-foreground">
          법인 기본정보
        </h1>
        <p className="text-[13px] text-muted-foreground">
          KYC 신청에 사용되는 법인 기본정보를 관리합니다. 등기부등본·사업자등록증과 일치해야 합니다.
        </p>
      </div>

      <Card>
        <CardContent className="p-0">
          <form onSubmit={onSave}>
            <FormGroup title="법인 식별정보">
              <FormRow label="법인명" required>
                <Input value={info.corporateName} onChange={(e) => update("corporateName", e.target.value)} placeholder="예) 주식회사 케이와이브이씨" />
              </FormRow>
              <FormRow label="법인등록번호" required>
                <Input value={info.corporateNo} onChange={(e) => update("corporateNo", e.target.value)} placeholder="000000-0000000" />
              </FormRow>
              <FormRow label="사업자등록번호" required>
                <Input value={info.businessNo} onChange={(e) => update("businessNo", e.target.value)} placeholder="000-00-00000" />
              </FormRow>
              <FormRow label="법인 유형">
                <select
                  className="flex h-9 w-full rounded-md border border-input bg-card px-3 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                  value={info.businessType}
                  onChange={(e) => update("businessType", e.target.value)}
                >
                  <option value="">선택</option>
                  {CORP_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </FormRow>
            </FormGroup>

            <div className="h-px w-full bg-border" />

            <FormGroup title="본점 소재지">
              <FormRow label="우편번호">
                <div className="flex gap-2">
                  <Input
                    className="max-w-[160px]"
                    value={zonecode}
                    placeholder="00000"
                    readOnly
                  />
                  <Button type="button" variant="outline" size="sm" onClick={onSearchAddress}>
                    주소 검색
                  </Button>
                </div>
              </FormRow>
              <FormRow label="기본 주소">
                <Input value={baseAddress} placeholder="주소 검색을 이용해 주세요" readOnly />
              </FormRow>
              <FormRow label="상세 주소">
                <Input
                  value={detailAddress}
                  onChange={(e) => setDetailAddress(e.target.value)}
                  placeholder="동·호수 등"
                />
              </FormRow>
            </FormGroup>

            <div className="flex items-center justify-between border-t border-border bg-secondary px-5 py-3.5">
              <p className="text-[12px]">
                {message ? (
                  <span className="text-success">{message}</span>
                ) : error ? (
                  <span className="text-destructive">{error}</span>
                ) : (
                  <span className="text-muted-foreground">저장 시 변경 이력이 기록됩니다.</span>
                )}
              </p>
              <Button type="submit" disabled={saving} className="rounded-[10px]">
                {saving ? "저장 중..." : "저장"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function FormGroup({
  title,
  children
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1 px-5 py-5">
      <div className="pb-2 text-[11px] font-bold uppercase tracking-[0.55px] text-subtle-foreground">
        {title}
      </div>
      <div className="flex flex-col">{children}</div>
    </div>
  );
}

function FormRow({
  label,
  required,
  children
}: {
  label: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-1 items-center gap-2 border-b border-row-border py-3 last:border-0 md:grid-cols-[180px_1fr]">
      <Label className="text-[13px] text-muted-foreground">
        {label}
        {required ? <span className="ml-1 text-destructive">*</span> : null}
      </Label>
      <div>{children}</div>
    </div>
  );
}
