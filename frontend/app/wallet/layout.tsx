import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";

const sections: NavSection[] = [
  {
    title: "Wallet",
    items: [
      { href: "/wallet", label: "내 VC" },
      { href: "/wallet/scan", label: "QR 스캔" },
      { href: "/wallet/vp/submit", label: "VP 제출" }
    ]
  }
];

export default function WalletLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar
        brand="KYvC"
        subtitle="VC Wallet"
        sections={sections}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header channel="모바일 앱 · VC Wallet" channelTag="MO · WALLET" />
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
