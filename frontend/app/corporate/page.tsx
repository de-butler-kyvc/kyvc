"use client";

import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { corporate } from "@/lib/api";
import { useApi } from "@/lib/use-api";

const STATUS_VARIANT: Record<string, "success" | "warning" | "default" | "secondary"> = {
  VC_ISSUED: "success",
  APPROVED: "success",
  COMPLETED: "success",
  SUPPLEMENT_REQUESTED: "warning",
  AI_REVIEWING: "default",
  IN_REVIEW: "default"
};

export default function CorporateDashboardPage() {
  const { data, error, loading } = useApi(() => corporate.dashboard());

  const corp = data?.corporate;
  const summary = data?.kycSummary ?? {};
  const issued = data?.credentialSummary?.issued ?? 0;
  const recent = data?.recentApplications ?? [];
  const alert = (data?.notifications ?? []).find(
    (n) => n.severity === "warning" || n.severity === "error"
  );
  const nextStep = data?.notifications?.find((n) => n.severity !== "info");

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-4 px-9 py-7">
      <Card className="flex flex-col gap-2.5 p-5">
        <h1 className="text-[16px] font-extrabold tracking-[-0.3px] text-foreground">
          법인 사용자 대시보드
        </h1>
        <div className="flex flex-wrap items-center gap-4 rounded-lg bg-secondary px-3.5 py-2.5 text-[12px]">
          <span className="font-semibold text-muted-foreground">법인 식별정보</span>
          <span className="font-bold text-foreground">
            {loading ? "..." : (corp?.corporateName ?? "법인 미등록")}
          </span>
          {corp?.businessNo ? (
            <span className="text-subtle-foreground">{corp.businessNo}</span>
          ) : null}
        </div>
        <div className="rounded-lg border border-accent-border bg-accent px-3.5 py-2.5 text-[12px] font-semibold text-accent-foreground">
          다음 단계 안내 · 신청 현황을 확인하고 다음 단계를 진행하세요.
        </div>
      </Card>

      {error ? (
        <Card>
          <CardContent className="p-4 text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      <div className="flex flex-col gap-1 pt-2">
        <h2 className="text-[20px] font-bold tracking-[-0.4px] text-foreground">
          {corp?.corporateName ? `안녕하세요, ${corp.corporateName} 님` : "안녕하세요"}
        </h2>
        {corp?.businessNo ? (
          <p className="text-[13px] text-muted-foreground">
            {corp.corporateName} · 사업자번호 {corp.businessNo}
          </p>
        ) : null}
      </div>

      {alert ? (
        <div className="flex items-start gap-2.5 rounded-[10px] border border-warning-border bg-warning-bg px-4 py-3 text-[13px] text-warning">
          <span aria-hidden>⚠</span>
          <span>{alert.message}</span>
        </div>
      ) : null}

      <div className="grid grid-cols-2 gap-3.5 md:grid-cols-4">
        <StatCard label="KYC 신청" value={summary.total ?? 0} />
        <StatCard label="심사중" value={summary.inReview ?? 0} tone="primary" />
        <StatCard label="보완필요" value={summary.supplement ?? 0} tone="warning" />
        <StatCard label="발급된 VC" value={issued} tone="success" />
      </div>

      <div className="grid gap-5 md:grid-cols-3">
        <div className="md:col-span-2 flex flex-col gap-3.5">
          <div className="flex items-center justify-between">
            <h3 className="text-[14px] font-bold text-foreground">최근 KYC 신청</h3>
            <Button asChild size="sm" className="rounded-[10px]">
              <Link href="/corporate/kyc/apply">새 KYC 신청</Link>
            </Button>
          </div>
          <Card className="overflow-hidden p-0">
            <table className="w-full text-[13px]">
              <thead className="bg-secondary text-left">
                <tr className="border-b border-border">
                  <Th>신청번호</Th>
                  <Th>법인 유형</Th>
                  <Th>신청일</Th>
                  <Th>상태</Th>
                  <Th>&nbsp;</Th>
                </tr>
              </thead>
              <tbody>
                {recent.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-10 text-center text-muted-foreground">
                      최근 신청 내역이 없습니다.
                    </td>
                  </tr>
                ) : (
                  recent.map((r) => (
                    <tr key={r.kycId} className="border-b border-row-border last:border-0">
                      <td className="px-4 py-4 font-mono text-[12px] text-foreground">
                        {r.applicationNo ?? `KYC-${r.kycId}`}
                      </td>
                      <td className="px-4 py-4">{r.corporateType ?? "-"}</td>
                      <td className="px-4 py-4 text-subtle-foreground">
                        {r.submittedAt?.slice(0, 10).replace(/-/g, ".") ?? "-"}
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant={STATUS_VARIANT[r.status ?? ""] ?? "secondary"}>
                          {r.status ?? "-"}
                        </Badge>
                      </td>
                      <td className="px-4 py-4 text-right">
                        <Link
                          href={`/corporate/kyc/detail?id=${r.kycId}`}
                          className="text-[12px] text-primary hover:underline"
                        >
                          상세 보기
                        </Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </Card>
        </div>

        <div className="flex flex-col gap-3">
          <h3 className="text-[14px] font-bold text-foreground">다음 단계 안내</h3>
          <Card className="flex flex-col gap-2 p-5">
            <div className="text-[13px] font-semibold text-warning">
              {nextStep?.message ? "보완 서류 제출 필요" : "진행 중인 작업이 없습니다"}
            </div>
            {nextStep?.message ? (
              <p className="text-[12px] leading-[1.8] text-muted-foreground">
                {nextStep.message}
              </p>
            ) : null}
            <Button asChild size="sm" className="mt-1 w-full rounded-[10px]">
              <Link href="/corporate/kyc">신청 내역 보기</Link>
            </Button>
          </Card>
        </div>
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  tone = "default"
}: {
  label: string;
  value: number;
  tone?: "default" | "primary" | "warning" | "success";
}) {
  const valueColor =
    tone === "primary"
      ? "text-primary"
      : tone === "warning"
        ? "text-warning"
        : tone === "success"
          ? "text-success"
          : "text-foreground";
  const isWarning = tone === "warning";
  return (
    <Card
      className={`flex flex-col items-center gap-2 px-4 py-5 ${
        isWarning ? "border-warning-border bg-warning-bg" : ""
      }`}
    >
      <div className="text-[11px] font-medium uppercase tracking-[0.55px] text-subtle-foreground">
        {label}
      </div>
      <div className={`text-[28px] font-bold leading-none ${valueColor}`}>
        {value}
      </div>
    </Card>
  );
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="px-4 py-2.5 text-[11px] font-bold text-subtle-foreground">
      {children}
    </th>
  );
}
