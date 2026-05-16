"use client";

import type {
  AdminVpRequestDetail,
  AdminVpRequestStatus,
} from "@/lib/api/admin-vp-request";
import { cn } from "@/lib/utils";

type VpRequestInfoCardProps = {
  detail: AdminVpRequestDetail;
  status: AdminVpRequestStatus;
};

const requestStatusLabel: Record<AdminVpRequestStatus, string> = {
  REQUESTED: "대기중",
  PRESENTED: "제출 완료",
  VALID: "검증 완료",
  INVALID: "검증 완료",
  REPLAY_SUSPECTED: "검증 실패",
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
  REPLAY_SUSPECTED: {
    label: "재사용 의심",
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

function displayPurpose(purpose: string) {
  return purpose === "ACCOUNT_OPENING" ? "KYC 인증 확인" : purpose;
}

function formatClaims(claims: string[]) {
  return claims.length > 0 ? "KYC VC" : "-";
}

export function VpRequestInfoCard({
  detail,
  status,
}: VpRequestInfoCardProps) {
  const verificationStatus = verificationStatusMeta[status];

  return (
    <section className="rounded-lg border border-slate-200 bg-white">
      <div className="border-b border-slate-100 p-5">
        <p className="text-xs text-slate-400">Request Detail</p>
        <h2 className="text-lg font-semibold text-slate-800">요청 정보</h2>
      </div>

      <div className="divide-y divide-slate-100">
        <InfoRow label="요청 목적">
          <span>{displayPurpose(detail.purpose)}</span>
          {detail.purpose !== "ACCOUNT_OPENING" ? null : (
            <span className="ml-2 font-mono text-xs text-slate-400">
              ACCOUNT_OPENING
            </span>
          )}
        </InfoRow>
        <InfoRow label="요청 Credential">
          <div className="flex flex-wrap items-center gap-2">
            <span>{formatClaims(detail.requestedClaims)}</span>
            {detail.requestedClaims.slice(0, 3).map((claim) => (
              <span
                key={claim}
                className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] text-slate-500"
              >
                {claim}
              </span>
            ))}
          </div>
        </InfoRow>
        <InfoRow label="요청 상태">
          <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700">
            {requestStatusLabel[status]}
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
            {formatDateTime(detail.submittedAt ?? null)}
          </span>
        </InfoRow>
        <InfoRow label="검증 일시">
          <span className="font-mono text-xs">
            {formatDateTime(detail.verifiedAt ?? null)}
          </span>
        </InfoRow>
        <InfoRow label="만료 일시">
          <span className="font-mono text-xs">
            {formatDateTime(detail.expiresAt)}
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
      </div>
    </section>
  );
}
