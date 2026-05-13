"use client";

import { PageHeader, StatusBadge, ChangeHistoryTable, defaultChangeRows } from "@/components/ui/PageHeader";
import Link from "next/link";

export default function IssuerPage() {
  return (
    <div>
      <PageHeader breadcrumb="Issuer / 키" title="Issuer 기술 상태 조회"
        actions={<Link href="/issuer/keys" className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50">키 참조 상태 →</Link>}
      />

            <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <p className="text-xs font-semibold text-slate-600 mb-3">설정값</p>
        <div className="space-y-2.5">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-500">XRPL 계정</span>
            <span className="text-xs font-mono text-slate-700">rKYvCIssuer001...</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-500">서명키 참조</span>
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono text-slate-700">KMS-KEY-ISSUER-001</span>
              <StatusBadge status="정상" />
            </div>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-500">KYC VC 지원</span>
            <StatusBadge status="지원" />
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-500">위임권한 VC 지원</span>
            <StatusBadge status="지원" />
          </div>
        </div>
      </div>

      <ChangeHistoryTable rows={defaultChangeRows} />
    </div>
  );
}
