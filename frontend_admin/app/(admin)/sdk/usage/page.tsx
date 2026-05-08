import Link from "next/link";

const usageData = [
  { verifier: "파이낸셜 파트너스", keyType: "API Key", calls: 1284, success: 1271, fail: 13, last: "2025.05.02 09:14", status: "정상" },
  { verifier: "파이낸셜 파트너스", keyType: "mTLS 인증서", calls: 432, success: 432, fail: 0, last: "2025.05.02 08:55", status: "정상" },
  { verifier: "비즈파트너 포털", keyType: "API Key", calls: 89, success: 85, fail: 4, last: "2025.05.02 07:30", status: "정상" },
  { verifier: "마켓플레이스 A", keyType: "API Key", calls: 0, success: 0, fail: 0, last: "-", status: "미사용" },
];

const statusBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  미사용: "bg-slate-100 text-slate-400",
  오류: "bg-red-100 text-red-600",
};

export default function SdkUsagePage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · SDK 관리</p>
          <h1 className="text-xl font-bold text-slate-800">SDK 사용 현황 조회</h1>
        </div>
      </div>

      {/* 요약 카드 */}
      <div className="grid grid-cols-4 gap-4">
        {[
          { label: "전체 호출", value: "1,805", color: "text-slate-700" },
          { label: "성공", value: "1,788", color: "text-green-600" },
          { label: "실패", value: "17", color: "text-red-500" },
          { label: "성공률", value: "99.1%", color: "text-blue-600" },
        ].map((card) => (
          <div key={card.label} className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs text-slate-400">{card.label}</p>
            <p className={`text-2xl font-bold mt-1 ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">Verifier별 SDK 호출 현황</h2>
          <span className="text-xs text-slate-400">기준: 2025.05.02 오늘</span>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50">
              <th className="text-left px-4 py-3 text-slate-500 font-medium">Verifier</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">키 유형</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">전체 호출</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">성공</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">실패</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">마지막 호출</th>
              <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
            </tr>
          </thead>
          <tbody>
            {usageData.map((row, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                <td className="px-4 py-3 text-slate-700 font-medium">{row.verifier}</td>
                <td className="px-4 py-3 text-slate-500 text-xs">{row.keyType}</td>
                <td className="px-4 py-3 text-slate-700 font-semibold">{row.calls.toLocaleString()}</td>
                <td className="px-4 py-3 text-green-600">{row.success.toLocaleString()}</td>
                <td className={`px-4 py-3 font-medium ${row.fail > 0 ? "text-red-500" : "text-slate-400"}`}>{row.fail}</td>
                <td className="px-4 py-3 text-slate-400 text-xs">{row.last}</td>
                <td className="px-4 py-3"><span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[row.status]}`}>{row.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}