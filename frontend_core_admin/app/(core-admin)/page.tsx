import { PageShell } from "@/components/page-shell";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function CoreAdminDashboardPage() {
  return (
    <PageShell
      title="코어 운영 대시보드"
      description="AI · VC · VP · XRPL 처리 현황"
      module="CA · DASHBOARD"
    >
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader>
            <CardDescription>오늘 AI 요청</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>VC 발급</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>VP 검증</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>XRPL TX</CardDescription>
            <CardTitle className="text-3xl">0</CardTitle>
          </CardHeader>
        </Card>
      </div>
    </PageShell>
  );
}
