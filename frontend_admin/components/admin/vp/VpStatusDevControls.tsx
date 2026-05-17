"use client";

import type { AdminVpRequestStatus } from "@/lib/api/admin-vp-request";

type VpStatusDevControlsProps = {
  status: AdminVpRequestStatus;
  onStatusChange: (status: AdminVpRequestStatus) => void;
};

const devStatuses: { status: AdminVpRequestStatus; label: string }[] = [
  { status: "REQUESTED", label: "REQUESTED" },
  { status: "PRESENTED", label: "PRESENTED" },
  { status: "VALID", label: "VALID" },
  { status: "INVALID", label: "INVALID" },
  { status: "EXPIRED", label: "EXPIRED" },
  { status: "CANCELLED", label: "CANCELLED" },
];

export function VpStatusDevControls({
  status,
  onStatusChange,
}: VpStatusDevControlsProps) {
  return (
    <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-4">
      <p className="mb-3 text-xs font-medium text-slate-500">
        개발 확인용 상태 변경
      </p>
      <div className="flex flex-wrap gap-2">
        {devStatuses.map((item) => (
          <button
            key={item.status}
            type="button"
            onClick={() => onStatusChange(item.status)}
            className={`rounded border px-3 py-1.5 text-xs font-medium transition-colors ${
              status === item.status
                ? "border-blue-600 bg-blue-600 text-white"
                : "border-slate-200 bg-white text-slate-600 hover:bg-slate-100"
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>
    </div>
  );
}
