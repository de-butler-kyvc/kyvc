"use client";

import Link from "next/link";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { kyc } from "@/lib/api";
import { useApi } from "@/lib/use-api";

export default function CorporateKycListPage() {
  const { data, error, loading } = useApi(() => kyc.list());
  const items = data?.items ?? [];

  return (
    <PageShell
      title="KYC 신청 내역"
      description="제출한 신청과 진행 상태를 확인합니다."
      module="UWEB-020 · M-03"
    >
      <Card>
        {error ? (
          <CardContent className="p-6 text-sm text-destructive">{error}</CardContent>
        ) : loading ? (
          <CardContent className="p-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </CardContent>
        ) : items.length === 0 ? (
          <CardContent className="p-12 text-center text-sm text-muted-foreground">
            신청 내역이 없습니다.
          </CardContent>
        ) : (
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="text-left text-xs uppercase text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">신청번호</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">신청일</th>
                  <th className="px-4 py-3">완료일</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {items.map((it) => (
                  <tr key={it.kycId} className="border-t">
                    <td className="px-4 py-3 font-mono text-xs">
                      {it.applicationNo ?? `KYC-${it.kycId}`}
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant="secondary">{it.status}</Badge>
                    </td>
                    <td className="px-4 py-3">{it.submittedAt?.slice(0, 10) ?? "-"}</td>
                    <td className="px-4 py-3">{it.completedAt?.slice(0, 10) ?? "-"}</td>
                    <td className="px-4 py-3 text-right">
                      <Button asChild variant="ghost" size="sm">
                        <Link href={`/corporate/kyc/detail?id=${it.kycId}`}>상세</Link>
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        )}
      </Card>
    </PageShell>
  );
}
