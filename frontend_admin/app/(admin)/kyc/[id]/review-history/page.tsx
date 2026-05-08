import Link from "next/link";

const history = [
  { date: "2025.05.02 10:30", action: "수동심사 대기 전환", actor: "시스템", type: "시스템", detail: "AI 보완필요 판정으로 수동심사 대기 상태 전환" },
  { date: "2025.05.02 09:18", action: "AI 자동심사 완료", actor: "KYvC-AI v2.1", type: "AI", detail: "신뢰도 72.4% · 보완필요 판정 · 대표자명 불일치 감지" },
  { date: "2025.05.02 09:14", action: "KYC 신청 접수", actor: "시스템", type: "시스템", detail: "법인 사용자 온라인 신청 · 서류 3건 업로드 완료" },
];

const typeBadge: Record<string, string> = {
  시스템: "bg-slate-100 text-slate-500",
  AI: "bg-blue-100 text-blue-600",
  수동: "bg-purple-100 text-purple-600",
};

export default async function ReviewHistoryPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = (await params);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">심사 이력 조회</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 요약 */}
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
          <h2 className="text-xs font-semibold text-slate-500">심사 이력 조회</h2>
          {[
            { label: "신청번호", value: id },
            { label: "법인명", value: "주식회사 케이원" },
            { label: "신청일", value: "2025.05.02 09:14" },
          ].map((item) => (
            <div key={item.label}>
              <p className="text-xs text-slate-400">{item.label}</p>
              <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
            </div>
          ))}
          <div>
            <p className="text-xs text-slate-400">현재 상태</p>
            <span className="bg-red-100 text-red-600 text-xs px-2 py-0.5 rounded-full font-medium">수동심사필요</span>
          </div>
          <div>
            <p className="text-xs text-slate-400">총 이력</p>
            <p className="text-slate-700 text-xs font-semibold mt-0.5">{history.length}건</p>
          </div>
        </div>

        {/* 우측 이력 테이블 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">전체 심사 이력</h2>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-5 py-3 text-slate-500 font-medium">일시</th>
                <th className="text-left px-5 py-3 text-slate-500 font-medium">처리 내용</th>
                <th className="text-left px-5 py-3 text-slate-500 font-medium">처리자</th>
                <th className="text-left px-5 py-3 text-slate-500 font-medium">유형</th>
                <th className="text-left px-5 py-3 text-slate-500 font-medium">상세</th>
              </tr>
            </thead>
            <tbody>
              {history.map((row, i) => (
                <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                  <td className="px-5 py-3.5 text-slate-400 text-xs whitespace-nowrap">{row.date}</td>
                  <td className="px-5 py-3.5 text-slate-700 font-medium">{row.action}</td>
                  <td className="px-5 py-3.5 text-slate-500 text-xs">{row.actor}</td>
                  <td className="px-5 py-3.5">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${typeBadge[row.type]}`}>{row.type}</span>
                  </td>
                  <td className="px-5 py-3.5 text-slate-400 text-xs">{row.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="flex items-center justify-between px-5 py-3">
            <span className="text-xs text-slate-400">총 {history.length}건</span>
            <Link href={`/kyc/${id}`} className="text-xs text-blue-600 hover:underline">← 신청 상세로</Link>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}