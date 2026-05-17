"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { PageShell } from "@/components/page-shell";
import {
  formatDate,
  statusBadgeVariant,
  statusLabel
} from "@/components/kyc/status-timeline";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  ApiError,
  type KycCompletionResponse,
  type KycStatusResponse,
  kyc as kycApi
} from "@/lib/api";

export default function CorporateKycCompletePage() {
  return (
    <Suspense>
      <Complete />
    </Suspense>
  );
}

function Complete() {
  const params = useSearchParams();
  const kycId = Number(params.get("id"));
  const valid = Number.isFinite(kycId) && kycId > 0;

  const [completion, setCompletion] = useState<KycCompletionResponse | null>(null);
  const [status, setStatus] = useState<KycStatusResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!valid) return;
    setError(null);
    Promise.all([
      kycApi.completion(kycId).catch(() => null),
      kycApi.status(kycId).catch(() => null)
    ])
      .then(([c, s]) => {
        setCompletion(c);
        setStatus(s);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [kycId, valid]);

  if (!valid) {
    return (
      <PageShell
        title="KYC 인증 완료"
        description="유효한 신청 ID가 필요합니다."
        module="UWEB-018"
        contentClassName="mx-auto flex w-full max-w-[920px] flex-col"
      >
        <Card>
          <CardContent className="text-sm text-muted-foreground">
            올바른 신청 ID를 포함한 URL로 접근해 주세요.
          </CardContent>
        </Card>
      </PageShell>
    );
  }

  const applicationNo = `KYC-${String(kycId).padStart(7, "0")}`;
  const finalStatus = completion?.status ?? status?.kycStatus;
  const credentialIssued = completion?.credentialIssued ?? finalStatus === "VC_ISSUED";

  return (
    <PageShell
      title="KYC 인증 완료"
      description="모든 심사가 완료되었습니다. 이어지는 단계를 확인하세요."
      module="UWEB-018 · M-04"
      contentClassName="mx-auto flex w-full max-w-[920px] flex-col"
    >
      {error ? (
        <Card>
          <CardContent className="text-sm text-destructive">{error}</CardContent>
        </Card>
      ) : null}

      <div className="form-card" style={{ maxWidth: 560 }}>
        <div style={{ textAlign: "center", padding: "24px 0 20px" }}>
          <div
            style={{
              width: 64,
              height: 64,
              borderRadius: "50%",
              background: "var(--success-soft)",
              color: "var(--success)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              margin: "0 auto 16px"
            }}
          >
            <Icon.Check size={30} />
          </div>
          <div
            style={{
              fontSize: 20,
              fontWeight: 800,
              letterSpacing: "-0.02em",
              marginBottom: 6
            }}
          >
            KYC 인증이 완료되었습니다
          </div>
          <div style={{ fontSize: 13.5, color: "var(--text-secondary)" }}>
            {completion?.message ??
              "심사가 완료되었습니다. 발급된 자격 증명을 지갑에서 확인하실 수 있습니다."}
          </div>
        </div>

        <div style={{ borderTop: "1px solid var(--divider)", paddingTop: 20 }}>
          <div className="kv-row">
            <div className="kv-key">신청 번호</div>
            <div className="kv-val mono">{applicationNo}</div>
          </div>
          <div className="kv-row">
            <div className="kv-key">상태</div>
            <div className="kv-val">
              <Badge variant={statusBadgeVariant(finalStatus)}>{statusLabel(finalStatus)}</Badge>
            </div>
          </div>
          {completion?.corporateName ? (
            <div className="kv-row">
              <div className="kv-key">법인명</div>
              <div className="kv-val">{completion.corporateName}</div>
            </div>
          ) : null}
          {completion?.approvedAt ? (
            <div className="kv-row">
              <div className="kv-key">승인일</div>
              <div className="kv-val">{formatDate(completion.approvedAt)}</div>
            </div>
          ) : status?.submittedAt ? (
            <div className="kv-row">
              <div className="kv-key">제출일</div>
              <div className="kv-val">{formatDate(status.submittedAt)}</div>
            </div>
          ) : null}
          <div className="kv-row">
            <div className="kv-key">VC 발급</div>
            <div className="kv-val">
              {credentialIssued ? (
                <Badge variant="success">발급 완료</Badge>
              ) : (
                <Badge variant="secondary">발급 대기</Badge>
              )}
            </div>
          </div>
          {completion?.credentialId ? (
            <div className="kv-row">
              <div className="kv-key">Credential ID</div>
              <div className="kv-val mono">#{completion.credentialId}</div>
            </div>
          ) : null}
        </div>

        <div className="form-actions right" style={{ marginTop: 20 }}>
          <Button asChild variant="link">
            <Link href="/corporate">대시보드로</Link>
          </Button>
          <Button asChild size="lg">
            <Link href="/wallet">지갑 열기</Link>
          </Button>
        </div>
      </div>
    </PageShell>
  );
}
