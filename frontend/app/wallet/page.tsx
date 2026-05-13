import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function WalletHomePage() {
  return (
    <PageShell
      title="내 VC"
      description="발급받은 Verifiable Credential 목록입니다."
      module="WALLET · CREDENTIALS"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          저장된 VC가 없습니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
