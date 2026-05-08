"use client";

import Link from "next/link";

import { Icon } from "@/components/design/icons";
import { corporate } from "@/lib/api";
import { useApi } from "@/lib/use-api";

const STATUS_LABEL: Record<string, { label: string; cls: string }> = {
  VC_ISSUED: { label: "VC발급완료", cls: "badge-success" },
  APPROVED: { label: "승인", cls: "badge-success" },
  COMPLETED: { label: "완료", cls: "badge-success" },
  SUPPLEMENT_REQUESTED: { label: "보완필요", cls: "badge-warning" },
  AI_REVIEWING: { label: "AI 심사중", cls: "badge-info" },
  IN_REVIEW: { label: "심사중", cls: "badge-info" }
};

export default function CorporateDashboardPage() {
  const { data, error, loading } = useApi(() => corporate.dashboard());

  const corp = data?.corporate;
  const summary = data?.kycSummary ?? {};
  const issued = data?.credentialSummary?.issued ?? 0;
  const recent = data?.recentApplications ?? [];
  const alert = (data?.notifications ?? []).find(
    (n) => n.severity === "warning" || n.severity === "error"
  );
  const nextStep = data?.notifications?.find((n) => n.severity !== "info");

  return (
    <>
      <div className="entity-bar">
        <span className="entity-bar-label">법인 식별정보</span>
        <span className="entity-bar-value">
          {loading ? "..." : (corp?.corporateName ?? "법인 미등록")}
        </span>
        {corp?.businessNo ? (
          <>
            <span className="entity-bar-divider" />
            <span className="entity-bar-id">{corp.businessNo}</span>
          </>
        ) : null}
      </div>

      <div className="greeting-block">
        <h2 className="greeting-name">
          {corp?.corporateName ? `안녕하세요, ${corp.corporateName} 님` : "안녕하세요"}
        </h2>
        {corp?.businessNo ? (
          <div className="greeting-meta">
            {corp.corporateName} · 사업자번호 {corp.businessNo}
          </div>
        ) : null}
      </div>

      {error ? (
        <div className="alert alert-warning">
          <Icon.Alert size={16} className="alert-icon" />
          <span>{error}</span>
        </div>
      ) : null}

      {alert ? (
        <div className="alert alert-warning">
          <Icon.Alert size={16} className="alert-icon" />
          <span>{alert.message}</span>
        </div>
      ) : null}

      <div className="stat-grid">
        <StatCard label="KYC 신청" value={summary.total ?? 0} />
        <StatCard label="심사중" value={summary.inReview ?? 0} tone="info" />
        <StatCard label="보완필요" value={summary.supplement ?? 0} tone="warn" highlight />
        <StatCard label="발급된 VC" value={issued} tone="success" />
      </div>

      <div className="dash-grid-2">
        <div>
          <div className="section-head">
            <h3 className="section-title">최근 KYC 신청</h3>
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
              {recent.length === 0 ? (
                <tr>
                  <td colSpan={5} className="empty-state">
                    최근 신청 내역이 없습니다.
                  </td>
                </tr>
              ) : (
                recent.map((r) => {
                  const status = STATUS_LABEL[r.status ?? ""] ?? {
                    label: r.status ?? "-",
                    cls: "badge-muted"
                  };
                  return (
                    <tr key={r.kycId}>
                      <td className="mono">{r.applicationNo ?? `KYC-${r.kycId}`}</td>
                      <td>{r.corporateType ?? "-"}</td>
                      <td>{r.submittedAt?.slice(0, 10).replace(/-/g, ".") ?? "-"}</td>
                      <td>
                        <span className={`badge ${status.cls}`}>{status.label}</span>
                      </td>
                      <td>
                        <Link href={`/corporate/kyc/detail?id=${r.kycId}`} className="link">
                          상세 보기
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div>
          <div className="section-head">
            <h3 className="section-title">다음 단계 안내</h3>
          </div>
          <div className={`next-step-card${nextStep ? " urgent" : ""}`}>
            <div className="next-step-title">
              {nextStep?.message ? "보완 서류 제출 필요" : "진행 중인 작업이 없습니다"}
            </div>
            {nextStep?.message ? (
              <p className="next-step-desc">{nextStep.message}</p>
            ) : null}
            <Link href="/corporate/kyc" className="btn btn-primary btn-block btn-sm">
              신청 내역 보기
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
