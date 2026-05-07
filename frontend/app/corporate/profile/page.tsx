"use client";

import { useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ApiError,
  type CorporateBasicInfo,
  corporate as corpApi
} from "@/lib/api";

const EMPTY: CorporateBasicInfo = {
  corporateName: "",
  businessNo: "",
  corporateNo: "",
  address: "",
  businessType: ""
};

export default function CorporateProfilePage() {
  const [info, setInfo] = useState<CorporateBasicInfo>(EMPTY);
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

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
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const update = (k: keyof CorporateBasicInfo, v: string) =>
    setInfo((prev) => ({ ...prev, [k]: v }));

  const onSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    setSaving(true);
    try {
      if (corporateId) {
        await corpApi.updateBasicInfo(corporateId, info);
      } else {
        const res = await corpApi.create(info);
        setCorporateId(res.corporateId);
      }
      setMessage("저장되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageShell
      title="법인 정보"
      description="법인 기본정보를 관리합니다."
      module="UWEB-002/003 · M-02"
    >
      <Card>
        <CardHeader>
          <CardTitle>기본 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSave} className="grid gap-4 md:grid-cols-2">
            <Field id="p-name" label="법인명" value={info.corporateName} onChange={(v) => update("corporateName", v)} />
            <Field id="p-corp-no" label="법인등록번호" value={info.corporateNo} onChange={(v) => update("corporateNo", v)} />
            <Field id="p-biz-no" label="사업자등록번호" value={info.businessNo} onChange={(v) => update("businessNo", v)} />
            <Field id="p-biz-type" label="업종" value={info.businessType} onChange={(v) => update("businessType", v)} />
            <div className="md:col-span-2">
              <Field id="p-addr" label="주소" value={info.address} onChange={(v) => update("address", v)} />
            </div>
            <div className="md:col-span-2 flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                {message ?? error ?? ""}
              </p>
              <Button type="submit" disabled={saving}>
                {saving ? "저장 중..." : "저장"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </PageShell>
  );
}

function Field({
  id,
  label,
  value,
  onChange
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="grid gap-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} value={value} onChange={(e) => onChange(e.target.value)} />
    </div>
  );
}
