"use client";

import { useRouter, useSearchParams } from "next/navigation";
import type { ReactNode } from "react";
import { Suspense, useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  type KycCompletionResponse,
  kyc as kycApi
} from "@/lib/api";

export default function KycApplyCompletePage() {
  return (
    <Suspense>
      <KycComplete />
    </Suspense>
  );
}

function KycComplete() {
  const router = useRouter();
  const params = useSearchParams();
  const fallbackId =
    typeof window !== "undefined"
      ? window.localStorage.getItem("kyvc.lastSubmittedKycId")
      : null;
  const idStr = params.get("id") ?? fallbackId;
  const kycId = idStr ? Number(idStr) : null;

  const [data, setData] = useState<KycCompletionResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!kycId || !Number.isFinite(kycId)) return;
    let cancelled = false;
    kycApi
      .completion(kycId)
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.");
      });
    return () => {
      cancelled = true;
    };
  }, [kycId]);

  const submittedAt =
    typeof window !== "undefined"
      ? window.localStorage.getItem("kyvc.lastSubmittedAt")
      : null;

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col px-9 py-8">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 신청 완료</h1>
          <p className="page-head-desc">신청이 접수되었습니다. 심사 진행 상태를 확인할 수 있습니다.</p>
        </div>
      </div>

      <section className="form-card max-w-[560px]">
        <div className="py-6 text-center">
          <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-success-bg text-success">
            <Icon.Check size={30} />
          </div>
          <div className="mb-1.5 text-[20px] font-extrabold tracking-[-0.02em]">
            KYC 신청 완료
          </div>
          <div className="text-[13.5px] text-muted-foreground">
            {data?.message ?? "제출 서류 확인 후 AI 심사와 운영 검토가 진행됩니다."}
          </div>
        </div>

        <div className="border-t border-row-border pt-5">
          <ConfirmRow label="신청번호" value={kycId ? `KYC-${kycId}` : "-"} mono />
          <ConfirmRow
            label="상태"
            value={
              <Badge variant="default">{data?.status ?? "심사 접수"}</Badge>
            }
          />
          <ConfirmRow label="접수일시" value={formatDateTime(submittedAt)} />
          <ConfirmRow
            label="법인명"
            value={data?.corporateName ?? "-"}
          />
          <ConfirmRow
            label="VC 상태"
            value={
              <Badge variant={data?.credentialIssued ? "success" : "outline"}>
                {data?.credentialIssued ? "발급 완료" : "발급 대기"}
              </Badge>
            }
          />
        </div>

        {error ? (
          <p className="mt-4 text-[12px] text-destructive">{error}</p>
        ) : null}

        <div className="form-actions right mt-5">
          <button type="button" className="link-button" onClick={() => router.push("/corporate")}>
            대시보드
          </button>
          <Button
            type="button"
            onClick={() =>
              router.push(kycId ? `/corporate/kyc/detail?id=${kycId}` : "/corporate/kyc")
            }
          >
            진행 상태 보기
          </Button>
        </div>
      </section>
    </div>
  );
}

function ConfirmRow({
  label,
  value,
  mono = false
}: {
  label: string;
  value: ReactNode;
  mono?: boolean;
}) {
  return (
    <div className="kv-row">
      <div className="kv-key">{label}</div>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value || "-"}</div>
    </div>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}
