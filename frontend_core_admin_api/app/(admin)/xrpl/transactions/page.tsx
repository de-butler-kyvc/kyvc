"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

type Net = "devnet" | "testnet" | "mainnet";

const PAGE_SIZE = 8;

const networkColor: Record<Net, string> = {
  mainnet: "bg-blue-100 text-blue-700",
  testnet: "bg-yellow-100 text-yellow-700",
  devnet:  "bg-slate-100 text-slate-600",
};

const networkNodeStatus: Record<Net, "정상" | "지연" | "장애"> = {
  devnet:  "정상",
  testnet: "지연",
  mainnet: "장애",
};

const allTxRows = [
  { network: "mainnet" as Net, txId: "A4F3B2C1D9E8...", fee: "12 drops", time: "142ms", date: "2025.05.02 16:30", status: "정상" },
  { network: "testnet" as Net, txId: "B7E2A1F4C3D8...", fee: "10 drops", time: "380ms", date: "2025.05.02 16:28", status: "지연" },
  { network: "devnet"  as Net, txId: "C9D1E3F2A5B4...", fee: "0 drops",  time: "88ms",  date: "2025.05.02 16:25", status: "정상" },
  { network: "mainnet" as Net, txId: "D2E5F8A1B4C7...", fee: "11 drops", time: "155ms", date: "2025.05.02 16:10", status: "정상" },
  { network: "testnet" as Net, txId: "E1F4A7B2C5D8...", fee: "9 drops",  time: "412ms", date: "2025.05.02 16:05", status: "지연" },
  { network: "devnet"  as Net, txId: "F3A6B9C2D5E8...", fee: "0 drops",  time: "92ms",  date: "2025.05.02 15:55", status: "정상" },
  { network: "mainnet" as Net, txId: "G5B8C1D4E7F2...", fee: "13 drops", time: "138ms", date: "2025.05.02 15:40", status: "정상" },
  { network: "mainnet" as Net, txId: "H7C2D5E8F1A4...", fee: "12 drops", time: "167ms", date: "2025.05.02 15:22", status: "정상" },
  { network: "testnet" as Net, txId: "I9D3E6F2A8B1...", fee: "10 drops", time: "355ms", date: "2025.05.02 15:10", status: "지연" },
  { network: "devnet"  as Net, txId: "J2E5F8A1B4C9...", fee: "0 drops",  time: "79ms",  date: "2025.05.02 14:58", status: "정상" },
  { network: "mainnet" as Net, txId: "K4F7A2B5C8D1...", fee: "14 drops", time: "148ms", date: "2025.05.02 14:45", status: "정상" },
  { network: "testnet" as Net, txId: "L6A1B4C7D2E5...", fee: "11 drops", time: "398ms", date: "2025.05.02 14:30", status: "지연" },
  { network: "devnet"  as Net, txId: "M8B3C6D9E4F7...", fee: "0 drops",  time: "95ms",  date: "2025.05.02 14:18", status: "정상" },
  { network: "mainnet" as Net, txId: "N1C4D7E2F5A8...", fee: "12 drops", time: "152ms", date: "2025.05.02 14:00", status: "정상" },
  { network: "mainnet" as Net, txId: "O3D6E9F4A1B7...", fee: "15 drops", time: "501ms", date: "2025.05.02 13:45", status: "장애" },
];

function getPageNums(cur: number, total: number): (number | "...")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (cur <= 4)       return [1,2,3,4,5,"...",total];
  if (cur >= total-3) return [1,"...",total-4,total-3,total-2,total-1,total];
  return [1,"...",cur-1,cur,cur+1,"...",total];
}

export default function XrplTransactionsPage() {
  const router = useRouter();
  const [filterNet,   setFilterNet]   = useState<"전체" | Net>("전체");
  const [filterStatus,setFilterStatus]= useState("전체 상태");
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => { setCurrentPage(1); }, [filterNet, filterStatus]);

  const filtered = allTxRows.filter((r) => {
    const matchNet    = filterNet    === "전체"    || r.network === filterNet;
    const matchStatus = filterStatus === "전체 상태" || r.status  === filterStatus;
    return matchNet && matchStatus;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paged      = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const nodeStatus = filterNet !== "전체" ? networkNodeStatus[filterNet] : null;

  return (
    <div>
      <PageHeader breadcrumb="XRPL > 트랜잭션 목록" title="XRPL 트랜잭션 목록" />

      {/* 필터 바 */}
      <div className="bg-white rounded-lg border border-slate-200 p-4 mb-4">
        <div className="flex items-center gap-4">
          <div>
            <label className="block text-xs text-slate-500 mb-1">네트워크</label>
            <select value={filterNet} onChange={(e) => setFilterNet(e.target.value as "전체" | Net)}
              className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none">
              <option value="전체">전체 네트워크</option>
              <option value="mainnet">mainnet</option>
              <option value="testnet">testnet</option>
              <option value="devnet">devnet</option>
            </select>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">노드 상태</label>
            <div className="mt-1.5">
              {nodeStatus ? <StatusBadge status={nodeStatus} /> : <span className="text-xs text-slate-400">네트워크 선택</span>}
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">처리 상태</label>
            <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}
              className="border border-slate-300 rounded-md px-2 py-1.5 text-xs text-slate-700 focus:outline-none">
              <option>전체 상태</option>
              <option>정상</option>
              <option>지연</option>
              <option>장애</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["네트워크", "상태", "트랜잭션 ID", "수수료", "응답시간", "일시", "상세"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {paged.length === 0 ? (
              <tr><td colSpan={7} className="px-5 py-8 text-center text-xs text-slate-400">검색 결과가 없습니다.</td></tr>
            ) : paged.map((row, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-5 py-3">
                  <span className={`text-[11px] font-semibold px-2 py-0.5 rounded ${networkColor[row.network]}`}>{row.network}</span>
                </td>
                <td className="px-5 py-3"><StatusBadge status={row.status} /></td>
                <td className="px-5 py-3 text-xs font-mono text-slate-600">{row.txId}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{row.fee}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{row.time}</td>
                <td className="px-5 py-3 text-xs text-slate-500">{row.date}</td>
                <td className="px-5 py-3">
                  <button onClick={() => router.push(`/xrpl/transactions/detail?txId=${row.txId}`)}
                    className="text-xs text-blue-500 hover:underline">상세 →</button>
                </td>
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
