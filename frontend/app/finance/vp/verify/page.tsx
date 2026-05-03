import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function FinanceVpVerifyPage() {
  return (
    <PageShell
      title="VP 검증"
      description="제출된 VP의 검증 결과를 확인합니다."
      module="FI · VP VERIFY"
    >
      <Card>
        <CardHeader>
          <CardTitle>최근 검증 결과</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          최근 VP 검증 이력이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
