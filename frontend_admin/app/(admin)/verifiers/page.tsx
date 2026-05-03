import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function AdminVerifiersPage() {
  return (
    <PageShell
      title="Verifier 플랫폼"
      description="SDK 연동 플랫폼 · API Key · Callback"
      module="M-09 · VERIFIER MGMT"
    >
      <Card>
        <CardHeader>
          <CardTitle>연동 플랫폼</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          외부 Verifier 플랫폼이 표시됩니다.
        </CardContent>
      </Card>
      <div className="flex justify-end">
        <Button>플랫폼 등록</Button>
      </div>
    </PageShell>
  );
}
