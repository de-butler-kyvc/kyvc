"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

export interface SchemaItem {
  id: string;
  version: string;
  fields: number;
  date: string;
  status: string;
}

const defaultSchemas: SchemaItem[] = [
  { id: "KYC-VC-Schema-v1.3",     version: "1.3.0", fields: 12, date: "2025.04.01", status: "활성" },
  { id: "KYC-VC-Schema-v1.2",     version: "1.2.0", fields: 10, date: "2024.10.01", status: "비활성" },
  { id: "DELEGATION-Schema-v1.0", version: "1.0.0", fields: 8,  date: "2024.07.01", status: "활성" },
];

export default function SchemaPage() {
  const router = useRouter();
  const [schemas,      setSchemas]      = useState<SchemaItem[]>(defaultSchemas);
  const [filterStatus, setFilterStatus] = useState("전체 상태");

  useEffect(() => {
    try {
      const stored = localStorage.getItem("kyvc_schemas");
      if (stored) {
        const extra: SchemaItem[] = JSON.parse(stored);
        setSchemas([...extra, ...defaultSchemas]);
      }
    } catch {}
  }, []);

  const filtered = schemas.filter(
    (s) => filterStatus === "전체 상태" || s.status === filterStatus
  );

  return (
    <div>
      <PageHeader
        breadcrumb="Schema 관리"
        title="Credential Schema 목록"
        actions={
          <button
            onClick={() => router.push("/schema/new")}
            className="bg-blue-600 text-white text-xs px-3 py-1.5 rounded-md hover:bg-blue-700 transition-colors"
          >
            + 새 Schema 등록
          </button>
        }
      />

      <div className="flex items-center gap-2 mb-3">
        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none"
        >
          <option>전체 상태</option>
          <option>활성</option>
          <option>비활성</option>
          <option>폐기</option>
        </select>
        {filterStatus !== "전체 상태" && (
          <span className="text-[11px] text-slate-400">{filtered.length}건</span>
        )}
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["Schema ID", "버전", "필드 수", "등록일", "상태", "상세"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-5 py-8 text-center text-xs text-slate-400">
                  해당 상태의 Schema가 없습니다.
                </td>
              </tr>
            ) : (
              filtered.map((s) => (
                <tr key={s.id + s.version} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                  <td className="px-5 py-3 text-xs text-blue-600 font-mono">{s.id}</td>
                  <td className="px-5 py-3 text-xs text-slate-600">{s.version}</td>
                  <td className="px-5 py-3 text-xs text-slate-700">{s.fields}개</td>
                  <td className="px-5 py-3 text-xs text-slate-500">{s.date}</td>
                  <td className="px-5 py-3"><StatusBadge status={s.status} /></td>
                  <td className="px-5 py-3">
                    <button
                      onClick={() => router.push(`/schema/${s.id}`)}
                      className="text-xs text-blue-500 hover:underline"
                    >
                      상세 →
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
