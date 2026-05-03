import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AiRunsPage() {
  return (
    <PageShell
      title="AI 처리 상태"
      description="요청 목록 · 실패율 · 비용 · 사용량"
      module="E-01 · AI RUNS"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          AI 처리 기록이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
