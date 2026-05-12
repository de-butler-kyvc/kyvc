"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";

type Method = "GET" | "POST" | "PUT" | "DELETE";

interface Endpoint {
  method: Method;
  path: string;
  desc: string;
}

interface Section {
  module: string;
  color: string;
  base: string;
  endpoints: Endpoint[];
}

const sections: Section[] = [
  {
    module: "AI", color: "bg-blue-100 text-blue-700", base: "/api/v1/ai",
    endpoints: [
      { method: "GET",  path: "/requests",          desc: "AI 요청 목록 조회" },
      { method: "GET",  path: "/requests/:id",       desc: "AI 요청 상세 조회" },
      { method: "POST", path: "/analyze",            desc: "문서 AI 분석 요청" },
      { method: "GET",  path: "/stats",              desc: "AI 처리 통계 조회" },
      { method: "GET",  path: "/settings",           desc: "AI 모델 설정 조회" },
      { method: "PUT",  path: "/settings",           desc: "AI 모델 설정 저장" },
      { method: "GET",  path: "/threshold",          desc: "임계치 설정 조회" },
      { method: "PUT",  path: "/threshold",          desc: "임계치 설정 저장" },
    ],
  },
  {
    module: "VC", color: "bg-emerald-100 text-emerald-700", base: "/api/v1/vc",
    endpoints: [
      { method: "GET",  path: "/status",             desc: "VC 발급 코어 상태 조회" },
      { method: "GET",  path: "/requests",           desc: "VC 발급 요청 목록" },
      { method: "GET",  path: "/requests/:id",       desc: "VC 발급 요청 상세" },
      { method: "POST", path: "/issue",              desc: "VC 발급 처리" },
    ],
  },
  {
    module: "VP", color: "bg-purple-100 text-purple-700", base: "/api/v1/vp",
    endpoints: [
      { method: "GET",  path: "/status",             desc: "VP 검증 코어 상태 조회" },
      { method: "POST", path: "/verify",             desc: "VP 검증 요청" },
      { method: "GET",  path: "/results/:id",        desc: "VP 검증 결과 조회" },
    ],
  },
  {
    module: "XRPL", color: "bg-yellow-100 text-yellow-700", base: "/api/v1/xrpl",
    endpoints: [
      { method: "GET",  path: "/status",             desc: "XRPL 네트워크 상태" },
      { method: "GET",  path: "/transactions",       desc: "트랜잭션 목록 조회" },
      { method: "GET",  path: "/transactions/:id",   desc: "트랜잭션 상세 조회" },
      { method: "POST", path: "/reprocess/:txId",    desc: "트랜잭션 재처리 요청" },
    ],
  },
  {
    module: "Schema", color: "bg-slate-100 text-slate-600", base: "/api/v1/schema",
    endpoints: [
      { method: "GET",    path: "/",                 desc: "Schema 목록 조회" },
      { method: "GET",    path: "/:id",              desc: "Schema 상세 조회" },
      { method: "POST",   path: "/",                 desc: "새 Schema 등록" },
      { method: "PUT",    path: "/:id",              desc: "Schema 수정" },
      { method: "DELETE", path: "/:id",              desc: "Schema 폐기" },
    ],
  },
  {
    module: "SDK", color: "bg-cyan-100 text-cyan-700", base: "/api/v1/sdk",
    endpoints: [
      { method: "GET",  path: "/status",             desc: "SDK 상태 조회" },
      { method: "GET",  path: "/metadata",           desc: "검증 메타데이터 조회" },
      { method: "POST", path: "/metadata/refresh",   desc: "메타데이터 갱신" },
      { method: "GET",  path: "/testvectors",        desc: "테스트 벡터 목록" },
      { method: "POST", path: "/testvectors/run",    desc: "전체 테스트 실행" },
      { method: "GET",  path: "/compatibility",      desc: "버전 호환성 조회" },
    ],
  },
  {
    module: "Issuer", color: "bg-orange-100 text-orange-700", base: "/api/v1/issuer",
    endpoints: [
      { method: "GET",  path: "/status",             desc: "Issuer 기술 상태 조회" },
      { method: "GET",  path: "/keys",               desc: "키 참조 상태 목록" },
      { method: "GET",  path: "/keys/:keyId",        desc: "키 상세 조회" },
    ],
  },
];

const METHOD_COLORS: Record<Method, string> = {
  GET:    "bg-emerald-100 text-emerald-700",
  POST:   "bg-blue-100 text-blue-700",
  PUT:    "bg-yellow-100 text-yellow-700",
  DELETE: "bg-red-100 text-red-700",
};

export default function ApiDocsPage() {
  const [openSection, setOpenSection] = useState<string | null>("AI");
  const [search, setSearch] = useState("");

  const query = search.toLowerCase();
  const filtered = sections
    .map((s) => ({
      ...s,
      endpoints: s.endpoints.filter(
        (e) =>
          !query ||
          e.path.toLowerCase().includes(query) ||
          e.desc.toLowerCase().includes(query) ||
          s.module.toLowerCase().includes(query)
      ),
    }))
    .filter((s) => !query || s.endpoints.length > 0);

  return (
    <div>
      <PageHeader breadcrumb="API 문서" title="API 문서" />

      {/* 기본 정보 */}
      <div className="bg-slate-800 rounded-lg p-4 mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400 mb-0.5">Base URL</p>
          <p className="text-sm font-mono text-emerald-400">https://core.kyvc.io</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-400 mb-0.5">인증</p>
          <p className="text-xs font-mono text-slate-300">Authorization: Bearer &lt;token&gt;</p>
        </div>
      </div>

      {/* 검색 */}
      <div className="mb-4">
        <input
          type="text"
          placeholder="엔드포인트 또는 설명 검색..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="border border-slate-300 rounded-md px-3 py-1.5 text-xs text-slate-700 focus:outline-none focus:border-blue-400 w-64"
        />
      </div>

      {/* 섹션 아코디언 */}
      <div className="space-y-2">
        {filtered.map((s) => (
          <div key={s.module} className="bg-white rounded-lg border border-slate-200 overflow-hidden">
            <button
              onClick={() => setOpenSection(openSection === s.module ? null : s.module)}
              className="w-full flex items-center justify-between px-5 py-3 hover:bg-slate-50 transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${s.color}`}>{s.module}</span>
                <span className="text-xs font-mono text-slate-500">{s.base}</span>
                <span className="text-[11px] text-slate-400">{s.endpoints.length}개 엔드포인트</span>
              </div>
              <span className="text-slate-400 text-xs">{openSection === s.module ? "▲" : "▼"}</span>
            </button>

            {openSection === s.module && (
              <div className="border-t border-slate-100">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left text-[11px] text-slate-400 font-medium px-5 py-2 w-20">메서드</th>
                      <th className="text-left text-[11px] text-slate-400 font-medium px-5 py-2">경로</th>
                      <th className="text-left text-[11px] text-slate-400 font-medium px-5 py-2">설명</th>
                    </tr>
                  </thead>
                  <tbody>
                    {s.endpoints.map((e, i) => (
                      <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                        <td className="px-5 py-2.5">
                          <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${METHOD_COLORS[e.method]}`}>
                            {e.method}
                          </span>
                        </td>
                        <td className="px-5 py-2.5 text-xs font-mono text-slate-700">
                          {s.base}{e.path}
                        </td>
                        <td className="px-5 py-2.5 text-xs text-slate-500">{e.desc}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
