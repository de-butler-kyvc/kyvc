"use client";
import { use } from "react";
import Link from "next/link";

const mockVc = {
  credentialId: "vc:kyvc:2025:corp-kyc-001",
  credentialType: "KYCVerifiableCredential",
  issuerDid: "did:kyvc:issuer:001",
  holderDid: "did:kyvc:holder:kim123",
  issuedAt: "2025.05.02 14:00",
  expiresAt: "2026.12.31",
  xrplTxHash: "A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6",
  mobileStored: "저장 완료",
  corp: "주식회사 케이원",
  kyc: "KYC-2025-0502-001",
  status: "활성",
};

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  폐기: "bg-red-100 text-red-600",
  만료: "bg-slate-100 text-slate-500",
  보류: "bg-orange-100 text-orange-600",
};

export default function VcDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href="/vc" className="hover:underline">VC 관리</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">VC 발급 상태 조회</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 요약 */}
        <div className="w-56 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4 space-y-3">
            <h2 className="text-xs font-semibold text-slate-500">VC 발급 상태 조회</h2>
            {[
              { label: "법인명", value: mockVc.corp },
              { label: "발급 상태", value: mockVc.status, isBadge: true },
              { label: "발급 요청일", value: mockVc.issuedAt },
              { label: "발급 완료일", value: mockVc.issuedAt },
            ].map((item) => (
              <div key={item.label}>
                <p className="text-xs text-slate-400">{item.label}</p>
                {item.isBadge ? (
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block ${statusBadge[item.value]}`}>
                    {item.value}
                  </span>
                ) : (
                  <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
                )}
              </div>
            ))}
          </div>

          {/* 액션 버튼 */}
          <Link
            href={`/vc/${id}/reissue`}
            className="block w-full text-center border border-slate-200 text-slate-600 py-2 rounded-lg text-sm hover:bg-slate-50 transition-colors"
          >
            VC 재발급 요청
          </Link>
          <Link
            href={`/vc/${id}/revoke`}
            className="block w-full text-center border border-red-200 text-red-500 py-2 rounded-lg text-sm hover:bg-red-50 transition-colors"
          >
            VC 폐기 요청
          </Link>
        </div>

        {/* 우측 상세 정보 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">VC 발급 상세 정보</h2>
          </div>
          <div className="p-5 space-y-0">
            {[
              { label: "Credential ID", value: mockVc.credentialId },
              { label: "Credential Type", value: mockVc.credentialType },
              { label: "Issuer DID", value: mockVc.issuerDid },
              { label: "Holder DID", value: mockVc.holderDid },
              { label: "발급일", value: mockVc.issuedAt },
              { label: "만료일", value: mockVc.expiresAt },
              { label: "XRPL Tx Hash", value: mockVc.xrplTxHash },
              { label: "모바일 저장 여부", value: mockVc.mobileStored, isGreen: true },
              { label: "연결 KYC 신청", value: mockVc.kyc, isLink: true },
            ].map((item) => (
              <div key={item.label} className="flex items-center border-b border-slate-50 py-3 last:border-0">
                <span className="text-sm text-slate-400 w-40 shrink-0">{item.label}</span>
                {item.isLink ? (
                  <Link href={`/kyc/${item.value}`} className="text-sm text-blue-600 hover:underline font-mono">
                    {item.value}
                  </Link>
                ) : item.isGreen ? (
                  <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">
                    {item.value}
                  </span>
                ) : (
                  <span className="text-sm text-slate-700 font-mono break-all">{item.value}</span>
                )}
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