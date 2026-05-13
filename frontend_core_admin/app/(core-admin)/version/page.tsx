import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function VersionPage() {
  return (
    <PageShell
      title="코어 버전"
      description="AI · VC · VP · XRPL 모듈 버전"
      module="CORE · VERSION"
    >
      <Card>
        <CardHeader>
          <CardTitle>모듈 버전</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          각 모듈별 현재 배포 버전이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
