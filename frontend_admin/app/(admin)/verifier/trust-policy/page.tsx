"use client";
import { useState, useEffect } from "react";
import { getVerifierList, getVerifierCallbacks, updateVerifierCallbacks, type Verifier, type VerifierCallback } from "@/lib/api/verifier";

export default function VerifierTrustPolicyPage() {
  const [verifiers, setVerifiers] = useState<Verifier[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [callbacks, setCallbacks] = useState<VerifierCallback | null>(null);
  const [loading, setLoading] = useState(true);
  const [policyLoading, setPolicyLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [callbackUrl, setCallbackUrl] = useState("");
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getVerifierList()
      .then((list) => {
        setVerifiers(list);
        if (list.length > 0) selectVerifier(list[0].verifierId);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "목록 로드 실패"))
      .finally(() => setLoading(false));
  }, []);

  const selectVerifier = (vid: string) => {
    setSelectedId(vid);
    setEditing(false);
    setPolicyLoading(true);
    getVerifierCallbacks(vid)
      .then((cb) => { setCallbacks(cb); setCallbackUrl(cb.callbackUrl ?? ""); })
      .catch(() => { setCallbacks(null); setCallbackUrl(""); })
      .finally(() => setPolicyLoading(false));
  };

  const handleSave = async () => {
    if (!selectedId) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateVerifierCallbacks(selectedId, { ...callbacks, callbackUrl });
      setCallbacks(updated);
      setEditing(false);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const selectedVerifier = verifiers.find((v) => v.verifierId === selectedId);

  const policyItems = [
    { label: "허용 Credential Type", value: selectedVerifier?.credentialTypes?.join(", ") ?? "-" },
    { label: "Callback URL", value: callbackUrl, editable: true },
    { label: "Callback Events", value: callbacks?.callbackEvents?.join(", ") ?? "-" },
    { label: "Callback 활성화", value: callbacks?.enabled ? "활성" : "비활성" },
  ];

  if (loading) return <div className="p-8 text-center text-slate-400">로딩 중...</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · Verifier</p>
          <h1 className="text-xl font-bold text-slate-800">Verifier별 신뢰정책 설정</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="flex gap-4">
        <div className="w-48 shrink-0 bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50">
            <p className="text-xs font-semibold text-slate-500">Verifier 선택</p>
          </div>
          {verifiers.map((v) => (
            <button key={v.verifierId} onClick={() => selectVerifier(v.verifierId)} className={`w-full text-left px-4 py-2.5 text-sm border-b border-slate-50 transition-colors ${selectedId === v.verifierId ? "bg-blue-50 text-blue-600 font-medium" : "text-slate-600 hover:bg-slate-50"}`}>
              {v.name}
            </button>
          ))}
          {verifiers.length === 0 && <p className="px-4 py-3 text-xs text-slate-400">데이터 없음</p>}
        </div>

        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">{selectedVerifier?.name ?? "-"} · 신뢰정책</h2>
            <button onClick={() => setEditing(!editing)} className={`px-4 py-1.5 rounded text-sm ${editing ? "border border-slate-200 text-slate-600" : "bg-blue-600 text-white hover:bg-blue-700"}`}>
              {editing ? "취소" : "편집"}
            </button>
          </div>
          {policyLoading ? (
            <div className="p-8 text-center text-slate-400 text-sm">로딩 중...</div>
          ) : (
            <div className="p-5 space-y-4">
              {saved && <div className="bg-green-50 border border-green-200 rounded px-4 py-2 text-sm text-green-700">저장됐습니다.</div>}
              {policyItems.map((item) => (
                <div key={item.label} className="flex items-center gap-4 border-b border-slate-50 pb-3 last:border-0 last:pb-0">
                  <span className="text-sm text-slate-500 w-44 shrink-0">{item.label}</span>
                  {editing && item.editable ? (
                    <input value={callbackUrl} onChange={(e) => setCallbackUrl(e.target.value)} className="flex-1 border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                  ) : (
                    <span className="text-sm text-slate-700 font-medium">{item.value}</span>
                  )}
                </div>
              ))}
              {editing && (
                <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                  <button onClick={() => setEditing(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
                  <button onClick={handleSave} disabled={saving} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60">{saving ? "저장 중..." : "저장"}</button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
