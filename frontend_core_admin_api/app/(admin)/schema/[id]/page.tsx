"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

interface SchemaField { name: string; type: string; required: boolean; desc: string }

const schemaDB: Record<string, {
  version: string; date: string; status: string; fields: SchemaField[];
}> = {
  "KYC-VC-Schema-v1.3": {
    version: "1.3.0", date: "2025.04.01", status: "활성",
    fields: [
      { name: "id",              type: "string",  required: true,  desc: "VC 고유 식별자" },
      { name: "type",            type: "string[]", required: true,  desc: "VC 타입 배열" },
      { name: "issuer",          type: "string",  required: true,  desc: "발급자 DID" },
      { name: "issuanceDate",    type: "string",  required: true,  desc: "발급일 (ISO 8601)" },
      { name: "expirationDate",  type: "string",  required: false, desc: "만료일 (ISO 8601)" },
      { name: "companyName",     type: "string",  required: true,  desc: "법인명" },
      { name: "businessNo",      type: "string",  required: true,  desc: "사업자등록번호" },
      { name: "representative",  type: "string",  required: true,  desc: "대표자명" },
      { name: "address",         type: "string",  required: false, desc: "법인 주소" },
      { name: "businessType",    type: "string",  required: false, desc: "업태" },
      { name: "trustScore",      type: "number",  required: true,  desc: "AI 신뢰도 점수" },
      { name: "proof",           type: "object",  required: true,  desc: "전자서명 정보" },
    ],
  },
  "KYC-VC-Schema-v1.2": {
    version: "1.2.0", date: "2024.10.01", status: "비활성",
    fields: [
      { name: "id",             type: "string",  required: true,  desc: "VC 고유 식별자" },
      { name: "type",           type: "string[]", required: true,  desc: "VC 타입 배열" },
      { name: "issuer",         type: "string",  required: true,  desc: "발급자 DID" },
      { name: "issuanceDate",   type: "string",  required: true,  desc: "발급일 (ISO 8601)" },
      { name: "companyName",    type: "string",  required: true,  desc: "법인명" },
      { name: "businessNo",     type: "string",  required: true,  desc: "사업자등록번호" },
      { name: "representative", type: "string",  required: true,  desc: "대표자명" },
      { name: "trustScore",     type: "number",  required: true,  desc: "AI 신뢰도 점수" },
      { name: "proof",          type: "object",  required: true,  desc: "전자서명 정보" },
      { name: "metadata",       type: "object",  required: false, desc: "추가 메타데이터" },
    ],
  },
  "DELEGATION-Schema-v1.0": {
    version: "1.0.0", date: "2024.07.01", status: "활성",
    fields: [
      { name: "id",           type: "string",  required: true,  desc: "VC 고유 식별자" },
      { name: "type",         type: "string[]", required: true,  desc: "VC 타입 배열" },
      { name: "issuer",       type: "string",  required: true,  desc: "발급자 DID" },
      { name: "delegator",    type: "string",  required: true,  desc: "위임자 DID" },
      { name: "delegatee",    type: "string",  required: true,  desc: "수임자 DID" },
      { name: "scope",        type: "string[]", required: true,  desc: "위임 권한 범위" },
      { name: "validFrom",    type: "string",  required: true,  desc: "위임 시작일" },
      { name: "validUntil",   type: "string",  required: false, desc: "위임 종료일" },
      { name: "proof",        type: "object",  required: true,  desc: "전자서명 정보" },
    ],
  },
};

interface Props { params: Promise<{ id: string }> }

export default function SchemaDetailPage({ params }: Props) {
  const { id } = use(params);
  const router = useRouter();

  const decodedId = decodeURIComponent(id);
  const data = schemaDB[decodedId] ?? schemaDB["KYC-VC-Schema-v1.3"];

  const [status,  setStatus]  = useState(data.status);
  const [saving,  setSaving]  = useState(false);
  const [toast,   setToast]   = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const handleStatusChange = async (next: string) => {
    if (!window.confirm(`Schema를 ${next} 상태로 변경하시겠습니까?`)) return;
    setSaving(true);
    try {
      // TODO: await fetch(`/api/schema/${id}/status`, { method: 'PUT', body: JSON.stringify({ status: next }) })
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
        breadcrumb={`Schema 관리 > ${decodedId}`}
        title="Schema 상세"
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
            <p className="text-[11px] text-slate-400 mb-1">Schema ID</p>
            <p className="text-sm font-mono text-slate-700">{decodedId}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">버전</p>
            <p className="text-sm font-mono text-slate-700">{data.version}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">등록일</p>
            <p className="text-sm text-slate-700">{data.date}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">필드 수</p>
            <p className="text-sm text-slate-700">{data.fields.length}개</p>
          </div>
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-slate-100">
          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-500">현재 상태</span>
            <StatusBadge status={status} />
          </div>
          <div className="flex gap-2">
            {status !== "활성" && (
              <button onClick={() => handleStatusChange("활성")} disabled={saving}
                className="bg-emerald-600 text-white text-xs px-3 py-1.5 rounded-md hover:bg-emerald-700 transition-colors disabled:opacity-50">
                활성화
              </button>
            )}
            {status !== "비활성" && status !== "폐기" && (
              <button onClick={() => handleStatusChange("비활성")} disabled={saving}
                className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors disabled:opacity-50">
                비활성화
              </button>
            )}
            {status !== "폐기" && (
              <button onClick={() => handleStatusChange("폐기")} disabled={saving}
                className="border border-red-300 text-red-500 text-xs px-3 py-1.5 rounded-md hover:bg-red-50 transition-colors disabled:opacity-50">
                폐기
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 필드 목록 */}
      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">필드 정의</p>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["필드명", "타입", "필수", "설명"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.fields.map((f, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3 text-xs font-mono text-slate-700">{f.name}</td>
                <td className="px-5 py-3 text-xs font-mono text-blue-600">{f.type}</td>
                <td className="px-5 py-3">
                  {f.required
                    ? <span className="text-[11px] font-semibold bg-red-100 text-red-600 px-1.5 py-0.5 rounded">필수</span>
                    : <span className="text-[11px] text-slate-400">선택</span>}
                </td>
                <td className="px-5 py-3 text-xs text-slate-500">{f.desc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
