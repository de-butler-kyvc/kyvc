"use client";

import { useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  type Agent,
  type AgentListItem,
  ApiError,
  type Representative,
  corporate as corpApi
} from "@/lib/api";

const EMPTY_REP: Representative = { name: "", birthDate: "", phone: "", email: "" };
const EMPTY_AGENT: Agent = { name: "", phone: "", email: "", authorityScope: "" };

export default function CorporateAgentsPage() {
  const [corporateId, setCorporateId] = useState<number | null>(null);
  const [rep, setRep] = useState<Representative>(EMPTY_REP);
  const [agent, setAgent] = useState<Agent>(EMPTY_AGENT);
  const [agents, setAgents] = useState<AgentListItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    corpApi
      .me()
      .then(async (res) => {
        const c = res.corporate as { corporateId?: number } | undefined;
        if (c?.corporateId) {
          setCorporateId(c.corporateId);
          const list = await corpApi.agents(c.corporateId).catch(() => ({ items: [] }));
          setAgents(list.items ?? []);
        }
        if (res.representative) setRep({ ...EMPTY_REP, ...res.representative });
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const saveRep = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!corporateId) {
      setError("법인 정보를 먼저 등록하세요.");
      return;
    }
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await corpApi.updateRepresentative(corporateId, rep);
      setMessage("대표자 정보가 저장되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const saveAgent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!corporateId) {
      setError("법인 정보를 먼저 등록하세요.");
      return;
    }
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      await corpApi.updateAgent(corporateId, agent);
      const list = await corpApi.agents(corporateId);
      setAgents(list.items ?? []);
      setAgent(EMPTY_AGENT);
      setMessage("대리인이 등록/수정되었습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <PageShell
      title="대표자 · 대리인"
      description="법인 대표자 및 위임 대리인을 관리합니다."
      module="UWEB-004/005/025 · M-02"
    >
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      {message ? <p className="text-sm text-muted-foreground">{message}</p> : null}

      <Card>
        <CardHeader>
          <CardTitle>대표자 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={saveRep} className="grid gap-4 md:grid-cols-2">
            <Field id="r-name" label="이름" value={rep.name} onChange={(v) => setRep({ ...rep, name: v })} />
            <Field id="r-birth" label="생년월일" value={rep.birthDate} onChange={(v) => setRep({ ...rep, birthDate: v })} placeholder="YYYY-MM-DD" />
            <Field id="r-phone" label="연락처" value={rep.phone} onChange={(v) => setRep({ ...rep, phone: v })} />
            <Field id="r-email" label="이메일" value={rep.email} onChange={(v) => setRep({ ...rep, email: v })} />
            <div className="md:col-span-2 flex justify-end">
              <Button type="submit" disabled={busy}>저장</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>등록된 대리인</CardTitle>
        </CardHeader>
        <CardContent>
          {agents.length === 0 ? (
            <p className="text-sm text-muted-foreground">등록된 대리인이 없습니다.</p>
          ) : (
            <ul className="grid gap-2">
              {agents.map((a) => (
                <li
                  key={a.agentId}
                  className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
                >
                  <div className="flex flex-col">
                    <span className="font-medium">{a.name}</span>
                    <span className="text-xs text-muted-foreground">{a.authorityScope}</span>
                  </div>
                  <Badge variant="outline">{a.status}</Badge>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>대리인 등록 / 수정</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={saveAgent} className="grid gap-4 md:grid-cols-2">
            <Field id="a-name" label="이름" value={agent.name} onChange={(v) => setAgent({ ...agent, name: v })} />
            <Field id="a-phone" label="연락처" value={agent.phone} onChange={(v) => setAgent({ ...agent, phone: v })} />
            <Field id="a-email" label="이메일" value={agent.email} onChange={(v) => setAgent({ ...agent, email: v })} />
            <Field id="a-scope" label="위임 권한 범위" value={agent.authorityScope} onChange={(v) => setAgent({ ...agent, authorityScope: v })} placeholder="예) KYC_SUBMIT" />
            <div className="md:col-span-2 flex justify-end">
              <Button type="submit" disabled={busy}>저장</Button>
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
