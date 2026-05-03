import Link from "next/link";

import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function CorporateDashboardPage() {
  return (
    <PageShell
      title="대시보드"
      description="진행 중인 KYC 신청과 발급된 VC를 확인하세요."
      module="M-03 · KYC APPLICATION"
    >
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardDescription>진행 중 신청</CardDescription>
            <CardTitle className="text-3xl font-semibold">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>승인 완료</CardDescription>
            <CardTitle className="text-3xl font-semibold">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>발급된 VC</CardDescription>
            <CardTitle className="text-3xl font-semibold">0</CardTitle>
          </CardHeader>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>새 KYC 신청</CardTitle>
          <CardDescription>온라인으로 법인 KYC 신청을 시작합니다.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link href="/corporate/kyc/apply">신청 시작</Link>
          </Button>
        </CardContent>
      </Card>
    </PageShell>
  );
}
