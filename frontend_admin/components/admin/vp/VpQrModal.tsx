"use client";

import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Loader2,
  QrCode,
  XCircle,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";

import type { AdminVpRequestStatus } from "@/lib/api/admin-vp-request";

import { VpQrPattern } from "./VpQrPattern";

type VpQrModalProps = {
  open: boolean;
  qrPayload: string;
  status: AdminVpRequestStatus;
  onClose: () => void;
};

const modalStatusMeta: Record<
  AdminVpRequestStatus,
  {
    title: string;
    description: string;
    icon: typeof QrCode;
    iconClassName: string;
    iconWrapClassName: string;
    showQr: boolean;
  }
> = {
  REQUESTED: {
    title: "VP 요청 QR 코드",
    description: "모바일 Wallet에서 QR 코드를 스캔하세요.",
    icon: QrCode,
    iconClassName: "text-blue-600",
    iconWrapClassName: "bg-blue-50",
    showQr: true,
  },
  PRESENTED: {
    title: "VP 제출 완료",
    description: "VP 제출이 완료되어 검증 결과를 확인하는 중입니다.",
    icon: Loader2,
    iconClassName: "text-indigo-600",
    iconWrapClassName: "bg-indigo-50",
    showQr: false,
  },
  VALID: {
    title: "VP 인증 완료",
    description: "VP 검증이 성공적으로 완료되었습니다.",
    icon: CheckCircle2,
    iconClassName: "text-green-600",
    iconWrapClassName: "bg-green-50",
    showQr: false,
  },
  INVALID: {
    title: "VP 인증 실패",
    description: "VP 검증에 실패했습니다. 다시 요청해주세요.",
    icon: XCircle,
    iconClassName: "text-red-600",
    iconWrapClassName: "bg-red-50",
    showQr: false,
  },
  REPLAY_SUSPECTED: {
    title: "VP 인증 실패",
    description: "재사용 또는 nonce 불일치가 의심됩니다. 다시 요청해주세요.",
    icon: AlertTriangle,
    iconClassName: "text-red-600",
    iconWrapClassName: "bg-red-50",
    showQr: false,
  },
  EXPIRED: {
    title: "QR 요청 만료",
    description: "QR 코드가 만료되었습니다. 다시 요청해주세요.",
    icon: Clock3,
    iconClassName: "text-orange-600",
    iconWrapClassName: "bg-orange-50",
    showQr: false,
  },
  CANCELLED: {
    title: "VP 요청 취소",
    description: "VP 요청이 취소되었습니다. 다시 요청해주세요.",
    icon: XCircle,
    iconClassName: "text-slate-500",
    iconWrapClassName: "bg-slate-100",
    showQr: false,
  },
};

export function VpQrModal({
  open,
  qrPayload,
  status,
  onClose,
}: VpQrModalProps) {
  const [mounted, setMounted] = useState(false);
  const meta = useMemo(() => modalStatusMeta[status], [status]);
  const Icon = meta.icon;

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!open || !mounted) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open, mounted]);

  if (!open || !mounted) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="vp-request-qr-title"
      aria-describedby="vp-request-qr-description"
      className="fixed inset-0 z-[1100] flex min-h-dvh items-center justify-center overflow-y-auto bg-slate-950/70 p-6 backdrop-blur-lg"
      onClick={onClose}
    >
      <div
        className="my-6 w-[min(92vw,400px)] rounded-lg bg-white p-6 text-center shadow-[0_28px_90px_rgba(15,23,42,0.35)]"
        onClick={(event) => event.stopPropagation()}
      >
        <div
          className={`mx-auto mb-4 flex size-14 items-center justify-center rounded-full ${meta.iconWrapClassName}`}
        >
          <Icon
            size={30}
            className={`${meta.iconClassName} ${status === "PRESENTED" ? "animate-spin" : ""}`}
          />
        </div>

        <h2
          id="vp-request-qr-title"
          className="text-xl font-extrabold tracking-normal text-slate-900"
        >
          {meta.title}
        </h2>
        <p
          id="vp-request-qr-description"
          aria-live="polite"
          className="mb-5 mt-2 min-h-[24px] text-sm leading-6 text-slate-500"
        >
          {meta.description}
        </p>

        {meta.showQr ? (
          <div className="flex min-h-64 items-center justify-center rounded-lg border border-slate-200 bg-white p-4">
            <VpQrPattern qrPayload={qrPayload} className="w-56" size={224} />
          </div>
        ) : (
          <div className="flex min-h-64 flex-col items-center justify-center rounded-lg border border-slate-200 bg-slate-50 px-6 py-8">
            <Icon
              size={56}
              className={`${meta.iconClassName} ${status === "PRESENTED" ? "animate-spin" : ""}`}
            />
            <p className="mt-4 text-sm font-medium text-slate-700">
              {meta.description}
            </p>
          </div>
        )}

        <button
          type="button"
          className="mt-5 h-11 w-full rounded-lg bg-slate-950 text-sm font-bold text-white transition-colors hover:bg-slate-800"
          onClick={onClose}
        >
          닫기
        </button>
      </div>
    </div>,
    document.body
  );
}
