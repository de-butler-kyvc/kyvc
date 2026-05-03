import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function WalletVpSubmitPage() {
  return (
    <PageShell
      title="VP 제출"
      description="요청된 항목을 확인하고 Verifiable Presentation을 제출합니다."
      module="WALLET · VP"
    >
      <Card>
        <CardHeader>
          <CardTitle>요청 정보</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          제출 대상 Verifier · 요청된 Claim · nonce 정보가 표시됩니다.
        </CardContent>
      </Card>
      <div className="flex justify-end gap-2">
        <Button variant="outline">취소</Button>
        <Button>제출</Button>
      </div>
    </PageShell>
  );
}
