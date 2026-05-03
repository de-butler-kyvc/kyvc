import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function XrplPage() {
  return (
    <PageShell
      title="XRPL 상태"
      description="노드 상태 · 트랜잭션 · 재처리"
      module="E-08 · XRPL"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>노드 상태</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            연결된 XRPL 노드 정보가 표시됩니다.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>최근 트랜잭션</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            최근 송신/수신 트랜잭션 이력이 표시됩니다.
          </CardContent>
        </Card>
      </div>
    </PageShell>
  );
}
