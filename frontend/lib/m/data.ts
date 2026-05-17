/**
 * 모바일 웹뷰에서 사용하는 정적/모의 데이터.
 * 실서비스에서는 Android 브리지(getWalletInfo, listWallets, saveVC 등)
 * 또는 백엔드 API(/api/user/credentials 등)로 대체된다.
 */

import type { CertItem } from "@/components/m/parts";

export const MOCK_CERTS: CertItem[] = [
  {
    issuer: "법원행정처",
    title: "법인등록증명서",
    status: "검증됨",
    id: "KYVC-CERT-240315",
    date: "2026.05.07",
    gradient: "linear-gradient(135deg,#111827 0%,#183b8f 48%,#7c3aed 100%)",
  },
  {
    issuer: "국세청",
    title: "사업자등록증",
    status: "활성",
    id: "KYVC-CERT-TAX-67890",
    date: "2026.04.28",
    gradient: "linear-gradient(135deg,#052e2b 0%,#0f766e 48%,#2563eb 100%)",
  },
  {
    issuer: "신한은행",
    title: "기업금융 인증서",
    status: "연동됨",
    id: "KYVC-CERT-BANK-SHB",
    date: "2026.05.02",
    gradient: "linear-gradient(135deg,#231942 0%,#5e3bce 50%,#00a3ff 100%)",
  },
];

export type MNotifItem = {
  id: number;
  type: "check" | "shield" | "bell" | "cert";
  cat: "vc" | "vp" | "warn";
  title: string;
  desc: string;
  time: string;
  unread: boolean;
  color: "green" | "blue" | "orange" | "purple";
  group: string;
};

export const MOCK_NOTIFS: MNotifItem[] = [
  {
    id: 1,
    type: "check",
    cat: "vc",
    title: "법인등록증명서 검증 성공",
    desc: "신한은행 기업금융에서 귀하의 증명서를 성공적으로 검증했습니다.",
    time: "오후 2:32",
    unread: true,
    color: "green",
    group: "오늘",
  },
  {
    id: 2,
    type: "shield",
    cat: "warn",
    title: "새로운 로그인 감지",
    desc: "새로운 기기(MacBook Pro)에서 지갑에 로그인했습니다.",
    time: "오전 10:15",
    unread: true,
    color: "blue",
    group: "오늘",
  },
  {
    id: 3,
    type: "bell",
    cat: "warn",
    title: "사업자등록증 만료 안내",
    desc: "등록된 사업자등록증 VC가 30일 후 만료됩니다.",
    time: "수요일",
    unread: false,
    color: "orange",
    group: "이번 주",
  },
  {
    id: 4,
    type: "cert",
    cat: "vp",
    title: "기업금융 인증서 발급",
    desc: "신한은행 기관으로부터 새로운 증명서를 발급받았습니다.",
    time: "월요일",
    unread: false,
    color: "purple",
    group: "이번 주",
  },
];

export const MOCK_SEED_WORDS = [
  "apple", "brave", "candy", "delta", "eagle", "flame",
  "grape", "happy", "igloo", "jelly", "koala", "lemon",
] as const;

const HIDDEN_KEY = "kyvc.m.hiddenCerts";

export function readHiddenCerts(): string[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(HIDDEN_KEY);
    return raw ? (JSON.parse(raw) as string[]) : [];
  } catch {
    return [];
  }
}

export function writeHiddenCerts(list: string[]) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(HIDDEN_KEY, JSON.stringify(list));
}

export function toggleHiddenCert(title: string): string[] {
  const cur = readHiddenCerts();
  const next = cur.includes(title)
    ? cur.filter((t) => t !== title)
    : [...cur, title];
  writeHiddenCerts(next);
  return next;
}
