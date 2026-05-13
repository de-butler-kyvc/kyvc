"use client";

import { useState, useEffect, useCallback } from "react";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";
import Link from "next/link";

const PAGE_SIZE = 10;

interface AiStats {
  total: number;
  success: number;
  failure: number;
  manual: number;
  avgTime: string;
  p95Time: string;
}

const mockStats: AiStats = {
  total: 203,
  success: 188,
  failure: 4,
  manual: 11,
  avgTime: "2.4s",
  p95Time: "4.1s",
};

export const mockAiRows = [
  { id: "AI-20250502-0203", kyc: "KYC-001", docType: "등기사항전부증명서",  model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 94.2, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "1.8s", status: "성공", extracted: { companyName: "주식회사 케이원",     bizNo: "123-45-67890", trustScore: 0.942 } },
  { id: "AI-20250502-0202", kyc: "KYC-002", docType: "주주명부",           model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 72.4, threshold: 0.80, result: "보완필요", judgment: "수동 심사", time: "2.9s", status: "보류", extracted: { companyName: "주식회사 테크놀로지",   bizNo: "234-56-78901", trustScore: 0.724 } },
  { id: "AI-20250502-0201", kyc: "KYC-003", docType: "사업자등록증",       model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 38.1, threshold: 0.80, result: "불충족",  judgment: "자동 거절", time: "3.2s", status: "실패", extracted: { companyName: "주식회사 솔루션즈",     bizNo: "345-67-89012", trustScore: 0.381 } },
  { id: "AI-20250502-0200", kyc: "KYC-004", docType: "정관",              model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 98.1, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "1.5s", status: "성공", extracted: { companyName: "주식회사 글로벌",       bizNo: "456-78-90123", trustScore: 0.981 } },
  { id: "AI-20250502-0199", kyc: "KYC-005", docType: "위임장",            model: "gpt-4o-mini", deployment: "kyvc-fb-01",   prompt: "v2.4.0", trust: 88.7, threshold: 0.75, result: "정상",    judgment: "자동 승인", time: "0.9s", status: "성공", extracted: { companyName: "주식회사 파트너스",     bizNo: "567-89-01234", trustScore: 0.887 } },
  { id: "AI-20250502-0198", kyc: "KYC-006", docType: "등기사항전부증명서",  model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 91.3, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "2.1s", status: "성공", extracted: { companyName: "주식회사 이노베이션",   bizNo: "678-90-12345", trustScore: 0.913 } },
  { id: "AI-20250502-0197", kyc: "KYC-007", docType: "사업자등록증",       model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 55.0, threshold: 0.80, result: "보완필요", judgment: "수동 심사", time: "2.7s", status: "보류", extracted: { companyName: "주식회사 네트웍스",     bizNo: "789-01-23456", trustScore: 0.550 } },
  { id: "AI-20250502-0196", kyc: "KYC-008", docType: "주주명부",           model: "gpt-4o-mini", deployment: "kyvc-fb-01",   prompt: "v2.4.0", trust: 97.5, threshold: 0.75, result: "정상",    judgment: "자동 승인", time: "1.1s", status: "성공", extracted: { companyName: "주식회사 홀딩스",       bizNo: "890-12-34567", trustScore: 0.975 } },
  { id: "AI-20250502-0195", kyc: "KYC-009", docType: "정관",              model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 29.3, threshold: 0.80, result: "불충족",  judgment: "자동 거절", time: "4.0s", status: "실패", extracted: { companyName: "주식회사 엔터프라이즈", bizNo: "901-23-45678", trustScore: 0.293 } },
  { id: "AI-20250502-0194", kyc: "KYC-010", docType: "위임장",            model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 85.6, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "1.6s", status: "성공", extracted: { companyName: "주식회사 코퍼레이션",   bizNo: "012-34-56789", trustScore: 0.856 } },
  { id: "AI-20250502-0193", kyc: "KYC-011", docType: "등기사항전부증명서",  model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 76.2, threshold: 0.80, result: "보완필요", judgment: "수동 심사", time: "2.4s", status: "보류", extracted: { companyName: "주식회사 시스템즈",     bizNo: "123-56-78901", trustScore: 0.762 } },
  { id: "AI-20250502-0192", kyc: "KYC-012", docType: "사업자등록증",       model: "gpt-4o-mini", deployment: "kyvc-fb-01",   prompt: "v2.4.0", trust: 93.8, threshold: 0.75, result: "정상",    judgment: "자동 승인", time: "0.8s", status: "성공", extracted: { companyName: "주식회사 컴퍼니",       bizNo: "234-67-89012", trustScore: 0.938 } },
  { id: "AI-20250502-0191", kyc: "KYC-013", docType: "주주명부",           model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 41.0, threshold: 0.80, result: "불충족",  judgment: "자동 거절", time: "3.5s", status: "실패", extracted: { companyName: "주식회사 그룹",         bizNo: "345-78-90123", trustScore: 0.410 } },
  { id: "AI-20250502-0190", kyc: "KYC-014", docType: "정관",              model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 99.0, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "1.3s", status: "성공", extracted: { companyName: "주식회사 인터내셔널",   bizNo: "456-89-01234", trustScore: 0.990 } },
  { id: "AI-20250502-0189", kyc: "KYC-015", docType: "위임장",            model: "gpt-4o-mini", deployment: "kyvc-fb-01",   prompt: "v2.4.0", trust: 68.4, threshold: 0.75, result: "보완필요", judgment: "수동 심사", time: "1.9s", status: "보류", extracted: { companyName: "주식회사 벤처스",       bizNo: "567-90-12345", trustScore: 0.684 } },
  { id: "AI-20250502-0188", kyc: "KYC-016", docType: "등기사항전부증명서",  model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 87.9, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "2.0s", status: "성공", extracted: { companyName: "주식회사 어소시에이츠", bizNo: "678-01-23456", trustScore: 0.879 } },
  { id: "AI-20250502-0187", kyc: "KYC-017", docType: "사업자등록증",       model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 22.7, threshold: 0.80, result: "불충족",  judgment: "자동 거절", time: "3.8s", status: "실패", extracted: { companyName: "주식회사 인더스트리",   bizNo: "789-12-34567", trustScore: 0.227 } },
  { id: "AI-20250502-0186", kyc: "KYC-018", docType: "주주명부",           model: "gpt-4o-mini", deployment: "kyvc-fb-01",   prompt: "v2.4.0", trust: 95.1, threshold: 0.75, result: "정상",    judgment: "자동 승인", time: "0.7s", status: "성공", extracted: { companyName: "주식회사 서비스",       bizNo: "890-23-45678", trustScore: 0.951 } },
  { id: "AI-20250502-0185", kyc: "KYC-019", docType: "정관",              model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 80.0, threshold: 0.80, result: "정상",    judgment: "자동 승인", time: "1.7s", status: "성공", extracted: { companyName: "주식회사 트레이딩",     bizNo: "901-34-56789", trustScore: 0.800 } },
  { id: "AI-20250502-0184", kyc: "KYC-020", docType: "위임장",            model: "gpt-4o",      deployment: "kyvc-prod-01", prompt: "v2.4.1", trust: 63.5, threshold: 0.80, result: "보완필요", judgment: "수동 심사", time: "2.6s", status: "보류", extracted: { companyName: "주식회사 파운데이션",   bizNo: "012-45-67890", trustScore: 0.635 } },
];

