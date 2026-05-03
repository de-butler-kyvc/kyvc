import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function VcVpStatusPage() {
  return (
    <PageShell
      title="VC / VP 코어 상태"
      description="발급 · 검증 처리 상태"
      module="E-05 · E-06"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>VC 발급</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            발급 큐 · 처리 시간 · 실패 사유
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>VP 검증</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            검증 처리 시간 · 실패율
          </CardContent>
        </Card>
      </div>
    </PageShell>
  );
}
