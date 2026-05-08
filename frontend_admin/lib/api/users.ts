import type { UserItem } from "@/types/kyc";

export async function getUserList(filters?: {
  search?: string;
  role?: string;
  status?: string;
}): Promise<UserItem[]> {
  let data = [
    { id: "corp_kim001", name: "김법인", role: "법인 사용자", status: "정상", lastLogin: "2025.05.02 09:14", regDate: "2024.03.15" },
    { id: "corp_lee002", name: "이대리", role: "법인 사용자", status: "정상", lastLogin: "2025.05.01 16:30", regDate: "2024.03.15" },
    { id: "corp_park003", name: "박담당", role: "법인 사용자", status: "잠금", lastLogin: "2025.04.20 11:00", regDate: "2024.07.01" },
    { id: "corp_choi004", name: "최연구", role: "법인 사용자", status: "비활성", lastLogin: "-", regDate: "2025.01.10" },
  ];

  // 프론트에서 필터링 (실제 API에서는 백엔드에서 처리)
  if (filters?.search) {
    const search = filters.search.toLowerCase();
    data = data.filter(item =>
      item.id.toLowerCase().includes(search) ||
      item.name.toLowerCase().includes(search)
    );
  }

  if (filters?.role && filters.role !== "전체 역할") {
    data = data.filter(item => item.role === filters.role);
  }

  if (filters?.status && filters.status !== "전체 상태") {
    data = data.filter(item => item.status === filters.status);
  }

  return data;
}