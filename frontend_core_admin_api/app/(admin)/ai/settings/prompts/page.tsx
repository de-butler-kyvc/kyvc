"use client";

import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

const prompts = [
  { id: "PROMPT-KYC-001", model: "gpt-4o", version: "v2.2", desc: "법인 KYC 문서 추출 v2", date: "2025.05.01", status: "활성" },
  { id: "PROMPT-KYC-001", model: "gpt-4o", version: "v2.1", desc: "법인 KYC 문서 추출 v1", date: "2025.03.15", status: "비활성" },
];

export default function PromptsPage() {
  const router = useRouter();

  return (
    <div>
      <PageHeader
        breadcrumb="AI 설정 > 프롬프트 목록"
        title="AI 프롬프트 목록"
        actions={
          <button
            onClick={() => router.push("/ai/settings/prompts/new")}
            className="bg-blue-600 text-white text-xs px-3 py-1.5 rounded-md hover:bg-blue-700 transition-colors"
          >
            + 새 프롬프트
          </button>
        }
      />

      <div className="flex items-center gap-2 mb-3">
        <select className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none">
          <option>전체 버전</option>
          <option>v2.2</option><option>v2.1</option>
        </select>
        <select className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none">
          <option>전체 상태</option>
          <option>활성</option><option>비활성</option>
        </select>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["프롬프트 ID", "모델/배포명", "버전", "설명", "등록일", "상태", "상세"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {prompts.map((p, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs text-slate-600 font-mono">{p.id}</td>
                <td className="px-5 py-3 text-xs text-slate-700">{p.model}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{p.version}</td>
                <td className="px-5 py-3 text-xs text-slate-700">{p.desc}</td>
                <td className="px-5 py-3 text-xs text-slate-500">{p.date}</td>
                <td className="px-5 py-3"><StatusBadge status={p.status} /></td>
                <td className="px-5 py-3">
                  <button
                    onClick={() => router.push(`/ai/settings/prompts/${p.id}/${p.version}`)}
                    className="text-xs text-blue-500 hover:underline"
                  >
                    상세 →
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
