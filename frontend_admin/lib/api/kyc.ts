import type { KycItem, DashboardStats } from "@/types/kyc";

// 나중에 실제 API로 교체: fetch("/api/admin/kyc")
export async function getKycList(filters?: {
  search?: string;
  status?: string;
  channel?: string;
}): Promise<KycItem[]> {
  let data = [
    { id: "KYC-2025-0502-001", corp: "주식회사 케이원", biz: "123-45-67890", type: "주식회사", date: "2025.05.02 09:14", channel: "웹", status: "수동심사필요", ai: "보완필요", reviewer: "김심사" },
    { id: "KYC-2025-0502-002", corp: "(주)테크비전", biz: "234-56-78901", type: "주식회사", date: "2025.05.02 09:32", channel: "금융사", status: "보완필요", ai: "불충족", reviewer: "이심사" },
    { id: "KYC-2025-0502-003", corp: "글로벌파트너스(주)", biz: "345-67-89012", type: "외국기업", date: "2025.05.02 10:01", channel: "웹", status: "심사중", ai: "정상", reviewer: "-" },
    { id: "KYC-2025-0502-004", corp: "한국무역(주)", biz: "456-78-90123", type: "주식회사", date: "2025.05.02 10:22", channel: "웹", status: "정상", ai: "정상", reviewer: "김심사" },
    { id: "KYC-2025-0501-018", corp: "미래금융(주)", biz: "567-89-01234", type: "유한회사", date: "2025.05.01 16:55", channel: "금융사", status: "불충족", ai: "불충족", reviewer: "박심사" },
    { id: "KYC-2025-0501-017", corp: "스타트로직(주)", biz: "678-90-12345", type: "주식회사", date: "2025.05.01 15:20", channel: "웹", status: "정상", ai: "정상", reviewer: "-" },
  ];

  // 프론트에서 필터링 (실제 API에서는 백엔드에서 처리)
  if (filters?.search) {
    const search = filters.search.toLowerCase();
    data = data.filter(item =>
      item.corp.toLowerCase().includes(search) ||
      item.biz.includes(search)
    );
  }

  if (filters?.status && filters.status !== "전체 상태") {
    data = data.filter(item => item.status === filters.status);
  }

  if (filters?.channel && filters.channel !== "전체 채널") {
    data = data.filter(item => item.channel === filters.channel);
  }

  return data;
}

export async function getDashboardStats(): Promise<DashboardStats> {
  return {
    todayKyc: 24,
    pendingManual: 7,
    pendingSupplement: 5,
    vcIssued: 18,
    vpCount: 142,
  };
}