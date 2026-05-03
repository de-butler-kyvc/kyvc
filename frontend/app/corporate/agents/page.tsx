import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function CorporateAgentsPage() {
  return (
    <PageShell
      title="대표자 · 대리인"
      description="법인 대표자 및 위임 대리인을 관리합니다."
      module="M-02 · CORPORATE"
    >
      <Card>
        <CardHeader>
          <CardTitle>등록된 대표자/대리인</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          대표자 및 대리인 목록이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
