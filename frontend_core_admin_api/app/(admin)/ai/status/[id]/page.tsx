"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";
import { mockAiRows } from "@/app/(admin)/ai/status/page";

interface Props {
  params: Promise<{ id: string }>;
}

export default function AiStatusDetailPage({ params }: Props) {
  const { id } = use(params);
  const router = useRouter();

  const row = mockAiRows.find((r) => r.id === id) ?? mockAiRows[0];

  const statusList = ["성공", "실패", "보류"] as const;

  return (
    <div>
      <PageHeader
        breadcrumb="AI 요청 상세"
        title="AI 요청 상세"
        actions={
          <button
            onClick={() => router.back()}
            className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors"
          >
            ← 목록으로
          </button>
        }
      />

      {/* 필드 그리드 */}
      <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <div className="grid grid-cols-3 gap-6 pb-5 border-b border-slate-100">
          <div>
            <p className="text-[11px] text-slate-400 mb-1.5">AI 요청 ID · Label</p>
            <p className="text-sm text-slate-700 font-mono">{row.id}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1.5">모델/배포명 · Text/Select</p>
            <p className="text-sm text-slate-700 font-mono">
              {row.model} / {row.deployment}
            </p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1.5">프롬프트 버전 · Select</p>
            <p className="text-sm text-slate-700">
              {row.prompt}{" "}
              <span className="text-slate-400 text-xs">(현재 적용)</span>
            </p>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-6 pt-5">
          <div>
            <p className="text-[11px] text-slate-400 mb-1.5">신뢰도 임계치 · Number</p>
            <div className="inline-flex items-center border border-slate-200 rounded px-3 py-1.5 bg-slate-50">
              <span className="text-sm text-slate-700 font-mono">
                {row.threshold.toFixed(2)}
              </span>
            </div>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1.5">처리 상태 · Badge</p>
            <div className="flex items-center gap-2">
              {statusList.map((s) => (
                <span
                  key={s}
                  className={`text-[11px] font-semibold px-2 py-0.5 rounded transition-opacity ${
                    row.status === s ? "opacity-100" : "opacity-30"
                  } ${
                    s === "성공"
                      ? "bg-emerald-100 text-emerald-700"
                      : s === "실패"
                      ? "bg-red-100 text-red-700"
                      : "bg-yellow-100 text-yellow-700"
                  }`}
                >
                  {s}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* AI 추출 결과 */}
      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">AI 추출 결과</p>
        </div>
        <div className="p-5">
          <table className="w-full">
            <tbody>
              <tr className="border-b border-slate-50">
                <td className="text-xs text-slate-400 py-3 w-36">법인명 추출값</td>
                <td className="text-sm text-slate-800 font-semibold py-3">
                  {row.extracted.companyName}
                </td>
              </tr>
              <tr className="border-b border-slate-50">
                <td className="text-xs text-slate-400 py-3">사업자번호</td>
                <td className="text-sm text-slate-800 font-semibold py-3 font-mono">
                  {row.extracted.bizNo}
                </td>
              </tr>
              <tr className="border-b border-slate-50">
                <td className="text-xs text-slate-400 py-3">신뢰도 점수</td>
                <td className="text-sm text-slate-800 font-bold py-3">
                  {row.extracted.trustScore.toFixed(3)}
                </td>
              </tr>
              <tr>
                <td className="text-xs text-slate-400 py-3">판단 결과</td>
                <td className="py-3">
                  <StatusBadge status={row.judgment} />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}