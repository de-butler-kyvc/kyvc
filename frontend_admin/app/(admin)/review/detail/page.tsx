import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default async function AdminReviewDetailPage({
  searchParams
}: {
  searchParams: Promise<{ id?: string }>;
}) {
  const { id } = await searchParams;
  const label = id ?? "(미지정)";
  return (
    <PageShell
      title={`수동심사 ${label}`}
      description="AI 결과 · 서류 · 판단 근거를 확인하고 최종 결정합니다."
      module="M-05 · DECISION"
    >
      <Card>
        <CardHeader>
          <CardTitle>AI 결과</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          코어 AI 결과 · 신뢰도 · Rule Engine 판단 근거
        </CardContent>
      </Card>
      <div className="flex flex-wrap justify-end gap-2">
        <Button variant="outline">보완 요청</Button>
        <Button variant="destructive">반려</Button>
        <Button>승인</Button>
      </div>
    </PageShell>
  );
}
