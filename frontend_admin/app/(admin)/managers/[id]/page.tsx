"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import {
  getAllAdminRoles,
  getAdminUserRoles,
  assignAdminUserRole,
  removeAdminUserRole,
  type AdminRole,
} from "@/lib/api/managers";

const roleBadge: Record<string, string> = {
  심사자: "bg-blue-100 text-blue-600",
  승인권자: "bg-purple-100 text-purple-600",
  운영자: "bg-orange-100 text-orange-600",
  조회자: "bg-slate-100 text-slate-500",
};

const statusBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  잠금: "bg-orange-100 text-orange-600",
  비활성: "bg-slate-100 text-slate-400",
};

const mockManagerMap: Record<string, {
  id: string; name: string; role: string; status: string;
  email: string; phone: string; lastLogin: string; regDate: string; mfa: string;
  auditLog: { date: string; action: string; target: string }[];
}> = {
  admin_kim: { id: "admin_kim", name: "김심사", role: "심사자", status: "정상", email: "kim@kyvc.io", phone: "010-1111-2222", lastLogin: "2025.05.02 09:00", regDate: "2024.01.10", mfa: "설정됨", auditLog: [{ date: "2025.05.02 09:05", action: "수동심사 승인", target: "KYC-2025-0042" }, { date: "2025.05.01 15:30", action: "보완요청 생성", target: "KYC-2025-0039" }] },
  admin_park: { id: "admin_park", name: "박심사", role: "승인권자", status: "정상", email: "park@kyvc.io", phone: "010-3333-4444", lastLogin: "2025.05.02 09:10", regDate: "2023.11.20", mfa: "설정됨", auditLog: [{ date: "2025.05.02 09:15", action: "정책 수정", target: "POL-001" }, { date: "2025.05.01 17:00", action: "최종 승인", target: "KYC-2025-0035" }] },
  admin_ops: { id: "admin_ops", name: "이운영", role: "운영자", status: "정상", email: "ops@kyvc.io", phone: "010-5555-6666", lastLogin: "2025.05.01 17:00", regDate: "2024.03.01", mfa: "미설정", auditLog: [{ date: "2025.05.01 16:55", action: "SDK 키 갱신", target: "SDK-2025-001" }] },
};

