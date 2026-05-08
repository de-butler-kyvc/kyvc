export async function getManagers(filters?: { search?: string; role?: string; status?: string }): Promise<any[]> {
  const data = [
    { id: "admin_kim", name: "김심사", role: "심사자", status: "정상", lastLogin: "2025.05.02 09:00", mfa: "설정됨" },
    { id: "admin_lee", name: "이심사", role: "심사자", status: "정상", lastLogin: "2025.05.02 08:45", mfa: "설정됨" },
    { id: "admin_park", name: "박심사", role: "승인권자", status: "정상", lastLogin: "2025.05.02 09:10", mfa: "설정됨" },
    { id: "admin_ops", name: "이운영", role: "운영자", status: "정상", lastLogin: "2025.05.01 17:00", mfa: "미설정" },
    { id: "admin_view", name: "최조회", role: "조회자", status: "잠금", lastLogin: "2025.04.28 11:00", mfa: "설정됨" },
  ];

  if (!filters) return data;

  return data.filter(item => {
    if (filters.search && !item.id.toLowerCase().includes(filters.search.toLowerCase()) && !item.name.toLowerCase().includes(filters.search.toLowerCase())) {
      return false;
    }
    if (filters.role && filters.role !== "전체 역할" && item.role !== filters.role) {
      return false;
    }
    if (filters.status && filters.status !== "전체 상태" && item.status !== filters.status) {
      return false;
    }
    return true;
  });
}