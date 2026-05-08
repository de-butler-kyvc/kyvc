export async function getVcList(filters?: { search?: string; status?: string }) {
  let data = [
    { id: "VC-001", corp: "주식회사 케이원",    credId: "vc:kyvc:2025:corp-001", type: "KYCVerifiableCredential", issuedAt: "2025.05.02 14:00", expiresAt: "2026.12.31", status: "활성" },
    { id: "VC-002", corp: "(주)테크비전",        credId: "vc:kyvc:2025:corp-002", type: "KYCVerifiableCredential", issuedAt: "2025.05.03 10:00", expiresAt: "2026.12.31", status: "활성" },
    { id: "VC-003", corp: "글로벌파트너스(주)",  credId: "vc:kyvc:2025:corp-003", type: "KYCVerifiableCredential", issuedAt: "2025.04.15 09:30", expiresAt: "2026.04.15", status: "폐기" },
    { id: "VC-004", corp: "한국무역(주)",        credId: "vc:kyvc:2025:corp-004", type: "KYCVerifiableCredential", issuedAt: "2025.03.01 11:00", expiresAt: "2026.03.01", status: "활성" },
    { id: "VC-005", corp: "미래금융(주)",        credId: "vc:kyvc:2025:corp-005", type: "KYCVerifiableCredential", issuedAt: "2025.02.20 15:00", expiresAt: "2025.12.31", status: "만료" },
    { id: "VC-006", corp: "스타트로직(주)",      credId: "vc:kyvc:2025:corp-006", type: "KYCVerifiableCredential", issuedAt: "2025.01.10 09:00", expiresAt: "2026.01.10", status: "활성" },
  ];

  if (filters?.search) {
    const s = filters.search.toLowerCase();
    data = data.filter(d => d.corp.toLowerCase().includes(s) || d.credId.includes(s));
  }
  if (filters?.status && filters.status !== "전체 상태") {
    data = data.filter(d => d.status === filters.status);
  }

  return data;
}
