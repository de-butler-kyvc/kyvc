import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";
import { Icon } from "@/components/design/icons";
import { AuthSessionGate } from "@/lib/session-gate";

const sections: NavSection[] = [
  {
    title: "KYC 신청",
    items: [
      { href: "/corporate", label: "대시보드", icon: <Icon.Grid /> },
      { href: "/corporate/kyc/apply", label: "KYC 신청", icon: <Icon.FilePlus /> },
      { href: "/corporate/kyc", label: "진행상태 조회", icon: <Icon.Activity /> }
    ]
  },
  {
    title: "법인정보",
    items: [
      { href: "/corporate/profile", label: "법인 기본정보", icon: <Icon.Home /> },
      { href: "/corporate/representative", label: "대표자 정보", icon: <Icon.User /> },
      { href: "/corporate/agents", label: "대리인 정보", icon: <Icon.UserCheck /> }
    ]
  },
  {
    title: "이력 관리",
    items: [
      { href: "/corporate/kyc/history", label: "KYC 신청 이력", icon: <Icon.Clock /> },
      { href: "/corporate/documents", label: "제출서류 관리", icon: <Icon.File /> },
      { href: "/corporate/vc", label: "VC 발급 이력", icon: <Icon.CheckSquare /> },
      { href: "/corporate/vp", label: "VP 제출 이력", icon: <Icon.Upload /> }
    ]
  }
];

export default function CorporateLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthSessionGate>
      <div className="dash-shell">
        <Sidebar
          brand="KYvC"
          subtitle="법인 사용자 웹"
          sections={sections}
          homeHref="/corporate"
        />
        <main className="dash-main">
          <Header channel="법인 사용자 웹" channelTag="CW · CORPORATE" />
          <div className="dash-content">{children}</div>
          <footer className="footer">
            © 2025 KYvC. All rights reserved.
          </footer>
        </main>
      </div>
    </AuthSessionGate>
  );
}
