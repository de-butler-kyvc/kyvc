"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import {
  getAdminUser, updateAdminUser,
  getAllAdminRoles, getAdminUserRoles,
  assignAdminUserRole, removeAdminUserRole,
  type AdminUser, type AdminUserDetail, type AdminRole,
} from "@/lib/api/managers";
import { getAuditLogs } from "@/lib/api/audit";

const roleBadge: Record<string, string> = {
  심사자: "bg-blue-100 text-blue-600",
  승인권자: "bg-purple-100 text-purple-600",
  운영자: "bg-orange-100 text-orange-600",
  조회자: "bg-slate-100 text-slate-500",
};

const statusBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  ACTIVE: "bg-green-100 text-green-600",
  잠금: "bg-orange-100 text-orange-600",
  LOCKED: "bg-orange-100 text-orange-600",
  비활성: "bg-slate-100 text-slate-400",
  INACTIVE: "bg-slate-100 text-slate-400",
};

export default function ManagerDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [data, setData] = useState<AdminUserDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [activeTab, setActiveTab] = useState("기본 정보");

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");

  const [assignedRoles, setAssignedRoles] = useState<AdminRole[]>([]);
  const [allRoles, setAllRoles] = useState<AdminRole[]>([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [rolesError, setRolesError] = useState<string | null>(null);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [roleActionLoading, setRoleActionLoading] = useState(false);

  const [auditLogs, setAuditLogs] = useState<{ id: string; date: string; action: string; target: string }[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    getAdminUser(id)
      .then((d) => { setData(d); setName(d.name ?? ""); setEmail(d.email ?? ""); setPhone(d.phone ?? ""); })
      .catch((err) => setError(err instanceof Error ? err.message : "불러오기 실패"))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (activeTab !== "권한 목록") return;
    setRolesLoading(true);
    setRolesError(null);
    Promise.all([getAdminUserRoles(id), getAllAdminRoles()])
      .then(([assigned, all]) => { setAssignedRoles(assigned); setAllRoles(all); })
      .catch((err) => setRolesError(err instanceof Error ? err.message : "권한 정보를 불러오지 못했습니다."))
      .finally(() => setRolesLoading(false));
  }, [activeTab, id]);

  useEffect(() => {
    if (activeTab !== "활동 이력") return;
    setAuditLoading(true);
    getAuditLogs({ actorId: id })
      .then((logs) => setAuditLogs(logs.map((l) => ({ id: l.id, date: l.date, action: l.action, target: l.target }))))
      .catch(() => setAuditLogs([]))
      .finally(() => setAuditLoading(false));
  }, [activeTab, id]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await updateAdminUser(id, { name, email, phone });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleAssignRole = async () => {
    if (!selectedRoleId) return;
    setRoleActionLoading(true);
    try {
      await assignAdminUserRole(id, selectedRoleId);
      const assigned = await getAdminUserRoles(id);
      setAssignedRoles(assigned);
      setSelectedRoleId("");
    } catch (err) {
      setRolesError(err instanceof Error ? err.message : "역할 추가에 실패했습니다.");
    } finally {
      setRoleActionLoading(false);
    }
  };

  const handleRemoveRole = async (roleId: string | number) => {
    setRoleActionLoading(true);
    try {
      await removeAdminUserRole(id, roleId);
      const assigned = await getAdminUserRoles(id);
      setAssignedRoles(assigned);
    } catch (err) {
      setRolesError(err instanceof Error ? err.message : "역할 제거에 실패했습니다.");
    } finally {
      setRoleActionLoading(false);
    }
  };

  const unassignedRoles = allRoles.filter((r) => !assignedRoles.some((ar) => String(ar.roleId) === String(r.roleId)));

  const tabs = ["기본 정보", "권한 목록", "활동 이력"];

  if (loading) return <div className="p-8 text-center text-slate-400">로딩 중...</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">증명서 관리자 · <Link href="/managers" className="hover:underline">관리자 사용자 관리</Link></p>
          <h1 className="text-xl font-bold text-slate-800">관리자 상세</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="flex gap-4">
        <div className="w-52 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <div className="flex flex-col items-center text-center mb-4">
              <div className="w-12 h-12 rounded-full bg-purple-100 text-purple-600 flex items-center justify-center text-lg font-bold mb-2">{data?.name?.[0] ?? "?"}</div>
              <p className="font-semibold text-slate-800 text-sm">{data?.name ?? "-"}</p>
              <span className={`mt-1 px-2 py-0.5 rounded text-xs font-medium ${roleBadge[data?.roleName ?? ""] ?? "bg-slate-100 text-slate-500"}`}>{data?.roleName ?? "-"}</span>
              <span className={`mt-1 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[data?.status ?? ""] ?? "bg-slate-100 text-slate-400"}`}>{data?.status ?? "-"}</span>
            </div>
            <div className="space-y-2 text-xs border-t border-slate-100 pt-3">
              {[
                { label: "사용자 ID", value: data?.adminUserId ?? id },
                { label: "이메일", value: data?.email ?? "-" },
                { label: "MFA", value: data?.mfaEnabled ? "설정됨" : "미설정" },
              ].map((item) => (
                <div key={item.label}><p className="text-slate-400">{item.label}</p><p className="text-slate-700 font-medium mt-0.5 break-all">{item.value}</p></div>
              ))}
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="flex border-b border-slate-100">
              {tabs.map((tab) => (
                <button key={tab} onClick={() => setActiveTab(tab)} className={`px-4 py-3 text-sm font-medium transition-colors ${activeTab === tab ? "text-blue-600 border-b-2 border-blue-600 -mb-px" : "text-slate-500 hover:text-slate-700"}`}>{tab}</button>
              ))}
            </div>
            <div className="p-5">
              {activeTab === "기본 정보" && (
                <div className="space-y-4">
                  {saved && <div className="bg-green-50 border border-green-200 rounded px-4 py-2 text-sm text-green-700">저장됐습니다.</div>}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">성명</label><input type="text" value={name} onChange={(e) => setName(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">사용자 ID</label><input type="text" value={data?.adminUserId ?? id} readOnly className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-slate-100 text-slate-400 cursor-not-allowed" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">이메일</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">휴대폰</label><input type="text" value={phone} onChange={(e) => setPhone(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" /></div>
                  </div>
                  <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                    <Link href="/managers" className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">취소</Link>
                    <button onClick={handleSave} disabled={saving} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-60">{saving ? "저장 중..." : "저장"}</button>
                  </div>
                </div>
              )}

              {activeTab === "권한 목록" && (
                <div className="space-y-4">
                  {rolesError && <div className="bg-red-50 border border-red-200 rounded px-4 py-3 text-sm text-red-600">{rolesError}</div>}
                  {rolesLoading ? (
                    <p className="text-sm text-slate-400 text-center py-6">불러오는 중...</p>
                  ) : (
                    <>
                      <div className="space-y-2">
                        <p className="text-xs text-slate-500 font-medium">현재 할당된 역할</p>
                        {assignedRoles.length === 0 ? (
                          <p className="text-sm text-slate-400 py-4 text-center bg-slate-50 rounded-lg border border-slate-100">할당된 역할이 없습니다.</p>
                        ) : assignedRoles.map((r) => (
                          <div key={r.roleId} className="flex items-center justify-between px-3 py-2.5 bg-slate-50 rounded-lg border border-slate-100">
                            <div className="flex items-center gap-2">
                              <span className="w-5 h-5 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold flex-shrink-0">✓</span>
                              <span className="text-sm text-slate-700 font-medium">{r.roleName}</span>
                              {r.description && <span className="text-xs text-slate-400">· {r.description}</span>}
                            </div>
                            <button onClick={() => handleRemoveRole(r.roleId)} disabled={roleActionLoading} className="text-xs text-red-500 hover:text-red-700 disabled:opacity-40">제거</button>
                          </div>
                        ))}
                      </div>
                      <div className="border-t border-slate-100 pt-4">
                        <p className="text-xs text-slate-500 font-medium mb-2">역할 추가</p>
                        <div className="flex gap-2">
                          <select value={selectedRoleId} onChange={(e) => setSelectedRoleId(e.target.value)} className="flex-1 border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" disabled={roleActionLoading}>
                            <option value="">역할 선택</option>
                            {unassignedRoles.map((r) => <option key={r.roleId} value={String(r.roleId)}>{r.roleName}</option>)}
                          </select>
                          <button onClick={handleAssignRole} disabled={roleActionLoading || !selectedRoleId} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60">{roleActionLoading ? "처리 중..." : "추가"}</button>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              )}

              {activeTab === "활동 이력" && (
                auditLoading ? <p className="text-sm text-slate-400 text-center py-6">로딩 중...</p> : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 bg-slate-50">
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">일시</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">액션</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">대상</th>
                      </tr>
                    </thead>
                    <tbody>
                      {auditLogs.length === 0 ? (
                        <tr><td colSpan={3} className="px-3 py-6 text-center text-slate-400 text-sm">활동 이력이 없습니다.</td></tr>
                      ) : auditLogs.map((log) => (
                        <tr key={log.id} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-3 py-2.5 text-slate-400 text-xs">{log.date}</td>
                          <td className="px-3 py-2.5 text-slate-700">{log.action}</td>
                          <td className="px-3 py-2.5 text-blue-600 text-xs">{log.target}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC 증명서 관리자 · 증명서 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
