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
  const [items, setItems] = useState<KycApplicationResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    kycApi
      .list()
      .then((res) => {
        if (cancelled) return;
        const list = Array.isArray(res) ? res : res ? [res] : [];
        const sorted = [...list].sort((a, b) => {
          const ad = a.submittedAt ?? a.createdAt ?? "";
          const bd = b.submittedAt ?? b.createdAt ?? "";
          return bd.localeCompare(ad);
        });
        setItems(sorted);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setItems([]);
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
      contentClassName="mx-auto flex w-full max-w-[920px] flex-col"
    >
      <Card>
        {loading ? (
          <CardContent className="p-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </CardContent>
        ) : error ? (
          <CardContent className="p-6 text-sm text-destructive">{error}</CardContent>
        ) : items.length === 0 ? (
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
                {items.map((item) => (
                  <tr key={item.kycId} className="border-t">
                    <td className="px-4 py-3 font-mono text-xs">
                      KYC-{item.kycId}
                    </td>
                    <td className="px-4 py-3">{item.corporateTypeCode ?? "-"}</td>
                    <td className="px-4 py-3">
                      <Badge variant={STATUS_VARIANT[item.kycStatus ?? ""] ?? "secondary"}>
                        {item.kycStatus ?? "-"}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      {(item.submittedAt ?? item.createdAt)?.slice(0, 10) ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button asChild variant="ghost" size="sm">
                        <Link href={`/corporate/kyc/detail?id=${item.kycId}`}>상세</Link>
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
