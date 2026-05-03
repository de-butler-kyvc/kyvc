import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default async function CorporateKycDocumentsPage({
  searchParams
}: {
  searchParams: Promise<{ id?: string }>;
}) {
  const { id } = await searchParams;
  const label = id ?? "(미지정)";
  return (
    <PageShell
      title={`서류 보완 — ${label}`}
      description="심사역의 보완 요청에 따라 서류를 추가 제출합니다."
      module="M-04 · DOCUMENT"
    >
      <Card>
        <CardHeader>
          <CardTitle>보완 요청 항목</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          요청된 추가 서류가 여기에 표시됩니다.
        </CardContent>
      </Card>
    </PageShell>
  );
}
