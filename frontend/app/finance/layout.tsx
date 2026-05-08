import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";
import { Icon } from "@/components/design/icons";

const sections: NavSection[] = [
  {
    title: "업무",
    items: [
      { href: "/finance", label: "대시보드", icon: <Icon.Grid /> },
      { href: "/finance/kyc", label: "방문 KYC", icon: <Icon.FilePlus /> }
    ]
  },
  {
    title: "VP",
    items: [
      { href: "/finance/vp/request", label: "VP 요청", icon: <Icon.Shield /> },
      { href: "/finance/vp/verify", label: "VP 검증", icon: <Icon.CheckSquare /> }
    ]
  }
];

export default function FinanceLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="dash-shell">
      <Sidebar
        brand="KYvC"
        subtitle="금융사 업무"
        sections={sections}
        homeHref="/finance"
      />
      <main className="dash-main">
        <Header channel="금융사 업무 화면" channelTag="FI · FINANCE" />
        <div className="dash-content">{children}</div>
        <footer className="footer">
          © 2025 KYvC. All rights reserved.
        </footer>
      </main>
    </div>
  );
}
