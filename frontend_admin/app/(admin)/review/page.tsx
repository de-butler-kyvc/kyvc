import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminReviewPage() {
  return (
    <PageShell
      title="수동 심사"
      description="AI 자동심사 결과를 받아 최종 승인 · 반려 · 보완요청을 결정합니다."
      module="M-05 · REVIEW"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          심사 대기 큐가 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
