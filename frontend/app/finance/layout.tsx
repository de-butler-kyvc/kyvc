import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";

const sections: NavSection[] = [
  {
    title: "업무",
    items: [
      { href: "/finance", label: "대시보드" },
      { href: "/finance/kyc", label: "방문 KYC" }
    ]
  },
  {
    title: "VP",
    items: [
      { href: "/finance/vp/request", label: "VP 요청" },
      { href: "/finance/vp/verify", label: "VP 검증" }
    ]
  }
];

export default function FinanceLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar
        brand="KYvC"
        subtitle="금융사 업무"
        sections={sections}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header channel="금융사 업무 화면" channelTag="FI · FINANCE" />
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
