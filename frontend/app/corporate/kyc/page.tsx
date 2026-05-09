"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { PageShell } from "@/components/page-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  ApiError,
  kyc as kycApi,
  type KycApplicationResponse
} from "@/lib/api";

const STATUS_VARIANT: Record<string, "secondary" | "default" | "outline" | "success" | "destructive"> = {
  DRAFT: "outline",
  SUBMITTED: "secondary",
  IN_REVIEW: "secondary",
  AI_REVIEWING: "secondary",
  SUPPLEMENT_REQUESTED: "destructive",
  APPROVED: "success",
  COMPLETED: "success",
  VC_ISSUED: "success",
  REJECTED: "destructive"
};

export default function CorporateKycListPage() {
  const [current, setCurrent] = useState<KycApplicationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [empty, setEmpty] = useState(false);

  useEffect(() => {
    let cancelled = false;
    kycApi
      .current()
      .then((res) => {
        if (cancelled) return;
        setCurrent(res);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setEmpty(true);
        } else {
          setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <PageShell
      title="KYC 신청 내역"
      description="현재 진행 중인 KYC 신청과 상태를 확인합니다."
      module="UWEB-020 · M-03"
    >
      <Card>
        {loading ? (
          <CardContent className="p-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </CardContent>
        ) : error ? (
          <CardContent className="p-6 text-sm text-destructive">{error}</CardContent>
        ) : empty || !current ? (
          <CardContent className="p-12 text-center text-sm text-muted-foreground">
            <p>진행 중인 KYC 신청이 없습니다.</p>
            <Button asChild className="mt-4">
              <Link href="/corporate/kyc/apply">새 KYC 신청 시작</Link>
            </Button>
          </CardContent>
        ) : (
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="text-left text-xs uppercase text-muted-foreground">
                <tr>
                  <th className="px-4 py-3">신청번호</th>
                  <th className="px-4 py-3">법인 유형</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">신청일</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr className="border-t">
                  <td className="px-4 py-3 font-mono text-xs">
                    KYC-{current.kycId}
                  </td>
                  <td className="px-4 py-3">{current.corporateTypeCode ?? "-"}</td>
                  <td className="px-4 py-3">
                    <Badge variant={STATUS_VARIANT[current.kycStatus ?? ""] ?? "secondary"}>
                      {current.kycStatus ?? "-"}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    {(current.submittedAt ?? current.createdAt)?.slice(0, 10) ?? "-"}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button asChild variant="ghost" size="sm">
                      <Link href={`/corporate/kyc/detail?id=${current.kycId}`}>상세</Link>
                    </Button>
                  </td>
                </tr>
              </tbody>
            </table>
          </CardContent>
        )}
      </Card>
    </PageShell>
  );
}
