"use client";

import { CheckCircle2, XCircle } from "lucide-react";
import { useState } from "react";

import type {
  AdminVpSubmittedClaim,
  AdminVpRequestSession,
  AdminVpRequestStatus,
} from "@/lib/api/admin-vp-request";
import { getMockAdminVpSubmittedClaims } from "@/lib/api/admin-vp-request";
import { cn } from "@/lib/utils";

import { VpClaimsModal } from "./VpClaimsModal";
import { VpStatusDevControls } from "./VpStatusDevControls";

type VpVerificationResultViewProps = {
  session: AdminVpRequestSession;
  onStatusChange: (status: AdminVpRequestStatus) => void;
};

const resultStatusMeta = {
  VALID: {
    label: "VP 검증 성공",
    description: "KYC 인증이 유효합니다.",
    className: "border-green-200 bg-green-50 text-green-700",
    icon: CheckCircle2,
  },
  INVALID: {
    label: "VP 검증 실패",
    description: "검증 항목을 확인해 주세요.",
    className: "border-red-200 bg-red-50 text-red-700",
    icon: XCircle,
  },
} as const;

const verificationStatusLabel: Record<AdminVpRequestStatus, string> = {
  REQUESTED: "VP 제출 대기중",
  PRESENTED: "VP 제출 완료 / 검증중",
  VALID: "VP 검증 성공",
  INVALID: "VP 검증 실패",
  EXPIRED: "QR 만료",
  CANCELLED: "요청 취소",
};

const requestStatusLabel: Record<AdminVpRequestStatus, string> = {
  REQUESTED: "대기중",
  PRESENTED: "제출 완료",
  VALID: "검증 완료",
  INVALID: "검증 완료",
  EXPIRED: "만료",
  CANCELLED: "취소",
};

function formatDateTime(value: string | null) {
  if (!value) return "-";

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function SummaryRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-[104px_1fr] gap-3 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="min-w-0 font-medium text-slate-800">{value}</dd>
    </div>
  );
}

function ResultBadge({
  children,
  tone = "green",
}: {
  children: React.ReactNode;
  tone?: "green" | "red";
}) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full border px-2.5 py-1 text-xs font-medium",
        tone === "green"
          ? "border-green-200 bg-green-50 text-green-700"
          : "border-red-200 bg-red-50 text-red-700"
      )}
    >
      {children}
    </span>
  );
}

export function VpVerificationResultView({
  session,
  onStatusChange,
}: VpVerificationResultViewProps) {
  const [claimsModalOpen, setClaimsModalOpen] = useState(false);
  const [submittedClaims, setSubmittedClaims] = useState<
    AdminVpSubmittedClaim[]
  >([]);
  const isValid = session.status === "VALID";
  const meta = isValid ? resultStatusMeta.VALID : resultStatusMeta.INVALID;
  const Icon = meta.icon;
  const resultRows = [
    { label: "VP 형식 검증", value: isValid ? "통과" : "실패" },
    { label: "VC 서명 검증", value: isValid ? "유효" : "서명 불일치" },
    { label: "VC 상태 조회", value: isValid ? "유효" : "확인 필요" },
    { label: "Issuer 신뢰 확인", value: isValid ? "화이트리스트 포함" : "확인 필요" },
    { label: "Nonce 검증", value: isValid ? "일치" : "불일치" },
  ];

  const handleClaimsOpen = async () => {
    const claims = await getMockAdminVpSubmittedClaims();

    setSubmittedClaims(claims);
    setClaimsModalOpen(true);
  };

  return (
    <>
      <div className="grid gap-4 lg:grid-cols-[240px_1fr]">
        <aside className="space-y-3">
          <section className="rounded-lg border border-slate-200 bg-white p-4">
            <h2 className="mb-4 text-base font-semibold text-slate-800">
              VP 요청 정보
            </h2>
            <dl className="space-y-3">
              <SummaryRow
                label="요청 목적"
                value={session.payload.purpose}
              />
              <SummaryRow label="요청 Credential" value="KYC VC" />
              <SummaryRow
                label="요청 상태"
                value={requestStatusLabel[session.status]}
              />
              <SummaryRow
                label="검증 상태"
                value={verificationStatusLabel[session.status]}
              />
              <SummaryRow
                label="제출 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(session.submittedAt)}
                  </span>
                }
              />
              <SummaryRow
                label="검증 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(session.verifiedAt)}
                  </span>
                }
              />
            </dl>
          </section>

          <button
            type="button"
            className="w-full rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white"
            onClick={handleClaimsOpen}
          >
            VP 검증 상세
          </button>
          <button
            type="button"
            className="w-full rounded border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600"
          >
            법인 고객 연결
          </button>
        </aside>

        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <div
            className={cn(
              "flex min-h-[68px] items-center justify-center rounded-lg border px-4 py-5 text-center",
              meta.className
            )}
          >
            <div>
              <div className="flex items-center justify-center gap-2 text-lg font-semibold">
                <Icon size={20} />
                {meta.label}
              </div>
              <p className="mt-1 text-sm">{meta.description}</p>
            </div>
          </div>

          <div className="mt-5">
            <p className="mb-2 text-sm font-medium text-slate-500">
              검증 결과 상세
            </p>
            <div className="overflow-hidden rounded-lg border border-slate-200">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50">
                    <th className="px-4 py-3 text-left font-medium text-slate-500">
                      검증 항목
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-slate-500">
                      결과
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {resultRows.map((row) => (
                    <tr
                      key={row.label}
                      className="border-b border-slate-100 last:border-0"
                    >
                      <td className="px-4 py-3 text-slate-700">
                        {row.label}
                      </td>
                      <td className="px-4 py-3">
                        <ResultBadge tone={isValid ? "green" : "red"}>
                          {row.value}
                        </ResultBadge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="mt-3 rounded-lg bg-slate-50 px-4 py-3">
            <dl className="grid gap-2 text-sm sm:grid-cols-2">
              <SummaryRow label="요청 목적" value={session.payload.purpose} />
              <SummaryRow label="요청 Credential" value="KYC VC" />
              <SummaryRow
                label="VP 요청 ID"
                value={
                  <span className="font-mono text-xs">
                    {session.payload.vpRequestId}
                  </span>
                }
              />
              <SummaryRow
                label="검증 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(session.verifiedAt)}
                  </span>
                }
              />
            </dl>
          </div>

          <div className="mt-4">
            <VpStatusDevControls
              status={session.status}
              onStatusChange={onStatusChange}
            />
          </div>
        </section>
      </div>

      <VpClaimsModal
        open={claimsModalOpen}
        claims={submittedClaims}
        onClose={() => setClaimsModalOpen(false)}
      />
    </>
  );
}
