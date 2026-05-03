import { Header } from "@/components/nav/header";
import { Sidebar, type NavSection } from "@/components/nav/sidebar";

const sections: NavSection[] = [
  {
    title: "운영",
    items: [{ href: "/", label: "대시보드" }]
  },
  {
    title: "AI",
    items: [
      { href: "/ai/settings", label: "AI 설정" },
      { href: "/ai/runs", label: "AI 처리 상태" }
    ]
  },
  {
    title: "Credential",
    items: [
      { href: "/schemas", label: "Credential Schema" },
      { href: "/vc-vp", label: "VC / VP 코어 상태" }
    ]
  },
  {
    title: "Ledger",
    items: [{ href: "/xrpl", label: "XRPL 상태" }]
  },
  {
    title: "SDK",
    items: [{ href: "/sdk-meta", label: "SDK 메타데이터" }]
  },
  {
    title: "버전",
    items: [{ href: "/version", label: "코어 버전" }]
  }
];

export default function CoreAdminLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar
        brand="KYvC"
        subtitle="코어 어드민"
        sections={sections}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header channel="코어 어드민" channelTag="CA · TECH OPS" />
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
