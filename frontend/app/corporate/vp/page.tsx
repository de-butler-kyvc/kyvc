"use client";

import { useEffect, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { credentials as credentialsApi, type CredentialSummary } from "@/lib/api";

export default function CorporateVpHistoryPage() {
  const [credentials, setCredentials] = useState<CredentialSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    credentialsApi
      .list()
      .then((res) => {
        if (!cancelled) setCredentials(res.credentials ?? []);
      })
      .catch(() => {
        if (!cancelled) setCredentials([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="mx-auto flex w-full max-w-[1040px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">VP 제출 이력 조회</h1>
          <p className="page-head-desc">검증기관에 제출한 VP 검증 결과를 확인합니다.</p>
        </div>
      </div>

      <table className="table">
        <thead>
          <tr>
            <th>검증기관</th>
            <th>제출 VC</th>
            <th>제출일시</th>
            <th>KYC 신청번호</th>
            <th>결과</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={5} className="empty-state">불러오는 중...</td>
            </tr>
          ) : credentials.length === 0 ? (
            <tr>
              <td colSpan={5} className="empty-state">VP 제출 이력이 없습니다.</td>
            </tr>
          ) : (
            credentials.map((credential) => (
              <tr key={credential.credentialId}>
                <td className="font-semibold">-</td>
                <td>{credential.credentialTypeCode ?? "KYC_CREDENTIAL"}</td>
                <td className="mono text-[12.5px]">-</td>
                <td className="mono text-[12.5px] text-muted-foreground">
                  {credential.kycId ? `KYC-${credential.kycId}` : "-"}
                </td>
                <td>
                  <Badge variant="secondary">제출 이력 없음</Badge>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
