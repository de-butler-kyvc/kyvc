"use client";

import { getDashboardStats, getKycList } from "@/lib/api/kyc";
import Link from "next/link";
import { useState, useEffect } from "react";
import type { KycStatus, KycChannel } from "@/types/kyc";

const statusBadge: Record<KycStatus, string> = {
  "수동심사필요": "bg-red-100 text-red-600",
  "보완필요": "bg-orange-100 text-orange-600",
  "심사중": "bg-blue-100 text-blue-600",
  "정상": "bg-green-100 text-green-600",
  "불충족": "bg-slate-100 text-slate-500",
};

export default function DashboardPage() {
  const [stats, setStats] = useState<any>(null);
  const [kycList, setKycList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 3;

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsData, kycData] = await Promise.all([
        getDashboardStats(),
        getKycList({ search: searchTerm, status: statusFilter }),
      ]);
      setStats(statsData);
      setKycList(kycData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    fetchData();
  };

  const handleReset = () => {
    setSearchTerm("");
    setStatusFilter("전체 상태");
    setCurrentPage(1);
    fetchData();
  };

  const totalPages = Math.max(1, Math.ceil(kycList.length / ITEMS_PER_PAGE));
  const paginatedList = kycList.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);

  const statCards = [
    { label: "오늘 KYC 신청", value: stats?.todayKyc, sub: "금일 신규 접수", color: "text-blue-600" },
    { label: "수동심사 대기", value: stats?.pendingManual, sub: stats?.pendingManual > 0 ? "즉시 처리 필요" : "대기 없음", color: "text-red-500" },
    { label: "보완요청 대기", value: stats?.pendingSupplement, sub: stats?.pendingSupplement > 0 ? `${stats.pendingSupplement}건 처리 대기` : "대기 없음", color: "text-orange-500" },
    { label: "VC 발급 완료", value: stats?.vcIssued, sub: "금일 기준", color: "text-green-600" },
    { label: "VP 검증 건수", value: stats?.vpCount, sub: "금일 기준", color: "text-slate-700" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">백엔드 어드민 대시보드</h1>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {/* 요약 카드 */}
      <div className="grid grid-cols-5 gap-4">
        {statCards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs text-slate-500">{card.label}</p>
            <p className={`text-3xl font-bold mt-1 ${card.color}`}>
              {loading ? "..." : card.value ?? "-"}
            </p>
            <p className="text-xs text-slate-400 mt-1">{card.sub}</p>
          </div>
        ))}
      </div>

      {/* 테이블 */}
      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 p-4 border-b border-slate-100">
          <input
            type="text"
            placeholder="법인명 또는 사업자번호 검색"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-56 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 상태</option>
            <option>수동심사필요</option>
            <option>보완필요</option>
            <option>심사중</option>
            <option>정상</option>
          </select>
          <select className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
            <option>오늘</option>
            <option>이번 주</option>
            <option>이번 달</option>
          </select>
          <button
            onClick={handleSearch}
            className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700"
          >
            검색
          </button>
          <button
            onClick={handleReset}
            className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50"
          >
            초기화
          </button>
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-500">로딩 중...</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 text-slate-500 font-medium">신청번호</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">법인명</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">사업자등록번호</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">신청일시</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">심사자</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">최근 처리일</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3 text-blue-600 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.corp}</td>
                  <td className="px-4 py-3 text-slate-500">{row.biz}</td>
                  <td className="px-4 py-3 text-slate-500">{row.date}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status as KycStatus]}`}>{row.status}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{row.reviewer}</td>
                  <td className="px-4 py-3 text-slate-500">{row.date}</td>
                  <td className="px-4 py-3">
                    <Link href={`/kyc/${row.id}`} className="text-blue-600 hover:underline text-xs">상세 →</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex items-center justify-between px-4 py-3">
          <span className="text-xs text-slate-400">총 {kycList.length}건</span>
          <div className="flex gap-1">
            <button
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40"
            >‹</button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`w-7 h-7 rounded text-xs ${currentPage === page ? "bg-blue-600 text-white" : "border border-slate-200 hover:bg-slate-50"}`}
              >{page}</button>
            ))}
            <button
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40"
            >›</button>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
