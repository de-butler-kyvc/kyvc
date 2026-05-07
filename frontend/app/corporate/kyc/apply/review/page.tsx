"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { StepIndicator } from "@/components/kyc/step-indicator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ApiError, type KycDocument, kyc as kycApi } from "@/lib/api";

const TYPE_LABEL: Record<string, string> = {
  BUSINESS_LICENSE: "사업자등록증",
  CORP_REGISTRY: "등기사항전부증명서",
  SHAREHOLDERS: "주주명부",
  ARTICLES: "정관",
  POA: "위임장",
  OTHER: "기타"
};

type SummaryDoc = KycDocument & { fileSize?: number };

export default function KycApplyReviewPage() {
  const router = useRouter();
  const [kycId, setKycId] = useState<number | null>(null);
  const [documents, setDocuments] = useState<SummaryDoc[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const id =
      typeof window !== "undefined"
        ? Number(window.localStorage.getItem("kyvc.currentKycId"))
        : 0;
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    setKycId(id);
    Promise.all([
      kycApi.documents(id).then((r) => r.items ?? []),
      kycApi.submissionSummary(id).catch(() => null)
    ])
      .then(([items, summary]) => {
        const merged: SummaryDoc[] = items.map((it) => {
          const s = summary?.documents.find((d) => d.documentId === it.documentId);
          return { ...it, ...s };
        });
        setDocuments(merged);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  const onSubmit = async () => {
    if (!kycId) return;
    setBusy(true);
    setError(null);
    try {
      const res = await kycApi.submit(kycId);
      if (typeof window !== "undefined") {
        window.localStorage.removeItem("kyvc.currentKycId");
      }
      router.push(`/corporate/kyc/detail?id=${res.kycId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "제출에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-6 px-9 py-8">
      <StepIndicator current={5} />

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          업로드 서류 미리보기
        </h1>
        <p className="text-[13px] text-destructive">
          업로드된 서류 목록과 해시를 확인하세요.
        </p>
      </div>

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-[13px]">
            <thead className="bg-secondary text-left">
              <tr className="border-b border-border">
                <Th>파일명</Th>
                <Th>서류 유형</Th>
                <Th>크기</Th>
                <Th>해시</Th>
                <Th>상태</Th>
                <Th>&nbsp;</Th>
              </tr>
            </thead>
            <tbody>
              {documents.length === 0 ? (
                <tr>
                  <td
                    colSpan={6}
                    className="px-4 py-10 text-center text-muted-foreground"
                  >
                    업로드된 서류가 없습니다.
                  </td>
                </tr>
              ) : (
                documents.map((d) => (
                  <tr key={d.documentId} className="border-b border-row-border last:border-0">
                    <td className="px-4 py-4 font-medium text-foreground">
                      {d.fileName ?? "-"}
                    </td>
                    <td className="px-4 py-4 text-muted-foreground">
                      {TYPE_LABEL[d.documentType] ?? d.documentType}
                    </td>
                    <td className="px-4 py-4 text-subtle-foreground">
                      {formatSize(d.fileSize)}
                    </td>
                    <td className="px-4 py-4 font-mono text-[12px] text-subtle-foreground">
                      {d.fileHash ? `${d.fileHash.slice(0, 8)}...` : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant="success">{d.status ?? "완료"}</Badge>
                    </td>
                    <td className="px-4 py-4 text-right">
                      <button
                        type="button"
                        onClick={() => router.push("/corporate/kyc/apply/upload")}
                        className="text-[12px] text-muted-foreground hover:text-foreground hover:underline"
                      >
                        교체
                      </button>
                    </td>
                  </tr>
                ))
              )}
              <tr>
                <td colSpan={6} className="border-t border-row-border">
                  <button
                    type="button"
                    onClick={() => router.push("/corporate/kyc/apply/upload")}
                    className="block w-full px-4 py-3 text-center text-[13px] text-muted-foreground hover:bg-secondary/40 hover:text-foreground"
                  >
                    + 서류 추가하기
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </CardContent>
      </Card>

      {error ? <p className="text-[12px] text-destructive">{error}</p> : null}

      <div className="flex items-center gap-2">
        <Button onClick={onSubmit} disabled={busy} className="rounded-[10px] px-5">
          {busy ? "제출 중..." : "다음 — 저장 옵션 →"}
        </Button>
        <Button
          variant="outline"
          className="rounded-[10px] px-5"
          onClick={() => router.push("/corporate/kyc/apply/upload")}
        >
          이전
        </Button>
      </div>
    </div>
  );
}

function formatSize(bytes?: number) {
  if (!bytes) return "-";
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(0)}KB`;
  return `${bytes}B`;
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th className="px-4 py-2.5 text-[11px] font-bold text-subtle-foreground">{children}</th>
  );
}
