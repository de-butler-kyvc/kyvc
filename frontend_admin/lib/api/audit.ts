export async function getAuditLogs(filters?: { search?: string; action?: string }): Promise<any[]> {
  const data = [
    { id: "LOG-A-009821", date: "2025.05.02 16:30", actor: "admin_kim", action: "수동심사", target: "KYC-001", content: "심사 결과: 보완필요 → 승인", ip: "10.0.1.45", result: "성공" },
    { id: "LOG-A-009820", date: "2025.05.02 15:55", actor: "admin_park", action: "정책 변경", target: "POL-003", content: "블랙리스트 등록: did:bad:issuer:999", ip: "10.0.1.22", result: "성공" },
    { id: "LOG-A-009819", date: "2025.05.02 14:00", actor: "system", action: "VC 발급", target: "KYC-001", content: "VC 자동 발급 완료: vc:kyvc:2025:001", ip: "-", result: "성공" },
    { id: "LOG-A-009818", date: "2025.05.02 11:00", actor: "admin_lee", action: "보완요청", target: "KYC-002", content: "보완요청 생성: SUP-001, SUP-002", ip: "10.0.1.67", result: "성공" },
    { id: "LOG-A-009817", date: "2025.05.02 09:30", actor: "admin_view", action: "KYC 조회", target: "KYC-003", content: "KYC 신청 상세 조회", ip: "10.0.1.99", result: "권한 오류" },
  ];

  if (!filters) return data;

  return data.filter(item => {
    if (filters.search && !item.actor.toLowerCase().includes(filters.search.toLowerCase()) && !item.target.toLowerCase().includes(filters.search.toLowerCase())) {
      return false;
    }
    if (filters.action && filters.action !== "전체 액션 유형" && item.action !== filters.action) {
      return false;
    }
    return true;
  });
}