import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function SdkMetaPage() {
  return (
    <PageShell
      title="SDK 메타데이터"
      description="Schema · Issuer 메타 · 테스트 벡터"
      module="E-09 · SDK META"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          SDK 배포용 메타데이터가 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
