import type { VerifierItem } from "@/types/kyc";

export async function getVerifierList(filters?: { search?: string; status?: string }): Promise<VerifierItem[]> {
  const data = [
    { id: "VER-001", name: "파이낸셜 파트너스", domain: "financial-partners.co.kr", type: "코어 도입형", credential: "KYC VC, 위임권한 VC", regDate: "2024.09.01", status: "활성" },
    { id: "VER-002", name: "비즈파트너 포털", domain: "bizpartner.com", type: "SDK-only", credential: "KYC VC", regDate: "2025.01.15", status: "활성" },
    { id: "VER-003", name: "마켓플레이스 A", domain: "marketplace-a.kr", type: "SDK-only", credential: "KYC VC", regDate: "2025.03.10", status: "심사중" },
    { id: "VER-004", name: "파트너 포털 B", domain: "partner-b.co.kr", type: "코어 도입형", credential: "KYC VC", regDate: "2025.04.20", status: "비활성" },
  ];

  if (!filters) return data;

  return data.filter(item => {
    if (filters.search && !item.name.toLowerCase().includes(filters.search.toLowerCase()) && !item.domain.toLowerCase().includes(filters.search.toLowerCase())) {
      return false;
    }
    if (filters.status && filters.status !== "전체 상태" && item.status !== filters.status) {
      return false;
    }
    return true;
  });
}