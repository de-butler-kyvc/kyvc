"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  kyc as kycApi,
  type KycApplicationResponse,
} from "@/lib/api";

const ACTIVE_STATUSES = new Set([
  "DRAFT",
  "SUBMITTED",
  "AI_REVIEWING",
  "NEED_SUPPLEMENT",
  "MANUAL_REVIEW",
]);
const REVIEW = new Set(["SUBMITTED", "AI_REVIEWING", "MANUAL_REVIEW"]);
const SUPPLEMENT = new Set(["NEED_SUPPLEMENT"]);

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

  const filtered = useMemo(
    () =>
      items.filter((item) => {
        const status = item.kycStatus ?? "";
        return ACTIVE_STATUSES.has(status);
      }),
    [items],
  );

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 진행상태 조회</h1>
          <p className="page-head-desc">현재 진행 중인 KYC 신청과 상태를 확인합니다.</p>
        </div>
      </div>

      <table className="table">
        <thead>
          <tr>
            <th>신청번호</th>
            <th>법인 유형</th>
            <th>상태</th>
            <th>신청일</th>
            <th>상세</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <EmptyRow text="불러오는 중..." />
          ) : error ? (
            <EmptyRow text={error} danger />
          ) : filtered.length === 0 ? (
            <EmptyRow text="현재 진행 중인 KYC 신청이 없습니다." />
          ) : (
            filtered.map((item) => (
              <tr key={item.kycId}>
                <td className="mono text-[13px]">KYC-{item.kycId}</td>
                <td>{item.corporateTypeCode ?? "-"}</td>
                <td>
                  <Badge variant={variant(item.kycStatus)}>{label(item.kycStatus)}</Badge>
                </td>
                <td>{formatDate(item.submittedAt ?? item.createdAt)}</td>
                <td>
                  <Button asChild variant="link" size="sm">
                    <Link href={`/corporate/kyc/detail?id=${item.kycId}`}>상세 보기</Link>
                  </Button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      {!loading && !error && items.length === 0 ? (
        <Button asChild className="mt-4 self-start">
          <Link href="/corporate/kyc/apply">KYC 신청 시작</Link>
        </Button>
      ) : null}
    </div>
  );
}

function EmptyRow({ text, danger = false }: { text: string; danger?: boolean }) {
  return (
    <tr>
      <td colSpan={5} className={`empty-state ${danger ? "text-destructive" : ""}`}>
        {text}
      </td>
    </tr>
  );
}

function formatDate(value?: string | null) {
  return value ? value.slice(0, 10).replace(/-/g, ".") : "-";
}

function label(status?: string | null) {
  return (
    {
      DRAFT: "작성중",
      SUBMITTED: "접수완료",
      IN_REVIEW: "심사중",
      AI_REVIEWING: "AI 심사중",
      NEED_SUPPLEMENT: "보완필요",
      MANUAL_REVIEW: "수동 심사중",
      APPROVED: "승인",
      COMPLETED: "완료",
      VC_ISSUED: "VC 발급완료",
      REJECTED: "반려",
    }[status ?? ""] ?? status ?? "-"
  );
}

function variant(status?: string | null) {
  if (SUPPLEMENT.has(status ?? "") || status === "REJECTED") return "destructive";
  if (REVIEW.has(status ?? "")) return "secondary";
  return "outline";
}
