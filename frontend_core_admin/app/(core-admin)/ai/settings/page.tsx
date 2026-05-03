import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function AiSettingsPage() {
  return (
    <PageShell
      title="AI 설정"
      description="모델 · 프롬프트 · 임계치 · Azure OpenAI 연결"
      module="E-01 · E-02 · AI"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>모델 / 프롬프트</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            현재 활성 모델 · 프롬프트 버전이 표시됩니다.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>임계치</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            자동 통과 / 보완 / 반려 임계치 설정
          </CardContent>
        </Card>
      </div>
      <div className="flex justify-end">
        <Button>변경사항 저장</Button>
      </div>
    </PageShell>
  );
}
