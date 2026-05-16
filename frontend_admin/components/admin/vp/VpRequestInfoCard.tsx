"use client";

import type {
  AdminVpRequestSession,
  AdminVpRequestStatus,
} from "@/lib/api/admin-vp-request";
import { cn } from "@/lib/utils";

import { VpStatusDevControls } from "./VpStatusDevControls";

type VpRequestInfoCardProps = {
  session: AdminVpRequestSession;
  onStatusChange: (status: AdminVpRequestStatus) => void;
};

const requestStatusLabel: Record<AdminVpRequestStatus, string> = {
  REQUESTED: "대기중",
  PRESENTED: "제출 완료",
  VALID: "검증 완료",
  INVALID: "검증 완료",
  EXPIRED: "만료",
  CANCELLED: "취소",
};

const verificationStatusMeta: Record<
  AdminVpRequestStatus,
  { label: string; className: string }
> = {
  REQUESTED: {
    label: "VP 제출 대기중",
    className: "bg-blue-50 text-blue-700 border-blue-100",
  },
  PRESENTED: {
    label: "VP 제출 완료 / 검증중",
    className: "bg-indigo-50 text-indigo-700 border-indigo-100",
  },
  VALID: {
    label: "VP 검증 성공",
    className: "bg-green-50 text-green-700 border-green-100",
  },
  INVALID: {
    label: "VP 검증 실패",
    className: "bg-red-50 text-red-700 border-red-100",
  },
  EXPIRED: {
    label: "QR 만료",
    className: "bg-orange-50 text-orange-700 border-orange-100",
  },
  CANCELLED: {
    label: "요청 취소",
    className: "bg-slate-100 text-slate-600 border-slate-200",
  },
};

function formatDateTime(value: string | null) {
  if (!value) return "-";

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function InfoRow({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-[140px_1fr] gap-4 px-5 py-4 text-sm">
      <dt className="font-medium text-slate-500">{label}</dt>
      <dd className="min-w-0 text-slate-800">{children}</dd>
    </div>
  );
}

export function VpRequestInfoCard({
  session,
  onStatusChange,
}: VpRequestInfoCardProps) {
  const verificationStatus = verificationStatusMeta[session.status];

  return (
    <section className="rounded-lg border border-slate-200 bg-white">
      <div className="border-b border-slate-100 p-5">
        <p className="text-xs text-slate-400">Request Detail</p>
        <h2 className="text-lg font-semibold text-slate-800">요청 정보</h2>
      </div>

      <div className="divide-y divide-slate-100">
        <InfoRow label="요청 목적">{session.payload.purpose}</InfoRow>
        <InfoRow label="요청 Credential">KYC VC</InfoRow>
        <InfoRow label="요청 상태">
          <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700">
            {requestStatusLabel[session.status]}
          </span>
        </InfoRow>
        <InfoRow label="검증 상태">
          <span
            className={cn(
              "inline-flex rounded-full border px-2.5 py-1 text-xs font-medium",
              verificationStatus.className
            )}
          >
            {verificationStatus.label}
          </span>
        </InfoRow>
        <InfoRow label="제출 일시">
          <span className="font-mono text-xs">
            {formatDateTime(session.submittedAt)}
          </span>
        </InfoRow>
        <InfoRow label="검증 일시">
          <span className="font-mono text-xs">
            {formatDateTime(session.verifiedAt)}
          </span>
        </InfoRow>
      </div>

      <div className="space-y-4 border-t border-slate-100 p-5">
        <div className="rounded-lg bg-blue-50 px-4 py-3 text-sm text-blue-800">
          <p>고객에게 모바일 Wallet으로 왼쪽 QR을 스캔하도록 안내하세요.</p>
          <p className="mt-1">
            VP 제출이 완료되면 검증 결과가 자동으로 표시됩니다.
          </p>
        </div>

        <VpStatusDevControls
          status={session.status}
          onStatusChange={onStatusChange}
        />
      </div>
    </section>
  );
}
