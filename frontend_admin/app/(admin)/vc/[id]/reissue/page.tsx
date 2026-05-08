"use client";
import { use, useState } from "react";
import Link from "next/link";

export default function VcReissuePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = () => {
    if (!reason.trim()) return;
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
    }, 800);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href="/vc" className="hover:underline">VC 관리</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">VC 재발급 요청</h1>
        </div>
      </div>

      {success ? (
        <div className="flex items-center justify-center py-20">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-12 text-center max-w-md w-full">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-5">
              <span className="text-green-600 text-3xl leading-none">✓</span>
            </div>
            <h2 className="text-lg font-bold text-slate-800 mb-2">재발급 요청 완료</h2>
            <p className="text-sm text-slate-500 mb-1">
              VC 재발급 요청이 정상적으로 접수되었습니다.
            </p>
            <p className="text-xs text-slate-400 mb-8">
              검토 후 새로운 VC가 발급되며 기존 VC는 자동 폐기됩니다.
            </p>
            <Link
              href={`/vc/${id}`}
              className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 inline-block transition-colors"
            >
              VC 상세로 돌아가기
            </Link>
          </div>
        </div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">VC 재발급 요청</h2>
            {[
              { label: "Credential ID", value: id },
              { label: "법인명", value: "주식회사 케이원" },
              { label: "Credential Type", value: "KYC VC" },
              { label: "발급일", value: "2025.05.02 14:00" },
              { label: "만료일", value: "2026.12.31" },
            ].map((item) => (
              <div key={item.label}>
                <p className="text-xs text-slate-400">{item.label}</p>
                <p className="text-slate-700 text-xs font-medium mt-0.5 break-all">{item.value}</p>
              </div>
            ))}
            <div>
              <p className="text-xs text-slate-400">현재 상태</p>
              <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">활성</span>
            </div>
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-6">
            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">현재 VC 정보</h2>
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-4 space-y-2 text-sm">
                {[
                  { label: "Credential ID", value: id },
                  { label: "Credential Type", value: "KYCVerifiableCredential" },
                  { label: "Issuer DID", value: "did:kyvc:issuer:001" },
                  { label: "Holder DID", value: "did:kyvc:holder:kim123" },
                  { label: "XRPL Tx Hash", value: "A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6" },
                ].map((item) => (
                  <div key={item.label} className="flex gap-4">
                    <span className="text-slate-400 text-xs w-28 shrink-0">{item.label}</span>
                    <span className="text-slate-700 text-xs font-mono break-all">{item.value}</span>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">
                재발급 사유 <span className="text-red-500">*</span>
              </h2>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={4}
                placeholder="재발급이 필요한 사유를 입력해주세요. (예: 정보 변경, 만료 임박, 분실 등)"
                className="w-full border border-slate-200 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
              />
            </div>

            <div className="bg-orange-50 border border-orange-200 rounded-lg p-4 text-sm text-orange-700">
              <p className="font-medium mb-1">⚠ 재발급 안내</p>
              <p className="text-xs leading-relaxed">
                재발급 시 기존 VC는 자동으로 폐기되며, 새로운 VC가 발급됩니다. 사용자 Wallet의 VC도 갱신됩니다.
              </p>
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-slate-400">요청자: 김심사 (admin@kyvc.kr)</p>
              <div className="flex gap-2">
                <Link
                  href={`/vc/${id}`}
                  className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
                >
                  취소
                </Link>
                <button
                  onClick={handleSubmit}
                  disabled={loading || !reason.trim()}
                  className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
                >
                  {loading ? "처리 중..." : "재발급 요청"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
