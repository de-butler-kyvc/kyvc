"use client";

import { useState } from "react";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

const PAGE_SIZE = 8;

const allLogs = [
  { id: "LOG-C-00482", date: "2025.05.02", time: "16:30", by: "admin_core", module: "AI",     action: "AI 설정 변경",   target: "임계치",           content: "0.75 → 0.80",    result: "성공" },
  { id: "LOG-C-00481", date: "2025.05.02", time: "15:55", by: "admin_core", module: "Schema", action: "Schema 변경",   target: "KYC-VC-Schema",    content: "v1.2 → v1.3",    result: "성공" },
  { id: "LOG-C-00480", date: "2025.05.02", time: "14:00", by: "system",     module: "XRPL",   action: "XRPL 재처리",   target: "TX-B7E2...",        content: "재처리 성공",      result: "성공" },
  { id: "LOG-C-00479", date: "2025.05.02", time: "11:20", by: "admin_dev",  module: "VC",     action: "VC 설정 변경",   target: "서명 알고리즘",     content: "ES256 → ES256K",  result: "성공" },
  { id: "LOG-C-00478", date: "2025.05.02", time: "10:05", by: "admin_core", module: "AI",     action: "프롬프트 변경",  target: "PROMPT-KYC-001",   content: "v2.1 → v2.2",    result: "성공" },
  { id: "LOG-C-00477", date: "2025.05.01", time: "17:44", by: "system",     module: "XRPL",   action: "XRPL 재처리",   target: "TX-A4F3...",        content: "재처리 실패",      result: "실패" },
  { id: "LOG-C-00476", date: "2025.05.01", time: "16:10", by: "admin_core", module: "SDK",    action: "SDK 메타데이터", target: "kyvc-sdk-2.3.1",   content: "메타데이터 갱신",  result: "성공" },
  { id: "LOG-C-00475", date: "2025.05.01", time: "14:33", by: "admin_core", module: "AI",     action: "Azure 설정",    target: "AZURE-CONN-001",   content: "Endpoint 변경",   result: "성공" },
  { id: "LOG-C-00474", date: "2025.05.01", time: "13:00", by: "admin_dev",  module: "Schema", action: "Schema 등록",   target: "CONSENT-Schema",   content: "v1.0.0 신규 등록", result: "성공" },
  { id: "LOG-C-00473", date: "2025.05.01", time: "11:15", by: "admin_core", module: "VP",     action: "VP 설정 변경",   target: "검증 정책",          content: "strict → lenient", result: "성공" },
  { id: "LOG-C-00472", date: "2025.04.30", time: "18:00", by: "system",     module: "AI",     action: "AI 자동 처리",  target: "KYC-00312",        content: "자동 승인",        result: "성공" },
  { id: "LOG-C-00471", date: "2025.04.30", time: "15:22", by: "admin_core", module: "Issuer", action: "키 갱신",        target: "KEY-0041",          content: "만료 키 갱신",     result: "성공" },
  { id: "LOG-C-00470", date: "2025.04.30", time: "09:50", by: "admin_dev",  module: "XRPL",   action: "네트워크 변경",  target: "XRPL 네트워크",     content: "testnet → mainnet", result: "실패" },
  { id: "LOG-C-00469", date: "2025.04.29", time: "16:45", by: "admin_core", module: "VC",     action: "VC 발급 승인",  target: "KYC-00298",        content: "수동 승인",        result: "성공" },
  { id: "LOG-C-00468", date: "2025.04.29", time: "14:30", by: "system",     module: "SDK",    action: "테스트 실행",   target: "sdk-testvector",   content: "전체 3건 통과",    result: "성공" },
];

const MODULES = ["전체 모듈", "AI", "VC", "VP", "XRPL", "Schema", "SDK", "Issuer"];
const DATE_OPTS = ["전체 기간", "오늘", "이번 주", "이번 달"];

function getDateNumbers(opt: string): Set<string> | null {
  const today = "2025.05.02";
  if (opt === "오늘") return new Set([today]);
  if (opt === "이번 주") return new Set(["2025.04.28","2025.04.29","2025.04.30","2025.05.01","2025.05.02"]);
  if (opt === "이번 달") return new Set(allLogs.filter(l => l.date.startsWith("2025.05")).map(l => l.date));
  return null;
}

