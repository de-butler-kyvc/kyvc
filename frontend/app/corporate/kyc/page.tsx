import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function CorporateKycListPage() {
  return (
    <PageShell
      title="KYC 신청 내역"
      description="제출한 신청과 진행 상태를 확인합니다."
      module="M-03 · APPLICATION HISTORY"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          신청 내역이 없습니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
