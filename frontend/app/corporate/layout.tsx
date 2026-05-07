import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";

const sections: NavSection[] = [
  {
    title: "KYC",
    items: [
      { href: "/corporate", label: "대시보드" },
      { href: "/corporate/kyc/apply", label: "KYC 신청" },
      { href: "/corporate/kyc", label: "신청 내역" }
    ]
  },
  {
    title: "법인정보",
    items: [
      { href: "/corporate/profile", label: "법인 기본정보" },
      { href: "/corporate/representative", label: "대표자 정보" },
      { href: "/corporate/agents", label: "대리인 정보" }
    ]
  }
];

export default function CorporateLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar
        brand="KYvC"
        subtitle="법인 사용자 웹"
        sections={sections}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header channel="법인 사용자 웹" channelTag="CW · CORPORATE" />
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
