"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import MfaModal from "@/components/MfaModal";
import {
  createVerifierKey,
  getVerifier,
  getVerifierKeys,
  updateVerifier,
  approveVerifier,
  suspendVerifier,
  type VerifierApiKey,
  type VerifierDetail,
} from "@/lib/api/verifier";

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
  const [keys, setKeys] = useState<VerifierApiKey[]>([]);
  const [keysLoading, setKeysLoading] = useState(true);
  const [keyName, setKeyName] = useState("운영 SDK Key");
  const [keyExpiresAt, setKeyExpiresAt] = useState("");
  const [creatingKey, setCreatingKey] = useState(false);
  const [showKeyMfa, setShowKeyMfa] = useState(false);
  const [showVerifierMfa, setShowVerifierMfa] = useState(false);
  const [verifierMfaAction, setVerifierMfaAction] = useState<"approve" | "suspend" | null>(null);
  const [issuedSecret, setIssuedSecret] = useState<VerifierApiKey | null>(null);

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

    setKeysLoading(true);
    getVerifierKeys(id)
      .then(setKeys)
      .catch((err) => setError(err instanceof Error ? err.message : "API Key 목록을 불러오지 못했습니다."))
      .finally(() => setKeysLoading(false));
  }, [id]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await updateVerifier(id, { name, domain, callbackUrl: domain, contactEmail, description });
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
    setVerifierMfaAction("approve");
    setShowVerifierMfa(true);
  };

  const handleSuspend = async () => {
    if (!confirm("중지하시겠습니까?")) return;
    setVerifierMfaAction("suspend");
    setShowVerifierMfa(true);
  };

  const handleVerifierMfaConfirm = async (mfaToken: string) => {
    if (!verifierMfaAction) return;
    setShowVerifierMfa(false);
    try {
      if (verifierMfaAction === "approve") {
        await approveVerifier(id, { comment: "관리자 승인", mfaToken });
        setData((d) => d ? { ...d, status: "ACTIVE" } : d);
      } else {
        await suspendVerifier(id, { reasonCode: "ADMIN_REQUEST", comment: "관리자 중지", mfaToken });
        setData((d) => d ? { ...d, status: "SUSPENDED" } : d);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Verifier 상태 변경에 실패했습니다.");
    } finally {
      setVerifierMfaAction(null);
    }
  };

  const handleCreateKey = () => {
    if (!keyName.trim()) {
      setError("API Key 이름을 입력해주세요.");
      return;
    }
    setShowKeyMfa(true);
  };

  const handleKeyMfaConfirm = async (mfaToken: string) => {
    setShowKeyMfa(false);
    setCreatingKey(true);
    setError(null);
    try {
      const created = await createVerifierKey(id, {
        name: keyName.trim(),
        expiresAt: keyExpiresAt ? `${keyExpiresAt}T23:59:59` : undefined,
        mfaToken,
      });
      setIssuedSecret(created);
      const nextKeys = await getVerifierKeys(id);
      setKeys(nextKeys);
    } catch (err) {
      setError(err instanceof Error ? err.message : "API Key 발급에 실패했습니다.");
    } finally {
      setCreatingKey(false);
    }
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

          <div className="mt-4 bg-white rounded-lg border border-slate-200">
            <div className="p-4 border-b border-slate-100 flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-700">SDK API Key</p>
              <button
                onClick={handleCreateKey}
                disabled={creatingKey}
                className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60"
              >
                {creatingKey ? "발급 중..." : "신규 키 발급"}
              </button>
            </div>
            <div className="p-5 space-y-4">
              {issuedSecret && (
                <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3">
                  <p className="text-sm font-semibold text-green-700">신규 API Key가 발급되었습니다.</p>
                  <p className="mt-1 text-xs text-green-700">secret은 최초 1회만 표시됩니다.</p>
                  <div className="mt-3 rounded border border-green-200 bg-white px-3 py-2 text-xs font-mono text-slate-700 break-all">
                    {issuedSecret.secret ?? "-"}
                  </div>
                </div>
              )}

              <div className="grid grid-cols-[1fr_180px] gap-3">
                <div className="space-y-1.5">
                  <label className="text-sm text-slate-600 font-medium">키 이름</label>
                  <input
                    type="text"
                    value={keyName}
                    onChange={(e) => setKeyName(e.target.value)}
                    className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-sm text-slate-600 font-medium">만료일</label>
                  <input
                    type="date"
                    value={keyExpiresAt}
                    onChange={(e) => setKeyExpiresAt(e.target.value)}
                    className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50"
                  />
                </div>
              </div>

              {keysLoading ? (
                <p className="text-sm text-slate-400 py-6 text-center">API Key 목록을 불러오는 중...</p>
              ) : keys.length === 0 ? (
                <p className="text-sm text-slate-400 py-6 text-center">발급된 API Key가 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">Key ID</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">이름</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">Prefix</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">만료</th>
                      <th className="text-left px-4 py-3 text-slate-500 font-medium">최근 사용</th>
                    </tr>
                  </thead>
                  <tbody>
                    {keys.map((key) => (
                      <tr key={key.keyId} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-4 py-3 text-xs font-mono text-slate-500">{key.keyId}</td>
                        <td className="px-4 py-3 text-slate-700">{key.keyName ?? "-"}</td>
                        <td className="px-4 py-3 text-xs font-mono text-blue-600">{key.keyPrefix ?? "-"}</td>
                        <td className="px-4 py-3">
                          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                            {key.status ?? key.keyStatusCode ?? "-"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-500">{key.expiresAt?.slice(0, 10) ?? "-"}</td>
                        <td className="px-4 py-3 text-xs text-slate-500">{key.lastUsedAt?.slice(0, 16).replace("T", " ") ?? "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showKeyMfa && (
        <MfaModal
          purpose="IMPORTANT_ACTION"
          onConfirm={handleKeyMfaConfirm}
          onClose={() => setShowKeyMfa(false)}
        />
      )}
      {showVerifierMfa && (
        <MfaModal
          purpose="IMPORTANT_ACTION"
          onConfirm={handleVerifierMfaConfirm}
          onClose={() => { setShowVerifierMfa(false); setVerifierMfaAction(null); }}
        />
      )}
    </div>
  );
}
