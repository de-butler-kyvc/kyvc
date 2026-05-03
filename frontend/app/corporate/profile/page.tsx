import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function CorporateProfilePage() {
  return (
    <PageShell
      title="법인 정보"
      description="법인 기본정보를 관리합니다."
      module="M-02 · CORPORATE"
    >
      <Card>
        <CardHeader>
          <CardTitle>기본 정보</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          법인명 · 등록번호 · 주소 · 업종 등이 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
