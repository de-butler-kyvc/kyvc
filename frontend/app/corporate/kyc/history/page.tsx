"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError, kyc as kycApi, type KycApplicationHistoryItem } from "@/lib/api";

const TABS = [
  { id: "all", label: "전체" },
  { id: "review", label: "심사중" },
  { id: "done", label: "완료" },
  { id: "supplement", label: "보완" },
  { id: "auth", label: "VC 발급" }
] as const;

const REVIEW = new Set(["SUBMITTED", "IN_REVIEW", "AI_REVIEWING"]);
const SUPPLEMENT = new Set(["NEED_SUPPLEMENT", "SUPPLEMENT_REQUESTED"]);
const DONE = new Set(["APPROVED", "COMPLETED", "VC_ISSUED"]);

export default function CorporateKycHistoryPage() {
  const [tab, setTab] = useState<(typeof TABS)[number]["id"]>("all");
  const [rows, setRows] = useState<KycApplicationHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    kycApi
      .history({ page: 0, size: 100 })
      .then((res) => {
        if (cancelled) return;
        setRows(res.items ?? []);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setRows([]);
        else setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
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
      rows.filter((row) => {
        const status = row.kycStatusCode ?? "";
        if (tab === "all") return true;
        if (tab === "review") return REVIEW.has(status);
        if (tab === "done") return DONE.has(status);
        if (tab === "supplement") return SUPPLEMENT.has(status);
        if (tab === "auth") return status === "VC_ISSUED" || Boolean(row.credentialId);
        return true;
      }),
    [rows, tab]
  );

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 신청 이력</h1>
          <p className="page-head-desc">신청한 법인 KYC의 진행 상태와 완료 이력을 확인합니다.</p>
        </div>
      </div>

      <div className="mb-4 flex w-fit gap-1 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] p-1">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`rounded-[var(--radius-sm)] px-3.5 py-1.5 text-[13px] ${tab === item.id ? "bg-[var(--surface)] font-semibold text-foreground shadow-sm" : "text-muted-foreground"}`}
            onClick={() => setTab(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <table className="table">
        <thead>
          <tr>
            <th>신청번호</th>
            <th>법인 유형</th>
            <th>신청일</th>
            <th>완료일</th>
            <th>상태</th>
            <th>상세</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <EmptyRow text="불러오는 중..." />
          ) : error ? (
            <EmptyRow text={error} danger />
          ) : filtered.length === 0 ? (
            <EmptyRow text="표시할 KYC 이력이 없습니다." />
          ) : (
            filtered.map((row) => (
              <tr key={row.kycId}>
                <td className="mono text-[13px]">KYC-{row.kycId}</td>
                <td>{row.corporateTypeCode ?? "-"}</td>
                <td>{formatDate(row.submittedAt ?? row.createdAt)}</td>
                <td className="text-muted-foreground">{formatFinishedAt(row)}</td>
                <td>
                  <Badge variant={variant(row.kycStatusCode)}>{label(row.kycStatusCode)}</Badge>
                </td>
                <td>
                  <Button asChild variant="link" size="sm">
                    <Link href={`/corporate/kyc/detail?id=${row.kycId}`}>상세 보기</Link>
                  </Button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function EmptyRow({ text, danger = false }: { text: string; danger?: boolean }) {
  return (
    <tr>
      <td colSpan={6} className={`empty-state ${danger ? "text-destructive" : ""}`}>
        {text}
      </td>
    </tr>
  );
}

function formatDate(value?: string | null) {
  return value ? value.slice(0, 10).replace(/-/g, ".") : "-";
}

function formatFinishedAt(row: KycApplicationHistoryItem) {
  const status = row.kycStatusCode ?? "";
  if (DONE.has(status)) {
    return formatDate(row.approvedAt ?? row.credentialIssuedAt ?? row.updatedAt);
  }
  if (status === "REJECTED") {
    return formatDate(row.rejectedAt ?? row.updatedAt);
  }
  return "-";
}

function label(status?: string | null) {
  return (
    {
      DRAFT: "작성중",
      SUBMITTED: "제출됨",
      IN_REVIEW: "심사중",
      AI_REVIEWING: "AI 심사중",
      NEED_SUPPLEMENT: "보완필요",
      APPROVED: "승인",
      COMPLETED: "완료",
      VC_ISSUED: "VC 발급완료",
      REJECTED: "반려"
    }[status ?? ""] ?? status ?? "-"
  );
}

function variant(status?: string | null) {
  if (SUPPLEMENT.has(status ?? "") || status === "REJECTED") return "destructive";
  if (DONE.has(status ?? "")) return "success";
  if (REVIEW.has(status ?? "")) return "secondary";
  return "outline";
}
