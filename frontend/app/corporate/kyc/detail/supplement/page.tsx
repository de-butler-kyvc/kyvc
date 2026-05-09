"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { PageShell } from "@/components/page-shell";
import { formatDate } from "@/components/kyc/status-timeline";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  ApiError,
  type CorporateProfile,
  type Supplement,
  corporate as corpApi,
  kyc as kycApi
} from "@/lib/api";

export default function CorporateKycSupplementPage() {
  return (
    <Suspense>
      <SupplementView />
    </Suspense>
  );
}

function SupplementView() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const supplementId = Number(params.get("supplementId"));
  const valid =
    Number.isFinite(kycId) && kycId > 0 && Number.isFinite(supplementId) && supplementId > 0;

  const [supplement, setSupplement] = useState<Supplement | null>(null);
  const [corp, setCorp] = useState<CorporateProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!valid) return;
    setError(null);
    Promise.all([
      kycApi.supplementDetail(kycId, supplementId),
      corpApi.me().catch(() => null)
    ])
      .then(([s, c]) => {
        setSupplement(s);
        setCorp(c);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [kycId, supplementId, valid]);

  if (!valid) {
    return (
      <PageShell title="보완 요청 상세" description="유효한 ID가 필요합니다." module="UWEB-016">
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            올바른 신청 ID(id)와 보완 요청 ID(supplementId)가 필요합니다.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const applicationNo = `KYC-${String(kycId).padStart(7, "0")}`;
  const items = supplement?.requestedDocumentTypeCodes ?? [];
  const isCompleted =
    supplement?.supplementStatus === "COMPLETED" ||
    supplement?.supplementStatus === "SUBMITTED";

  return (
    <PageShell
      title="보완 요청 상세"
      description="요청된 보완 항목과 마감일을 확인하고 재제출을 진행합니다."
      module="UWEB-016 · M-04"
    >
      {error ? (
        <Card>
          <CardContent className="text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      <div className="form-card">
        <div className="form-card-header">
          <div>
            <div className="form-card-title">{applicationNo}</div>
            {corp ? (
              <div className="form-card-meta">
                {corp.corporateName} · {corp.businessRegistrationNo}
              </div>
            ) : null}
          </div>
          <Badge variant={isCompleted ? "success" : "warning"}>
            <Icon.Alert size={11} />{" "}
            {isCompleted ? "제출 완료" : "보완 필요"}
          </Badge>
        </div>

        <div className="kv-row">
          <div className="kv-key">제출 마감</div>
          <div
            className="kv-val"
            style={{
              color: isCompleted ? "var(--text-primary)" : "var(--danger)",
              fontWeight: 600
            }}
          >
            {formatDate(supplement?.dueAt)}
          </div>
        </div>
        {supplement?.requestedAt ? (
          <div className="kv-row">
            <div className="kv-key">요청일</div>
            <div className="kv-val">{formatDate(supplement.requestedAt)}</div>
          </div>
        ) : null}
        {supplement?.requestReason ? (
          <div className="kv-row">
            <div className="kv-key">요청 사유</div>
            <div className="kv-val">{supplement.requestReason}</div>
          </div>
        ) : null}

        <div style={{ marginTop: 16 }}>
          <div
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: "var(--text-secondary)",
              marginBottom: 12
            }}
          >
            보완 요청 항목
          </div>
          {items.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              {supplement?.message ?? "요청 항목이 없습니다."}
            </p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {items.map((code, i) => (
                <div
                  key={code + i}
                  style={{
                    border: "1px solid var(--border)",
                    borderRadius: "var(--radius-md)",
                    padding: "14px 16px"
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 8,
                      marginBottom: 6
                    }}
                  >
                    <div
                      style={{
                        width: 20,
                        height: 20,
                        borderRadius: "50%",
                        background: "var(--danger-soft)",
                        color: "var(--danger)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0
                      }}
                    >
                      <Icon.Alert size={11} />
                    </div>
                    <span style={{ fontSize: 13.5, fontWeight: 700 }}>
                      {documentTypeLabel(code)}
                    </span>
                  </div>
                  <div
                    style={{
                      fontSize: 13,
                      color: "var(--text-secondary)",
                      lineHeight: 1.6,
                      paddingLeft: 28
                    }}
                  >
                    {supplement?.message ??
                      supplement?.requestReason ??
                      "심사역 요청에 따라 서류를 재제출해 주세요."}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="form-actions right">
        <Button asChild variant="ghost">
          <Link href={`/corporate/kyc/detail?id=${kycId}`}>← 진행 상태</Link>
        </Button>
        {!isCompleted ? (
          <Button asChild size="lg">
            <Link
              href={`/corporate/kyc/detail/documents?id=${kycId}&supplementId=${supplementId}`}
            >
              보완 서류 업로드
            </Link>
          </Button>
        ) : null}
      </div>
    </PageShell>
  );
}

function documentTypeLabel(code?: string) {
  if (!code) return "-";
  const map: Record<string, string> = {
    BUSINESS_REGISTRATION: "사업자등록증",
    CORPORATE_REGISTRATION: "등기사항전부증명서",
    SHAREHOLDER_LIST: "주주명부",
    ARTICLES_OF_INCORPORATION: "정관",
    POWER_OF_ATTORNEY: "위임장",
    REPRESENTATIVE_ID: "대표자 신분증",
    AGENT_ID: "대리인 신분증",
    OTHER: "기타"
  };
  return map[code] ?? code;
}
