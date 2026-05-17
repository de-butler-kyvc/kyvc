"use client";

import { getKycList } from "@/lib/api/kyc";
import { kycDetailPath } from "@/lib/navigation/admin-routes";
import Link from "next/link";
import { useState, useEffect } from "react";

const statusBadge: Record<string, string> = {
  "작성중": "bg-slate-100 text-slate-500",
  "제출완료": "bg-blue-100 text-blue-600",
  "AI 심사중": "bg-blue-100 text-blue-600",
  "수동심사": "bg-red-100 text-red-600",
  "보완요청": "bg-orange-100 text-orange-600",
  "승인완료": "bg-green-100 text-green-600",
  "VC 발급완료": "bg-green-100 text-green-600",
  "반려": "bg-slate-100 text-slate-500",
};

const aiBadge: Record<string, string> = {
  "보완필요": "bg-orange-100 text-orange-600",
  "불충족": "bg-red-100 text-red-600",
  "정상": "bg-green-100 text-green-600",
};

const ITEMS_PER_PAGE = 15;

export default function KycPage() {
  const [kycList, setKycList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("전체 상태");
  const [channelFilter, setChannelFilter] = useState("전체 채널");
  const [currentPage, setCurrentPage] = useState(1);

  const fetchKycList = async (search = searchTerm, status = statusFilter, channel = channelFilter) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getKycList({ search, status, channel });
      setKycList(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchKycList();
  }, []);

  const handleSearch = () => {
    setCurrentPage(1);
    fetchKycList();
  };

  const handleReset = () => {
    setSearchTerm(""); setStatusFilter("전체 상태"); setChannelFilter("전체 채널");
    setCurrentPage(1);
    fetchKycList("", "전체 상태", "전체 채널");
  };

  const totalPages = Math.max(1, Math.ceil(kycList.length / ITEMS_PER_PAGE));
  const paginatedList = kycList.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">증명서 관리자</p>
          <h1 className="text-xl font-bold text-slate-800">KYC 신청 목록</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 p-4 border-b border-slate-100 flex-wrap">
          <input
            type="text"
            placeholder="법인명 / 사업자등록번호"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm w-48 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 상태</option>
            <option>작성중</option>
            <option>제출완료</option>
            <option>AI 심사중</option>
            <option>수동심사</option>
            <option>보완요청</option>
            <option>승인완료</option>
            <option>반려</option>
            <option>VC 발급완료</option>
          </select>
          <select
            value={channelFilter}
            onChange={(e) => setChannelFilter(e.target.value)}
            className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none"
          >
            <option>전체 채널</option>
            <option>웹</option>
            <option>금융사</option>
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
                <th className="px-4 py-3 w-8"><input type="checkbox" /></th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">신청번호</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">법인명</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">사업자등록번호</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">법인 유형</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">신청일시</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">KYC 상태</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">AI 판단(참고)</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">심사자</th>
                <th className="text-left px-4 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {paginatedList.map((row) => (
                <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3"><input type="checkbox" /></td>
                  <td className="px-4 py-3 text-blue-600 font-medium">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.corp}</td>
                  <td className="px-4 py-3 text-slate-500">{row.biz}</td>
                  <td className="px-4 py-3 text-slate-500">{row.type}</td>
                  <td className="px-4 py-3 text-slate-500">{row.date}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status] || "bg-slate-100 text-slate-500"}`}>{row.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${aiBadge[row.ai] || "bg-slate-100 text-slate-500"}`}>{row.ai}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{row.reviewer}</td>
                  <td className="px-4 py-3">
                    <Link href={kycDetailPath(row.id)} className="text-blue-600 hover:underline text-xs">상세 →</Link>
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
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              disabled={currentPage === 1}
              className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40"
            >
              {"<"}
            </button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`w-7 h-7 rounded text-xs ${currentPage === page ? "bg-blue-600 text-white" : "border border-slate-200 hover:bg-slate-50"}`}
              >
                {page}
              </button>
            ))}
            <button
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
              disabled={currentPage === totalPages}
              className="w-7 h-7 rounded border border-slate-200 text-xs hover:bg-slate-50 disabled:opacity-40"
            >
              {">"}
            </button>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC 증명서 관리자 · 증명서 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
