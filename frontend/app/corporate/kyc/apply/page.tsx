"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ApiError, corporate as corpApi, kyc as kycApi } from "@/lib/api";

const REQUIRED_DOCS = [
  "사업자등록증",
  "등기사항전부증명서",
  "주주명부",
  "위임장 (대리 시)"
];

const UPLOAD_RULES = [
  "PDF, JPG, PNG",
  "파일당 최대 20MB",
  "최근 3개월 이내 발급",
  "컬러 스캔 권장"
];

type Identity = {
  corporateId?: number;
  corporateName: string;
  businessNo: string;
  representativeName: string;
};

export default function KycApplyStartPage() {
  const router = useRouter();
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    corpApi
      .me()
      .then((res) => {
        const c = res.corporate as
          | { corporateId?: number; corporateName?: string; businessNo?: string }
          | undefined;
        setIdentity({
          corporateId: c?.corporateId,
          corporateName: c?.corporateName ?? "",
          businessNo: c?.businessNo ?? "",
          representativeName: res.representative?.name ?? ""
        });
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, []);

  const onStart = async () => {
    if (!identity?.corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const corporateType =
        (typeof window !== "undefined"
          ? window.localStorage.getItem("kyvc.corporateType")
          : null) ?? "";
      const created = await kycApi.create({
        corporateId: identity.corporateId,
        applicationType: "ONLINE",
        corporateType: corporateType || undefined
      });
      if (typeof window !== "undefined") {
        window.localStorage.setItem("kyvc.currentKycId", String(created.kycId));
      }
      router.push("/corporate/kyc/apply/type");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "신청 생성 중 오류가 발생했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col gap-5 px-9 py-8">
      <Card>
        <CardContent className="flex flex-col gap-3 px-6 py-5">
          <h2 className="text-[15px] font-bold text-foreground">KYC 신청 시작</h2>
          <div className="flex flex-wrap items-center gap-3 rounded-md bg-secondary px-3.5 py-2.5 text-[12px]">
            <span className="font-semibold text-muted-foreground">법인 식별정보</span>
            <span className="font-bold text-foreground">
              {identity?.corporateName || "-"}
            </span>
            <span className="text-subtle-foreground">{identity?.businessNo || "-"}</span>
          </div>
          <div className="rounded-md border border-accent-border bg-accent px-3.5 py-2.5 text-[12px] font-semibold text-accent-foreground">
            KYC 신청을 시작하려면 아래 단계를 진행하세요.
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-col gap-1.5">
        <h1 className="text-[22px] font-bold tracking-[-0.4px] text-foreground">
          KYC 신청 시작
        </h1>
        <p className="text-[13px] text-destructive">
          신규 KYC 신청을 시작합니다. 아래 정보를 확인해주세요.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-4 px-7 py-6">
          <h2 className="text-[15px] font-bold text-foreground">법인 식별정보</h2>
          <div className="overflow-hidden rounded-lg border border-border">
            <ConfirmRow label="법인명" value={identity?.corporateName} />
            <ConfirmRow label="사업자등록번호" value={identity?.businessNo} />
            <ConfirmRow label="대표자" value={identity?.representativeName} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-4 px-7 py-6">
          <h2 className="text-[15px] font-bold text-foreground">준비사항 안내</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <InfoBox title="필요 서류" items={REQUIRED_DOCS} />
            <InfoBox title="업로드 규정" items={UPLOAD_RULES} />
          </div>
        </CardContent>
      </Card>

      {error ? <p className="text-[12px] text-destructive">{error}</p> : null}

      <div className="flex items-center gap-2">
        <Button onClick={onStart} disabled={busy} className="rounded-[10px] px-5">
          {busy ? "처리 중..." : "신청 시작 →"}
        </Button>
        <Button
          variant="outline"
          className="rounded-[10px] px-5"
          onClick={() => router.push("/corporate")}
        >
          취소
        </Button>
      </div>
    </div>
  );
}

function ConfirmRow({ label, value }: { label: string; value?: string }) {
  return (
    <div className="grid grid-cols-[160px_1fr] border-b border-row-border last:border-0">
      <div className="bg-secondary px-4 py-3 text-[13px] text-muted-foreground">
        {label}
      </div>
      <div className="px-4 py-3 text-[13px] font-medium text-foreground">
        {value || "-"}
      </div>
    </div>
  );
}

function InfoBox({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="rounded-lg border border-border bg-secondary/50 p-4">
      <h3 className="pb-2 text-[13px] font-bold text-foreground">{title}</h3>
      <ul className="grid gap-1.5 text-[13px] text-muted-foreground">
        {items.map((item) => (
          <li key={item}>· {item}</li>
        ))}
      </ul>
    </div>
  );
}
