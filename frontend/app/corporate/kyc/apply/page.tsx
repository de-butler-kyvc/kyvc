"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { useCorporateProfile } from "@/lib/session-context";

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

export default function KycApplyStartPage() {
  const router = useRouter();
  const { profile, loading } = useCorporateProfile();
  const [error, setError] = useState<string | null>(null);

  const onStart = () => {
    if (loading) return;
    if (!profile?.corporateId) {
      setError("법인 기본정보를 먼저 등록하세요.");
      return;
    }
    if (!profile?.representativeName) {
      setError("대표자 정보를 먼저 등록하세요.");
      return;
    }
    setError(null);
    router.push("/corporate/kyc/apply/type");
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">KYC 신청 시작</h1>
          <p className="page-head-desc">
            신규 KYC 신청을 시작합니다. 법인 정보와 준비 서류를 확인해주세요.
          </p>
        </div>
      </div>

      <StepIndicator current={1} />

      <section className="form-card">
        <div className="form-card-header">
          <div className="form-card-title">법인 식별정보</div>
        </div>
        <ConfirmRow label="법인명" value={profile?.corporateName} />
        <ConfirmRow
          label="사업자등록번호"
          value={profile?.businessRegistrationNo}
          mono
        />
        <ConfirmRow
          label="법인등록번호"
          value={profile?.corporateRegistrationNo ?? undefined}
          mono
        />
        <ConfirmRow label="대표자" value={profile?.representativeName} />
      </section>

      <div className="dash-grid-2 mt-4">
        <section className="form-card m-0">
          <div className="form-card-header">
            <div className="form-card-title">필요 서류</div>
          </div>
          <InfoList icon="file" items={REQUIRED_DOCS} />
        </section>
        <section className="form-card m-0">
          <div className="form-card-header">
            <div className="form-card-title">업로드 규정</div>
          </div>
          <InfoList icon="check" items={UPLOAD_RULES} />
        </section>
      </div>

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate")}>
          취소
        </Button>
        <Button type="button" onClick={onStart} >
          신청 시작
        </Button>
      </div>
    </div>
  );
}

function ConfirmRow({
  label,
  value,
  mono = false
}: {
  label: string;
  value?: string;
  mono?: boolean;
}) {
  return (
    <div className="kv-row">
      <div className="kv-key">{label}</div>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value || "-"}</div>
    </div>
  );
}

function InfoList({ icon, items }: { icon: "file" | "check"; items: string[] }) {
  return (
    <ul className="m-0 flex list-none flex-col gap-2.5 p-0">
      {items.map((item) => (
        <li key={item} className="flex items-center gap-2.5 text-[13.5px]">
          {icon === "file" ? <Icon.File size={16} /> : <Icon.Check size={14} />}
          <span>{item}</span>
        </li>
      ))}
    </ul>
  );
}
