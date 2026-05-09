"use client";

import { useRouter } from "next/navigation";
import { type ChangeEvent, useEffect, useState } from "react";

import { type CorporateSummary } from "@/components/corporate/info-card";
import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";
import { ApiError, type AgentResponse, corporate as corpApi } from "@/lib/api";

type FileMeta = {
  name: string;
  size: string;
  file?: File;
  uploaded?: boolean;
};

type AgentDraft = {
  name: string;
  relation: string;
  phone: string;
  email: string;
  poaDoc: FileMeta | null;
};

const DEFAULTS: AgentDraft = {
  name: "",
  relation: "",
  phone: "",
  email: "",
  poaDoc: null
};

const formatSize = (size: number) => {
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))}KB`;
  return `${(size / 1024 / 1024).toFixed(1)}MB`;
};

export default function CorporateAgentsPage() {
  const router = useRouter();
  const [corp, setCorp] = useState<CorporateSummary | null>(null);
  const [useAgent, setUseAgent] = useState(true);
  const [form, setForm] = useState<AgentDraft>(DEFAULTS);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    corpApi
      .me()
      .then(async (res) => {
        setCorp({
          corporateId: res.corporateId,
          corporateName: res.corporateName ?? "",
          businessNo: res.businessRegistrationNo ?? "",
          corporateNo: res.corporateRegistrationNo ?? "",
          representativeName: res.representativeName ?? "",
          corporateType: res.corporateTypeCode ?? ""
        });

        try {
          const agents = await corpApi.agents(res.corporateId);
          const saved = agents[0];
          if (saved) {
            setForm(fromAgentResponse(saved));
            setUseAgent(true);
          }
        } catch {
          setForm(DEFAULTS);
        }
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const update = <K extends keyof AgentDraft>(field: K, value: AgentDraft[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
    setMessage(null);
  };

  const onFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    update("poaDoc", { name: file.name, size: formatSize(file.size), file });
    event.target.value = "";
  };

  const persist = async () => {
    setError(null);
    setMessage(null);

    if (!useAgent) {
      setMessage("대리 신청을 사용하지 않는 것으로 설정했습니다.");
      return true;
    }

    if (!corp?.corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return false;
    }

    if (!form.name || !form.relation || !form.phone || !form.poaDoc) {
      setError("대리인 성명, 관계/직책, 연락처, 위임장은 필수입니다.");
      return false;
    }

    if (form.phone && !/^[0-9-]+$/.test(form.phone)) {
      setError("연락처는 숫자와 - 만 입력해 주세요.");
      return false;
    }

    if (form.email && !/\S+@\S+\.\S+/.test(form.email)) {
      setError("이메일 형식이 올바르지 않습니다.");
      return false;
    }

    setIsSaving(true);
    try {
      await corpApi.updateAgent(corp.corporateId, {
        name: form.name,
        phone: form.phone,
        email: form.email,
        relationshipOrPosition: form.relation,
        powerOfAttorneyFile: form.poaDoc.file
      });
      setMessage("대리인 정보가 저장되었습니다.");
      return true;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장에 실패했습니다.");
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  const onContinue = async () => {
    const ok = await persist();
    if (ok) router.push("/corporate/kyc/apply");
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">대리인 정보 등록</h1>
          <p className="page-head-desc">
            대표자가 아닌 사용자가 KYC를 신청하는 경우 위임장과 함께 대리인 정보를 등록해야 합니다.
          </p>
        </div>
      </div>

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">대리 신청 사용</div>
        </div>
        <label className="flex cursor-pointer items-start gap-3">
          <input
            type="checkbox"
            checked={useAgent}
            onChange={(event) => {
              setUseAgent(event.target.checked);
              setMessage(null);
            }}
            className="mt-1 size-[18px] accent-[var(--accent)]"
          />
          <span>
            <span className="block text-[14px] font-semibold">대리 신청 사용</span>
            <span className="mt-1 block text-[13px] text-muted-foreground">
              체크 시 대리인 정보를 등록합니다.
            </span>
          </span>
        </label>
      </section>

      {useAgent ? (
        <section className="form-card">
          <div className="form-card-header">
            <div className="form-card-title">대리인 식별정보</div>
            <div className="form-card-meta">필수</div>
          </div>

          <div className="form-grid">
            <TextField
              label="대리인 성명"
              required
              placeholder="대리인 성명 입력"
              value={form.name}
              onChange={(event) => update("name", event.target.value)}
            />
            <TextField
              label="관계 / 직책"
              required
              placeholder="예) 재무팀장"
              value={form.relation}
              onChange={(event) => update("relation", event.target.value)}
            />
            <TextField
              label="연락처"
              required
              placeholder="010-0000-0000"
              value={form.phone}
              onChange={(event) => update("phone", event.target.value)}
            />
            <TextField
              label="이메일"
              type="email"
              placeholder="이메일 입력"
              value={form.email}
              onChange={(event) => update("email", event.target.value)}
            />

            <div className="field col-span-2">
              <span className="field-label">
                위임장 <span style={{ color: "var(--danger)" }}>*</span>
              </span>
              <span className="field-help">
                법인 인감 날인된 위임장 (PDF · 최대 10MB)
              </span>
              {form.poaDoc ? (
                <div className="file-pill">
                  <Icon.File />
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-[13px] font-semibold">{form.poaDoc.name}</div>
                    <div className="text-[12px] text-muted-foreground">
                      {form.poaDoc.size} · {form.poaDoc.uploaded ? "등록됨" : "검증 완료"}
                    </div>
                  </div>
                  <Badge variant="success">
                    <Icon.Check size={11} /> 완료
                  </Badge>
                  <label className="btn btn-ghost btn-sm">
                    교체
                    <input
                      type="file"
                      className="hidden"
                      accept=".pdf"
                      onChange={onFileChange}
                    />
                  </label>
                </div>
              ) : (
                <label className="upload-tile min-h-[110px]">
                  <Icon.Upload className="upload-tile-icon" />
                  <span className="upload-tile-text">위임장 업로드</span>
                  <span className="upload-tile-meta">PDF · 최대 10MB</span>
                  <input
                    type="file"
                    className="hidden"
                    accept=".pdf"
                    onChange={onFileChange}
                  />
                </label>
              )}
            </div>
          </div>
        </section>
      ) : null}

      {error || message ? (
        <p className="mt-4 text-[12px]">
          {message ? (
            <span className="text-success">{message}</span>
          ) : (
            <span className="text-destructive">{error}</span>
          )}
        </p>
      ) : null}

      <div className="form-actions right">
        <Button type="button" disabled={isSaving} onClick={onContinue}>
          저장
        </Button>
      </div>
    </div>
  );
}

function fromAgentResponse(agent: AgentResponse): AgentDraft {
  return {
    name: agent.name ?? "",
    relation: agent.relationshipOrPosition ?? agent.authorityScope ?? "",
    phone: agent.phoneNumber ?? "",
    email: agent.email ?? "",
    poaDoc:
      agent.delegationDocumentId != null
        ? { name: "등록된 위임장", size: "서버 보관", uploaded: true }
        : null
  };
}
