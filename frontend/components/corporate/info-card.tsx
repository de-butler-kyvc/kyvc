export type CorporateSummary = {
  corporateId?: number;
  corporateName: string;
  businessNo: string;
  corporateNo: string;
  representativeName: string;
  corporateType: string;
};

export function CorporateInfoCard({ summary }: { summary: CorporateSummary | null }) {
  return (
    <div className="form-card">
      <div className="form-card-header">
        <div className="form-card-title">법인 기본정보</div>
      </div>
      <div className="form-grid">
        <Field label="법인명" value={summary?.corporateName} />
        <Field label="사업자등록번호" value={summary?.businessNo} mono />
        <Field label="법인등록번호" value={summary?.corporateNo} mono />
        <Field label="대표자명" value={summary?.representativeName} />
        <Field label="법인 유형" value={summary?.corporateType} />
      </div>
    </div>
  );
}

function Field({
  label,
  value,
  mono = false
}: {
  label: string;
  value?: string;
  mono?: boolean;
}) {
  return (
    <div className="kv-row" style={{ borderBottom: 0, padding: 0, gap: 8, alignItems: "flex-start", flexDirection: "column" }}>
      <span className="kv-key" style={{ width: "auto" }}>
        {label}
      </span>
      <div className={`kv-val${mono ? " mono" : ""}`}>{value || "-"}</div>
    </div>
  );
}