function getPageNums(cur: number, total: number): (number | "...")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (cur <= 4)       return [1,2,3,4,5,"...",total];
  if (cur >= total-3) return [1,"...",total-4,total-3,total-2,total-1,total];
  return [1,"...",cur-1,cur,cur+1,"...",total];
}

export default function AuditLogPage() {
  const [search,      setSearch]      = useState("");
  const [filterMod,   setFilterMod]   = useState("전체 모듈");
  const [filterDate,  setFilterDate]  = useState("전체 기간");
  const [currentPage, setCurrentPage] = useState(1);

  const dateSet = getDateNumbers(filterDate);

  const filtered = allLogs.filter((l) => {
    const q = search.toLowerCase();
    const matchSearch = !q || l.id.toLowerCase().includes(q) || l.action.includes(q) || l.target.includes(q);
    const matchMod    = filterMod  === "전체 모듈" || l.module === filterMod;
    const matchDate   = !dateSet || dateSet.has(l.date);
    return matchSearch && matchMod && matchDate;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paged      = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleFilterChange = (fn: () => void) => { fn(); setCurrentPage(1); };

  return (
    <div>
      <PageHeader breadcrumb="감사로그" title="코어 감사로그 조회" />

      {/* 필터 */}
      <div className="flex items-center gap-2 mb-3">
        <input
          type="text"
          placeholder="로그 ID / 액션 / 대상 검색"
          value={search}
          onChange={(e) => handleFilterChange(() => setSearch(e.target.value))}
          className="border border-slate-300 rounded-md px-3 py-1.5 text-xs text-slate-700 focus:outline-none focus:border-blue-400 w-44"
        />
        <select value={filterMod}  onChange={(e) => handleFilterChange(() => setFilterMod(e.target.value))}
          className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none">
          {MODULES.map((m) => <option key={m}>{m}</option>)}
        </select>
        <select value={filterDate} onChange={(e) => handleFilterChange(() => setFilterDate(e.target.value))}
          className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none">
          {DATE_OPTS.map((d) => <option key={d}>{d}</option>)}
        </select>
        <button onClick={() => { setSearch(""); setFilterMod("전체 모듈"); setFilterDate("전체 기간"); setCurrentPage(1); }}
          className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors">
          초기화
        </button>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["로그 ID", "일자", "시간", "처리자", "모듈", "액션 유형", "대상", "변경 내용", "결과"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-4 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {paged.length === 0 ? (
              <tr><td colSpan={9} className="px-4 py-8 text-center text-xs text-slate-400">검색 결과가 없습니다.</td></tr>
            ) : paged.map((log) => (
              <tr key={log.id} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-4 py-3 text-xs font-mono text-slate-600">{log.id}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{log.date}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{log.time}</td>
                <td className="px-4 py-3 text-xs text-slate-600">{log.by}</td>
                <td className="px-4 py-3 text-xs text-slate-600">{log.module}</td>
                <td className="px-4 py-3 text-xs text-slate-700">{log.action}</td>
                <td className="px-4 py-3 text-xs font-mono text-slate-600">{log.target}</td>
                <td className="px-4 py-3 text-xs text-slate-600">{log.content}</td>
                <td className="px-4 py-3"><StatusBadge status={log.result} /></td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="px-4 py-3 border-t border-slate-100 flex items-center justify-between">
          <p className="text-xs text-slate-400">총 {filtered.length}건 · {currentPage} / {totalPages} 페이지</p>
          <div className="flex items-center gap-1">
            <button onClick={() => setCurrentPage((p) => Math.max(1, p-1))} disabled={currentPage === 1}
              className="w-6 h-6 text-xs rounded text-slate-500 hover:bg-slate-100 disabled:opacity-30">{"<"}</button>
            {getPageNums(currentPage, totalPages).map((p, i) =>
              p === "..." ? <span key={`e${i}`} className="text-xs text-slate-400 px-1">...</span> :
              <button key={p} onClick={() => setCurrentPage(p as number)}
                className={`w-6 h-6 text-xs rounded transition-colors ${currentPage === p ? "bg-blue-600 text-white" : "text-slate-500 hover:bg-slate-100"}`}>{p}</button>
            )}
            <button onClick={() => setCurrentPage((p) => Math.min(totalPages, p+1))} disabled={currentPage === totalPages}
              className="w-6 h-6 text-xs rounded text-slate-500 hover:bg-slate-100 disabled:opacity-30">{">"}</button>
          </div>
        </div>
      </div>
    </div>
  );
}
