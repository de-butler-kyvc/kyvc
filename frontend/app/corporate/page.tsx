"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Icon } from "@/components/design/icons";
import {
  ApiError,
  corporate,
  credentials as credentialsApi,
  kyc as kycApi,
  notifications as notificationsApi,
  type KycApplicationResponse,
  type Notification,
  type UserDashboardResponse
} from "@/lib/api";
import { useCorporateProfile } from "@/lib/session-context";
import { useApi } from "@/lib/use-api";

const STATUS_LABEL: Record<string, { label: string; cls: string }> = {
  VC_ISSUED: { label: "VC발급완료", cls: "badge-success" },
  COMPLETED: { label: "완료", cls: "badge-success" },
  APPROVED: { label: "승인", cls: "badge-success" },
  SUPPLEMENT_REQUESTED: { label: "보완필요", cls: "badge-warning" },
  AI_REVIEWING: { label: "AI 심사중", cls: "badge-info" },
  IN_REVIEW: { label: "심사중", cls: "badge-info" },
  SUBMITTED: { label: "제출됨", cls: "badge-info" },
  DRAFT: { label: "작성중", cls: "badge-muted" }
};

const IN_REVIEW_STATUSES = new Set(["AI_REVIEWING", "IN_REVIEW", "SUBMITTED"]);

function formatDate(iso?: string | null) {
  if (!iso) return "-";
  return iso.slice(0, 10).replace(/-/g, ".");
}

function statusBadge(status?: string) {
  const fallback = { label: status ?? "-", cls: "badge-muted" };
  return STATUS_LABEL[status ?? ""] ?? fallback;
}

