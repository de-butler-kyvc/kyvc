"use client";

import { QRCodeSVG } from "qrcode.react";

import { cn } from "@/lib/utils";

type VpQrPatternProps = {
  qrPayload: string;
  className?: string;
  inactive?: boolean;
  size?: number;
};

export function VpQrPattern({
  qrPayload,
  className,
  inactive = false,
  size = 144,
}: VpQrPatternProps) {
  return (
    <div
      aria-label="VP 요청 QR"
      className={cn(
        "flex aspect-square items-center justify-center rounded bg-white transition-opacity",
        inactive && "opacity-55",
        className
      )}
    >
      {qrPayload ? (
        <QRCodeSVG
          value={qrPayload}
          size={size}
          marginSize={1}
          level="M"
          className="h-full w-full"
        />
      ) : (
        <span className="text-xs font-medium text-slate-400">QR 없음</span>
      )}
    </div>
  );
}
