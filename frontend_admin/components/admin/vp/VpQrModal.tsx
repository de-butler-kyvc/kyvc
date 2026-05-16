"use client";

import type { AdminVpRequestPayload } from "@/lib/api/admin-vp-request";

import { VpQrPattern } from "./VpQrPattern";

type VpQrModalProps = {
  open: boolean;
  payload: AdminVpRequestPayload;
  statusText: string;
  onClose: () => void;
};

export function VpQrModal({
  open,
  payload,
  statusText,
  onClose,
}: VpQrModalProps) {
  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="vp-request-qr-title"
      className="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-900/65 p-6"
      onClick={onClose}
    >
      <div
        className="w-[min(92vw,380px)] rounded-lg bg-white p-6 text-center shadow-[0_24px_80px_rgba(15,23,42,0.22)]"
        onClick={(event) => event.stopPropagation()}
      >
        <h2
          id="vp-request-qr-title"
          className="text-xl font-extrabold tracking-normal text-slate-900"
        >
          VP 요청 QR 코드
        </h2>
        <p className="mb-5 mt-2 text-sm leading-6 text-slate-500">
          모바일 Wallet에서 QR 코드를 스캔하세요
        </p>

        <div className="flex min-h-64 items-center justify-center rounded-lg border border-slate-200 bg-white p-4">
          <VpQrPattern payload={payload} className="w-56" />
        </div>

        <p
          aria-live="polite"
          className="mb-[18px] mt-4 min-h-[22px] text-sm leading-6 text-slate-700"
        >
          {statusText}
        </p>

        <button
          type="button"
          className="h-11 w-full rounded-lg bg-slate-950 text-sm font-bold text-white transition-colors hover:bg-slate-800"
          onClick={onClose}
        >
          닫기
        </button>
      </div>
    </div>
  );
}