export default function CorporateDashboardPage() {
  const { data: dash, error: dashError, loading: dashLoading } = useApi<UserDashboardResponse>(
    () => corporate.dashboard()
  );

  const corporateRegistered = !!dash?.corporateRegistered;
  const activeKycId = dash?.activeKycId;

  const { profile } = useCorporateProfile();

  const [activeKyc, setActiveKyc] = useState<KycApplicationResponse | null>(null);
  useEffect(() => {
    if (!activeKycId) {
      setActiveKyc(null);
      return;
    }
    let cancelled = false;
    kycApi.current()
      .then((k) => {
        if (!cancelled) setActiveKyc(k);
      })
      .catch(() => {
        if (!cancelled) setActiveKyc(null);
      });
    return () => {
      cancelled = true;
    };
  }, [activeKycId]);

  const [notes, setNotes] = useState<Notification[]>([]);
  useEffect(() => {
    let cancelled = false;
    notificationsApi.list({ page: 0, size: 5 })
      .then((res) => {
        if (!cancelled) setNotes(res?.content ?? []);
      })
      .catch(() => {
        if (!cancelled) setNotes([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const [credentialCount, setCredentialCount] = useState(0);
  useEffect(() => {
    let cancelled = false;
    credentialsApi.list()
      .then((res) => {
        if (!cancelled) setCredentialCount(res?.totalCount ?? 0);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setCredentialCount(0);
        else setCredentialCount(0);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const corporateName = dash?.corporateName ?? profile?.corporateName ?? null;
  const businessNo = profile?.businessRegistrationNo ?? null;
  const supplementCount = dash?.needSupplementCount ?? 0;
  const inReviewCount = dash?.activeKycStatus && IN_REVIEW_STATUSES.has(dash.activeKycStatus) ? 1 : 0;
  const totalKycCount = activeKycId ? 1 : 0;

  const supplementNote = notes.find((n) => !n.read && /보완|supplement/i.test(n.message + n.title));
  const firstUnread = notes.find((n) => !n.read);
  const alertNote = supplementNote ?? firstUnread;

  return (
    <>
      <div className="entity-bar">
        <span className="entity-bar-label">법인 식별정보</span>
        <span className="entity-bar-value">
          {dashLoading ? "..." : (corporateName ?? "법인 미등록")}
        </span>
        {businessNo ? (
          <>
            <span className="entity-bar-divider" />
            <span className="entity-bar-id">{businessNo}</span>
          </>
        ) : null}
      </div>

      <div className="greeting-block">
        <h2 className="greeting-name">
          {corporateName ? `안녕하세요, ${corporateName} 님` : "안녕하세요"}
        </h2>
        {businessNo ? (
          <div className="greeting-meta">
            {corporateName} · 사업자번호 {businessNo}
          </div>
        ) : null}
      </div>

      {dashError ? (
        <div className="alert alert-warning">
          <Icon.Alert size={16} className="alert-icon" />
          <span>{dashError}</span>
        </div>
      ) : null}

      {!corporateRegistered && !dashLoading ? (
        <div className="alert alert-warning">
          <Icon.Alert size={16} className="alert-icon" />
          <span>
            아직 법인 정보가 등록되지 않았습니다.{" "}
            <Link href="/corporate/profile" className="link">
              법인 기본정보 등록하기
            </Link>
          </span>
        </div>
      ) : null}

      {alertNote ? (
        <div className="alert alert-warning">
          <Icon.Alert size={16} className="alert-icon" />
          <span>{alertNote.message}</span>
        </div>
      ) : null}

      <div className="stat-grid">
        <StatCard label="진행 KYC" value={totalKycCount} />
        <StatCard label="심사중" value={inReviewCount} tone="info" />
        <StatCard label="보완필요" value={supplementCount} tone="warn" highlight={supplementCount > 0} />
        <StatCard label="발급된 VC" value={credentialCount} tone="success" />
      </div>

      <div className="dash-grid-2">
        <div>
          <div className="section-head">
            <h3 className="section-title">현재 KYC 신청</h3>
            <Link href="/corporate/kyc/apply" className="btn btn-primary btn-sm">
              <Icon.Plus size={14} />새 KYC 신청
            </Link>
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>신청번호</th>
                <th>법인 유형</th>
                <th>신청일</th>
                <th>상태</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {!activeKycId ? (
                <tr>
                  <td colSpan={5} className="empty-state">
                    진행 중인 KYC 신청이 없습니다.
                  </td>
                </tr>
              ) : (
                (() => {
                  const status = statusBadge(activeKyc?.kycStatus ?? dash?.activeKycStatus);
                  return (
                    <tr key={activeKycId}>
                      <td className="mono">{`KYC-${activeKycId}`}</td>
                      <td>{activeKyc?.corporateTypeCode ?? "-"}</td>
                      <td>{formatDate(activeKyc?.submittedAt ?? activeKyc?.createdAt)}</td>
                      <td>
                        <span className={`badge ${status.cls}`}>{status.label}</span>
                      </td>
                      <td>
                        <Link href={`/corporate/kyc/detail?id=${activeKycId}`} className="link">
                          상세 보기
                        </Link>
                      </td>
                    </tr>
                  );
                })()
              )}
            </tbody>
          </table>
        </div>

        <div>
          <div className="section-head">
            <h3 className="section-title">다음 단계 안내</h3>
          </div>
          <div className={`next-step-card${supplementCount > 0 ? " urgent" : ""}`}>
            <div className="next-step-title">
              {supplementCount > 0
                ? "보완 서류 제출이 필요합니다"
                : activeKycId
                  ? "KYC 심사 진행 중"
                  : "신규 KYC 신청을 시작하세요"}
            </div>
            {supplementCount > 0 ? (
              <p className="next-step-desc">
                보완 요청 {supplementCount}건이 있습니다. 신청 상세 화면에서 서류를 보완해 제출해 주세요.
              </p>
            ) : alertNote?.message ? (
              <p className="next-step-desc">{alertNote.message}</p>
            ) : null}
            <Link
              href={activeKycId ? "/corporate/kyc" : "/corporate/kyc/apply"}
              className="btn btn-primary btn-block btn-sm"
            >
              {activeKycId ? "신청 내역 보기" : "KYC 신청 시작"}
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}

function StatCard({
  label,
  value,
  tone = "default",
  highlight = false
}: {
  label: string;
  value: number;
  tone?: "default" | "info" | "warn" | "success";
  highlight?: boolean;
}) {
  const valueClass =
    tone === "info"
      ? "stat-value value-info"
      : tone === "warn"
        ? "stat-value value-warn"
        : tone === "success"
          ? "stat-value value-success"
          : "stat-value";
  return (
    <div className={`stat-card${highlight ? " highlight" : ""}`}>
      <div className="stat-label">{label}</div>
      <div className={valueClass}>{value}</div>
    </div>
  );
}
