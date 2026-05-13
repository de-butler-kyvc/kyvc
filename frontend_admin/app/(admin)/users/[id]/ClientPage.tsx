"use client";
import { use, useState, useEffect } from "react";
import Link from "next/link";
import {
  deleteUser,
  getUserDetail,
  updateUser,
  updateUserStatus,
  type BackendUserDetail,
} from "@/lib/api/users";
import { kycDetailPath } from "@/lib/navigation/admin-routes";

const statusBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  잠금: "bg-orange-100 text-orange-600",
  비활성: "bg-slate-100 text-slate-500",
};

const kycStatusBadge: Record<string, string> = {
  수동심사필요: "bg-red-100 text-red-600",
  보완필요: "bg-orange-100 text-orange-600",
  심사중: "bg-blue-100 text-blue-600",
  정상: "bg-green-100 text-green-600",
};

const STATUS_API_TO_KO: Record<string, "정상" | "잠금" | "비활성"> = {
  ACTIVE: "정상",
  LOCKED: "잠금",
  INACTIVE: "비활성",
  정상: "정상",
  잠금: "잠금",
  비활성: "비활성",
};

function fmt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 10).replaceAll("-", ".");
}

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

const tabs = ["계정 정보", "법인 정보", "KYC 이력", "VC 이력", "대리인"];

