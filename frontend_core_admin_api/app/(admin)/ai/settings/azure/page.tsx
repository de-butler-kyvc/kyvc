"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";

const DEFAULTS = {
  model: "gpt-4o / kyvc-gpt4o-deploy",
  promptVer: "v2.2",
  threshold: "0.80",
  endpoint: "https://kyvc-openai.openai.azure.com/",
  apiKey: "sk-************************3f2a",
};

export default function AzurePage() {
  const [model,      setModel]      = useState(DEFAULTS.model);
  const [promptVer,  setPromptVer]  = useState(DEFAULTS.promptVer);
  const [threshold,  setThreshold]  = useState(DEFAULTS.threshold);
  const [endpoint,   setEndpoint]   = useState(DEFAULTS.endpoint);
  const [apiKey,     setApiKey]     = useState(DEFAULTS.apiKey);
  const [tested,     setTested]     = useState<boolean | null>(null);
  const [saving,     setSaving]     = useState(false);
  const [toast,      setToast]      = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const handleTest = async () => {
    setTested(null);
    // TODO: await fetch('/api/ai/azure/test', { method: 'POST', body: JSON.stringify({ endpoint, apiKey, model }) })
    await new Promise((r) => setTimeout(r, 800));
    setTested(true);
  };

  const handleSave = async () => {
    if (!tested) {
      showToast("저장 전에 연동 테스트를 먼저 실행해주세요.");
      return;
    }
    setSaving(true);
    try {
      // TODO: await fetch('/api/ai/azure', { method: 'POST', body: JSON.stringify({ model, promptVer, threshold, endpoint, apiKey }) })
      await new Promise((r) => setTimeout(r, 600));
      showToast("Azure OpenAI 설정이 저장되었습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="relative">
      {toast && (
        <div className="fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white bg-emerald-600 transition-all">
          {toast}
        </div>
      )}

      <PageHeader breadcrumb="AI 설정 > Azure OpenAI 연동 설정" title="Azure OpenAI 연동 설정" />

      <div className="bg-white rounded-lg border border-slate-200 p-6">
        <div className="grid grid-cols-2 gap-6 mb-6">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">AI 요청 ID · Label</label>
            <div className="text-sm text-slate-700 bg-slate-50 border border-slate-200 rounded-md px-3 py-2 font-mono">
              AZURE-CONN-001
            </div>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">모델/배포명 · Text/Select</label>
            <select value={model} onChange={(e) => { setModel(e.target.value); setTested(null); }}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400">
              <option>gpt-4o / kyvc-gpt4o-deploy</option>
              <option>gpt-4o-mini / kyvc-mini-deploy</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-6 mb-6">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">프롬프트 버전 · Select</label>
            <select value={promptVer} onChange={(e) => setPromptVer(e.target.value)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400">
              <option>v2.2</option><option>v2.1</option><option>v2.0</option>
            </select>
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">신뢰도 임계치 · Number</label>
            <input type="number" step="0.01" min="0" max="1" value={threshold}
              onChange={(e) => setThreshold(e.target.value)}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400" />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-6 mb-6">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">Azure Endpoint</label>
            <input type="text" value={endpoint}
              onChange={(e) => { setEndpoint(e.target.value); setTested(null); }}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400" />
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">API Key (마스킹)</label>
            <input type="password" value={apiKey}
              onChange={(e) => { setApiKey(e.target.value); setTested(null); }}
              className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm text-slate-700 focus:outline-none focus:border-blue-400" />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleTest}
            className="bg-blue-600 text-white text-sm px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            연동 테스트
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="bg-slate-700 text-white text-sm px-4 py-2 rounded-md hover:bg-slate-800 transition-colors disabled:opacity-50"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
          {tested === true && (
            <span className="text-xs text-emerald-600 font-medium ml-2">✓ 연동 성공</span>
          )}
        </div>
      </div>
    </div>
  );
}
