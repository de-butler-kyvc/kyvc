"use client";
import { useState, useEffect } from "react";
import MfaModal from "@/components/MfaModal";
import {
  getVerifierList, getVerifierKeys, createVerifierKey, revokeVerifierKey,
  type Verifier, type VerifierApiKey,
} from "@/lib/api/verifier";

const keyStatusBadge: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-600",
  활성: "bg-green-100 text-green-600",
  EXPIRING: "bg-orange-100 text-orange-600",
  "만료 임박": "bg-orange-100 text-orange-600",
  REVOKED: "bg-slate-100 text-slate-400 line-through",
  폐기: "bg-slate-100 text-slate-400 line-through",
};

function fmtDate(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 10).replaceAll("-", ".");
}

export default function SdkPage() {
  const [verifiers, setVerifiers] = useState<Verifier[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [keys, setKeys] = useState<VerifierApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [keysLoading, setKeysLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [showMfa, setShowMfa] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [keyName, setKeyName] = useState("운영 SDK Key");
  const [keyExpiresAt, setKeyExpiresAt] = useState("");
  const [issuedSecret, setIssuedSecret] = useState<VerifierApiKey | null>(null);
  const [pendingAction, setPendingAction] = useState<{ type: "issue" } | { type: "revoke"; keyId: string } | null>(null);

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
    setKeysLoading(true);
    getVerifierKeys(vid)
      .then(setKeys)
      .catch(() => setKeys([]))
      .finally(() => setKeysLoading(false));
  };

  const handleIssue = async () => {
    if (!selectedId) return;
    if (!keyName.trim()) {
      setError("키 이름을 입력해주세요.");
      return;
    }
    setPendingAction({ type: "issue" });
    setShowMfa(true);
  };

  const handleMfaConfirm = async (mfaToken: string) => {
    if (!selectedId || !pendingAction) return;
    setShowMfa(false);
    setIssuing(true);
    try {
      if (pendingAction.type === "issue") {
        const created = await createVerifierKey(selectedId, {
          name: keyName.trim(),
          expiresAt: keyExpiresAt ? `${keyExpiresAt}T23:59:59` : undefined,
          mfaToken,
        });
        setIssuedSecret(created);
        setShowIssueModal(false);
      } else {
        await revokeVerifierKey(selectedId, pendingAction.keyId, {
          reason: "관리자 요청",
          mfaToken,
        });
      }
      const updated = await getVerifierKeys(selectedId);
      setKeys(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : "키 처리에 실패했습니다.");
    } finally {
      setIssuing(false);
      setPendingAction(null);
    }
  };

  const handleRevoke = async (keyId: string) => {
    if (!selectedId || !confirm("키를 폐기하시겠습니까?")) return;
    setPendingAction({ type: "revoke", keyId });
    setShowMfa(true);
  };

  const selectedVerifier = verifiers.find((v) => v.verifierId === selectedId);

  if (loading) return <div className="p-8 text-center text-slate-400">로딩 중...</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">SDK 연동키 관리</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="bg-white rounded-lg border border-slate-200 flex">
        <div className="w-48 border-r border-slate-200 p-4 shrink-0">
          <p className="text-xs text-slate-400 mb-3 font-medium">Verifier 선택</p>
          <div className="space-y-1">
            {verifiers.map((v) => (
              <button key={v.verifierId} onClick={() => selectVerifier(v.verifierId)} className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${v.verifierId === selectedId ? "bg-blue-600 text-white font-medium" : "text-slate-600 hover:bg-slate-50"}`}>
                {v.name}
              </button>
            ))}
            {verifiers.length === 0 && <p className="text-xs text-slate-400">데이터 없음</p>}
          </div>
        </div>

        <div className="flex-1 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-slate-700">{selectedVerifier?.name ?? "-"} — 연동키</h2>
            <button onClick={() => setShowIssueModal(true)} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">+ 신규 키 발급</button>
          </div>

          {issuedSecret && (
            <div className="mb-4 rounded-lg border border-green-200 bg-green-50 px-4 py-3">
              <p className="text-sm font-semibold text-green-700">신규 API Key가 발급되었습니다.</p>
              <p className="mt-1 text-xs text-green-700">secret은 최초 1회만 표시됩니다.</p>
              <div className="mt-3 rounded border border-green-200 bg-white px-3 py-2 text-xs font-mono text-slate-700 break-all">
                {issuedSecret.secret ?? "-"}
              </div>
            </div>
          )}

          {keysLoading ? (
            <div className="py-8 text-center text-slate-400 text-sm">로딩 중...</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">키 ID</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">Prefix</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">발급일</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">만료일</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">상태</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">관리</th>
                </tr>
              </thead>
              <tbody>
                {keys.length === 0 ? (
                  <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400 text-sm">발급된 키가 없습니다.</td></tr>
                ) : keys.map((row) => (
                  <tr key={row.keyId} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="px-4 py-3 text-slate-700 font-mono text-xs">{row.keyId}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs font-mono">{row.keyPrefix ?? "-"}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs">{fmtDate(row.createdAt ?? row.issuedAt)}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs">{fmtDate(row.expiresAt)}</td>
                    <td className="px-4 py-3"><span className={`px-2 py-0.5 rounded-full text-xs font-medium ${keyStatusBadge[row.status ?? ""] ?? "bg-slate-100 text-slate-500"}`}>{row.status ?? "-"}</span></td>
                    <td className="px-4 py-3">
                      {row.status !== "REVOKED" && (
                        <button onClick={() => handleRevoke(row.keyId)} className="text-red-500 hover:text-red-700 text-xs font-medium">폐기</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showIssueModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">신규 키 발급</h2>
            <div><label className="text-xs text-slate-500 mb-1 block">Verifier</label><p className="text-sm font-medium text-slate-700">{selectedVerifier?.name ?? "-"}</p></div>
            <div>
              <label className="text-xs text-slate-500 mb-1 block">키 이름</label>
              <input value={keyName} onChange={(e) => setKeyName(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <div>
              <label className="text-xs text-slate-500 mb-1 block">만료일</label>
              <input type="date" value={keyExpiresAt} onChange={(e) => setKeyExpiresAt(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowIssueModal(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleIssue} disabled={issuing} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60">{issuing ? "발급 중..." : "발급"}</button>
            </div>
          </div>
        </div>
      )}

      {showMfa && (
        <MfaModal
          purpose="IMPORTANT_ACTION"
          onConfirm={handleMfaConfirm}
          onClose={() => { setShowMfa(false); setPendingAction(null); }}
        />
      )}
    </div>
  );
}
