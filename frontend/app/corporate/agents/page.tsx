"use client";

import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { TextField } from "@/components/ui/text-field";
import {
  type AgentListItem,
  ApiError,
  corporate as corpApi
} from "@/lib/api";

type AgentForm = {
  name: string;
  phone: string;
  email: string;
  scopes: string[];
};

const DEFAULTS: AgentForm = { name: "", phone: "", email: "", scopes: [] };

const SCOPES = [
  { value: "KYC_SUBMIT", label: "KYC 신청·제출" },
  { value: "DOC_UPLOAD", label: "서류 업로드" },
  { value: "VC_RECEIVE", label: "VC 수령" },
  { value: "VC_REVOKE", label: "VC 폐기 요청" }
];

const STATUS_LABEL: Record<
  string,
  { label: string; variant: "success" | "warning" | "secondary" }
> = {
  ACTIVE: { label: "활성", variant: "success" },
  PENDING: { label: "승인 대기", variant: "warning" },
  REVOKED: { label: "해제", variant: "secondary" }
};

export default function CorporateAgentsPage() {
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [agents, setAgents] = useState<AgentListItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting }
  } = useForm<AgentForm>({ defaultValues: DEFAULTS });

  useEffect(() => {
    corpApi
      .me()
      .then(async (res) => {
        const c = res.corporate as { corporateId?: number } | undefined;
        if (!c?.corporateId) return;
        setCorporateId(c.corporateId);
        const list = await corpApi.agents(c.corporateId).catch(() => ({ items: [] }));
        setAgents(list.items ?? []);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const onSave = handleSubmit(async (data) => {
    if (!corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    if (data.scopes.length === 0) {
      setError("위임 권한 범위를 1개 이상 선택해 주세요.");
      return;
    }
    setError(null);
    setMessage(null);
    try {
      await corpApi.updateAgent(corporateId, {
        name: data.name,
        phone: data.phone,
        email: data.email,
        authorityScope: data.scopes.join(",")
      });
      const list = await corpApi.agents(corporateId);
      setAgents(list.items ?? []);
      reset(DEFAULTS);
      setMessage("대리인이 등록/수정되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    }
  });

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-5 px-9 py-8">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">대리인 정보</h1>
        <p className="text-[13px] text-muted-foreground">
          KYC 신청·서류 제출을 위임할 대리인을 관리합니다. 위임 권한 범위를 최소한으로 부여하는 것을 권장합니다.
        </p>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="flex items-center justify-between border-b border-border px-5 py-3.5">
            <h2 className="text-[14px] font-bold text-foreground">등록된 대리인</h2>
            <span className="text-[12px] text-muted-foreground">{agents.length}명</span>
          </div>
          {agents.length === 0 ? (
            <p className="px-5 py-10 text-center text-[13px] text-muted-foreground">
              등록된 대리인이 없습니다.
            </p>
          ) : (
            <table className="w-full text-[13px]">
              <thead className="bg-secondary text-left">
                <tr className="border-b border-border">
                  <Th>이름</Th>
                  <Th>위임 권한</Th>
                  <Th>상태</Th>
                </tr>
              </thead>
              <tbody>
                {agents.map((a) => {
                  const status =
                    STATUS_LABEL[a.status] ?? { label: a.status, variant: "secondary" as const };
                  const scopeLabels = a.authorityScope
                    ? a.authorityScope
                        .split(",")
                        .map((v) => SCOPES.find((s) => s.value === v.trim())?.label ?? v)
                        .join(", ")
                    : "-";
                  return (
                    <tr key={a.agentId} className="border-b border-row-border last:border-0">
                      <td className="px-4 py-4 font-medium text-foreground">{a.name}</td>
                      <td className="px-4 py-4 text-muted-foreground">{scopeLabels}</td>
                      <td className="px-4 py-3">
                        <Badge variant={status.variant}>{status.label}</Badge>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="px-7 py-7">
          <form onSubmit={onSave} className="flex flex-col gap-5" noValidate>
            <h2 className="text-[15px] font-bold text-foreground">대리인 등록 / 수정</h2>
            <div className="grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
              <TextField
                label="이름"
                required
                placeholder="이름 입력"
                error={errors.name?.message}
                {...register("name", { required: "이름은 필수입니다" })}
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

            <div className="flex flex-col gap-2">
              <Label className="text-[13px] text-foreground">
                위임 권한 범위<span className="ml-0.5 text-destructive">*</span>
              </Label>
              <Controller
                control={control}
                name="scopes"
                render={({ field }) => (
                  <div className="flex flex-wrap gap-2">
                    {SCOPES.map((s) => {
                      const on = field.value.includes(s.value);
                      return (
                        <button
                          type="button"
                          key={s.value}
                          onClick={() =>
                            field.onChange(
                              on
                                ? field.value.filter((v: string) => v !== s.value)
                                : [...field.value, s.value]
                            )
                          }
                          className={`rounded-full border px-3 py-1.5 text-[12px] font-medium transition-colors ${
                            on
                              ? "border-accent-border bg-accent text-accent-foreground"
                              : "border-border bg-card text-muted-foreground hover:bg-secondary"
                          }`}
                        >
                          {s.label}
                        </button>
                      );
                    })}
                  </div>
                )}
              />
            </div>

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

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="px-4 py-2.5 text-[11px] font-bold text-subtle-foreground">{children}</th>
  );
}
