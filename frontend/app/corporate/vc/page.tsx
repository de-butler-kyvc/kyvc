"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  credentials as credentialsApi,
  didInstitutions,
  type CredentialSummary
} from "@/lib/api";

export default function CorporateVcHistoryPage() {
  const [rows, setRows] = useState<CredentialSummary[]>([]);
  const [issuerNames, setIssuerNames] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    credentialsApi
      .list()
      .then((res) => {
        const credentials = res.credentials ?? [];
        if (!cancelled) setRows(credentials);
        return resolveIssuerNames(credentials);
      })
      .then((names) => {
        if (!cancelled) setIssuerNames(names);
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

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
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

      <div className="table-scroll">
        <table className="table history-table">
          <thead>
            <tr>
              <th>XRPL CredentialType</th>
              <th>발급기관명</th>
              <th>발급기관 DID</th>
              <th>KYC 신청번호</th>
              <th>상태</th>
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
                  <td className="font-semibold" title={row.credentialTypeCode ?? "KYC_CREDENTIAL"}>
                    {truncateMiddle(row.credentialTypeCode ?? "KYC_CREDENTIAL")}
                  </td>
                  <td className="font-semibold">{issuerName(row.issuerDid, issuerNames)}</td>
                  <td className="mono text-[12.5px] text-muted-foreground" title={row.issuerDid}>
                    {row.issuerDid ? truncateMiddle(row.issuerDid) : "-"}
                  </td>
                  <td className="mono text-[12.5px] text-muted-foreground">
                    {row.kycId ? `KYC-${row.kycId}` : "-"}
                  </td>
                  <td>
                    <Badge variant={row.credentialStatusCode === "VALID" ? "success" : "secondary"}>
                      {row.credentialStatusCode ?? "-"}
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

function Empty({ text }: { text: string }) {
  return (
    <tr>
      <td colSpan={5} className="empty-state">
        {text}
      </td>
    </tr>
  );
}

function truncateMiddle(value: string) {
  return value.length > 11 ? `${value.slice(0, 4)}...${value.slice(-4)}` : value;
}

async function resolveIssuerNames(rows: CredentialSummary[]) {
  const issuerDids = Array.from(
    new Set(rows.map((row) => row.issuerDid).filter((did): did is string => !!did))
  );
  const pairs = await Promise.all(
    issuerDids.map(async (did) => {
      try {
        const institution = await didInstitutions.get(did);
        return [did, institution.institutionName] as const;
      } catch {
        return [did, ""] as const;
      }
    })
  );
  return Object.fromEntries(pairs.filter(([, name]) => name));
}

function issuerName(issuerDid: string | undefined, issuerNames: Record<string, string>) {
  if (!issuerDid) return "-";
  return issuerNames[issuerDid] ?? "미등록 기관";
}
