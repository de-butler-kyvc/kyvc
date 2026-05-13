"use client";

import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function WalletVcDetailPage() {
  return (
    <Suspense>
      <WalletVcDetail />
    </Suspense>
  );
}

function WalletVcDetail() {
  const searchParams = useSearchParams();
  const id = searchParams.get("id") ?? undefined;
  const label = id ?? "(미지정)";
  return (
    <PageShell
      title={`VC ${label}`}
      description="Credential 상세 정보 및 발급 메타데이터를 확인합니다."
      module="WALLET · VC DETAIL"
    >
      <Card>
        <CardHeader>
          <CardTitle>Credential Subject</CardTitle>
        </CardHeader>
        <CardContent className="font-mono text-xs text-muted-foreground">
          {`{ /* VC payload preview */ }`}
        </CardContent>
      </Card>
    </PageShell>
  );
}
