import type { IssuerItem } from "@/types/kyc";

export async function getIssuerList(filters?: { search?: string; type?: string; status?: string }): Promise<IssuerItem[]> {
  const data = [
    { id: "POL-001", did: "did:kyvc:issuer:001", type: "화이트리스트", credential: "KYC VC", scope: "플랫폼 전체", period: "2025.01.01~2026.12.31", status: "활성" },
    { id: "POL-002", did: "did:kyvc:issuer:002", type: "화이트리스트", credential: "위임권한 VC", scope: "파이낸셜 파트너스", period: "2025.03.01~2026.03.01", status: "활성" },
    { id: "POL-003", did: "did:bad:issuer:999", type: "블랙리스트", credential: "전체", scope: "플랫폼 전체", period: "2025.04.01~", status: "차단" },
    { id: "POL-004", did: "did:kyvc:issuer:003", type: "화이트리스트", credential: "KYC VC", scope: "비즈파트너 포털", period: "2025.05.01~2027.05.01", status: "심사중" },
  ];

  if (!filters) return data;

  return data.filter(item => {
    if (filters.search && !item.id.toLowerCase().includes(filters.search.toLowerCase()) && !item.did.toLowerCase().includes(filters.search.toLowerCase())) {
      return false;
    }
    if (filters.type && filters.type !== "전체 정책 유형" && item.type !== filters.type) {
      return false;
    }
    if (filters.status && filters.status !== "전체 상태" && item.status !== filters.status) {
      return false;
    }
    return true;
  });
}