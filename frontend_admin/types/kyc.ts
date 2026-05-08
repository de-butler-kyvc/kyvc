export type KycStatus = "수동심사필요" | "보완필요" | "심사중" | "정상" | "불충족";
export type KycChannel = "웹" | "금융사";

export interface KycItem {
  id: string;
  corp: string;
  biz: string;
  type: string;
  date: string;
  channel: KycChannel;
  status: KycStatus;
  ai: string;
  reviewer: string;
}

export interface VpItem {
  id: string;
  corp: string;
  verifier: string;
  purpose: string;
  vc: string;
  result: "성공" | "실패" | "만료";
  reason: string;
  date: string;
}

export interface UserItem {
  id: string;
  name: string;
  role: string;
  status: "정상" | "잠금" | "비활성";
  lastLogin: string;
  regDate: string;
}

export interface IssuerItem {
  id: string;
  did: string;
  type: "화이트리스트" | "블랙리스트";
  credential: string;
  scope: string;
  period: string;
  status: "활성" | "차단" | "심사중";
}

export interface VerifierItem {
  id: string;
  name: string;
  domain: string;
  type: "코어 도입형" | "SDK-only";
  credential: string;
  regDate: string;
  status: "활성" | "심사중" | "비활성";
}

export interface DashboardStats {
  todayKyc: number;
  pendingManual: number;
  pendingSupplement: number;
  vcIssued: number;
  vpCount: number;
}