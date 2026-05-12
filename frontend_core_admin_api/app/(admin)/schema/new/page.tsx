"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/ui/PageHeader";
import type { SchemaItem } from "@/app/(admin)/schema/page";

const SCHEMA_TYPES = ["KYC-VC-Schema", "DELEGATION-Schema", "CONSENT-Schema", "IDENTITY-Schema"];

export default function SchemaNewPage() {
  const router = useRouter();

  const [schemaType, setSchemaType] = useState(SCHEMA_TYPES[0]);
  const [version,    setVersion]    = useState("");
  const [fields,     setFields]     = useState("");
  const [status,     setStatus]     = useState("활성");
  const [saving,     setSaving]     = useState(false);
  const [errors,     setErrors]     = useState<Record<string, string>>({});

  const validate = () => {
    const e: Record<string, string> = {};
    if (!version.match(/^\d+\.\d+\.\d+$/)) e.version = "버전 형식이 올바르지 않습니다. (예: 1.0.0)";
    if (!fields || isNaN(Number(fields)) || Number(fields) < 1) e.fields = "1 이상의 숫자를 입력해주세요.";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    setSaving(true);
    try {
      // TODO: await fetch('/api/schema', { method: 'POST', body: JSON.stringify({ ... }) })
      await new Promise((r) => setTimeout(r, 500));

      const newSchema: SchemaItem = {
        id:      `${schemaType}-v${version.split(".")[0]}.${version.split(".")[1]}`,
        version,
        fields:  Number(fields),
        date:    new Date().toLocaleDateString("ko-KR").replace(/\. /g, ".").replace(".", ".").slice(0, 10),
        status,
      };

      try {
        const stored = localStorage.getItem("kyvc_schemas");
        const existing: SchemaItem[] = stored ? JSON.parse(stored) : [];
        localStorage.setItem("kyvc_schemas", JSON.stringify([newSchema, ...existing]));
      } catch {}

      router.push("/schema");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        breadcrumb="Schema 관리 > 새 Schema 등록"
        title="새 Schema 등록"
        actions={
          <button
            onClick={() => router.back()}
            className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors"
          >
            ← 목록으로
          </button>
        }
      />

      <div className="bg-white rounded-lg border border-slate-200 p-6 max-w-2xl">
        <div className="space-y-5">

          {/* Schema 유형 */}
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">Schema 유형</label>
            <select
              value={schemaType}
              onChange={(e) => setSchemaType(e.target.value)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400"
            >
              {SCHEMA_TYPES.map((t) => <option key={t}>{t}</option>)}
            </select>
          </div>

          {/* 생성될 Schema ID 미리보기 */}
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">Schema ID (자동 생성)</label>
            <div className="text-sm font-mono text-slate-500 bg-slate-50 border border-slate-200 rounded-md px-3 py-2">
              {version.match(/^\d+\.\d+/)
                ? `${schemaType}-v${version.split(".")[0]}.${version.split(".")[1]}`
                : <span className="text-slate-300">버전 입력 시 자동 완성</span>}
            </div>
          </div>

          {/* 버전 */}
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">버전 <span className="text-red-400">*</span></label>
            <input
              type="text"
              placeholder="예: 1.4.0"
              value={version}
              onChange={(e) => { setVersion(e.target.value); setErrors((p) => ({ ...p, version: "" })); }}
              className={`w-full border rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400 ${errors.version ? "border-red-400" : "border-slate-300"}`}
            />
            {errors.version && <p className="text-[11px] text-red-500 mt-1">{errors.version}</p>}
          </div>

          {/* 필드 수 */}
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">필드 수 <span className="text-red-400">*</span></label>
            <input
              type="number"
              min={1}
              placeholder="예: 12"
              value={fields}
              onChange={(e) => { setFields(e.target.value); setErrors((p) => ({ ...p, fields: "" })); }}
              className={`w-full border rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400 ${errors.fields ? "border-red-400" : "border-slate-300"}`}
            />
            {errors.fields && <p className="text-[11px] text-red-500 mt-1">{errors.fields}</p>}
          </div>

          {/* 상태 */}
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">상태</label>
            <div className="flex gap-3">
              {["활성", "비활성"].map((s) => (
                <label key={s} className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="radio"
                    name="status"
                    value={s}
                    checked={status === s}
                    onChange={() => setStatus(s)}
                    className="accent-blue-600"
                  />
                  <span className="text-sm text-slate-700">{s}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 mt-8 pt-5 border-t border-slate-100">
          <button
            onClick={handleSave}
            disabled={saving}
            className="bg-blue-600 text-white text-sm px-5 py-2 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            {saving ? "등록 중..." : "Schema 등록"}
          </button>
          <button
            onClick={() => router.back()}
            disabled={saving}
            className="border border-slate-300 text-slate-600 text-sm px-4 py-2 rounded-md hover:bg-slate-50 transition-colors"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  );
}
