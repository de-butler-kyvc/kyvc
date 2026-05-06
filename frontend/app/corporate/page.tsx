import Link from "next/link";

type SummaryCard = {
  label: string;
  value: string;
  tone: "default" | "primary" | "warning" | "success";
};

const summaryCards: SummaryCard[] = [
  { label: "KYC 신청", value: "2", tone: "default" },
  { label: "심사중", value: "1", tone: "primary" },
  { label: "보완필요", value: "1", tone: "warning" },
  { label: "발급된 VC", value: "1", tone: "success" }
];

type StatusBadge = {
  label: string;
  className: string;
};

type KycRow = {
  id: string;
  type: string;
  date: string;
  status: StatusBadge;
};

const recentKyc: KycRow[] = [
  {
    id: "KYC-2025-0042",
    type: "주식회사",
    date: "2025.04.30",
    status: {
      label: "보완필요",
      className: "border-warning-soft-border bg-warning-soft text-warning"
    }
  },
  {
    id: "KYC-2025-0038",
    type: "주식회사",
    date: "2025.04.22",
    status: {
      label: "VC발급완료",
      className: "border-success-soft-border bg-success-soft text-success"
    }
  }
];

const valueToneClass: Record<SummaryCard["tone"], string> = {
  default: "text-foreground",
  primary: "text-primary",
  warning: "text-warning",
  success: "text-success"
};

const labelToneClass: Record<SummaryCard["tone"], string> = {
  default: "text-muted-strong",
  primary: "text-muted-strong",
  warning: "text-warning",
  success: "text-muted-strong"
};

const cardSurfaceClass: Record<SummaryCard["tone"], string> = {
  default: "bg-card border-border",
  primary: "bg-card border-border",
  warning: "bg-warning-soft border-warning-soft-border",
  success: "bg-card border-border"
};

export default function CorporateDashboardPage() {
  return (
    <div className="flex flex-col gap-4 px-9 py-7">
      <section className="flex flex-col gap-2.5 rounded-[14px] border border-border bg-card px-[21px] py-[17px]">
        <h1 className="text-[16px] font-extrabold tracking-[-0.3px] text-foreground">
          법인 사용자 대시보드
        </h1>
        <div className="flex items-center gap-4 rounded-lg bg-muted px-3.5 pb-2.5 pt-3">
          <span className="text-[12px] font-semibold text-muted-foreground">
            법인 식별정보
          </span>
          <span className="text-[13px] font-bold text-foreground">
            주식회사 케이원
          </span>
          <span className="text-[12px] text-muted-strong">123-45-67890</span>
        </div>
        <div className="rounded-lg border border-primary-soft-border bg-primary-soft px-[15px] py-[11px]">
          <p className="text-[12px] font-semibold text-primary">
            다음 단계 안내 · 신청 현황을 확인하고 다음 단계를 진행하세요.
          </p>
        </div>
      </section>

      <section className="flex flex-col gap-[3px] pb-2">
        <h2 className="text-[20px] font-bold tracking-[-0.4px] text-foreground">
          안녕하세요, 김겸 님
        </h2>
        <p className="text-[13px] text-destructive">
          주식회사 테크플로우 · 사업자번호 123-45-67890
        </p>
      </section>

      <div
        role="alert"
        className="flex items-start gap-2.5 rounded-[10px] border border-warning-soft-border bg-warning-soft px-[17px] py-[13px] text-[13px] text-warning"
      >
        <span aria-hidden>⚠</span>
        <span>
          보완 요청 1건이 있습니다. 등기사항전부증명서 불일치 — 확인 후 서류를
          재업로드해주세요.
        </span>
      </div>

      <div className="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
        {summaryCards.map((card) => (
          <div
            key={card.label}
            className={`flex flex-col items-center gap-2 rounded-[14px] border px-[17px] py-[21px] shadow-[0px_1px_1.5px_rgba(0,0,0,0.06)] ${cardSurfaceClass[card.tone]}`}
          >
            <span
              className={`text-[11px] uppercase tracking-[0.55px] ${labelToneClass[card.tone]}`}
            >
              {card.label}
            </span>
            <span
              className={`text-[28px] font-bold leading-none ${valueToneClass[card.tone]}`}
            >
              {card.value}
            </span>
          </div>
        ))}
      </div>

      <div className="grid gap-5 pt-2 lg:grid-cols-3">
        <div className="flex flex-col gap-3.5 lg:col-span-2">
          <div className="flex items-center justify-between">
            <h3 className="text-[14px] font-bold text-foreground">
              최근 KYC 신청
            </h3>
            <Link
              href="/corporate/kyc/apply"
              className="rounded-[10px] bg-primary px-5 py-2.5 text-[13px] font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
            >
              새 KYC 신청
            </Link>
          </div>
          <div className="overflow-hidden rounded-[14px] border border-border">
            <table className="w-full table-fixed text-left">
              <thead>
                <tr className="bg-muted text-[11px] font-bold text-muted-strong">
                  <th className="w-[26%] px-3.5 py-2.5 font-bold">신청번호</th>
                  <th className="w-[16%] px-3.5 py-2.5 font-bold">법인 유형</th>
                  <th className="w-[20%] px-3.5 py-2.5 font-bold">신청일</th>
                  <th className="w-[22%] px-3.5 py-2.5 font-bold">상태</th>
                  <th className="w-[16%] px-3.5 py-2.5" />
                </tr>
              </thead>
              <tbody>
                {recentKyc.map((row, idx) => (
                  <tr
                    key={row.id}
                    className={
                      idx === recentKyc.length - 1
                        ? ""
                        : "border-b border-border-subtle"
                    }
                  >
                    <td className="px-3.5 py-4 font-mono text-[12px] text-foreground">
                      {row.id}
                    </td>
                    <td className="px-3.5 py-4 text-[13px] text-foreground">
                      {row.type}
                    </td>
                    <td className="px-3.5 py-4 text-[13px] text-muted-strong">
                      {row.date}
                    </td>
                    <td className="px-3.5 py-4">
                      <span
                        className={`inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold ${row.status.className}`}
                      >
                        {row.status.label}
                      </span>
                    </td>
                    <td className="px-3.5 py-4">
                      <Link
                        href={`/corporate/kyc/detail/${row.id}`}
                        className="text-[12px] text-primary hover:underline"
                      >
                        상세 보기
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <aside className="flex flex-col gap-3">
          <h3 className="text-[14px] font-bold text-foreground">다음 단계 안내</h3>
          <div className="flex flex-col gap-[7px] rounded-[14px] border border-border bg-card px-[19px] py-[17px] shadow-[0px_1px_1.5px_rgba(0,0,0,0.06)]">
            <p className="text-[13px] font-semibold text-warning">
              보완 서류 제출 필요
            </p>
            <p className="text-[12px] leading-[21.6px] text-muted-foreground">
              등기사항전부증명서를 최신본으로 재제출해주세요. 대표자명 불일치가
              확인되었습니다.
            </p>
            <Link
              href="/corporate/kyc"
              className="mt-1 inline-flex items-center justify-center rounded-[10px] bg-primary px-2 py-2 text-[13px] font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
            >
              보완 서류 제출
            </Link>
          </div>
        </aside>
      </div>
    </div>
  );
}
