import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminVcPage() {
  return (
    <PageShell
      title="VC 발급 업무 상태"
      description="발급 요청 · 완료 · Wallet 저장 여부를 추적합니다."
      module="M-06 · VC ORCHESTRATION"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          발급 요청 목록이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
