import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function AdminIssuerPolicyPage() {
  return (
    <PageShell
      title="Issuer 신뢰정책"
      description="화이트리스트 · 블랙리스트 · 정책 승인"
      module="M-08 · ISSUER TRUST POLICY"
    >
      <Card>
        <CardHeader>
          <CardTitle>등록된 Issuer</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          신뢰 정책 목록이 표시됩니다.
        </CardContent>
      </Card>
      <div className="flex justify-end">
        <Button>Issuer 추가</Button>
      </div>
    </PageShell>
  );
}
