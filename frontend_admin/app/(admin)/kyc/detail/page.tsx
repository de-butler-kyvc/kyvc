"use client";

import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

import { PageShell } from "@/components/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function AdminKycDetailPage() {
  return (
    <Suspense>
      <AdminKycDetail />
    </Suspense>
  );
}

function AdminKycDetail() {
  const searchParams = useSearchParams();
  const id = searchParams.get("id") ?? undefined;
  const label = id ?? "(미지정)";
  return (
    <PageShell
      title={`KYC 신청 ${label}`}
      description="신청 상세 · 서류 · AI 결과 · 심사 이력"
      module="M-03 / M-05"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>법인 정보</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            법인 기본정보가 표시됩니다.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>AI 자동 심사 결과</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            코어로부터 수신된 심사 결과 · 판단 근거가 표시됩니다.
          </CardContent>
        </Card>
      </div>
    </PageShell>
  );
}
