import {
  getCredential,
  getCredentials,
  requestCredentialReissue,
  requestCredentialRevoke,
  type KycCredential,
  type KycCredentialDetail,
} from "@/lib/api/credentials";

const STATUS_API_TO_KO: Record<string, string> = {
  ACTIVE: "활성",
  ISSUED: "활성",
  REVOKED: "폐기",
  EXPIRED: "만료",
  SUSPENDED: "보류",
  PENDING: "보류",
  활성: "활성",
  폐기: "폐기",
  만료: "만료",
  보류: "보류",
};

const STATUS_KO_TO_API: Record<string, string> = {
  활성: "ACTIVE",
  폐기: "REVOKED",
  만료: "EXPIRED",
  보류: "SUSPENDED",
};

export interface VcListItem {
  id: string;
  corp: string;
  credId: string;
  type: string;
  issuedAt: string;
  expiresAt: string;
  status: string;
}

export interface VcDetail {
  credentialId: string;
  credentialType: string;
  issuerDid: string;
  holderDid: string;
  issuedAt: string;
  expiresAt: string;
  xrplTxHash: string;
  mobileStored: string;
  corp: string;
  kyc: string;
  status: string;
}

function normalizeStatus(status?: string): string {
  if (!status) return "-";
  return STATUS_API_TO_KO[status] ?? status;
}

function formatMobileStored(value: KycCredentialDetail["mobileStored"]): string {
  if (value === true) return "저장 완료";
  if (value === false) return "미저장";
  if (value === "Y") return "저장 완료";
  if (value === "N") return "미저장";
  return value || "-";
}

function toListItem(c: KycCredential): VcListItem {
  return {
    id: c.credentialId,
    corp: c.corporationName ?? "-",
    credId: c.credentialId,
    type: c.credentialType ?? c.credentialTypeCode ?? "KYCVerifiableCredential",
    issuedAt: c.issuedAt ?? "-",
    expiresAt: c.expiresAt ?? "-",
    status: normalizeStatus(c.status ?? c.credentialStatusCode),
  };
}

function toDetail(c: KycCredentialDetail): VcDetail {
  return {
    credentialId: c.credentialId,
    credentialType: c.credentialType ?? c.credentialTypeCode ?? "KYCVerifiableCredential",
    issuerDid: c.issuerDid ?? "-",
    holderDid: c.holderDid ?? "-",
    issuedAt: c.issuedAt ?? "-",
    expiresAt: c.expiresAt ?? "-",
    xrplTxHash: c.xrplTxHash ?? c.transactionHash ?? "-",
    mobileStored: formatMobileStored(c.mobileStored ?? c.walletSavedYn),
    corp: c.corporationName ?? c.corporateName ?? "-",
    kyc: c.applicationId ?? (c.kycId != null ? String(c.kycId) : "-"),
    status: normalizeStatus(c.status ?? c.credentialStatusCode),
  };
}

export async function getVcList(filters?: { search?: string; status?: string }): Promise<VcListItem[]> {
  const apiStatus =
    filters?.status && filters.status !== "전체 상태"
      ? STATUS_KO_TO_API[filters.status] ?? filters.status
      : undefined;

  const items = await getCredentials({ status: apiStatus });
  const mapped = items.map(toListItem);

  if (!filters?.search?.trim()) return mapped;

  const keyword = filters.search.trim().toLowerCase();
  return mapped.filter(
    (item) =>
      item.corp.toLowerCase().includes(keyword) ||
      item.credId.toLowerCase().includes(keyword)
  );
}

export async function getVcDetail(credentialId: string): Promise<VcDetail> {
  const item = await getCredential(credentialId);
  return toDetail(item);
}

export async function requestVcReissue(credentialId: string, reason: string): Promise<void> {
  await requestCredentialReissue(credentialId, { reason });
}

export async function requestVcRevoke(
  credentialId: string,
  payload: { reason: string; detail?: string }
): Promise<void> {
  await requestCredentialRevoke(credentialId, payload);
}
