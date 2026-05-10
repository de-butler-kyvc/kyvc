"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { StepIndicator } from "@/components/kyc/step-indicator";
import { Button } from "@/components/ui/button";
import { ApiError, kyc as kycApi } from "@/lib/api";
import { getCurrentKycId } from "@/lib/kyc-flow";

type StoreOption = "STORE" | "DELETE";

const OPTIONS: {
  value: StoreOption;
  icon: React.ReactNode;
  name: string;
  desc: string;
  retention?: string;
}[] = [
  {
    value: "STORE",
    icon: <Icon.Lock />,
    name: "원본 보관",
    desc: "암호화하여 안전하게 보관합니다. 이후 재심사·재발급 시 활용됩니다.",
    retention: "보관기간: KYC 완료 후 5년"
  },
  {
    value: "DELETE",
    icon: <Icon.Trash />,
    name: "AI 검토 후 삭제",
    desc: "AI 심사 완료 후 원본을 삭제합니다. 재심사 시 서류를 다시 제출해야 합니다."
  }
];

export default function KycApplyStoragePage() {
  const router = useRouter();
  const [kycId, setKycId] = useState<number | null>(null);
  const [selected, setSelected] = useState<StoreOption>("STORE");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const id = getCurrentKycId();
    if (!id) {
      router.push("/corporate/kyc/apply");
      return;
    }
    setKycId(id);
    kycApi
      .detail(id)
      .then((detail) => {
        if (detail.originalDocumentStoreOption === "DELETE") {
          setSelected("DELETE");
        } else if (detail.originalDocumentStoreOption === "STORE") {
          setSelected("STORE");
        }
      })
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : "조회에 실패했습니다.")
      );
  }, [router]);

  const onNext = async () => {
    if (!kycId) return;
    setBusy(true);
    setError(null);
    try {
      await kycApi.setDocumentStoreOption(kycId, selected);
      router.push("/corporate/kyc/apply/confirm");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "저장 옵션 저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">원본서류 저장 옵션 선택</h1>
          <p className="page-head-desc">제출 서류 원본을 어떻게 보관할지 선택해주세요.</p>
        </div>
      </div>

      <StepIndicator current={5} />

      <section className="form-card">
        <div className="type-radio-list">
          {OPTIONS.map((option) => {
            const active = selected === option.value;
            return (
              <button
                key={option.value}
                type="button"
                className={`type-radio-card ${active ? "selected" : ""}`}
                onClick={() => setSelected(option.value)}
              >
                <span className="type-radio-icon">{option.icon}</span>
                <span className="type-radio-info">
                  <span className="type-radio-name">{option.name}</span>
                  <span className="type-radio-docs">{option.desc}</span>
                  {option.retention ? (
                    <span className="type-radio-docs">{option.retention}</span>
                  ) : null}
                </span>
                <span className={`type-radio-dot ${active ? "selected" : ""}`} />
              </button>
            );
          })}
        </div>
      </section>

      <div className="alert alert-info mt-4">
        <span className="alert-icon">
          <Icon.Info size={16} />
        </span>
        <span>
          원본 보관 선택 시 개인정보 처리방침에 따라 암호화 저장됩니다. 언제든지 삭제를 요청할 수 있습니다.
        </span>
      </div>

      {error ? <p className="mt-4 text-[12px] text-destructive">{error}</p> : null}

      <div className="form-actions right">
        <Button type="button" variant="ghost" onClick={() => router.push("/corporate/kyc/apply/review")}>
          이전
        </Button>
        <Button type="button" disabled={busy} onClick={onNext}>
          {busy ? "저장 중..." : "다음 — 신청 내용 확인 →"}
        </Button>
      </div>
    </div>
  );
}
