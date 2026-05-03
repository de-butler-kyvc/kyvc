import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";

const sections: NavSection[] = [
  {
    title: "심사",
    items: [
      { href: "/", label: "대시보드" },
      { href: "/kyc", label: "KYC 신청" },
      { href: "/review", label: "수동 심사" }
    ]
  },
  {
    title: "법인 · 사용자",
    items: [{ href: "/corporates", label: "법인 사용자" }]
  },
  {
    title: "VC · VP",
    items: [
      { href: "/vc", label: "VC 발급 상태" },
      { href: "/vp", label: "VP 검증 결과" }
    ]
  },
  {
    title: "정책",
    items: [
      { href: "/issuer-policy", label: "Issuer 신뢰정책" },
      { href: "/verifiers", label: "Verifier 플랫폼" }
    ]
  },
  {
    title: "운영",
    items: [{ href: "/audit", label: "감사 로그" }]
  }
];

export default function AdminLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar
        brand="KYvC"
        subtitle="백엔드 어드민"
        sections={sections}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header channel="백엔드 어드민" channelTag="BA · BUSINESS OPS" />
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
