"use client";

import { QrCode, RefreshCcw, XCircle } from "lucide-react";

import type {
  AdminVpRequestDetail,
  AdminVpRequestStatus,
} from "@/lib/api/admin-vp-request";
import { cn } from "@/lib/utils";

import { VpQrPattern } from "./VpQrPattern";

type VpRequestQrCardProps = {
  detail: AdminVpRequestDetail;
  qrPayload: string;
  remainingSeconds: number;
  status: AdminVpRequestStatus;
  onRegenerate: () => void;
  onCancel: () => void;
  onQrClick: () => void;
  regenerating?: boolean;
  cancelling?: boolean;
};

function formatRemainingTime(seconds: number) {
  const safeSeconds = Math.max(0, seconds);
  const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, "0");
  const restSeconds = String(safeSeconds % 60).padStart(2, "0");

  return `${minutes}:${restSeconds}`;
}

export function VpRequestQrCard({
  detail,
  qrPayload,
  remainingSeconds,
  status,
  onRegenerate,
  onCancel,
  onQrClick,
  regenerating = false,
  cancelling = false,
}: VpRequestQrCardProps) {
  const cancelDisabled =
    status === "CANCELLED" ||
    status === "EXPIRED" ||
    status === "VALID" ||
    status === "INVALID" ||
    status === "REPLAY_SUSPECTED" ||
    cancelling;
  const inactive =
    cancelDisabled || (status !== "REQUESTED" && status !== "PRESENTED");
  const remainingLabel =
    status === "CANCELLED"
      ? "취소됨"
      : status === "EXPIRED"
        ? "만료됨"
        : formatRemainingTime(remainingSeconds);

  return (
    <section className="w-full max-w-[230px] rounded-lg border border-slate-200 bg-white">
      <div className="flex items-center justify-between border-b border-slate-100 p-4">
        <div>
          <p className="text-xs text-slate-400">VP Request</p>
          <h2 className="text-base font-semibold text-slate-800">VP 요청 QR</h2>
        </div>
        <div className="rounded-full bg-blue-50 p-2 text-blue-600">
          <QrCode size={18} />
        </div>
      </div>

      <div className="space-y-4 p-4">
        <button
          type="button"
          onClick={onQrClick}
          className={cn(
            "block w-full rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition-colors hover:border-blue-300 focus:outline-none focus:ring-2 focus:ring-blue-500/30",
            inactive && "border-slate-300"
          )}
          aria-label="VP 요청 QR 크게 보기"
        >
          <VpQrPattern
            qrPayload={qrPayload}
            inactive={inactive}
            className="mx-auto w-full max-w-[144px]"
          />
        </button>

        <div className="rounded-lg bg-slate-50 px-3 py-2.5">
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs font-medium text-slate-600">
              남은 유효시간
            </span>
            <span
              className={cn(
                "font-mono text-sm font-semibold",
                inactive ? "text-slate-400" : "text-blue-600"
              )}
            >
              {remainingLabel}
            </span>
          </div>
          <div className="mt-2 truncate font-mono text-[11px] text-slate-400">
            {detail.requestId}
          </div>
        </div>

        <div className="grid gap-2">
          <button
            type="button"
            onClick={onRegenerate}
            disabled={regenerating}
            className="inline-flex items-center justify-center gap-2 rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          >
            <RefreshCcw size={16} />
            {regenerating ? "생성 중..." : "QR 재생성"}
          </button>
          <button
            type="button"
            onClick={onCancel}
            disabled={cancelDisabled}
            className="inline-flex items-center justify-center gap-2 rounded border border-red-200 px-4 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <XCircle size={16} />
            {cancelling ? "취소 중..." : "요청 취소"}
          </button>
        </div>
      </div>
    </section>
  );
}
