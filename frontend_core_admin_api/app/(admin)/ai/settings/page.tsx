"use client";

import { useCallback, useEffect, useState } from "react";

import {
  getProviderSelectionHistory,
  getProviderSelectionOptions,
  getProviderSelections,
  updateProviderSelection,
  type ProviderCategory,
} from "@/lib/api/core-admin";

type JsonMap = Record<string, unknown>;

function stringify(value: unknown) {
  if (value == null) return "-";
  if (typeof value === "string") return value;
  return JSON.stringify(value, null, 2);
}

function extractProvider(data: JsonMap | null, category: ProviderCategory) {
  const selections = data?.selections;
  const source = selections && typeof selections === "object" ? selections : data;
  const value = (source as JsonMap | null)?.[category];

  if (typeof value === "string") return value;
  if (value && typeof value === "object") {
    const provider = (value as JsonMap).provider;
    return typeof provider === "string" ? provider : "";
  }

  return "";
}

export default function AiSettingsPage() {
  const [options, setOptions] = useState<JsonMap | null>(null);
  const [selections, setSelections] = useState<JsonMap | null>(null);
  const [history, setHistory] = useState<JsonMap | null>(null);
  const [ocrProvider, setOcrProvider] = useState("");
  const [llmProvider, setLlmProvider] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<ProviderCategory | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const showToast = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(null), 2200);
  };

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const [optionData, selectionData, historyData] = await Promise.all([
        getProviderSelectionOptions(),
        getProviderSelections(),
        getProviderSelectionHistory(10),
      ]);
      setOptions(optionData);
      setSelections(selectionData);
      setHistory(historyData);
      setOcrProvider(extractProvider(selectionData, "ocr"));
      setLlmProvider(extractProvider(selectionData, "llm"));
      setError(null);
    } catch {
      setError("코어 AI 설정 API 연결을 확인해주세요.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function save(category: ProviderCategory, provider: string) {
    if (!provider.trim()) {
      showToast("Provider 값을 입력해주세요.");
      return;
    }

    try {
      setSaving(category);
      await updateProviderSelection(category, {
        provider: provider.trim(),
        profile: "default",
        changed_by: "core-admin",
      });
      showToast("저장되었습니다.");
      await load();
    } catch {
      showToast("저장에 실패했습니다.");
    } finally {
      setSaving(null);
    }
  }

  return (
    <div className="relative">
      {toast ? (
        <div className="fixed right-5 top-5 z-50 rounded-lg bg-emerald-600 px-4 py-2.5 text-sm text-white shadow-lg">
          {toast}
        </div>
      ) : null}

      <div className="mb-4">
        <p className="mb-1 text-xs text-slate-400">
          코어 어드민 &gt; <span className="text-slate-600">AI 설정</span>
        </p>
        <h1 className="text-lg font-semibold text-slate-800">AI Provider 설정</h1>
      </div>

      {error ? (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      ) : null}

      <div className="mb-4 grid grid-cols-2 gap-4">
        <div className="rounded-lg border border-slate-200 bg-white p-5">
          <label className="mb-1.5 block text-xs text-slate-500">OCR Provider</label>
          <input
            value={ocrProvider}
            onChange={(event) => setOcrProvider(event.target.value)}
            placeholder={loading ? "불러오는 중..." : "azure_document_intelligence"}
            className="mb-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700 focus:border-blue-400 focus:outline-none"
          />
          <button
            onClick={() => save("ocr", ocrProvider)}
            disabled={saving === "ocr"}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
          >
            {saving === "ocr" ? "저장 중..." : "OCR 저장"}
          </button>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-5">
          <label className="mb-1.5 block text-xs text-slate-500">LLM Provider</label>
          <input
            value={llmProvider}
            onChange={(event) => setLlmProvider(event.target.value)}
            placeholder={loading ? "불러오는 중..." : "openai"}
            className="mb-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700 focus:border-blue-400 focus:outline-none"
          />
          <button
            onClick={() => save("llm", llmProvider)}
            disabled={saving === "llm"}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
          >
            {saving === "llm" ? "저장 중..." : "LLM 저장"}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        {[
          ["현재 선택", selections],
          ["선택 옵션", options],
          ["변경 이력", history],
        ].map(([title, data]) => (
          <div key={String(title)} className="rounded-lg border border-slate-200 bg-white">
            <div className="border-b border-slate-100 px-4 py-3 text-xs font-semibold text-slate-600">{String(title)}</div>
            <pre className="max-h-80 overflow-auto p-4 text-xs text-slate-700">{loading ? "불러오는 중..." : stringify(data)}</pre>
          </div>
        ))}
      </div>
    </div>
  );
}
