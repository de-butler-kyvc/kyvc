import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function WalletScanPage() {
  return (
    <PageShell
      title="QR 스캔"
      description="검증자(Verifier) 측 QR을 스캔하여 VP 제출 절차를 시작합니다."
      module="WALLET · QR"
    >
      <Card>
        <CardContent className="flex h-72 items-center justify-center text-sm text-muted-foreground">
          카메라 프리뷰 영역
        </CardContent>
      </Card>
    </PageShell>
  );
}
