import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default async function WalletVcDetailPage({
  searchParams
}: {
  searchParams: Promise<{ id?: string }>;
}) {
  const { id } = await searchParams;
  const label = id ?? "(미지정)";
  return (
    <PageShell
      title={`VC ${label}`}
      description="Credential 상세 정보 및 발급 메타데이터를 확인합니다."
      module="WALLET · VC DETAIL"
    >
      <Card>
        <CardHeader>
          <CardTitle>Credential Subject</CardTitle>
        </CardHeader>
        <CardContent className="font-mono text-xs text-muted-foreground">
          {`{ /* VC payload preview */ }`}
        </CardContent>
      </Card>
    </PageShell>
  );
}