function getPageNumbers(current: number, total: number): (number | "...")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "...", total];
  if (current >= total - 3) return [1, "...", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "...", current - 1, current, current + 1, "...", total];
}

export default function AiStatusPage() {
  const [stats, setStats] = useState<AiStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [search, setSearch] = useState("");
  const [filterModel, setFilterModel] = useState("전체 모델");
  const [filterPrompt, setFilterPrompt] = useState("전체 프롬프트 버전");
  const [filterStatus, setFilterStatus] = useState("전체 처리 상태");
  const [currentPage, setCurrentPage] = useState(1);

  const fetchStats = useCallback(async () => {
    setLoading(true);
    try {
      // TODO: replace with actual API call
      // const res = await fetch('/api/ai/stats');
      // const data = await res.json();
      await new Promise((r) => setTimeout(r, 400));
      setStats(mockStats);
      setLastUpdated(new Date());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, [fetchStats]);

  // 필터가 바뀌면 첫 페이지로 리셋
  useEffect(() => {
    setCurrentPage(1);
  }, [search, filterModel, filterPrompt, filterStatus]);

  const statCards = stats
    ? [
        { label: "총 요청",        value: stats.total.toLocaleString(),   sub: "오늘 기준" },
        { label: "성공",           value: stats.success.toLocaleString(),  sub: `${((stats.success / stats.total) * 100).toFixed(1)}%` },
        { label: "실패",           value: stats.failure.toLocaleString(),  sub: `${((stats.failure / stats.total) * 100).toFixed(1)}%` },
        { label: "수동심사 전환",   value: stats.manual.toLocaleString(),   sub: `${((stats.manual / stats.total) * 100).toFixed(1)}%` },
        { label: "평균 처리시간",   value: stats.avgTime,                   sub: `P95 ${stats.p95Time}` },
      ]
    : [];

  const filteredRows = mockAiRows.filter((row) => {
    const q = search.toLowerCase();
    const matchSearch = q === "" || row.id.toLowerCase().includes(q) || row.kyc.toLowerCase().includes(q);
    const matchModel  = filterModel  === "전체 모델"           || row.model  === filterModel;
    const matchPrompt = filterPrompt === "전체 프롬프트 버전"  || row.prompt === filterPrompt;
    const matchStatus = filterStatus === "전체 처리 상태"      || row.status === filterStatus;
    return matchSearch && matchModel && matchPrompt && matchStatus;
  });

  const totalPages = Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE));
  const pagedRows  = filteredRows.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const pageNums   = getPageNumbers(currentPage, totalPages);

  return (
    <div>
      <PageHeader
        breadcrumb="AI 처리현황"
        title="AI 처리현황"
        actions={
          <div className="flex items-center gap-2">
            {lastUpdated && (
              <span className="text-[11px] text-slate-400">
                {lastUpdated.toLocaleTimeString("ko-KR")} 기준
              </span>
            )}
            <button
              onClick={fetchStats}
              disabled={loading}
              className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors disabled:opacity-50"
            >
              {loading ? "새로고침 중..." : "새로고침"}
            </button>
          </div>
        }
      />

      {/* 통계 카드 */}
      <div className="grid grid-cols-5 gap-3 mb-4">
        {stats === null
          ? Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="bg-white rounded-lg border border-slate-200 p-4 animate-pulse">
                <div className="h-3 bg-slate-100 rounded w-16 mb-2" />
                <div className="h-7 bg-slate-100 rounded w-12 mb-1.5" />
                <div className="h-2.5 bg-slate-100 rounded w-10" />
              </div>
            ))
          : statCards.map((s) => (
              <div
                key={s.label}
                className={`bg-white rounded-lg border border-slate-200 p-4 transition-opacity ${loading ? "opacity-60" : ""}`}
              >
                <p className="text-xs text-slate-500 mb-1">{s.label}</p>
                <p className="text-2xl font-bold text-slate-800">{s.value}</p>
                <p className="text-[11px] text-slate-400 mt-0.5">{s.sub}</p>
              </div>
            ))}
      </div>

      {/* 필터 */}
      <div className="flex items-center gap-2 mb-3">
        <input
          type="text"
          placeholder="AI 요청 ID 검색"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="border border-slate-300 rounded-md px-3 py-1.5 text-xs text-slate-700 focus:outline-none focus:border-blue-400 w-40"
        />
        <select
          value={filterModel}
          onChange={(e) => setFilterModel(e.target.value)}
          className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none"
        >
          <option>전체 모델</option>
          <option>gpt-4o</option>
          <option>gpt-4o-mini</option>
        </select>
        <select
          value={filterPrompt}
          onChange={(e) => setFilterPrompt(e.target.value)}
          className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none"
        >
          <option>전체 프롬프트 버전</option>
          <option>v2.4.1</option>
          <option>v2.4.0</option>
        </select>
        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none"
        >
          <option>전체 처리 상태</option>
          <option>성공</option>
          <option>실패</option>
          <option>보류</option>
        </select>
        <button
          onClick={() => {
            setSearch("");
            setFilterModel("전체 모델");
            setFilterPrompt("전체 프롬프트 버전");
            setFilterStatus("전체 처리 상태");
          }}
          className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors"
        >
          초기화
        </button>
      </div>

      {/* 테이블 */}
      <div className="bg-white rounded-lg border border-slate-200">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["요청 ID", "신청번호", "문서 유형", "모델/배포명", "프롬프트 버전", "신뢰도", "판단 결과", "처리시간", "상태", "상세"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-4 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {pagedRows.length === 0 ? (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-xs text-slate-400">
                  검색 결과가 없습니다.
                </td>
              </tr>
            ) : (
              pagedRows.map((row) => (
                <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50">
                  <td className="px-4 py-3 text-xs text-slate-600 font-mono">{row.id}</td>
                  <td className="px-4 py-3 text-xs text-blue-600">{row.kyc}</td>
                  <td className="px-4 py-3 text-xs text-slate-700">{row.docType}</td>
                  <td className="px-4 py-3 text-xs text-slate-600 font-mono">{row.model} / {row.deployment}</td>
                  <td className="px-4 py-3 text-xs text-slate-600">{row.prompt}</td>
                  <td className={`px-4 py-3 text-xs font-semibold ${row.trust >= 80 ? "text-emerald-600" : row.trust >= 60 ? "text-yellow-600" : "text-red-600"}`}>
                    {row.trust}%
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-600">{row.result}</td>
                  <td className="px-4 py-3 text-xs text-slate-500">{row.time}</td>
                  <td className="px-4 py-3"><StatusBadge status={row.status} /></td>
                  <td className="px-4 py-3">
                    <Link href={`/ai/status/${row.id}`} className="text-xs text-blue-500 hover:underline">
                      상세 →
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* 페이지네이션 */}
        <div className="px-4 py-3 border-t border-slate-100 flex items-center justify-between">
          <p className="text-xs text-slate-400">
            총 {filteredRows.length}건 &middot; {currentPage} / {totalPages} 페이지
          </p>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="w-6 h-6 text-xs rounded text-slate-500 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              &lt;
            </button>
            {pageNums.map((p, i) =>
              p === "..." ? (
                <span key={`ellipsis-${i}`} className="text-xs text-slate-400 px-1">
                  ...
                </span>
              ) : (
                <button
                  key={p}
                  onClick={() => setCurrentPage(p as number)}
                  className={`w-6 h-6 text-xs rounded transition-colors ${
                    currentPage === p
                      ? "bg-blue-600 text-white"
                      : "text-slate-500 hover:bg-slate-100"
                  }`}
                >
                  {p}
                </button>
              )
            )}
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="w-6 h-6 text-xs rounded text-slate-500 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              &gt;
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}