"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import { getVerifier, updateVerifier, approveVerifier, suspendVerifier, type VerifierDetail } from "@/lib/api/verifier";

const statusBadge: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-600",
  PENDING: "bg-orange-100 text-orange-600",
  SUSPENDED: "bg-slate-100 text-slate-500",
};

export default function VerifierDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [data, setData] = useState<VerifierDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const [name, setName] = useState("");
  const [domain, setDomain] = useState("");
  const [callbackUrl, setCallbackUrl] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    setLoading(true);
    getVerifier(id)
      .then((d) => {
        setData(d);
        setName(d.name ?? "");
        setDomain(d.domain ?? "");
        setContactEmail(d.contactEmail ?? "");
        setDescription(d.description ?? "");
      })
      .catch((err) => setError(err instanceof Error ? err.message : "불러오기 실패"))
      .finally(() => setLoading(false));
  }, [id]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await updateVerifier(id, { name, domain, contactEmail, description });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleApprove = async () => {
    if (!confirm("승인하시겠습니까?")) return;
    try { await approveVerifier(id); setData((d) => d ? { ...d, status: "ACTIVE" } : d); } catch (err) { setError(err instanceof Error ? err.message : "승인 실패"); }
  };

  const handleSuspend = async () => {
    if (!confirm("중지하시겠습니까?")) return;
    try { await suspendVerifier(id); setData((d) => d ? { ...d, status: "SUSPENDED" } : d); } catch (err) { setError(err instanceof Error ? err.message : "중지 실패"); }
  };

  if (loading) return <div className="p-8 text-center text-slate-400">로딩 중...</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · <Link href="/verifier" className="hover:underline">Verifier 플랫폼 목록</Link></p>
          <h1 className="text-xl font-bold text-slate-800">Verifier 플랫폼 상세</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="flex gap-4">
        <div className="w-52 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs font-semibold text-slate-500 mb-3">Verifier 플랫폼 정보</p>
            <div className="space-y-3 text-xs">
              <div><p className="text-slate-400">상태</p><span className={`inline-block mt-0.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[data?.status ?? ""] ?? "bg-slate-100 text-slate-500"}`}>{data?.status ?? "-"}</span></div>
              <div><p className="text-slate-400">플랫폼 ID</p><p className="text-blue-600 font-medium mt-0.5 break-all">{data?.verifierId ?? id}</p></div>
              <div><p className="text-slate-400">연동 유형</p><p className="text-slate-700 mt-0.5">{data?.verifierType ?? "-"}</p></div>
            </div>
            <div className="mt-3 space-y-1.5 border-t border-slate-100 pt-3">
              {data?.status !== "ACTIVE" && (
                <button onClick={handleApprove} className="w-full text-left px-3 py-2 rounded text-xs border border-blue-200 hover:bg-blue-50 text-blue-600">승인</button>
              )}
              {data?.status === "ACTIVE" && (
                <button onClick={handleSuspend} className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600">중지</button>
              )}
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="p-4 border-b border-slate-100"><p className="text-sm font-semibold text-slate-700">플랫폼 기본 정보</p></div>
            <div className="p-5 space-y-4">
              {saved && <div className="flex items-center gap-2 bg-green-50 border border-green-200 rounded-lg px-4 py-3 text-green-700 text-sm font-medium"><span>✓</span> 변경사항이 저장되었습니다.</div>}
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">플랫폼명</label>
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">도메인</label>
                <input type="text" value={domain} onChange={(e) => setDomain(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">담당자 이메일</label>
                <input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">설명</label>
                <input type="text" value={description} onChange={(e) => setDescription(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Link href="/verifier" className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">취소</Link>
                <button onClick={handleSave} disabled={saving} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-60">{saving ? "저장 중..." : "저장"}</button>
              </div>
            </div>
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
