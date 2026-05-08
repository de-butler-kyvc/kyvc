import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";
import { Icon } from "@/components/design/icons";

const sections: NavSection[] = [
  {
    title: "Wallet",
    items: [
      { href: "/wallet", label: "내 VC", icon: <Icon.CheckSquare /> },
      { href: "/wallet/scan", label: "QR 스캔", icon: <Icon.Search /> },
      { href: "/wallet/vp/submit", label: "VP 제출", icon: <Icon.Upload /> }
    ]
  }
];

export default function WalletLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="dash-shell">
      <Sidebar
        brand="KYvC"
        subtitle="VC Wallet"
        sections={sections}
        homeHref="/wallet"
      />
      <main className="dash-main">
        <Header channel="모바일 앱 · VC Wallet" channelTag="MO · WALLET" />
        <div className="dash-content">{children}</div>
        <footer className="footer">
          © 2025 KYvC. All rights reserved.
        </footer>
      </main>
    </div>
  );
}
