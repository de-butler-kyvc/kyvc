"use client";

import { getVcDetail, requestVcReissue, type VcDetail } from "@/lib/api/vc";
import { vcDetailPath } from "@/lib/navigation/admin-routes";
import Link from "next/link";
import { use, useEffect, useState } from "react";

export default function VcReissuePage({ params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = use(params);
  const id = decodeURIComponent(rawId);
  const [vc, setVc] = useState<VcDetail | null>(null);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;

    const fetchDetail = async () => {
      setDetailLoading(true);
      setError("");
      try {
        const data = await getVcDetail(id);
        if (alive) setVc(data);
      } catch (err) {
        if (alive) setError(err instanceof Error ? err.message : "VC 정보를 불러오지 못했습니다.");
      } finally {
        if (alive) setDetailLoading(false);
      }
    };

    fetchDetail();
    return () => {
      alive = false;
    };
  }, [id]);

  const handleSubmit = async () => {
    if (!reason.trim()) return;
    setLoading(true);
    setError("");
    try {
      await requestVcReissue(id, reason.trim());
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "재발급 요청 처리에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            증명서 관리자 · <Link href="/vc" className="hover:underline">VC 관리</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">VC 재발급 요청</h1>
        </div>
      </div>

      {success ? (
        <div className="flex items-center justify-center py-20">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-12 text-center max-w-md w-full">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-5">
              <span className="text-green-600 text-2xl font-bold leading-none">OK</span>
            </div>
            <h2 className="text-lg font-bold text-slate-800 mb-2">재발급 요청 완료</h2>
            <p className="text-sm text-slate-500 mb-1">VC 재발급 요청이 정상적으로 접수되었습니다.</p>
            <p className="text-xs text-slate-400 mb-8">검토 후 새로운 VC가 발급되며 기존 VC는 자동 폐기됩니다.</p>
            <Link
              href={vcDetailPath(id)}
              className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 inline-block transition-colors"
            >
              VC 상세로 돌아가기
            </Link>
          </div>
        </div>
      ) : detailLoading ? (
        <div className="bg-white rounded-lg border border-slate-200 p-8 text-center text-slate-500">로딩 중...</div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">VC 재발급 요청</h2>
            {[
              { label: "Credential ID", value: vc?.credentialId ?? id },
              { label: "법인명", value: vc?.corp ?? "-" },
              { label: "Credential Type", value: vc?.credentialType ?? "-" },
              { label: "발급일", value: vc?.issuedAt ?? "-" },
              { label: "만료일", value: vc?.expiresAt ?? "-" },
            ].map((item) => (
              <div key={item.label}>
                <p className="text-xs text-slate-400">{item.label}</p>
                <p className="text-slate-700 text-xs font-medium mt-0.5 break-all">{item.value}</p>
              </div>
            ))}
            <div>
              <p className="text-xs text-slate-400">현재 상태</p>
              <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">{vc?.status ?? "-"}</span>
            </div>
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-6">
            {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">현재 VC 정보</h2>
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-4 space-y-2 text-sm">
                {[
                  { label: "Credential ID", value: vc?.credentialId ?? id },
                  { label: "Credential Type", value: vc?.credentialType ?? "-" },
                  { label: "Issuer DID", value: vc?.issuerDid ?? "-" },
                  { label: "Holder DID", value: vc?.holderDid ?? "-" },
                  { label: "XRPL Tx Hash", value: vc?.xrplTxHash ?? "-" },
                ].map((item) => (
                  <div key={item.label} className="flex gap-4">
                    <span className="text-slate-400 text-xs w-28 shrink-0">{item.label}</span>
                    <span className="text-slate-700 text-xs font-mono break-all">{item.value}</span>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">재발급 사유 <span className="text-red-500">*</span></h2>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={4}
                placeholder="재발급이 필요한 사유를 입력해주세요. (예: 정보 변경, 만료 임박, 분실 등)"
                className="w-full border border-slate-200 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
              />
            </div>

            <div className="bg-orange-50 border border-orange-200 rounded-lg p-4 text-sm text-orange-700">
              <p className="font-medium mb-1">재발급 안내</p>
              <p className="text-xs leading-relaxed">재발급 시 기존 VC는 자동으로 폐기되며, 새로운 VC가 발급됩니다. 사용자 Wallet의 VC도 갱신됩니다.</p>
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-slate-400">요청자: 현재 로그인 관리자</p>
              <div className="flex gap-2">
                <Link href={vcDetailPath(id)} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
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
    </div>
  );
}