export default function ManagerDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const raw = mockManagerMap[id] ?? mockManagerMap["admin_kim"];

  const [name, setName] = useState(raw.name);
  const [email, setEmail] = useState(raw.email);
  const [phone, setPhone] = useState(raw.phone);
  const [role, setRole] = useState(raw.role);
  const [status, setStatus] = useState(raw.status);
  const [saved, setSaved] = useState(false);
  const [activeTab, setActiveTab] = useState("기본 정보");

  // 권한 목록 탭 상태
  const [assignedRoles, setAssignedRoles] = useState<AdminRole[]>([]);
  const [allRoles, setAllRoles] = useState<AdminRole[]>([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [rolesError, setRolesError] = useState<string | null>(null);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [roleActionLoading, setRoleActionLoading] = useState(false);

  const tabs = ["기본 정보", "권한 목록", "활동 이력"];
  const handleSave = () => { setSaved(true); setTimeout(() => setSaved(false), 2000); };

  useEffect(() => {
    if (activeTab !== "권한 목록") return;

    setRolesLoading(true);
    setRolesError(null);

    Promise.all([getAdminUserRoles(id), getAllAdminRoles()])
      .then(([assigned, all]) => {
        setAssignedRoles(assigned);
        setAllRoles(all);
      })
      .catch((err) => {
        setRolesError(err instanceof Error ? err.message : "권한 정보를 불러오지 못했습니다.");
      })
      .finally(() => setRolesLoading(false));
  }, [activeTab, id]);

  const handleAssignRole = async () => {
    if (!selectedRoleId) return;
    setRoleActionLoading(true);
    setRolesError(null);
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
    setRolesError(null);
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

  const unassignedRoles = allRoles.filter(
    (r) => !assignedRoles.some((ar) => String(ar.roleId) === String(r.roleId))
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · <Link href="/managers" className="hover:underline">관리자 사용자 관리</Link></p>
          <h1 className="text-xl font-bold text-slate-800">관리자 상세</h1>
        </div>
      </div>

      <div className="flex gap-4">
        <div className="w-52 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <div className="flex flex-col items-center text-center mb-4">
              <div className="w-12 h-12 rounded-full bg-purple-100 text-purple-600 flex items-center justify-center text-lg font-bold mb-2">{raw.name[0]}</div>
              <p className="font-semibold text-slate-800 text-sm">{raw.name}</p>
              <span className={`mt-1 px-2 py-0.5 rounded text-xs font-medium ${roleBadge[raw.role] ?? "bg-slate-100 text-slate-500"}`}>{raw.role}</span>
              <span className={`mt-1 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[status]}`}>{status}</span>
            </div>
            <div className="space-y-2 text-xs border-t border-slate-100 pt-3">
              {[{ label: "사용자 ID", value: raw.id }, { label: "이메일", value: raw.email }, { label: "MFA", value: raw.mfa }, { label: "최근 접속", value: raw.lastLogin }, { label: "등록일", value: raw.regDate }].map((item) => (
                <div key={item.label}><p className="text-slate-400">{item.label}</p><p className="text-slate-700 font-medium mt-0.5 break-all">{item.value}</p></div>
              ))}
            </div>
          </div>
          <div className="bg-white rounded-lg border border-slate-200 p-4 space-y-2">
            <p className="text-xs font-semibold text-slate-500 mb-2">계정 관리</p>
            <button onClick={() => setStatus("정상")} className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600">계정 활성화</button>
            <button onClick={() => setStatus("잠금")} className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600">계정 잠금</button>
            <button className="w-full text-left px-3 py-2 rounded text-xs border border-orange-200 hover:bg-orange-50 text-orange-600">비밀번호 초기화</button>
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
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">성명</label><input type="text" value={name} onChange={(e) => setName(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">사용자 ID</label><input type="text" value={raw.id} readOnly className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-slate-100 text-slate-400 cursor-not-allowed" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">이메일</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">휴대폰</label><input type="text" value={phone} onChange={(e) => setPhone(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" /></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">역할</label><select value={role} onChange={(e) => setRole(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50"><option>심사자</option><option>승인권자</option><option>운영자</option><option>조회자</option></select></div>
                    <div className="space-y-1.5"><label className="text-sm text-slate-600 font-medium">MFA 설정</label><input type="text" value={raw.mfa} readOnly className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-slate-100 text-slate-500 cursor-not-allowed" /></div>
                  </div>
                  <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                    <Link href="/managers" className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">취소</Link>
                    <button onClick={handleSave} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">{saved ? "저장됨 ✓" : "저장"}</button>
                  </div>
                </div>
              )}

              {activeTab === "권한 목록" && (
                <div className="space-y-4">
                  {rolesError && (
                    <div className="bg-red-50 border border-red-200 rounded px-4 py-3 text-sm text-red-600">
                      {rolesError}
                    </div>
                  )}

                  {rolesLoading ? (
                    <p className="text-sm text-slate-400 text-center py-6">불러오는 중...</p>
                  ) : (
                    <>
                      {/* 할당된 역할 목록 */}
                      <div className="space-y-2">
                        <p className="text-xs text-slate-500 font-medium">현재 할당된 역할</p>
                        {assignedRoles.length === 0 ? (
                          <p className="text-sm text-slate-400 py-4 text-center bg-slate-50 rounded-lg border border-slate-100">
                            할당된 역할이 없습니다.
                          </p>
                        ) : (
                          assignedRoles.map((r) => (
                            <div
                              key={r.roleId}
                              className="flex items-center justify-between px-3 py-2.5 bg-slate-50 rounded-lg border border-slate-100"
                            >
                              <div className="flex items-center gap-2">
                                <span className="w-5 h-5 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold flex-shrink-0">✓</span>
                                <span className="text-sm text-slate-700 font-medium">{r.roleName}</span>
                                {r.description && (
                                  <span className="text-xs text-slate-400">· {r.description}</span>
                                )}
                              </div>
                              <button
                                onClick={() => handleRemoveRole(r.roleId)}
                                disabled={roleActionLoading}
                                className="text-xs text-red-500 hover:text-red-700 disabled:opacity-40 transition-colors"
                              >
                                제거
                              </button>
                            </div>
                          ))
                        )}
                      </div>

                      {/* 역할 추가 */}
                      <div className="border-t border-slate-100 pt-4">
                        <p className="text-xs text-slate-500 font-medium mb-2">역할 추가</p>
                        <div className="flex gap-2">
                          <select
                            value={selectedRoleId}
                            onChange={(e) => setSelectedRoleId(e.target.value)}
                            className="flex-1 border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                            disabled={roleActionLoading}
                          >
                            <option value="">역할 선택</option>
                            {unassignedRoles.map((r) => (
                              <option key={r.roleId} value={String(r.roleId)}>
                                {r.roleName}
                              </option>
                            ))}
                          </select>
                          <button
                            onClick={handleAssignRole}
                            disabled={roleActionLoading || !selectedRoleId}
                            className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60 transition-colors"
                          >
                            {roleActionLoading ? "처리 중..." : "추가"}
                          </button>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              )}

              {activeTab === "활동 이력" && (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50">
                      <th className="text-left px-3 py-2.5 text-slate-500 font-medium">일시</th>
                      <th className="text-left px-3 py-2.5 text-slate-500 font-medium">액션</th>
                      <th className="text-left px-3 py-2.5 text-slate-500 font-medium">대상</th>
                    </tr>
                  </thead>
                  <tbody>
                    {raw.auditLog.map((log, i) => (
                      <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-3 py-2.5 text-slate-400 text-xs">{log.date}</td>
                        <td className="px-3 py-2.5 text-slate-700">{log.action}</td>
                        <td className="px-3 py-2.5 text-blue-600 text-xs">{log.target}</td>
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
    </div>
  );
}
