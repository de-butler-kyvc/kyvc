"use client";

import { useEffect, useState } from "react";

import { Badge } from "@/components/ui/badge";
import {
  type UserVpPresentationSummary,
  userVpPresentations
} from "@/lib/api";

export default function CorporateVpHistoryPage() {
  const [items, setItems] = useState<UserVpPresentationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    userVpPresentations
      .list({ page: 0, size: 50 })
      .then((res) => {
        if (!cancelled) setItems(res.items ?? []);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setItems([]);
          setError(err instanceof Error ? err.message : "조회에 실패했습니다.");
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
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">VP 제출 이력 조회</h1>
          <p className="page-head-desc">검증기관에 제출한 VP 검증 결과를 확인합니다.</p>
        </div>
      </div>

      <div className="table-scroll">
        <table className="table history-table">
          <thead>
            <tr>
              <th>검증기관</th>
              <th>제출 목적</th>
              <th>요청 ID</th>
              <th>검증일시</th>
              <th>결과</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} className="empty-state">불러오는 중...</td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={5} className="empty-state text-destructive">{error}</td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-state">VP 제출 이력이 없습니다.</td>
              </tr>
            ) : (
              items.map((item) => (
                <tr key={item.presentationId}>
                  <td className="font-semibold">{item.verifierName ?? "-"}</td>
                  <td>{purposeLabel(item.purpose)}</td>
                  <td className="mono text-[12.5px] text-muted-foreground" title={item.requestId}>
                    {item.requestId ? truncateMiddle(item.requestId) : "-"}
                  </td>
                  <td className="mono text-[12.5px]">
                    {formatDate(item.presentedAt)}
                  </td>
                  <td>
                    <Badge variant={statusVariant(item.verificationStatus)}>
                      {statusLabel(item.verificationStatus)}
                    </Badge>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function truncateMiddle(value: string) {
  return value.length > 11 ? `${value.slice(0, 4)}...${value.slice(-4)}` : value;
}

function formatDate(value?: string) {
  return value ? value.slice(0, 16).replace("T", " ") : "-";
}

function purposeLabel(value?: string) {
  return (
    {
      ACCOUNT_OPENING: "계좌 개설",
      RE_AUTH: "재인증",
      KYC_VERIFICATION: "KYC 검증"
    }[value ?? ""] ?? value ?? "-"
  );
}

function statusLabel(value?: string) {
  return (
    {
      REQUESTED: "요청",
      PENDING: "대기",
      PRESENTED: "제출",
      VALID: "검증 완료",
      VERIFIED: "검증 완료",
      INVALID: "검증 실패",
      EXPIRED: "만료",
      REPLAY_SUSPECTED: "재제출 의심"
    }[value ?? ""] ?? value ?? "-"
  );
}

function statusVariant(value?: string) {
  if (value === "VALID" || value === "VERIFIED") return "success";
  if (value === "INVALID" || value === "EXPIRED" || value === "REPLAY_SUSPECTED") {
    return "destructive";
  }
  return "secondary";
}
