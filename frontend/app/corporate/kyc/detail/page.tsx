import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default async function CorporateKycDetailPage({
  searchParams
}: {
  searchParams: Promise<{ id?: string }>;
}) {
  const { id } = await searchParams;
  const label = id ?? "(미지정)";
  return (
    <PageShell
      title={`KYC 신청 ${label}`}
      description="진행 상태와 심사 결과를 확인합니다."
      module="M-03 / M-05 · STATUS"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardDescription>현재 상태</CardDescription>
            <CardTitle className="flex items-center gap-2">
              <Badge variant="secondary">제출 완료</Badge>
              <Badge variant="outline">AI 심사 중</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            AI 자동심사 후 수동심사 단계로 전환될 수 있습니다.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>제출 서류</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            업로드된 서류가 표시됩니다.
          </CardContent>
        </Card>
      </div>
    </PageShell>
  );
}
