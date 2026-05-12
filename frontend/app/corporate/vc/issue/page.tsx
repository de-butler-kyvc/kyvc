"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";

import { Icon } from "@/components/design/icons";
import { Button } from "@/components/ui/button";
import {
  ApiError,
  corporate as corporateApi,
  credentialOffers,
  credentials as credentialsApi,
  kyc as kycApi,
  type CorporateProfile,
  type CredentialOfferCreateResponse,
  type CredentialIssueGuideResponse,
  type KycApplicationResponse
} from "@/lib/api";

const KYC_STATUS = {
  APPROVED: "APPROVED",
} as const;

const OFFER_STATUS = {
  ACTIVE: "ACTIVE",
  USED: "USED",
  EXPIRED: "EXPIRED",
  FAILED: "FAILED",
} as const;

export default function CorporateVcIssuePage() {
  const [guide, setGuide] = useState<CredentialIssueGuideResponse | null>(null);
  const [profile, setProfile] = useState<CorporateProfile | null>(null);
  const [current, setCurrent] = useState<KycApplicationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [issuing, setIssuing] = useState(false);
  const [offer, setOffer] = useState<CredentialOfferCreateResponse | null>(null);
  const [qrValue, setQrValue] = useState<string | null>(null);
  const [offerStatus, setOfferStatus] = useState<string | null>(null);
  const [walletSaved, setWalletSaved] = useState(false);
  const [polling, setPolling] = useState(false);
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
  const isApproved = status === KYC_STATUS.APPROVED || guide?.issueAvailable === true;
  const canCreateOffer = Boolean(kycId && isApproved && !guide?.credentialIssued);

  useEffect(() => {
    if (!offer || !polling) return;
    let cancelled = false;
    let timer: number | null = null;

    const stopPolling = () => {
      if (timer) window.clearInterval(timer);
      timer = null;
      setPolling(false);
    };

    const poll = async () => {
      if (cancelled) return;
      if (new Date(offer.expiresAt).getTime() <= Date.now()) {
        setOfferStatus(OFFER_STATUS.EXPIRED);
        setMessage(null);
        stopPolling();
        return;
      }
      try {
        const res = await credentialOffers.status(offer.offerId);
        if (cancelled) return;
        setOfferStatus(res.offerStatus);
        setWalletSaved(res.walletSaved);
        if (res.walletSaved || res.offerStatus === OFFER_STATUS.USED) {
          setMessage("모바일 Wallet 저장이 완료되었습니다.");
          stopPolling();
          return;
        }
        if (res.offerStatus === OFFER_STATUS.EXPIRED) {
          setMessage(null);
          stopPolling();
          return;
        }
        if (res.offerStatus === OFFER_STATUS.FAILED) {
          setError("VC 발급 처리에 실패했습니다. QR을 다시 생성해주세요.");
          stopPolling();
        }
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "QR 상태 조회에 실패했습니다.");
      }
    };

    poll();
    timer = window.setInterval(poll, 2500);
    return () => {
      cancelled = true;
      if (timer) window.clearInterval(timer);
    };
  }, [offer, polling]);

  const issue = async () => {
    if (!kycId) {
      setError("VC를 발급할 KYC 신청을 찾을 수 없습니다.");
      return;
    }
    if (!isApproved) {
      setError("KYC 승인 완료 후 VC 발급 QR을 생성할 수 있습니다.");
      return;
    }
    setError(null);
    setMessage(null);
    setOffer(null);
    setQrValue(null);
    setOfferStatus(null);
    setWalletSaved(false);
    setPolling(false);
    setIssuing(true);
    try {
      const res = await credentialOffers.createForKyc(kycId);
      setOffer(res);
      setQrValue(JSON.stringify(res.qrPayload));
      setOfferStatus(res.offerStatus);
      setMessage("모바일 앱에서 QR을 스캔해 VC를 Wallet에 저장하세요.");
      setPolling(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "VC 발급 QR 생성에 실패했습니다.");
    } finally {
      setIssuing(false);
    }
  };

  const offerExpired = offerStatus === OFFER_STATUS.EXPIRED;
  const offerFailed = offerStatus === OFFER_STATUS.FAILED;
  const offerCompleted = walletSaved || offerStatus === OFFER_STATUS.USED;

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

        {qrValue && offer ? (
          <div className="mt-5 rounded-[var(--radius-md)] border border-[var(--border)] bg-white px-5 py-5 text-center">
            {offerCompleted ? (
              <div className="flex flex-col items-center gap-3 py-4">
                <div className="flex size-12 items-center justify-center rounded-full bg-[var(--success-soft)] text-success">
                  <Icon.Check size={22} />
                </div>
                <div>
                  <div className="text-[15px] font-semibold">Wallet 저장 완료</div>
                  <p className="mt-1 text-[12px] text-muted-foreground">
                    모바일 앱에서 VC 저장이 완료되었습니다.
                  </p>
                </div>
              </div>
            ) : (
              <>
                <div className="inline-flex rounded-[12px] border border-[var(--border)] bg-white p-3">
                  <QRCodeSVG value={qrValue} size={184} marginSize={1} />
                </div>
                <p className="mt-4 text-[13px] font-semibold">
                  모바일 앱에서 QR을 스캔해 VC를 Wallet에 저장하세요.
                </p>
                <p className="mt-1 text-[12px] text-muted-foreground">
                  만료 시각: {new Date(offer.expiresAt).toLocaleString("ko-KR")}
                </p>
                {polling && offerStatus === OFFER_STATUS.ACTIVE ? (
                  <p className="mt-2 text-[12px] text-[var(--accent)]">
                    모바일 저장 완료를 확인하는 중입니다.
                  </p>
                ) : null}
                {offerExpired ? (
                  <p className="mt-2 text-[12px] text-destructive">
                    QR이 만료되었습니다. 다시 생성해주세요.
                  </p>
                ) : null}
                {offerFailed ? (
                  <p className="mt-2 text-[12px] text-destructive">
                    VC 발급 처리에 실패했습니다. QR을 다시 생성해주세요.
                  </p>
                ) : null}
              </>
            )}
          </div>
        ) : null}

        <div className="form-actions right mt-5">
          <Button asChild variant="link">
            <Link href="/corporate">대시보드</Link>
          </Button>
          {offerCompleted ? (
            <Button asChild size="lg">
              <Link href="/corporate/vc">VC 이력 보기</Link>
            </Button>
          ) : (
            <Button type="button" size="lg" disabled={issuing || !canCreateOffer} onClick={issue}>
              {offerExpired || offerFailed ? "QR 다시 생성" : issuing ? "QR 생성 중..." : "VC 발급 QR 생성"}
            </Button>
          )}
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
