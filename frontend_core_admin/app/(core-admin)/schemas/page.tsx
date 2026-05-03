import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function SchemasPage() {
  return (
    <PageShell
      title="Credential Schema"
      description="VC 스키마 등록 · 버전 관리"
      module="E-07 · SCHEMA"
    >
      <Card>
        <CardHeader>
          <CardTitle>등록된 Schema</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          KYC · 위임권한 · 법인 인증 등 Schema 목록이 표시됩니다.
        </CardContent>
      </Card>
      <div className="flex justify-end">
        <Button>새 Schema 등록</Button>
      </div>
    </PageShell>
  );
}
