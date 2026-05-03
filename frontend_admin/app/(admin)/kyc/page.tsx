import { PageShell } from "@/components/page-shell";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminKycListPage() {
  return (
    <PageShell
      title="KYC 신청"
      description="신청 목록 · 상세 · 상태 관리"
      module="M-03 · APPLICATION"
    >
      <Card>
        <CardContent className="p-12 text-center text-sm text-muted-foreground">
          신청 목록 테이블이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
