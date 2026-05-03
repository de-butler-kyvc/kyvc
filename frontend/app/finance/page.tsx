import { PageShell } from "@/components/page-shell";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function FinanceDashboardPage() {
  return (
    <PageShell
      title="대시보드"
      description="금융사 업무 진행 현황입니다."
      module="FI · DASHBOARD"
    >
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardDescription>오늘 검증 요청</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>방문 KYC 진행</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>이번 주 검증 결과</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
      </div>
    </PageShell>
  );
}
