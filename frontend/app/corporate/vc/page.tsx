"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError, credentials as credentialsApi, type CredentialDetailResponse, type CredentialSummary } from "@/lib/api";

export default function CorporateVcHistoryPage() {
  const [rows, setRows] = useState<CredentialSummary[]>([]);
  const [detail, setDetail] = useState<CredentialDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    credentialsApi
      .list()
      .then((res) => {
        if (!cancelled) setRows(res.credentials ?? []);
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

  const loadDetail = async (credentialId: number) => {
    setError(null);
    try {
      setDetail(await credentialsApi.detail(credentialId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "상세 조회에 실패했습니다.");
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1040px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">VC 발급 이력 조회</h1>
          <p className="page-head-desc">발급된 법인 KYC Credential 이력을 확인합니다.</p>
        </div>
        <div className="page-head-actions">
          <Button asChild>
            <Link href="/corporate/vc/issue">VC 발급 안내</Link>
          </Button>
        </div>
      </div>

      {error ? (
        <div className="alert alert-warning mb-4">
          <Icon.Alert size={16} className="alert-icon" />
          <span>{error}</span>
        </div>
      ) : null}

      <table className="table">
        <thead>
          <tr>
            <th>Credential 유형</th>
            <th>발급자</th>
            <th>KYC 신청번호</th>
            <th>발급일</th>
            <th>만료일</th>
            <th>상태</th>
            <th className="text-right">상세</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <Empty text="불러오는 중..." />
          ) : rows.length === 0 ? (
            <Empty text="발급된 VC가 없습니다." />
          ) : (
            rows.map((row) => (
              <tr key={row.credentialId}>
                <td className="font-semibold">{row.credentialTypeCode ?? "KYC_CREDENTIAL"}</td>
                <td className="text-muted-foreground">{row.issuerDid ?? "KYvC Platform"}</td>
                <td className="mono text-[12.5px] text-muted-foreground">
                  {row.kycId ? `KYC-${row.kycId}` : "-"}
                </td>
                <td>{formatDate(row.issuedAt)}</td>
                <td>{formatDate(row.expiresAt)}</td>
                <td>
                  <Badge variant={row.credentialStatusCode === "VALID" ? "success" : "secondary"}>
                    {row.credentialStatusCode ?? "-"}
                  </Badge>
                </td>
                <td className="text-right">
                  <Button type="button" variant="ghost" size="sm" onClick={() => loadDetail(row.credentialId)}>
                    <Icon.Eye size={14} />
                  </Button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      {detail ? (
        <section className="form-card mt-5 max-w-[720px]">
          <div className="form-card-header">
            <div className="form-card-title">Credential 상세</div>
            <div className="form-card-meta">#{detail.credentialId}</div>
          </div>
          <Row label="외부 ID" value={detail.credentialExternalId ?? "-"} mono />
          <Row label="VC Hash" value={detail.vcHash ?? "-"} mono />
          <Row label="XRPL Tx" value={detail.xrplTxHash ?? "-"} mono />
          <Row label="Holder DID" value={detail.holderDid ?? "-"} mono />
          <Row label="Wallet 저장" value={detail.walletSaved || detail.walletSavedYn === "Y" ? "Y" : "N"} />
        </section>
      ) : null}
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <tr>
      <td colSpan={7} className="empty-state">
        {text}
      </td>
    </tr>
  );
}

function Row({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="kv-row">
      <div className="kv-key">{label}</div>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value}</div>
    </div>
  );
}

function formatDate(value?: string | null) {
  return value ? value.slice(0, 10).replace(/-/g, ".") : "-";
}
