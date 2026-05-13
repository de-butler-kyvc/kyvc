"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";

const DEFAULTS = { autoApprove: "0.85", manualReview: "0.70", autoReject: "0.40" };

export default function ThresholdPage() {
  const [autoApprove,  setAutoApprove]  = useState(DEFAULTS.autoApprove);
  const [manualReview, setManualReview] = useState(DEFAULTS.manualReview);
  const [autoReject,   setAutoReject]   = useState(DEFAULTS.autoReject);
  const [saving,       setSaving]       = useState(false);
  const [toast,        setToast]        = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // TODO: await fetch('/api/ai/threshold', { method: 'POST', body: JSON.stringify({ autoApprove, manualReview, autoReject }) })
      await new Promise((r) => setTimeout(r, 600));
      showToast("임계치 설정이 저장 및 적용되었습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    setAutoApprove(DEFAULTS.autoApprove);
    setManualReview(DEFAULTS.manualReview);
    setAutoReject(DEFAULTS.autoReject);
    showToast("초기화되었습니다.");
  };

  return (
    <div className="relative">
      {toast && (
        <div className="fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white bg-emerald-600 transition-all">
          {toast}
        </div>
      )}

      <PageHeader breadcrumb="AI 설정 > 임계치 설정" title="AI 임계치 설정" />

      <div className="bg-white rounded-lg border border-slate-200 p-6">
        <div className="grid grid-cols-3 gap-6 mb-6">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">프롬프트 ID · Label</label>
            <div className="text-sm text-slate-700 bg-slate-50 border border-slate-200 rounded-md px-3 py-2 font-mono">
              PROMPT-KYC-001
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">모델/배포명</label>
            <div className="text-sm text-slate-700 bg-slate-50 border border-slate-200 rounded-md px-3 py-2 font-mono">
              gpt-4o / kyvc-gpt4o-deploy
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">버전 · Select</label>
            <select className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400">
              <option>v2.2 (현재 적용)</option>
              <option>v2.1</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-6 mb-6">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">자동 승인 임계치</label>
            <input
              type="number" step="0.01" min="0" max="1"
              value={autoApprove}
              onChange={(e) => setAutoApprove(e.target.value)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400"
            />
            <p className="text-[10px] text-slate-400 mt-1">이상 → 자동 승인</p>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">수동심사 전환 임계치</label>
            <input
              type="number" step="0.01" min="0" max="1"
              value={manualReview}
              onChange={(e) => setManualReview(e.target.value)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400"
            />
            <p className="text-[10px] text-slate-400 mt-1">미만 → 수동심사 전환</p>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">자동 반려 임계치</label>
            <input
              type="number" step="0.01" min="0" max="1"
              value={autoReject}
              onChange={(e) => setAutoReject(e.target.value)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400"
            />
            <p className="text-[10px] text-slate-400 mt-1">미만 → 자동 반려</p>
          </div>
        </div>

        <div className="flex items-center gap-2 mb-6">
          <button
            onClick={handleSave}
            disabled={saving}
            className="bg-blue-600 text-white text-sm px-4 py-2 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            {saving ? "적용 중..." : "저장 및 적용"}
          </button>
          <button
            onClick={handleReset}
            disabled={saving}
            className="border border-slate-300 text-slate-600 text-sm px-4 py-2 rounded-md hover:bg-slate-50 transition-colors disabled:opacity-50"
          >
            초기화
          </button>
        </div>

        <div>
          <p className="text-xs font-semibold text-slate-600 mb-3">처리 상태 · Badge — 최근 임계치 적용 현황</p>
          <div className="flex items-center gap-3">
            <span className="bg-emerald-100 text-emerald-700 text-xs font-semibold px-3 py-1.5 rounded-md">자동 승인 145건</span>
            <span className="bg-yellow-100 text-yellow-700 text-xs font-semibold px-3 py-1.5 rounded-md">수동심사 전환 37건</span>
            <span className="bg-red-100 text-red-700 text-xs font-semibold px-3 py-1.5 rounded-md">자동 반려 21건</span>
          </div>
        </div>
      </div>
    </div>
  );
}
