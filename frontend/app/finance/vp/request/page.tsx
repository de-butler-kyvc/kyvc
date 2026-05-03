import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function FinanceVpRequestPage() {
  return (
    <PageShell
      title="VP 요청"
      description="고객의 Wallet에서 VP 제출을 받기 위한 요청을 생성합니다."
      module="FI · VP REQUEST"
    >
      <Card>
        <CardHeader>
          <CardTitle>요청 항목</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          요청 Claim · Schema · 만료시간 · nonce 설정 영역
        </CardContent>
      </Card>
      <div className="flex justify-end">
        <Button>QR 생성</Button>
      </div>
    </PageShell>
  );
}
