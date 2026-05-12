"use client";

import { useState } from "react";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";
import Link from "next/link";

type NetworkKey = "devnet" | "testnet" | "mainnet";

const networkData: Record<NetworkKey, {
  status: "정상" | "지연" | "장애";
  blockHeight: string;
  responseTime: string;
  fee: string;
  lastTx: string;
}> = {
  devnet:  { status: "정상", blockHeight: "89,432,101", responseTime: "98ms",  fee: "12 drops", lastTx: "A4F3B2C1D9E8F7A6..." },
  testnet: { status: "지연", blockHeight: "45,219,033", responseTime: "342ms", fee: "15 drops", lastTx: "C9D2E4F1A7B3C8D5..." },
  mainnet: { status: "장애", blockHeight: "78,901,445", responseTime: "—",     fee: "—",        lastTx: "—" },
};

export default function XrplPage() {
  const [network, setNetwork] = useState<NetworkKey>("devnet");
  const data = networkData[network];

  return (
    <div>
      <PageHeader
        breadcrumb="XRPL"
        title="XRPL 네트워크 상태"
        actions={
          <div className="flex gap-2">
            <Link href="/xrpl/transactions" className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50">트랜잭션 목록</Link>
            <Link href="/xrpl/reprocess"    className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50">재처리 관리</Link>
          </div>
        }
      />

      <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <div className="grid grid-cols-4 gap-4 mb-5">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">XRPL 네트워크</label>
            <select
              value={network}
              onChange={(e) => setNetwork(e.target.value as NetworkKey)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400"
            >
              <option value="devnet">devnet</option>
              <option value="testnet">testnet</option>
              <option value="mainnet">mainnet</option>
            </select>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">노드 상태</label>
            <div className="mt-2">
              <StatusBadge status={data.status} />
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">최근 트랜잭션 ID</label>
            <p className="text-xs font-mono text-slate-600 mt-2">{data.lastTx}</p>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">수수료 / 응답시간</label>
            <p className="text-xs text-slate-600 mt-2">
              {data.fee === "—"
                ? <span className="text-red-500">연결 불가</span>
                : <><b>{data.fee}</b> &nbsp; <b>{data.responseTime}</b></>
              }
            </p>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4">
          <div className={`rounded-lg p-4 ${data.status === "정상" ? "bg-emerald-50" : data.status === "지연" ? "bg-yellow-50" : "bg-red-50"}`}>
            <p className="text-xs text-slate-500 mb-1">{network} 블록 높이</p>
            <p className={`text-2xl font-bold ${data.status === "정상" ? "text-emerald-600" : data.status === "지연" ? "text-yellow-600" : "text-red-400"}`}>
              {data.blockHeight}
            </p>
          </div>
          <div className="bg-slate-50 rounded-lg p-4">
            <p className="text-xs text-slate-500 mb-1">{network} 응답시간</p>
            <p className={`text-2xl font-bold ${data.responseTime === "—" ? "text-red-400" : "text-slate-700"}`}>
              {data.responseTime}
            </p>
          </div>
          <div className="bg-slate-50 rounded-lg p-4">
            <p className="text-xs text-slate-500 mb-1">{network} 수수료</p>
            <p className={`text-2xl font-bold ${data.fee === "—" ? "text-red-400" : "text-slate-700"}`}>
              {data.fee}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
