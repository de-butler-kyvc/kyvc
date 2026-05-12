"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

const promptDB: Record<string, Record<string, {
  model: string; desc: string; date: string; status: string; content: string;
}>> = {
  "PROMPT-KYC-001": {
    "v2.2": {
      model: "gpt-4o", desc: "법인 KYC 문서 추출 v2", date: "2025.05.01", status: "활성",
      content: `당신은 법인 KYC 문서에서 핵심 정보를 추출하는 전문가입니다.

다음 문서에서 아래 항목을 추출하세요:
- 법인명 (상호)
- 사업자등록번호
- 대표자명
- 주소
- 업태 및 종목

응답 형식:
{
  "company_name": "...",
  "business_no": "...",
  "representative": "...",
  "address": "...",
  "business_type": "..."
}

신뢰도 점수를 0.0~1.0 사이 값으로 함께 반환하세요.`,
    },
    "v2.1": {
      model: "gpt-4o", desc: "법인 KYC 문서 추출 v1", date: "2025.03.15", status: "비활성",
      content: `법인 KYC 문서에서 법인명, 사업자번호, 대표자를 추출하세요.

JSON 형식으로 반환하세요:
{ "company_name": "...", "business_no": "...", "representative": "..." }`,
    },
  },
};

interface Props { params: Promise<{ id: string; version: string }> }

export default function PromptDetailPage({ params }: Props) {
  const { id, version } = use(params);
  const router = useRouter();

  const data = promptDB[id]?.[version] ?? promptDB["PROMPT-KYC-001"]["v2.2"];
  const [status,  setStatus]  = useState(data.status);
  const [saving,  setSaving]  = useState(false);
  const [toast,   setToast]   = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const toggleStatus = async () => {
    const next = status === "활성" ? "비활성" : "활성";
    if (!window.confirm(`프롬프트를 ${next} 상태로 변경하시겠습니까?`)) return;
    setSaving(true);
    try {
      // TODO: await fetch(`/api/ai/prompts/${id}/${version}/status`, { method: 'PUT', body: JSON.stringify({ status: next }) })
      await new Promise((r) => setTimeout(r, 500));
      setStatus(next);
      showToast(`${next} 상태로 변경되었습니다.`);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="relative">
      {toast && (
        <div className={`fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white transition-all ${toast.type === "success" ? "bg-emerald-600" : "bg-red-600"}`}>
          {toast.msg}
        </div>
      )}

      <PageHeader
        breadcrumb={`AI 설정 > 프롬프트 목록 > ${id}`}
        title="프롬프트 상세"
        actions={
          <button onClick={() => router.back()}
            className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors">
            ← 목록으로
          </button>
        }
      />

      {/* 메타 정보 */}
      <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <div className="grid grid-cols-4 gap-6 mb-5">
          <div>
            <p className="text-[11px] text-slate-400 mb-1">프롬프트 ID</p>
            <p className="text-sm font-mono text-slate-700">{id}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">모델</p>
            <p className="text-sm font-mono text-slate-700">{data.model}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">버전</p>
            <p className="text-sm font-mono text-slate-700">{version}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">등록일</p>
            <p className="text-sm text-slate-700">{data.date}</p>
          </div>
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-slate-100">
          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-500">현재 상태</span>
            <StatusBadge status={status} />
          </div>
          <button
            onClick={toggleStatus}
            disabled={saving}
            className={`text-xs px-3 py-1.5 rounded-md transition-colors disabled:opacity-50 ${
              status === "활성"
                ? "border border-slate-300 text-slate-600 hover:bg-slate-50"
                : "bg-emerald-600 text-white hover:bg-emerald-700"
            }`}
          >
            {saving ? "변경 중..." : status === "활성" ? "비활성화" : "활성화"}
          </button>
        </div>
      </div>

      {/* 프롬프트 본문 */}
      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-3 border-b border-slate-100 flex items-center justify-between">
          <p className="text-xs font-semibold text-slate-600">프롬프트 내용</p>
          <span className="text-[11px] text-slate-400">{data.desc}</span>
        </div>
        <pre className="px-5 py-4 text-xs font-mono text-slate-700 whitespace-pre-wrap leading-relaxed bg-slate-50 rounded-b-lg">
          {data.content}
        </pre>
      </div>
    </div>
  );
}
