import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminCorporatesPage() {
  return (
    <PageShell
      title="법인 사용자"
      description="법인 계정 · 대표자 · 대리인 정보를 관리합니다."
      module="M-02 · CORPORATE"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          법인 사용자 목록이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
