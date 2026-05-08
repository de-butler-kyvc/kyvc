import type { VpItem } from "@/types/kyc";

export async function getVpList(filters?: {
  search?: string;
  result?: string;
}): Promise<VpItem[]> {
  let data = [
    { id: "VP-2025-0502-0142", corp: "주식회사 케이원", verifier: "파이낸셜 파트너스", purpose: "KYC 인증 확인", vc: "vc:kyvc:2025:001", result: "성공", reason: "-", date: "2025.05.02 16:00" },
    { id: "VP-2025-0502-0141", corp: "(주)테크비전", verifier: "비즈파트너 포털", purpose: "기업 로그인", vc: "vc:kyvc:2025:002", result: "실패", reason: "nonce 불일치", date: "2025.05.02 15:30" },
    { id: "VP-2025-0502-0140", corp: "글로벌파트너스(주)", verifier: "파이낸셜 파트너스", purpose: "KYC 인증 확인", vc: "vc:kyvc:2025:003", result: "성공", reason: "-", date: "2025.05.02 14:55" },
    { id: "VP-2025-0502-0139", corp: "한국무역(주)", verifier: "비즈파트너 포털", purpose: "기업 로그인", vc: "vc:kyvc:2025:004", result: "만료", reason: "QR 토큰 만료", date: "2025.05.02 14:10" },
    { id: "VP-2025-0502-0138", corp: "미래금융(주)", verifier: "파이낸셜 파트너스", purpose: "KYC 인증 확인", vc: "vc:kyvc:2025:005", result: "실패", reason: "Issuer 미신뢰", date: "2025.05.02 13:40" },
  ];

  // 프론트에서 필터링 (실제 API에서는 백엔드에서 처리)
  if (filters?.search) {
    const search = filters.search.toLowerCase();
    data = data.filter(item =>
      item.corp.toLowerCase().includes(search) ||
      item.verifier.toLowerCase().includes(search) ||
      item.vc.toLowerCase().includes(search)
    );
  }

  if (filters?.result && filters.result !== "전체 결과") {
    data = data.filter(item => item.result === filters.result);
  }

  return data;
}