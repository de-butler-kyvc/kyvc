"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  corporate as corporateApi,
  credentials as credentialsApi,
  kyc as kycApi,
  type CorporateProfile,
  type CredentialIssueGuideResponse,
  type KycApplicationResponse
} from "@/lib/api";

export default function CorporateVcIssuePage() {
  const router = useRouter();
  const [guide, setGuide] = useState<CredentialIssueGuideResponse | null>(null);
  const [profile, setProfile] = useState<CorporateProfile | null>(null);
  const [current, setCurrent] = useState<KycApplicationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [issuing, setIssuing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.allSettled([credentialsApi.issueGuide(), corporateApi.me(), kycApi.current()]).then(
      ([guideResult, profileResult, currentResult]) => {
        if (cancelled) return;
        if (guideResult.status === "fulfilled") setGuide(guideResult.value);
        if (profileResult.status === "fulfilled") setProfile(profileResult.value);
        if (currentResult.status === "fulfilled") setCurrent(currentResult.value);
        setLoading(false);
      }
    );
    return () => {
      cancelled = true;
    };
  }, []);

  const kycId = guide?.latestKycId ?? current?.kycId;
  const status = guide?.kycStatus ?? current?.kycStatus;

  const issue = async () => {
    if (!kycId) {
      setError("VC를 발급할 KYC 신청을 찾을 수 없습니다.");
      return;
    }
    setError(null);
    setMessage(null);
    setIssuing(true);
    try {
      await kycApi.credentialOffer(kycId);
      setMessage("VC 발급 요청이 완료되었습니다.");
      router.push("/corporate/vc");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "VC 발급 요청에 실패했습니다.");
    } finally {
      setIssuing(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[920px] flex-col">
      <div className="page-head">
        <div>
          <h1 className="page-head-title">VC 발급 안내</h1>
          <p className="page-head-desc">KYC 심사 완료 후 발급 가능한 법인 VC 정보를 확인합니다.</p>
        </div>
      </div>

      <section className="form-card" style={{ maxWidth: 560 }}>
        <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[rgba(42,91,255,0.2)] bg-[var(--accent-soft)] px-5 py-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-[10px] bg-[var(--accent)] text-white">
            <Icon.Shield size={22} />
          </div>
          <div>
            <div className="text-[11px] font-bold uppercase tracking-[0.05em] text-[var(--accent)]">
              KYC Credential
            </div>
            <div className="mt-0.5 text-[14px] font-semibold">
              {profile?.corporateName ?? guide?.guideTitle ?? "법인 VC"}
            </div>
            <div className="mono text-[12px] text-muted-foreground">
              {kycId ? `KYC-${kycId}` : loading ? "조회 중..." : "-"}
            </div>
          </div>
        </div>

        <div className="mt-5">
          <Row label="Credential 유형" value="KYC_CREDENTIAL" />
          <Row label="발급자" value="KYvC Platform" />
          <Row label="상태" value={status ?? "-"} />
          <Row label="대상 법인" value={profile?.corporateName ?? "-"} />
          <Row label="KYC 신청번호" value={kycId ? `KYC-${kycId}` : "-"} mono />
          <Row
            label="발급 가능 여부"
            value={guide?.issueAvailable ? "가능" : guide?.credentialIssued ? "발급 완료" : "-"}
          />
        </div>

        {guide?.guideMessage ? (
          <p className="mt-4 text-[13px] text-muted-foreground">{guide.guideMessage}</p>
        ) : null}
        {error || message ? (
          <p className="mt-4 text-[12px]">
            {message ? (
              <span className="text-success">{message}</span>
            ) : (
              <span className="text-destructive">{error}</span>
            )}
          </p>
        ) : null}

        <div className="form-actions right mt-5">
          <Button asChild variant="link">
            <Link href="/corporate">대시보드</Link>
          </Button>
          <Button type="button" size="lg" disabled={issuing || !kycId || guide?.credentialIssued} onClick={issue}>
            VC 발급
          </Button>
        </div>
      </section>
    </div>
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
