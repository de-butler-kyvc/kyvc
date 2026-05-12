"use client";

import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

const testVectors = [
  { case: "정상 VC 검증", input: "valid_vc_jwt", expected: "VALID", status: "통과" },
  { case: "만료 VC 검증", input: "expired_vc_jwt", expected: "EXPIRED", status: "통과" },
  { case: "서명 오류 VC", input: "tampered_vc_jwt", expected: "INVALID_SIGNATURE", status: "통과" },
];

export default function SdkPage() {
  return (
    <div>
      <PageHeader breadcrumb="SDK" title="SDK 상태조회 API 모니터링" />

      <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <div className="grid grid-cols-2 gap-6 mb-5">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">SDK 버전 · Text</label>
            <div className="text-sm font-mono text-slate-700 bg-slate-50 border border-slate-200 rounded-md px-3 py-2">
              kyvc-verifier-sdk-java-2.3.1
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">상태조회 Endpoint · URL</label>
            <div className="text-sm font-mono text-slate-700 bg-slate-50 border border-slate-200 rounded-md px-3 py-2">
              https://core.kyvc.io/api/v1/status
            </div>
          </div>
        </div>

        <p className="text-xs font-semibold text-slate-600 mb-3">테스트 벡터</p>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["케이스", "입력", "기대 결과", "상태"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-4 py-2">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {testVectors.map((v) => (
              <tr key={v.case} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-4 py-2.5 text-xs text-slate-700">{v.case}</td>
                <td className="px-4 py-2.5 text-xs font-mono text-slate-600">{v.input}</td>
                <td className="px-4 py-2.5 text-xs font-mono text-slate-600">{v.expected}</td>
                <td className="px-4 py-2.5"><StatusBadge status={v.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
