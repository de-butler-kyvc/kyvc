"use client";

import type { AdminVpSubmittedClaim } from "@/lib/api/admin-vp-request";

type VpClaimsModalProps = {
  open: boolean;
  claims: AdminVpSubmittedClaim[];
  onClose: () => void;
};

export function VpClaimsModal({
  open,
  claims,
  onClose,
}: VpClaimsModalProps) {
  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="vp-claims-title"
      className="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-900/65 p-6"
      onClick={onClose}
    >
      <div
        className="w-[min(92vw,560px)] rounded-lg bg-white p-6 shadow-[0_24px_80px_rgba(15,23,42,0.22)]"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="text-center">
          <h2
            id="vp-claims-title"
            className="text-xl font-extrabold tracking-normal text-slate-900"
          >
            VP 검증 상세
          </h2>
          <p className="mb-5 mt-2 text-sm leading-6 text-slate-500">
            고객 Wallet에서 제출한 클레임 내용
          </p>
        </div>

        <div className="overflow-hidden rounded-lg border border-slate-200">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-4 py-3 text-left font-medium text-slate-500">
                  클레임
                </th>
                <th className="px-4 py-3 text-left font-medium text-slate-500">
                  값
                </th>
                <th className="px-4 py-3 text-left font-medium text-slate-500">
                  출처
                </th>
              </tr>
            </thead>
            <tbody>
              {claims.map((claim) => (
                <tr
                  key={claim.label}
                  className="border-b border-slate-100 last:border-0"
                >
                  <td className="px-4 py-3 text-slate-500">{claim.label}</td>
                  <td className="px-4 py-3 font-medium text-slate-800">
                    {claim.value}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500">
                    {claim.source}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <button
          type="button"
          className="mt-5 h-11 w-full rounded-lg bg-slate-950 text-sm font-bold text-white transition-colors hover:bg-slate-800"
          onClick={onClose}
        >
          닫기
        </button>
      </div>
    </div>
  );
}
