import Link from "next/link";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

const channels = [
  {
    href: "/corporate",
    title: "법인 사용자 웹",
    tag: "CW · CORPORATE",
    description: "온라인 KYC 신청 · 진행상태 조회"
  },
  {
    href: "/wallet",
    title: "모바일 앱 · VC Wallet",
    tag: "MO · WALLET",
    description: "VC 저장 · QR 스캔 · VP 제출"
  },
  {
    href: "/finance",
    title: "금융사 업무 화면",
    tag: "FI · FINANCE",
    description: "방문 KYC · VP 요청·검증"
  }
];

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen max-w-5xl flex-col gap-10 px-8 py-16">
      <div>
        <div className="font-mono text-xs uppercase tracking-widest text-muted-foreground">
          KYvC / Application Front
        </div>
        <h1 className="mt-2 text-4xl font-semibold tracking-tight">
          법인 KYC 플랫폼
        </h1>
        <p className="mt-3 max-w-2xl text-sm text-muted-foreground">
          채널별 진입점입니다. 백엔드/코어 어드민은 별도 애플리케이션을 사용하세요.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {channels.map((channel) => (
          <Link key={channel.href} href={channel.href} className="block">
            <Card className="h-full transition-colors hover:border-foreground">
              <CardHeader>
                <Badge variant="outline" className="w-fit font-mono">
                  {channel.tag}
                </Badge>
                <CardTitle className="mt-2">{channel.title}</CardTitle>
                <CardDescription>{channel.description}</CardDescription>
              </CardHeader>
              <CardContent />
            </Card>
          </Link>
        ))}
      </div>
    </main>
  );
}