export default function UserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [user, setUser] = useState<BackendUserDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loadingData, setLoadingData] = useState(true);

  const [status, setStatus] = useState<"정상" | "잠금" | "비활성">("정상");
  const [activeTab, setActiveTab] = useState("계정 정보");
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editForm, setEditForm] = useState({ name: "", email: "", phone: "" });
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  useEffect(() => {
    getUserDetail(id)
      .then((data) => {
        setUser(data);
        setStatus(STATUS_API_TO_KO[data.status] ?? "정상");
        setEditForm({
          name: data.name ?? "",
          email: data.email ?? "",
          phone: data.phoneNumber ?? "",
        });
      })
      .catch((err) => {
        setLoadError(err instanceof Error ? err.message : "데이터를 불러오지 못했습니다.");
      })
      .finally(() => setLoadingData(false));
  }, [id]);

  const handleStatusChange = async (nextStatus: "정상" | "잠금" | "비활성") => {
    setActionLoading(true);
    setActionError(null);
    try {
      await updateUserStatus(id, nextStatus);
      setStatus(nextStatus);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleEditSave = async () => {
    if (!editForm.name || !editForm.email) {
      setEditError("성명과 이메일은 필수입니다.");
      return;
    }
    setEditLoading(true);
    setEditError(null);
    try {
      await updateUser(id, {
        name: editForm.name,
        email: editForm.email,
        phoneNumber: editForm.phone || undefined,
      });
      setUser((prev) => prev ? { ...prev, name: editForm.name, email: editForm.email, phoneNumber: editForm.phone } : prev);
      setShowEditModal(false);
    } catch (err) {
      setEditError(err instanceof Error ? err.message : "정보 수정에 실패했습니다.");
    } finally {
      setEditLoading(false);
    }
  };

  const handleDelete = async () => {
    setActionLoading(true);
    setActionError(null);
    try {
      await deleteUser(id);
      setShowDeleteConfirm(false);
      window.location.href = "/users";
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "계정 삭제에 실패했습니다.");
    } finally {
      setActionLoading(false);
    }
  };

  if (loadingData) {
    return (
      <div className="flex items-center justify-center py-32 text-slate-500 text-sm">
        불러오는 중...
      </div>
    );
  }

  if (loadError || !user) {
    return (
      <div className="space-y-4">
        <p className="text-xs text-slate-400">백엔드 어드민 · <Link href="/users" className="hover:underline">법인 사용자 관리</Link></p>
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-4 text-sm text-red-600">
          {loadError ?? "사용자 정보를 불러오지 못했습니다."}
        </div>
      </div>
    );
  }

  const corp = user.corporation;
  const kycList = user.kycApplications ?? [];
  const vcList = user.verifiableCredentials ?? [];
  const agentList = user.agents ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href="/users" className="hover:underline">법인 사용자 관리</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">법인 사용자 상세</h1>
        </div>
      </div>

      {actionError && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
          {actionError}
        </div>
      )}

      <div className="flex gap-4">
        {/* 좌측 프로필 */}
        <div className="w-56 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <div className="flex flex-col items-center text-center mb-4">
              <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-lg font-bold mb-2">
                {user.name[0]}
              </div>
              <p className="font-semibold text-slate-800 text-sm">{user.name}</p>
              <p className="text-xs text-slate-400 mt-0.5">{user.role ?? "법인 사용자"}</p>
              <span className={`mt-2 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[status]}`}>
                {status}
              </span>
            </div>
            <div className="space-y-2 text-xs border-t border-slate-100 pt-3">
              {[
                { label: "사용자 ID", value: user.userId },
                { label: "이메일", value: user.email ?? "-" },
                { label: "최근 접속", value: fmtDt(user.lastLoginAt) },
                { label: "등록일", value: fmt(user.createdAt) },
                { label: "MFA", value: user.mfaEnabled === true ? "설정됨" : user.mfaEnabled === false ? "미설정" : "-" },
              ].map((item) => (
                <div key={item.label}>
                  <p className="text-slate-400">{item.label}</p>
                  <p className="text-slate-700 font-medium mt-0.5 break-all">{item.value}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white rounded-lg border border-slate-200 p-4 space-y-2">
            <p className="text-xs font-semibold text-slate-500 mb-2">계정 관리</p>
            <button
              onClick={() => handleStatusChange("정상")}
              disabled={actionLoading}
              className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600 disabled:opacity-50"
            >
              계정 활성화
            </button>
            <button
              onClick={() => handleStatusChange("잠금")}
              disabled={actionLoading}
              className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600 disabled:opacity-50"
            >
              계정 잠금
            </button>
            <button
              onClick={() => handleStatusChange("비활성")}
              disabled={actionLoading}
              className="w-full text-left px-3 py-2 rounded text-xs border border-orange-200 hover:bg-orange-50 text-orange-600 disabled:opacity-50"
            >
              비활성 처리
            </button>
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="w-full text-left px-3 py-2 rounded text-xs border border-red-200 hover:bg-red-50 text-red-500"
            >
              계정 삭제
            </button>
          </div>
        </div>

        {/* 우측 탭 */}
        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="flex border-b border-slate-100">
              {tabs.map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`px-4 py-3 text-sm font-medium transition-colors ${
                    activeTab === tab ? "text-blue-600 border-b-2 border-blue-600 -mb-px" : "text-slate-500 hover:text-slate-700"
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>

            <div className="p-5">
              {activeTab === "계정 정보" && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    {[
                      { label: "사용자 ID", value: user.userId },
                      { label: "성명", value: user.name },
                      { label: "이메일", value: user.email ?? "-" },
                      { label: "휴대폰", value: user.phoneNumber ?? "-" },
                      { label: "역할", value: user.role ?? "법인 사용자" },
                      { label: "MFA 설정", value: user.mfaEnabled === true ? "설정됨" : user.mfaEnabled === false ? "미설정" : "-" },
                      { label: "최근 접속일", value: fmtDt(user.lastLoginAt) },
                      { label: "등록일", value: fmt(user.createdAt) },
                    ].map((item) => (
                      <div key={item.label} className="bg-slate-50 rounded p-3">
                        <p className="text-xs text-slate-400 mb-1">{item.label}</p>
                        <p className="text-sm text-slate-700 font-medium">{item.value}</p>
                      </div>
                    ))}
                  </div>
                  <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                    <button
                      onClick={() => {
                        setEditForm({ name: user.name, email: user.email ?? "", phone: user.phoneNumber ?? "" });
                        setEditError(null);
                        setShowEditModal(true);
                      }}
                      className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                    >
                      정보 수정
                    </button>
                  </div>
                </div>
              )}

              {activeTab === "법인 정보" && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    {[
                      { label: "법인명", value: corp?.corporationName ?? "-" },
                      { label: "법인 유형", value: corp?.corporationType ?? "-" },
                      { label: "사업자등록번호", value: corp?.businessRegistrationNumber ?? "-" },
                      { label: "법인등록번호", value: corp?.corporateRegistrationNumber ?? "-" },
                      { label: "대표자명", value: corp?.representativeName ?? "-" },
                      { label: "주소", value: corp?.address ?? "-" },
                    ].map((item) => (
                      <div key={item.label} className="bg-slate-50 rounded p-3">
                        <p className="text-xs text-slate-400 mb-1">{item.label}</p>
                        <p className="text-sm text-slate-700 font-medium">{item.value}</p>
                      </div>
                    ))}
                  </div>
                  <div className="flex justify-end pt-2 border-t border-slate-100">
                    <Link href="/kyc" className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">
                      KYC 신청 목록 보기
                    </Link>
                  </div>
                </div>
              )}

              {activeTab === "KYC 이력" && (
                kycList.length === 0 ? (
                  <p className="text-sm text-slate-400 py-6 text-center">KYC 이력이 없습니다.</p>
                ) : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 bg-slate-50">
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">신청번호</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">신청일</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">AI 신뢰도</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">상태</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">상세</th>
                      </tr>
                    </thead>
                    <tbody>
                      {kycList.map((row) => (
                        <tr key={row.applicationId} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-3 py-2.5 text-blue-600 font-medium">{row.applicationId}</td>
                          <td className="px-3 py-2.5 text-slate-500">{fmt(row.applicationDate)}</td>
                          <td className="px-3 py-2.5 text-slate-700">
                            {row.aiScore != null ? `${row.aiScore}%` : "-"}
                          </td>
                          <td className="px-3 py-2.5">
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${kycStatusBadge[row.status] ?? "bg-slate-100 text-slate-500"}`}>
                              {row.status}
                            </span>
                          </td>
                          <td className="px-3 py-2.5">
                            <Link href={kycDetailPath(row.applicationId)} className="text-blue-600 hover:underline text-xs">
                              상세 →
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              )}

              {activeTab === "VC 이력" && (
                vcList.length === 0 ? (
                  <p className="text-sm text-slate-400 py-6 text-center">VC 이력이 없습니다.</p>
                ) : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 bg-slate-50">
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">VC ID</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">유형</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">발급일</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">만료일</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">상태</th>
                      </tr>
                    </thead>
                    <tbody>
                      {vcList.map((row) => (
                        <tr key={row.vcId} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-3 py-2.5 text-blue-600 font-medium text-xs">{row.vcId}</td>
                          <td className="px-3 py-2.5 text-slate-700">{row.vcType ?? "-"}</td>
                          <td className="px-3 py-2.5 text-slate-500">{fmt(row.issuedAt)}</td>
                          <td className="px-3 py-2.5 text-slate-500">{fmt(row.expiresAt)}</td>
                          <td className="px-3 py-2.5">
                            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-600">
                              {row.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              )}

              {activeTab === "대리인" && (
                agentList.length === 0 ? (
                  <p className="text-sm text-slate-400 py-6 text-center">등록된 대리인이 없습니다.</p>
                ) : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 bg-slate-50">
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">대리인명</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">직책</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">권한 범위</th>
                        <th className="text-left px-3 py-2.5 text-slate-500 font-medium">위임 종료일</th>
                      </tr>
                    </thead>
                    <tbody>
                      {agentList.map((row, i) => (
                        <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                          <td className="px-3 py-2.5 text-slate-700 font-medium">{row.agentName}</td>
                          <td className="px-3 py-2.5 text-slate-500">{row.agentRole ?? "-"}</td>
                          <td className="px-3 py-2.5 text-slate-500">{row.authorizedScope ?? "-"}</td>
                          <td className="px-3 py-2.5 text-slate-500">{fmt(row.delegationExpiresAt)}</td>
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
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {/* 계정 삭제 확인 모달 */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">계정 삭제</h2>
            <p className="text-sm text-slate-600">
              <span className="font-medium text-red-600">{user.name} ({user.userId})</span> 계정을 삭제하시겠습니까?<br />
              <span className="text-xs text-slate-400">이 작업은 되돌릴 수 없습니다.</span>
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowDeleteConfirm(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleDelete} disabled={actionLoading} className="bg-red-500 text-white px-4 py-1.5 rounded text-sm hover:bg-red-600 disabled:opacity-60">
                {actionLoading ? "삭제 중..." : "삭제 확인"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 정보 수정 모달 */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">정보 수정</h2>
            {editError && (
              <div className="bg-red-50 border border-red-200 rounded px-3 py-2 text-sm text-red-600">
                {editError}
              </div>
            )}
            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">성명 <span className="text-red-500">*</span></label>
                <input
                  value={editForm.name}
                  onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))}
                  className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">이메일 <span className="text-red-500">*</span></label>
                <input
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                  className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">휴대폰</label>
                <input
                  value={editForm.phone}
                  onChange={(e) => setEditForm((f) => ({ ...f, phone: e.target.value }))}
                  className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => { setShowEditModal(false); setEditError(null); }}
                className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50"
              >
                취소
              </button>
              <button
                onClick={handleEditSave}
                disabled={editLoading}
                className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60"
              >
                {editLoading ? "저장 중..." : "저장"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
