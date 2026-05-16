"use client";

import type { AdminVpRequestPayload } from "@/lib/api/admin-vp-request";
import { cn } from "@/lib/utils";

type VpQrPatternProps = {
  payload: AdminVpRequestPayload;
  className?: string;
  inactive?: boolean;
};

const QR_SIZE = 21;

function getFinderCell(row: number, col: number): boolean | null {
  const zones = [
    { row: 0, col: 0 },
    { row: 0, col: QR_SIZE - 7 },
    { row: QR_SIZE - 7, col: 0 },
  ];

  for (const zone of zones) {
    const localRow = row - zone.row;
    const localCol = col - zone.col;
    const inZone =
      localRow >= 0 && localRow < 7 && localCol >= 0 && localCol < 7;

    if (!inZone) continue;

    const outer =
      localRow === 0 || localRow === 6 || localCol === 0 || localCol === 6;
    const center =
      localRow >= 2 && localRow <= 4 && localCol >= 2 && localCol <= 4;

    return outer || center;
  }

  return null;
}

function buildQrCells(payload: AdminVpRequestPayload) {
  const source = JSON.stringify(payload);

  return Array.from({ length: QR_SIZE * QR_SIZE }, (_, index) => {
    const row = Math.floor(index / QR_SIZE);
    const col = index % QR_SIZE;
    const finderCell = getFinderCell(row, col);

    if (finderCell !== null) return finderCell;

    const charCode = source.charCodeAt(index % source.length);
    return (charCode + row * 7 + col * 13 + index * 17) % 5 < 2;
  });
}

export function VpQrPattern({
  payload,
  className,
  inactive = false,
}: VpQrPatternProps) {
  const cells = buildQrCells(payload);

  return (
    <div
      aria-label="VP 요청 QR"
      className={cn(
        "grid aspect-square grid-cols-[repeat(21,minmax(0,1fr))] gap-0.5 transition-opacity",
        inactive && "opacity-55",
        className
      )}
    >
      {cells.map((filled, index) => (
        <span
          key={index}
          className={cn(
            "aspect-square rounded-[1px]",
            filled ? "bg-slate-950" : "bg-white"
          )}
        />
      ))}
    </div>
  );
}
