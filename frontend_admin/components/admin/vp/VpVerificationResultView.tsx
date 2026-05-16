"use client";

import { AlertTriangle, CheckCircle2, XCircle } from "lucide-react";
import { useMemo, useState } from "react";

import type {
  AdminVpRequestCheck,
  AdminVpRequestDetail,
  AdminVpRequestStatus,
  AdminVpSubmittedClaim,
} from "@/lib/api/admin-vp-request";
import { cn } from "@/lib/utils";

import { VpClaimsModal } from "./VpClaimsModal";

type VpVerificationResultViewProps = {
  detail: AdminVpRequestDetail;
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
  REPLAY_SUSPECTED: {
    label: "VP 검증 실패",
    description: "재사용 또는 nonce 불일치가 의심됩니다.",
    className: "border-red-200 bg-red-50 text-red-700",
    icon: AlertTriangle,
  },
} as const;

const verificationStatusLabel: Record<AdminVpRequestStatus, string> = {
  REQUESTED: "VP 제출 대기중",
  PRESENTED: "VP 제출 완료 / 검증중",
  VALID: "VP 검증 성공",
  INVALID: "VP 검증 실패",
  REPLAY_SUSPECTED: "재사용 의심",
  EXPIRED: "QR 만료",
  CANCELLED: "요청 취소",
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

function formatDateTime(value?: string | null) {
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

function displayPurpose(purpose: string) {
  return purpose === "ACCOUNT_OPENING" ? "KYC 인증 확인" : purpose;
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

function ResultBadge({ resultCode }: { resultCode: string }) {
  const tone =
    resultCode === "PASSED"
      ? "green"
      : resultCode === "FAILED"
        ? "red"
        : "orange";

  return (
    <span
      className={cn(
        "inline-flex rounded-full border px-2.5 py-1 text-xs font-medium",
        tone === "green" &&
          "border-green-200 bg-green-50 text-green-700",
        tone === "red" && "border-red-200 bg-red-50 text-red-700",
        tone === "orange" &&
          "border-orange-200 bg-orange-50 text-orange-700"
      )}
    >
      {resultCode}
    </span>
  );
}

function fallbackChecks(status: AdminVpRequestStatus): AdminVpRequestCheck[] {
  const isValid = status === "VALID";
  const resultCode = isValid ? "PASSED" : "CHECK_REQUIRED";
  const message = isValid
    ? "검증이 정상 처리되었습니다."
    : "상세 검증 항목 확인이 필요합니다.";

  return [
    "VP 형식 검증",
    "VC 서명 검증",
    "VC 상태 조회",
    "Issuer 신뢰 확인",
    "Nonce 검증",
  ].map((checkName) => ({
    checkType: checkName,
    checkName,
    resultCode,
    message,
  }));
}

function buildSubmittedClaims(
  detail: AdminVpRequestDetail
): AdminVpSubmittedClaim[] {
  const result = detail.result;

  return [
    { label: "법인명", value: result?.corporateName ?? "-", source: "KYC VC" },
    {
      label: "사업자등록번호",
      value: result?.businessRegistrationNo ?? "-",
      source: "KYC VC",
    },
    {
      label: "법인번호",
      value: result?.corporateRegistrationNo ?? "-",
      source: "KYC VC",
    },
    {
      label: "대표자명",
      value: result?.representativeName ?? "-",
      source: "KYC VC",
    },
    { label: "KYC 상태", value: result?.kycStatus ?? "-", source: "KYC VC" },
    {
      label: "VC 상태",
      value: result?.credentialStatus ?? "-",
      source: "KYC VC",
    },
    {
      label: "VC 발급일",
      value: formatDateTime(result?.credentialIssuedAt),
      source: "KYC VC",
    },
    {
      label: "VC 만료일",
      value: formatDateTime(result?.credentialExpiresAt),
      source: "KYC VC",
    },
  ];
}

export function VpVerificationResultView({
  detail,
}: VpVerificationResultViewProps) {
  const [claimsModalOpen, setClaimsModalOpen] = useState(false);
  const status =
    detail.status === "REPLAY_SUSPECTED" ? "REPLAY_SUSPECTED" : detail.status;
  const meta =
    status === "VALID"
      ? resultStatusMeta.VALID
      : status === "REPLAY_SUSPECTED"
        ? resultStatusMeta.REPLAY_SUSPECTED
        : resultStatusMeta.INVALID;
  const Icon = meta.icon;
  const checks =
    detail.checks && detail.checks.length > 0
      ? detail.checks
      : fallbackChecks(status);
  const submittedClaims = useMemo(() => buildSubmittedClaims(detail), [detail]);

  return (
    <>
      <div className="grid gap-4 lg:grid-cols-[240px_1fr]">
        <aside className="space-y-3">
          <section className="rounded-lg border border-slate-200 bg-white p-4">
            <h2 className="mb-4 text-base font-semibold text-slate-800">
              VP 요청 정보
            </h2>
            <dl className="space-y-3">
              <SummaryRow label="요청 목적" value={displayPurpose(detail.purpose)} />
              <SummaryRow label="요청 Credential" value="KYC VC" />
              <SummaryRow label="요청 상태" value={requestStatusLabel[status]} />
              <SummaryRow
                label="검증 상태"
                value={verificationStatusLabel[status]}
              />
              <SummaryRow
                label="제출 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(detail.submittedAt)}
                  </span>
                }
              />
              <SummaryRow
                label="검증 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(detail.verifiedAt)}
                  </span>
                }
              />
            </dl>
          </section>

          <button
            type="button"
            className="w-full rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white"
            onClick={() => setClaimsModalOpen(true)}
          >
            VP 검증 상세
          </button>
          <button
            type="button"
            disabled
            className="w-full rounded border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-400 disabled:cursor-not-allowed"
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
                    <th className="px-4 py-3 text-left font-medium text-slate-500">
                      상세 메시지
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {checks.map((check) => (
                    <tr
                      key={`${check.checkType}-${check.checkName}`}
                      className="border-b border-slate-100 last:border-0"
                    >
                      <td className="px-4 py-3 text-slate-700">
                        {check.checkName}
                      </td>
                      <td className="px-4 py-3">
                        <ResultBadge resultCode={check.resultCode} />
                      </td>
                      <td className="px-4 py-3 text-slate-500">
                        {check.message}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="mt-3 rounded-lg bg-slate-50 px-4 py-3">
            <dl className="grid gap-2 text-sm sm:grid-cols-2">
              <SummaryRow label="요청 목적" value={displayPurpose(detail.purpose)} />
              <SummaryRow label="요청 Credential" value="KYC VC" />
              <SummaryRow
                label="VP 요청 ID"
                value={
                  <span className="font-mono text-xs">{detail.requestId}</span>
                }
              />
              <SummaryRow
                label="법인명"
                value={detail.result?.corporateName ?? detail.corporateName ?? "-"}
              />
              <SummaryRow
                label="제출 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(detail.submittedAt)}
                  </span>
                }
              />
              <SummaryRow
                label="검증 일시"
                value={
                  <span className="font-mono text-xs">
                    {formatDateTime(detail.verifiedAt)}
                  </span>
                }
              />
            </dl>
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
