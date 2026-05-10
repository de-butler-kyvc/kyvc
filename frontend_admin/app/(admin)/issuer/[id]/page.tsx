"use client";
import { use, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getIssuerPolicy, updateIssuerPolicy, disableIssuerPolicy } from "@/lib/api/issuer";
import type { IssuerPolicyDetail } from "@/lib/api/issuer";
import MfaModal from "@/components/MfaModal";

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  차단: "bg-red-100 text-red-600",
  심사중: "bg-orange-100 text-orange-600",
};

const typeBadge: Record<string, string> = {
  화이트리스트: "bg-green-100 text-green-600",
  블랙리스트: "bg-red-100 text-red-600",
};

const credentialOptions = ["KYC VC", "위임권한 VC", "전체"];
const scopeOptions = ["플랫폼 전체", "파이낸셜 파트너스", "비즈파트너 포털"];

function toKoreanPolicyType(policyType: string) {
  return policyType === "WHITELIST" ? "화이트리스트" : policyType === "BLACKLIST" ? "블랙리스트" : policyType;
}

function parsePolicyId(id: string | undefined) {
  if (id == null || id === "") return NaN;
  const numeric = id.replace(/\D/g, "");
  return numeric ? Number(numeric) : NaN;
}

export default function IssuerDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const { id } = use(params);
  const policyId = parsePolicyId(id);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [policy, setPolicy] = useState<IssuerPolicyDetail | null>(null);
  const [showMfa, setShowMfa] = useState(false);

  const [did, setDid] = useState("");
  const [type, setType] = useState("화이트리스트");
  const [credential, setCredential] = useState(credentialOptions[0]);
  const [scope, setScope] = useState(scopeOptions[0]);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [status, setStatus] = useState("활성");
  const [reason, setReason] = useState("");

  const handleSave = () => {
    if (!policy || Number.isNaN(policyId)) return;
    setShowMfa(true);
  };

  const handleMfaConfirm = async (mfaToken: string) => {
    setShowMfa(false);
    try {
      setSaving(true);
      setError(null);
      await updateIssuerPolicy(policyId, {
        issuerName: policy!.issuerName,
        credentialTypes: [credential],
        status,
        reason,
        mfaToken,
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (saveError) {
      setError(`저장 중 오류가 발생했습니다: ${(saveError as Error).message}`);
    } finally {
      setSaving(false);
    }
  };

  useEffect(() => {
    async function loadPolicy() {
      if (Number.isNaN(policyId)) {
        setError("유효한 policyId가 아닙니다. URL에 숫자 ID를 포함해야 합니다.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await getIssuerPolicy(policyId);
        setPolicy(data);
        setDid(data.issuerDid);
        setType(toKoreanPolicyType(data.policyType));
        setCredential(data.credentialTypes?.[0] ?? credentialOptions[0]);
        setScope(scopeOptions[0]);
        setStatus(data.status ?? "활성");
        setReason(data.reason ?? "");
      } catch (fetchError) {
        setError(`정책 정보를 불러오는 중 오류가 발생했습니다: ${(fetchError as Error).message}`);
      } finally {
        setLoading(false);
      }
    }

    loadPolicy();
  }, [policyId]);

  const handleDisable = async () => {
    if (!policy || Number.isNaN(policyId)) return;
    if (!window.confirm("이 신뢰정책을 비활성화(삭제)하시겠습니까?")) return;

    try {
      setDeleting(true);
      setError(null);
      await disableIssuerPolicy(policyId);
      router.push("/issuer");
    } catch (disableError) {
      setError(`삭제 중 오류가 발생했습니다: ${(disableError as Error).message}`);
    } finally {
      setDeleting(false);
    }
  };

  const headerLabel = policy ? `${id} · Issuer 신뢰정책 상세` : "Issuer 신뢰정책 상세";

  if (loading) {
    return <div className="p-6 text-sm text-slate-500">정책 정보를 불러오는 중입니다...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · <Link href="/issuer" className="hover:underline">Issuer 신뢰정책 목록</Link></p>
          <h1 className="text-xl font-bold text-slate-800">Issuer 신뢰정책 상세</h1>
        </div>
      </div>

      {error ? (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">{error}</div>
      ) : null}

      <div className="flex gap-4">
        <div className="w-52 shrink-0">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs font-semibold text-slate-500 mb-3">{headerLabel}</p>
            <div className="space-y-3 text-xs">
              <div><p className="text-slate-400">Issuer ID</p><p className="text-slate-700 font-medium mt-0.5 break-all">{did}</p></div>
              <div><p className="text-slate-400">정책 유형</p><span className={`inline-block mt-0.5 px-2 py-0.5 rounded text-xs font-medium ${typeBadge[type]}`}>{type}</span></div>
              <div><p className="text-slate-400">상태</p><span className={`inline-block mt-0.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[status] ?? "bg-slate-100 text-slate-600"}`}>{status}</span></div>
              <div><p className="text-slate-400">등록일</p><p className="text-slate-700 mt-0.5">{policy?.createdAt ?? "-"}</p></div>
              <div><p className="text-slate-400">수정일</p><p className="text-slate-700 mt-0.5">{policy?.updatedAt ?? "-"}</p></div>
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="p-4 border-b border-slate-100"><p className="text-sm font-semibold text-slate-700">정책 정보 입력</p></div>
            <div className="p-5 space-y-4">
              {saved && (
                <div className="flex items-center gap-2 bg-green-50 border border-green-200 rounded-lg px-4 py-3 text-green-700 text-sm font-medium">
                  <span>✓</span> 변경사항이 저장되었습니다.
                </div>
              )}
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">Issuer ID / DID</label>
                <input type="text" value={did} onChange={(e) => setDid(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">정책 유형</label>
                <select value={type} onChange={(e) => setType(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  <option>화이트리스트</option>
                  <option>블랙리스트</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">Credential Type</label>
                <select value={credential} onChange={(e) => setCredential(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  {credentialOptions.map((opt) => <option key={opt}>{opt}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">적용 범위</label>
                <select value={scope} onChange={(e) => setScope(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  {scopeOptions.map((opt) => <option key={opt}>{opt}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">적용 기간</label>
                <div className="flex items-center gap-2">
                  <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
                  <span className="text-slate-400 text-sm">~</span>
                  <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">상태</label>
                <select value={status} onChange={(e) => setStatus(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  <option>활성</option>
                  <option>차단</option>
                  <option>심사중</option>
                </select>
              </div>
              {type === "블랙리스트" && (
                <div className="space-y-1.5">
                  <label className="text-sm text-slate-600 font-medium">차단 사유</label>
                  <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={3} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
                </div>
              )}
              <div className="flex justify-between gap-2 pt-2 border-t border-slate-100">
                <button type="button" onClick={handleDisable} disabled={deleting} className="px-4 py-1.5 border border-red-200 text-red-600 rounded text-sm hover:bg-red-50 disabled:opacity-60">
                  {deleting ? "삭제 중..." : "삭제"}
                </button>
                <div className="flex gap-2">
                  <Link href="/issuer" className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">취소</Link>
                  <button onClick={handleSave} disabled={saving} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-60">
                    {saving ? "저장 중..." : "저장"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showMfa && (
        <MfaModal onConfirm={handleMfaConfirm} onClose={() => setShowMfa(false)} />
      )}
    </div>
  );
}