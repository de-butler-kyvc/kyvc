"use client";
import { useState } from "react";

const dataByMonth: Record<string, {
  cards: { label: string; value: string; sub: string; color: string }[];
  kycStats: { label: string; value: number; color: string; total: number }[];
  vpStats: { label: string; value: number; color: string; total: number }[];
}> = {
  "2025년 5월": {
    cards: [
      { label: "KYC 처리 건수", value: "203", sub: "전월 대비 +18%", color: "text-blue-600" },
      { label: "자동 승인율", value: "71.4%", sub: "145건 자동 처리", color: "text-green-600" },
      { label: "보완율", value: "18.2%", sub: "37건 보완요청", color: "text-orange-500" },
      { label: "VC 발급 건수", value: "168", sub: "발급 성공률 82.8%", color: "text-slate-700" },
    ],
    kycStats: [
      { label: "자동 승인", value: 145, color: "bg-green-500", total: 203 },
      { label: "보완요청", value: 37, color: "bg-orange-400", total: 203 },
      { label: "수동심사", value: 16, color: "bg-blue-500", total: 203 },
      { label: "반려", value: 5, color: "bg-red-500", total: 203 },
    ],
    vpStats: [
      { label: "검증 성공", value: 3847, color: "bg-green-500", total: 4380 },
      { label: "검증 실패", value: 341, color: "bg-red-500", total: 4380 },
      { label: "QR 만료", value: 192, color: "bg-orange-400", total: 4380 },
    ],
  },
  "2025년 4월": {
    cards: [
      { label: "KYC 처리 건수", value: "172", sub: "전월 대비 +5%", color: "text-blue-600" },
      { label: "자동 승인율", value: "68.0%", sub: "117건 자동 처리", color: "text-green-600" },
      { label: "보완율", value: "20.3%", sub: "35건 보완요청", color: "text-orange-500" },
      { label: "VC 발급 건수", value: "142", sub: "발급 성공률 82.6%", color: "text-slate-700" },
    ],
    kycStats: [
      { label: "자동 승인", value: 117, color: "bg-green-500", total: 172 },
      { label: "보완요청", value: 35, color: "bg-orange-400", total: 172 },
      { label: "수동심사", value: 14, color: "bg-blue-500", total: 172 },
      { label: "반려", value: 6, color: "bg-red-500", total: 172 },
    ],
    vpStats: [
      { label: "검증 성공", value: 3201, color: "bg-green-500", total: 3650 },
      { label: "검증 실패", value: 298, color: "bg-red-500", total: 3650 },
      { label: "QR 만료", value: 151, color: "bg-orange-400", total: 3650 },
    ],
  },
  "2025년 3월": {
    cards: [
      { label: "KYC 처리 건수", value: "163", sub: "전월 대비 +2%", color: "text-blue-600" },
      { label: "자동 승인율", value: "65.0%", sub: "106건 자동 처리", color: "text-green-600" },
      { label: "보완율", value: "22.1%", sub: "36건 보완요청", color: "text-orange-500" },
      { label: "VC 발급 건수", value: "131", sub: "발급 성공률 80.4%", color: "text-slate-700" },
    ],
    kycStats: [
      { label: "자동 승인", value: 106, color: "bg-green-500", total: 163 },
      { label: "보완요청", value: 36, color: "bg-orange-400", total: 163 },
      { label: "수동심사", value: 15, color: "bg-blue-500", total: 163 },
      { label: "반려", value: 6, color: "bg-red-500", total: 163 },
    ],
    vpStats: [
      { label: "검증 성공", value: 2980, color: "bg-green-500", total: 3400 },
      { label: "검증 실패", value: 271, color: "bg-red-500", total: 3400 },
      { label: "QR 만료", value: 149, color: "bg-orange-400", total: 3400 },
    ],
  },
};

export default function ReportPage() {
  const [month, setMonth] = useState("2025년 5월");
  const [channel, setChannel] = useState("전체 채널");
  const [displayed, setDisplayed] = useState("2025년 5월");

  const data = dataByMonth[displayed];

  const handleQuery = () => {
    setDisplayed(month);
  };

  const handleExcelExport = () => {
    const d = dataByMonth[displayed];
    const rows = [
      ["구분", "항목", "값"],
      ...d.cards.map(c => ["요약", c.label, c.value]),
      ...d.kycStats.map(s => ["KYC", s.label, String(s.value)]),
      ...d.vpStats.map(s => ["VP", s.label, String(s.value)]),
    ];
    const csv = rows.map(r => r.map(v => `"${v}"`).join(",")).join("\n");
    const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `report-${displayed.replace(/\s/g, "-")}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handlePdfExport = () => {
    window.print();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">운영 리포트</h1>
        </div>
      </div>

      {/* 필터 + 내보내기 */}
      <div className="bg-white rounded-lg border border-slate-200 p-4 flex items-center gap-3">
        <select value={month} onChange={e => setMonth(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
          <option>2025년 5월</option>
          <option>2025년 4월</option>
          <option>2025년 3월</option>
        </select>
        <select value={channel} onChange={e => setChannel(e.target.value)} className="border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none">
          <option>전체 채널</option>
          <option>웹</option>
          <option>금융사</option>
        </select>
        <button onClick={handleQuery} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">조회</button>
        <div className="ml-auto flex gap-2">
          <button onClick={handlePdfExport} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">PDF 내보내기</button>
          <button onClick={handleExcelExport} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">Excel 내보내기</button>
        </div>
      </div>

      {/* 요약 카드 */}
      <div className="grid grid-cols-4 gap-4">
        {data.cards.map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 p-5">
            <p className="text-xs text-slate-500">{card.label}</p>
            <p className={`text-3xl font-bold mt-1 ${card.color}`}>{card.value}</p>
            <p className="text-xs text-slate-400 mt-1">{card.sub}</p>
          </div>
        ))}
      </div>

      {/* 차트 영역 */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white rounded-lg border border-slate-200 p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-slate-700">KYC 상태별 분포</h2>
            <span className="text-xs text-slate-400">{displayed} · {channel}</span>
          </div>
          <div className="space-y-3">
            {data.kycStats.map((item) => (
              <div key={item.label} className="flex items-center gap-3">
                <span className="text-xs text-slate-500 w-16 shrink-0">{item.label}</span>
                <div className="flex-1 bg-slate-100 rounded-full h-4 overflow-hidden">
                  <div className={`h-4 rounded-full ${item.color}`} style={{ width: `${(item.value / item.total) * 100}%` }} />
                </div>
                <span className="text-xs text-slate-600 w-10 text-right shrink-0">{item.value}건</span>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg border border-slate-200 p-5">
          <h2 className="text-sm font-semibold text-slate-700 mb-4">VP 검증 현황</h2>
          <div className="space-y-3">
            {data.vpStats.map((item) => (
              <div key={item.label} className="flex items-center gap-3">
                <span className="text-xs text-slate-500 w-16 shrink-0">{item.label}</span>
                <div className="flex-1 bg-slate-100 rounded-full h-4 overflow-hidden">
                  <div className={`h-4 rounded-full ${item.color}`} style={{ width: `${(item.value / item.total) * 100}%` }} />
                </div>
                <span className="text-xs text-slate-600 w-14 text-right shrink-0">{item.value.toLocaleString()}건</span>
              </div>
            ))}
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
