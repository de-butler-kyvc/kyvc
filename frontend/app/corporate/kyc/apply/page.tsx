import { PageShell } from "@/components/page-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function CorporateKycApplyPage() {
  return (
    <PageShell
      title="KYC 신청"
      description="법인 기본 정보와 서류를 제출하면 자동 심사가 진행됩니다."
      module="M-03 · NEW APPLICATION"
    >
      <Card>
        <CardHeader>
          <CardTitle>법인 정보</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="grid gap-2">
            <Label htmlFor="corp-name">법인명</Label>
            <Input id="corp-name" placeholder="예) 주식회사 케이와이브이씨" />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="corp-no">법인등록번호</Label>
            <Input id="corp-no" placeholder="000000-0000000" />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="biz-no">사업자등록번호</Label>
            <Input id="biz-no" placeholder="000-00-00000" />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="rep-name">대표자명</Label>
            <Input id="rep-name" />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>서류 업로드</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm text-muted-foreground">
          <div>· 법인등기부등본</div>
          <div>· 사업자등록증</div>
          <div>· 주주명부 / 정관</div>
          <div>· 대리인 위임장 (선택)</div>
        </CardContent>
      </Card>

      <div className="flex justify-end gap-2">
        <Button variant="outline">임시 저장</Button>
        <Button>제출</Button>
      </div>
    </PageShell>
  );
}
