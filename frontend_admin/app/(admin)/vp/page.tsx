import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminVpPage() {
  return (
    <PageShell
      title="VP 검증 업무 결과"
      description="검증 결과 · 실패 사유 · 제출 기관별 통계"
      module="M-07 · VP BUSINESS"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          VP 검증 이력이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
