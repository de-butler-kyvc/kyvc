"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ApiError,
  type Representative,
  corporate as corpApi
} from "@/lib/api";

const EMPTY: Representative = { name: "", birthDate: "", phone: "", email: "" };

export default function CorporateRepresentativePage() {
  const [rep, setRep] = useState<Representative>(EMPTY);
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as { corporateId?: number } | undefined;
        if (c?.corporateId) setCorporateId(c.corporateId);
        if (res.representative) setRep({ ...EMPTY, ...res.representative });
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const update = (k: keyof Representative, v: string) =>
    setRep((prev) => ({ ...prev, [k]: v }));

  const onSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await corpApi.updateRepresentative(corporateId, rep);
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
          UWEB-004 · M-02
        </div>
        <h1 className="text-[20px] font-bold tracking-[-0.4px] text-foreground">
          대표자 정보
        </h1>
        <p className="text-[13px] text-muted-foreground">
          법인 등기부등본 상의 대표자 정보를 입력합니다.
        </p>
      </div>

      <Card>
        <CardContent className="p-0">
          <form onSubmit={onSave}>
            <FormGroup title="대표자">
              <FormRow label="이름" required>
                <Input value={rep.name} onChange={(e) => update("name", e.target.value)} />
              </FormRow>
              <FormRow label="생년월일" required>
                <Input
                  type="date"
                  value={rep.birthDate}
                  onChange={(e) => update("birthDate", e.target.value)}
                />
              </FormRow>
              <FormRow label="휴대폰" required>
                <Input
                  value={rep.phone}
                  onChange={(e) => update("phone", e.target.value)}
                  placeholder="010-0000-0000"
                />
              </FormRow>
              <FormRow label="이메일" required>
                <Input
                  type="email"
                  value={rep.email}
                  onChange={(e) => update("email", e.target.value)}
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
                  <span className="text-muted-foreground">
                    대표자 정보 변경 시 KYC 재심사가 필요할 수 있습니다.
                  </span>
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
