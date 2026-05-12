"use client";

import { useState } from "react";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

interface TestVector {
  case: string;
  input: string;
  expected: string;
  status: string;
}

const initialVectors: TestVector[] = [
  { case: "정상 VC 검증",  input: "valid_vc_jwt",    expected: "VALID",              status: "통과" },
  { case: "만료 VC 검증",  input: "expired_vc_jwt",  expected: "EXPIRED",            status: "통과" },
  { case: "서명 오류 VC",  input: "tampered_vc_jwt", expected: "INVALID_SIGNATURE",  status: "통과" },
];

export default function SdkTestVectorsPage() {
  const [vectors,  setVectors]  = useState<TestVector[]>(initialVectors);
  const [running,  setRunning]  = useState(false);
  const [toast,    setToast]    = useState<{ msg: string; type: "success" | "error" } | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [newVec,   setNewVec]   = useState({ case: "", input: "", expected: "" });

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const handleRunAll = async () => {
    setRunning(true);
    setVectors((prev) => prev.map((v) => ({ ...v, status: "대기" })));
    try {
      // TODO: await fetch('/api/sdk/testvectors/run')
      for (let i = 0; i < vectors.length; i++) {
        await new Promise((r) => setTimeout(r, 400));
        setVectors((prev) =>
          prev.map((v, idx) => (idx === i ? { ...v, status: "통과" } : v))
        );
      }
      showToast(`전체 ${vectors.length}건 테스트 완료`);
    } finally {
      setRunning(false);
    }
  };

  const handleAddVector = () => {
    if (!newVec.case || !newVec.input || !newVec.expected) {
      showToast("모든 항목을 입력해주세요.", "error");
      return;
    }
    setVectors((prev) => [...prev, { ...newVec, status: "대기" }]);
    setNewVec({ case: "", input: "", expected: "" });
    setShowForm(false);
    showToast("벡터가 추가되었습니다.");
  };

  return (
    <div className="relative">
      {toast && (
        <div
          className={`fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white transition-all ${
            toast.type === "success" ? "bg-emerald-600" : "bg-red-600"
          }`}
        >
          {toast.msg}
        </div>
      )}

      <PageHeader breadcrumb="SDK > 테스트 벡터" title="SDK 테스트 벡터 관리" />

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
        <table className="w-full mb-4">
          <thead>
            <tr className="border-b border-slate-100">
              {["케이스", "입력", "기대 결과", "상태"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-4 py-2">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {vectors.map((v, i) => (
              <tr key={i} className="border-b border-slate-50 hover:bg-slate-50 last:border-0">
                <td className="px-4 py-2.5 text-xs text-slate-700">{v.case}</td>
                <td className="px-4 py-2.5 text-xs font-mono text-slate-600">{v.input}</td>
                <td className="px-4 py-2.5 text-xs font-mono text-slate-600">{v.expected}</td>
                <td className="px-4 py-2.5"><StatusBadge status={v.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* 벡터 추가 인라인 폼 */}
        {showForm && (
          <div className="bg-slate-50 border border-slate-200 rounded-md p-4 mb-3 grid grid-cols-3 gap-3 items-end">
            <div>
              <label className="block text-[11px] text-slate-500 mb-1">케이스명</label>
              <input
                value={newVec.case}
                onChange={(e) => setNewVec((p) => ({ ...p, case: e.target.value }))}
                placeholder="예: 폐기 VC 검증"
                className="w-full border border-slate-300 rounded px-2 py-1.5 text-xs focus:outline-none focus:border-blue-400"
              />
            </div>
            <div>
              <label className="block text-[11px] text-slate-500 mb-1">입력</label>
              <input
                value={newVec.input}
                onChange={(e) => setNewVec((p) => ({ ...p, input: e.target.value }))}
                placeholder="예: revoked_vc_jwt"
                className="w-full border border-slate-300 rounded px-2 py-1.5 text-xs font-mono focus:outline-none focus:border-blue-400"
              />
            </div>
            <div>
              <label className="block text-[11px] text-slate-500 mb-1">기대 결과</label>
              <input
                value={newVec.expected}
                onChange={(e) => setNewVec((p) => ({ ...p, expected: e.target.value }))}
                placeholder="예: REVOKED"
                className="w-full border border-slate-300 rounded px-2 py-1.5 text-xs font-mono focus:outline-none focus:border-blue-400"
              />
            </div>
            <div className="col-span-3 flex gap-2">
              <button
                onClick={handleAddVector}
                className="bg-blue-600 text-white text-xs px-3 py-1.5 rounded-md hover:bg-blue-700 transition-colors"
              >
                추가
              </button>
              <button
                onClick={() => { setShowForm(false); setNewVec({ case: "", input: "", expected: "" }); }}
                className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-100 transition-colors"
              >
                취소
              </button>
            </div>
          </div>
        )}

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowForm((v) => !v)}
            className="bg-blue-600 text-white text-xs px-3 py-1.5 rounded-md hover:bg-blue-700 transition-colors"
          >
            + 벡터 추가
          </button>
          <button
            onClick={handleRunAll}
            disabled={running}
            className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors disabled:opacity-50"
          >
            {running ? "테스트 실행 중..." : "전체 테스트 실행"}
          </button>
        </div>
      </div>
    </div>
  );
}
