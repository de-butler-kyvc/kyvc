"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { StepIndicator } from "@/components/kyc/step-indicator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ApiError, kyc as kycApi } from "@/lib/api";

const TYPE_LABEL: Record<string, string> = {
  STOCK: "주식회사",
  LIMITED: "유한회사",
  NONPROFIT: "비영리법인",
  GROUP: "조합·단체",
  FOREIGN: "외국기업"
};

const FALLBACK_REQUIRED = ["사업자등록증", "등기사항전부증명서", "주주명부"];
const FALLBACK_OPTIONAL = ["정관", "위임장"];

const HINT: Record<string, string> = {
  정관: "해당 시",
  위임장: "대리 시"
};

export default function KycApplyDocsPage() {
  const router = useRouter();
  const [corporateType, setCorporateType] = useState<string>("");
  const [required, setRequired] = useState<string[]>(FALLBACK_REQUIRED);
  const [optional, setOptional] = useState<string[]>(FALLBACK_OPTIONAL);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const stored =
      typeof window !== "undefined"
        ? window.localStorage.getItem("kyvc.corporateType") ?? "STOCK"
        : "STOCK";
    setCorporateType(stored);
    kycApi
      .documentRequirements(stored)
      .then((r) => {
        if (r.requiredDocuments?.length) setRequired(r.requiredDocuments);
        if (r.optionalDocuments?.length) setOptional(r.optionalDocuments);
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-6 px-9 py-8">
      <StepIndicator current={3} />

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          필수서류 안내
        </h1>
        <p className="text-[13px] text-destructive">
          {TYPE_LABEL[corporateType] ?? "법인"} KYC 심사에 필요한 서류 목록입니다.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardContent className="flex flex-col gap-3 px-6 py-5">
            <h2 className="text-[14px] font-bold text-foreground">필수 서류</h2>
            <ul className="flex flex-col gap-2">
              {required.map((d) => (
                <DocRow key={d} name={d} hint="필수" tone="primary" />
              ))}
            </ul>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex flex-col gap-3 px-6 py-5">
            <h2 className="text-[14px] font-bold text-foreground">선택 서류</h2>
            <ul className="flex flex-col gap-2">
              {optional.map((d) => (
                <DocRow key={d} name={d} hint={HINT[d] ?? "선택"} tone="muted" />
              ))}
            </ul>
            <p className="pt-1 text-[12px] text-muted-foreground">
              등기사항전부증명서는 발급일 기준 3개월 이내 서류만 인정됩니다.
            </p>
          </CardContent>
        </Card>
      </div>

      {error ? <p className="text-[12px] text-destructive">{error}</p> : null}

      <div className="flex items-center gap-2">
        <Button
          onClick={() => router.push("/corporate/kyc/apply/upload")}
          className="rounded-[10px] px-5"
        >
          서류 업로드 시작 →
        </Button>
        <Button
          variant="outline"
          className="rounded-[10px] px-5"
          onClick={() => router.push("/corporate/kyc/apply/type")}
        >
          이전
        </Button>
      </div>
    </div>
  );
}

function DocRow({
  name,
  hint,
  tone
}: {
  name: string;
  hint: string;
  tone: "primary" | "muted";
}) {
  return (
    <li className="flex items-center justify-between rounded-md border border-row-border bg-secondary/40 px-3.5 py-2.5">
      <span className="text-[13px] text-foreground">{name}</span>
      <Badge variant={tone === "primary" ? "warning" : "outline"}>{hint}</Badge>
    </li>
  );
}
