import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function FinanceKycPage() {
  return (
    <PageShell
      title="방문 KYC"
      description="창구에서 진행하는 KYC 절차입니다."
      module="FI · ON-SITE KYC"
    >
      <Card>
        <CardHeader>
          <CardTitle>방문 KYC 목록</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          진행 중인 방문 KYC 케이스가 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
