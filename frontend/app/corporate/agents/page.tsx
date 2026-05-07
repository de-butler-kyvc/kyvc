"use client";

import { useEffect, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  type Agent,
  type AgentListItem,
  ApiError,
  corporate as corpApi
} from "@/lib/api";

const EMPTY: Agent = { name: "", phone: "", email: "", authorityScope: "" };

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
  const [agent, setAgent] = useState<Agent>(EMPTY);
  const [scopes, setScopes] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

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

  const toggleScope = (v: string) =>
    setScopes((prev) =>
      prev.includes(v) ? prev.filter((s) => s !== v) : [...prev, v]
    );

  const onSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const payload: Agent = { ...agent, authorityScope: scopes.join(",") };
      await corpApi.updateAgent(corporateId, payload);
      const list = await corpApi.agents(corporateId);
      setAgents(list.items ?? []);
      setAgent(EMPTY);
      setScopes([]);
      setMessage("대리인이 등록/수정되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-4 px-9 py-7">
      <div className="flex flex-col gap-1">
        <div className="font-mono text-[10px] uppercase tracking-[0.6px] text-subtle-foreground">
          UWEB-005/025 · M-02
        </div>
        <h1 className="text-[20px] font-bold tracking-[-0.4px] text-foreground">
          대리인 정보
        </h1>
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
                  const status = STATUS_LABEL[a.status] ?? {
                    label: a.status,
                    variant: "secondary" as const
                  };
                  return (
                    <tr key={a.agentId} className="border-b border-row-border last:border-0">
                      <td className="px-4 py-4 font-medium text-foreground">{a.name}</td>
                      <td className="px-4 py-4 text-muted-foreground">
                        {a.authorityScope
                          ? a.authorityScope
                              .split(",")
                              .map(
                                (v) =>
                                  SCOPES.find((s) => s.value === v.trim())?.label ?? v
                              )
                              .join(", ")
                          : "-"}
                      </td>
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
        <CardContent className="p-0">
          <form onSubmit={onSave}>
            <FormGroup title="대리인 등록 / 수정">
              <FormRow label="이름" required>
                <Input
                  value={agent.name}
                  onChange={(e) => setAgent({ ...agent, name: e.target.value })}
                />
              </FormRow>
              <FormRow label="휴대폰" required>
                <Input
                  value={agent.phone}
                  onChange={(e) => setAgent({ ...agent, phone: e.target.value })}
                  placeholder="010-0000-0000"
                />
              </FormRow>
              <FormRow label="이메일" required>
                <Input
                  type="email"
                  value={agent.email}
                  onChange={(e) => setAgent({ ...agent, email: e.target.value })}
                />
              </FormRow>
              <FormRow label="위임 권한 범위" required>
                <div className="flex flex-wrap gap-2">
                  {SCOPES.map((s) => {
                    const on = scopes.includes(s.value);
                    return (
                      <button
                        type="button"
                        key={s.value}
                        onClick={() => toggleScope(s.value)}
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
                    대리인 등록 시 본인 인증 절차가 진행됩니다.
                  </span>
                )}
              </p>
              <Button type="submit" disabled={busy} className="rounded-[10px]">
                {busy ? "저장 중..." : "저장"}
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
    <div className="grid grid-cols-1 items-start gap-2 border-b border-row-border py-3 last:border-0 md:grid-cols-[180px_1fr]">
      <Label className="text-[13px] text-muted-foreground md:pt-2">
        {label}
        {required ? <span className="ml-1 text-destructive">*</span> : null}
      </Label>
      <div>{children}</div>
    </div>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="px-4 py-2.5 text-[11px] font-bold text-subtle-foreground">
      {children}
    </th>
  );
}
