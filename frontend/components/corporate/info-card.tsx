import { Card, CardContent } from "@/components/ui/card";

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
    <Card>
      <CardContent className="flex flex-col gap-4 px-6 py-5">
        <h2 className="text-[14px] font-bold text-foreground">법인 기본정보</h2>
        <div className="grid grid-cols-1 gap-x-5 gap-y-3 md:grid-cols-2">
          <Field label="법인명" value={summary?.corporateName} />
          <Field label="사업자등록번호" value={summary?.businessNo} />
          <Field label="법인등록번호" value={summary?.corporateNo} />
          <Field label="대표자명" value={summary?.representativeName} />
          <Field label="법인 유형" value={summary?.corporateType} />
        </div>
      </CardContent>
    </Card>
  );
}

function Field({ label, value }: { label: string; value?: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-[12px] text-muted-foreground">{label}</span>
      <div className="flex h-9 items-center rounded-md border border-accent-border bg-accent/40 px-3 text-[13px] font-medium text-foreground">
        {value || "-"}
      </div>
    </div>
  );
}
