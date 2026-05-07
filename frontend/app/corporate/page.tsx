"use client";

import Link from "next/link";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { corporate } from "@/lib/api";
import { useApi } from "@/lib/use-api";

export default function CorporateDashboardPage() {
  const { data, error, loading } = useApi(() => corporate.dashboard());

  const corp = data?.corporate;
  const kyc = data?.kycSummary ?? {};
  const issued = data?.credentialSummary?.issued ?? 0;
  const recent = data?.recentApplications ?? [];
  const alerts = (data?.notifications ?? []).filter(
    (n) => n.severity === "warning" || n.severity === "error"
  );

  return (
    <PageShell
      title="대시보드"
      description="진행 중인 KYC 신청과 발급된 VC를 확인하세요."
      module="UWEB-001 · M-02 / M-03"
    >
      {error ? (
        <Card>
          <CardContent className="p-6 text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardDescription>법인 식별정보</CardDescription>
          <CardTitle className="flex flex-wrap items-center gap-2">
            {loading ? "..." : corp?.corporateName ?? "법인 미등록"}
            {corp?.businessNo ? (
              <Badge variant="outline" className="font-mono">
                {corp.businessNo}
              </Badge>
            ) : null}
          </CardTitle>
        </CardHeader>
      </Card>

      {alerts.length > 0 ? (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardContent className="p-4 text-sm">
            {alerts.map((a, i) => (
              <div key={i}>⚠ {a.message}</div>
            ))}
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-4 md:grid-cols-4">
        <Stat label="KYC 신청" value={kyc.total ?? 0} />
        <Stat label="심사중" value={kyc.inReview ?? 0} />
        <Stat label="보완필요" value={kyc.supplement ?? 0} />
        <Stat label="발급된 VC" value={issued} />
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>최근 신청</CardTitle>
          <Button asChild size="sm">
            <Link href="/corporate/kyc/apply">신청 시작</Link>
          </Button>
        </CardHeader>
        <CardContent>
          {recent.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              최근 신청 내역이 없습니다.
            </p>
          ) : (
            <table className="w-full text-sm">
              <thead className="text-left text-xs uppercase text-muted-foreground">
                <tr>
                  <th className="py-2">신청번호</th>
                  <th className="py-2">법인 유형</th>
                  <th className="py-2">신청일</th>
                  <th className="py-2">상태</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {recent.map((r) => (
                  <tr key={r.kycId} className="border-t">
                    <td className="py-2 font-mono text-xs">
                      {r.applicationNo ?? `KYC-${r.kycId}`}
                    </td>
                    <td className="py-2">{r.corporateType ?? "-"}</td>
                    <td className="py-2">{r.submittedAt?.slice(0, 10) ?? "-"}</td>
                    <td className="py-2">
                      <Badge variant="secondary">{r.status ?? "-"}</Badge>
                    </td>
                    <td className="py-2 text-right">
                      <Button asChild variant="ghost" size="sm">
                        <Link href={`/corporate/kyc/detail?id=${r.kycId}`}>상세</Link>
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </PageShell>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <Card>
      <CardHeader>
        <CardDescription>{label}</CardDescription>
        <CardTitle className="text-3xl font-semibold">{value}</CardTitle>
      </CardHeader>
    </Card>
  );
}
