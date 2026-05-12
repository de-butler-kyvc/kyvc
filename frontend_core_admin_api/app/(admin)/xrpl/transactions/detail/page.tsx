"use client";

import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

const txJson = `{
  "TransactionType": "Payment",
  "Account": "rKYvCIssuer001...",
  "Amount": "0",
  "Destination": "rKYvCHolder001...",
  "Memos": [{"Memo": {"MemoData": "vckyvc2025:KYC-001"}}],
  "Fee": "12",
  "LedgerIndex": 89432101,
  "hash": "A4F3B2C1D9E8F7A6..."
}`;

export default function XrplTxDetailPage() {
  return (
    <div>
      <PageHeader breadcrumb="XRPL > 트랜잭션 목록 > 트랜잭션 상세" title="XRPL 트랜잭션 상세" />

      <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <div className="grid grid-cols-4 gap-4 mb-5">
          <div>
            <label className="block text-xs text-slate-500 mb-1">XRPL 네트워크</label>
            <select className="w-full border border-slate-300 rounded-md px-2 py-1.5 text-xs focus:outline-none">
              <option>devnet</option>
            </select>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">노드 상태</label>
            <div className="flex gap-1.5 mt-1.5">
              <StatusBadge status="정상" /><StatusBadge status="지연" /><StatusBadge status="장애" />
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">트랜잭션 ID</label>
            <p className="text-xs font-mono text-slate-600 mt-1.5">A4F3B2C1D9E8F7A6...</p>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">수수료/응답시간</label>
            <p className="text-xs text-slate-600 mt-1.5">수수료 <b>12 drops</b> 응답 <b>142ms</b></p>
          </div>
        </div>
        <pre className="bg-slate-900 text-emerald-400 text-xs p-4 rounded-lg overflow-auto font-mono leading-relaxed">
          {txJson}
        </pre>
      </div>
    </div>
  );
}
