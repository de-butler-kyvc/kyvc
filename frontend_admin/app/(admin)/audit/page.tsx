import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminAuditPage() {
  return (
    <PageShell
      title="감사 로그"
      description="관리자 행위 · 심사 이력 · 정책 변경"
      module="M-11 · AUDIT"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          감사 로그 테이블이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
