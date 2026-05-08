"use client";

import { useRouter, useSearchParams } from "next/navigation";
import type { ReactNode } from "react";
import { Suspense } from "react";

import { Icon } from "@/components/design/icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

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
  const id =
    params.get("id") ??
    (typeof window !== "undefined" ? window.localStorage.getItem("kyvc.lastSubmittedKycId") : null);
  const submittedAt =
    typeof window !== "undefined" ? window.localStorage.getItem("kyvc.lastSubmittedAt") : null;

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
            제출 서류 확인 후 AI 심사와 운영 검토가 진행됩니다.
          </div>
        </div>

        <div className="border-t border-row-border pt-5">
          <ConfirmRow label="신청번호" value={id ? `KYC-${id}` : "-"} mono />
          <ConfirmRow label="상태" value={<Badge variant="default">심사 접수</Badge>} />
          <ConfirmRow label="접수일시" value={formatSubmittedAt(submittedAt)} />
          <ConfirmRow label="예상 소요" value="영업일 기준 1~3일" />
          <ConfirmRow label="VC 상태" value={<Badge variant="outline">발급 대기</Badge>} />
        </div>

        <div className="form-actions right mt-5">
          <button type="button" className="link-button" onClick={() => router.push("/corporate")}>
            대시보드
          </button>
          <Button type="button" onClick={() => router.push(id ? `/corporate/kyc/detail?id=${id}` : "/corporate/kyc")}>
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

function formatSubmittedAt(value?: string | null) {
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
