"use client";

import { getVcDetail, type VcDetail } from "@/lib/api/vc";
import {
  kycDetailPath,
  vcReissuePath,
  vcRevokePath,
} from "@/lib/navigation/admin-routes";
import Link from "next/link";
import { use, useEffect, useState } from "react";

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  폐기: "bg-red-100 text-red-600",
  만료: "bg-slate-100 text-slate-500",
  보류: "bg-orange-100 text-orange-600",
};

export default function VcDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = use(params);
  const id = decodeURIComponent(rawId);
  const [vc, setVc] = useState<VcDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;

    const fetchDetail = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getVcDetail(id);
        if (alive) setVc(data);
      } catch (err) {
        if (alive) setError(err instanceof Error ? err.message : "VC 상세 정보를 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    };

    fetchDetail();
    return () => {
      alive = false;
    };
  }, [id]);

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

      {loading ? (
        <div className="bg-white rounded-lg border border-slate-200 p-8 text-center text-slate-500">로딩 중...</div>
      ) : error ? (
        <div className="bg-red-50 rounded-lg border border-red-200 p-5 text-sm text-red-600">{error}</div>
      ) : vc ? (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 space-y-3">
            <div className="bg-white rounded-lg border border-slate-200 p-4 space-y-3">
              <h2 className="text-xs font-semibold text-slate-500">VC 발급 상태 조회</h2>
              {[
                { label: "법인명", value: vc.corp },
                { label: "발급 상태", value: vc.status, isBadge: true },
                { label: "발급 요청일", value: vc.issuedAt },
                { label: "발급 완료일", value: vc.issuedAt },
              ].map((item) => (
                <div key={item.label}>
                  <p className="text-xs text-slate-400">{item.label}</p>
                  {item.isBadge ? (
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium mt-0.5 inline-block ${statusBadge[item.value] ?? "bg-slate-100 text-slate-500"}`}>
                      {item.value}
                    </span>
                  ) : (
                    <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
                  )}
                </div>
              ))}
            </div>

            <Link
              href={vcReissuePath(id)}
              className="block w-full text-center border border-slate-200 text-slate-600 py-2 rounded-lg text-sm hover:bg-slate-50 transition-colors"
            >
              VC 재발급 요청
            </Link>
            <Link
              href={vcRevokePath(id)}
              className="block w-full text-center border border-red-200 text-red-500 py-2 rounded-lg text-sm hover:bg-red-50 transition-colors"
            >
              VC 폐기 요청
            </Link>
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200">
            <div className="px-5 py-4 border-b border-slate-100">
              <h2 className="text-sm font-semibold text-slate-700">VC 발급 상세 정보</h2>
            </div>
            <div className="p-5 space-y-0">
              {[
                { label: "Credential ID", value: vc.credentialId },
                { label: "Credential Type", value: vc.credentialType },
                { label: "Issuer DID", value: vc.issuerDid },
                { label: "Holder DID", value: vc.holderDid },
                { label: "발급일", value: vc.issuedAt },
                { label: "만료일", value: vc.expiresAt },
                { label: "XRPL Tx Hash", value: vc.xrplTxHash },
                { label: "모바일 저장 여부", value: vc.mobileStored, isGreen: vc.mobileStored === "저장 완료" },
                { label: "연결 KYC 신청", value: vc.kyc, isLink: vc.kyc !== "-" },
              ].map((item) => (
                <div key={item.label} className="flex items-center border-b border-slate-50 py-3 last:border-0">
                  <span className="text-sm text-slate-400 w-40 shrink-0">{item.label}</span>
                  {item.isLink ? (
                    <Link href={kycDetailPath(item.value)} className="text-sm text-blue-600 hover:underline font-mono">
                      {item.value}
                    </Link>
                  ) : item.isGreen ? (
                    <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">{item.value}</span>
                  ) : (
                    <span className="text-sm text-slate-700 font-mono break-all">{item.value}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
