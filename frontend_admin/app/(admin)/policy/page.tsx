"use client";
import { useState, useEffect } from "react";
import { getAiReviewPolicies, updateAiReviewPolicy, type AiReviewPolicy } from "@/lib/api/ai-review-policies";

const CORP_TYPES = ["주식회사", "유한회사", "합명/합자회사", "비영리법인", "조합", "외국기업"];

const CORP_TYPE_API: Record<string, string> = {
  주식회사: "CORPORATION",
  유한회사: "LIMITED",
  "합명/합자회사": "PARTNERSHIP",
  비영리법인: "NON_PROFIT",
  조합: "COOPERATIVE",
  외국기업: "FOREIGN",
};

function policyToFields(policy?: AiReviewPolicy) {
  if (!policy) return { "KYC 유효기간": "", "AI 자동승인 기준": "", "AI 자동반려 기준": "", "수동심사 기준": "" };
  return {
    "KYC 유효기간": "-",
    "AI 자동승인 기준": policy.autoApprovalThreshold != null ? `${policy.autoApprovalThreshold}% 이상` : "-",
    "AI 자동반려 기준": policy.autoRejectionThreshold != null ? `${policy.autoRejectionThreshold}% 미만` : "-",
    "수동심사 기준": policy.manualReviewThreshold != null ? `신뢰도 ${policy.manualReviewThreshold}%~${policy.autoApprovalThreshold ?? 100}%` : "-",
  };
}

export default function PolicyPage() {
  const [policies, setPolicies] = useState<AiReviewPolicy[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState("주식회사");
  const [editValues, setEditValues] = useState<Record<string, Record<string, string>>>({});
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    getAiReviewPolicies()
      .then((list) => {
        setPolicies(list);
        const initial: Record<string, Record<string, string>> = {};
        CORP_TYPES.forEach((type) => {
          const p = list.find((p) => p.corporationType === CORP_TYPE_API[type]);
          initial[type] = policyToFields(p);
        });
        setEditValues(initial);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "정책을 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, []);

  const currentPolicy = policies.find((p) => p.corporationType === CORP_TYPE_API[selected]);
  const currentValues = editValues[selected] ?? {};

  const handleChange = (label: string, value: string) => {
    setEditValues((prev) => ({ ...prev, [selected]: { ...prev[selected], [label]: value } }));
  };

  const handleSave = async () => {
    if (!currentPolicy) { setSaved(true); setTimeout(() => setSaved(false), 2000); return; }
    setSaving(true);
    setError(null);
    try {
      const autoApproval = parseFloat(currentValues["AI 자동승인 기준"]) || currentPolicy.autoApprovalThreshold;
      const autoReject = parseFloat(currentValues["AI 자동반려 기준"]) || currentPolicy.autoRejectionThreshold;
      const manual = parseFloat(currentValues["수동심사 기준"]) || currentPolicy.manualReviewThreshold;
      await updateAiReviewPolicy(currentPolicy.aiPolicyId, {
        autoApprovalThreshold: autoApproval,
        autoRejectionThreshold: autoReject,
        manualReviewThreshold: manual,
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const fields = Object.entries(currentValues);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">KYC 규칙 관리</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="bg-white rounded-lg border border-slate-200 flex">
        <div className="w-48 border-r border-slate-200 p-4 shrink-0">
          <p className="text-xs text-slate-400 mb-3 font-medium">KYC 규칙 관리</p>
          <div className="space-y-1">
            {CORP_TYPES.map((type) => (
              <button key={type} onClick={() => { setSelected(type); setSaved(false); }} className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${type === selected ? "bg-blue-50 text-blue-600 font-medium border border-blue-200" : "text-slate-600 hover:bg-slate-50"}`}>{type}</button>
            ))}
          </div>
        </div>

        <div className="flex-1 p-6">
          {loading ? (
            <div className="py-8 text-center text-slate-400 text-sm">로딩 중...</div>
          ) : (
            <>
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                  <h2 className="text-base font-semibold text-slate-800">{selected} KYC 규칙</h2>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${currentPolicy?.enabled ? "bg-green-100 text-green-600" : "bg-slate-100 text-slate-500"}`}>{currentPolicy?.enabled ? "활성" : currentPolicy ? "비활성" : "미설정"}</span>
                </div>
                <div className="flex gap-2">
                  <button onClick={() => setShowHistory(true)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">모델 정보</button>
                  <button onClick={handleSave} disabled={saving} className={`px-4 py-1.5 rounded text-sm transition-colors disabled:opacity-60 ${saved ? "bg-green-600 text-white" : "bg-blue-600 text-white hover:bg-blue-700"}`}>{saving ? "저장 중..." : saved ? "저장됨 ✓" : "규칙 저장"}</button>
                </div>
              </div>

              <div className="border border-slate-200 rounded-lg overflow-hidden">
                <table className="w-full text-sm">
                  <tbody>
                    {fields.map(([label, value]) => (
                      <tr key={label} className="border-b border-slate-100 last:border-0">
                        <td className="px-5 py-4 bg-slate-50 w-52 align-top pt-[18px]"><span className="text-slate-600 font-medium">{label}</span></td>
                        <td className="px-5 py-3">
                          <input type="text" value={value} onChange={(e) => handleChange(label, e.target.value)} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showHistory && currentPolicy && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-md p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-slate-800">{selected} · AI 정책 정보</h2>
              <button onClick={() => setShowHistory(false)} className="text-slate-400 hover:text-slate-600 text-lg leading-none">✕</button>
            </div>
            <div className="space-y-2 text-sm">
              {[
                { label: "정책 ID", value: currentPolicy.aiPolicyId },
                { label: "정책명", value: currentPolicy.policyName },
                { label: "모델 버전", value: currentPolicy.modelVersion ?? "-" },
                { label: "등록일", value: currentPolicy.createdAt?.slice(0, 10) ?? "-" },
                { label: "수정일", value: currentPolicy.updatedAt?.slice(0, 10) ?? "-" },
              ].map((i) => (
                <div key={i.label} className="flex gap-4"><span className="text-slate-400 w-24 shrink-0">{i.label}</span><span className="text-slate-700 font-medium">{i.value}</span></div>
              ))}
            </div>
            <div className="flex justify-end pt-2"><button onClick={() => setShowHistory(false)} className="bg-slate-100 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-200">닫기</button></div>
          </div>
        </div>
      )}
    </div>
  );
}
