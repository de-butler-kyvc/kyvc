import {
  getFinanceVpRequestDetail,
  getFinanceVpRequestList,
  type AdminVpRequestDetail,
  type AdminVpRequestStatus,
  type AdminVpRequestSummary,
} from "@/lib/api/admin-vp-request";

// ── 타입 ──────────────────────────────────────────────────────

export interface VpVerification {
  verificationId: string;
  corporationName?: string;
  verifierName?: string;
  purpose?: string;
  credentialId?: string;
  result?: string;
  failReason?: string;
  createdAt?: string;
}

export interface VpVerificationDetail extends VpVerification {
  holderDid?: string;
  verifierDid?: string;
  vpJson?: string;
  requestedAt?: string;
  respondedAt?: string;
}

function fmtDt(iso?: string | null) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

function displayPurpose(purpose?: string | null) {
  if (!purpose) return "-";
  return purpose === "ACCOUNT_OPENING" ? "KYC 인증 확인" : purpose;
}

function statusToResult(status?: string | null) {
  if (status === "VALID") return "성공";
  if (status === "INVALID" || status === "REPLAY_SUSPECTED") return "실패";
  if (status === "EXPIRED") return "만료";
  return status ?? "-";
}

function resultFilterToStatus(
  result?: string
): AdminVpRequestStatus | undefined {
  if (result === "성공") return "VALID";
  if (result === "실패") return "INVALID";
  if (result === "만료") return "EXPIRED";
  return undefined;
}

function toRow(summary: AdminVpRequestSummary) {
  return {
    id: summary.requestId,
    corp: summary.corporateName ?? "-",
    verifier: "KYvC Finance",
    purpose: displayPurpose(summary.purpose),
    vc:
      summary.requestedClaims && summary.requestedClaims.length > 0
        ? "KYC VC"
        : "-",
    result: statusToResult(summary.status),
    reason: "-",
    date: fmtDt(summary.verifiedAt ?? summary.createdAt ?? summary.requestedAt),
  };
}

function matchesSearch(
  row: ReturnType<typeof toRow>,
  search?: string
) {
  const keyword = search?.trim().toLowerCase();
  if (!keyword) return true;

  return [row.id, row.corp, row.verifier, row.purpose, row.vc]
    .join(" ")
    .toLowerCase()
    .includes(keyword);
}

// ── VP 검증 API ────────────────────────────────────────────────

/** GET /api/finance/verifier/vp-requests */
export async function getVpList(filters?: {
  search?: string;
  result?: string;
  verifierId?: string;
  from?: string;
  to?: string;
}): Promise<
  {
    id: string;
    corp: string;
    verifier: string;
    purpose: string;
    vc: string;
    result: string;
    reason: string;
    date: string;
  }[]
> {
  const response = await getFinanceVpRequestList({
    status: resultFilterToStatus(filters?.result),
    from: filters?.from,
    to: filters?.to,
  });

  return response.items.map(toRow).filter((row) => matchesSearch(row, filters?.search));
}

/** GET /api/finance/verifier/vp-requests/{requestId} */
export async function getVpDetail(
  verificationId: string
): Promise<VpVerificationDetail> {
  const detail: AdminVpRequestDetail = await getFinanceVpRequestDetail(
    verificationId
  );

  return {
    verificationId: detail.requestId,
    corporationName: detail.result?.corporateName ?? detail.corporateName ?? "-",
    verifierName: "KYvC Finance",
    purpose: displayPurpose(detail.purpose),
    credentialId: "KYC VC",
    result: statusToResult(detail.status),
    failReason: "-",
    createdAt: detail.createdAt ?? detail.expiresAt,
    requestedAt: detail.createdAt ?? undefined,
    respondedAt: detail.verifiedAt ?? undefined,
  };
}
