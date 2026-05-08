"use client";
import { use, useState } from "react";
import Link from "next/link";

const statusBadge: Record<string, string> = {
  정상: "bg-green-100 text-green-600",
  잠금: "bg-orange-100 text-orange-600",
  비활성: "bg-slate-100 text-slate-500",
};

const mockUser = {
  id: "corp_kim001",
  name: "김법인",
  email: "kim@keyone.co.kr",
  phone: "010-1234-5678",
  role: "법인 사용자",
  status: "정상" as const,
  lastLogin: "2025.05.02 09:14",
  regDate: "2024.03.15",
  mfa: "설정됨",
  corp: {
    name: "주식회사 케이원",
    biz: "123-45-67890",
    corp: "110111-0000000",
    type: "주식회사",
    rep: "김민준",
    address: "서울특별시 강남구 테헤란로 123",
  },
  kyc: [
    { id: "KYC-2025-0042", date: "2025.05.02", status: "수동심사필요", ai: "72.4%" },
    { id: "KYC-2025-0031", date: "2025.03.10", status: "정상", ai: "96.1%" },
    { id: "KYC-2025-0018", date: "2025.01.22", status: "보완필요", ai: "61.0%" },
  ],
  vc: [
    { id: "VC-2025-0031", type: "KYC VC", issued: "2025.03.15", expires: "2026.03.15", status: "활성" },
  ],
  agents: [
    { name: "이대리", role: "선임연구원", scope: "KYC 신청 및 VC 관리", expires: "2025.12.31" },
  ],
};

const kycStatusBadge: Record<string, string> = {
  수동심사필요: "bg-red-100 text-red-600",
  보완필요: "bg-orange-100 text-orange-600",
  심사중: "bg-blue-100 text-blue-600",
  정상: "bg-green-100 text-green-600",
};

const tabs = ["계정 정보", "법인 정보", "KYC 이력", "VC 이력", "대리인"];

export default function UserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [activeTab, setActiveTab] = useState("계정 정보");
  const [status, setStatus] = useState<"정상" | "잠금" | "비활성">(mockUser.status);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editForm, setEditForm] = useState({ name: mockUser.name, email: mockUser.email, phone: mockUser.phone });
  const user = { ...mockUser, id };

  const handleDelete = () => {
    alert(`계정 ${user.id}이(가) 삭제되었습니다.`);
    setShowDeleteConfirm(false);
  };

  const handlePasswordReset = () => {
    if (confirm(`${user.name}(${user.id})의 비밀번호를 초기화하시겠습니까?\n초기화 후 임시 비밀번호가 이메일로 발송됩니다.`)) {
      alert("비밀번호가 초기화되었습니다. 임시 비밀번호가 이메일로 발송됩니다.");
    }
  };

  const handleEditSave = () => {
    if (!editForm.name || !editForm.email) {
      alert("성명과 이메일은 필수입니다.");
      return;
    }
    alert("정보가 수정되었습니다.");
    setShowEditModal(false);
  };

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

      <div className="flex gap-4">
        <div className="w-56 shrink-0 space-y-3">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <div className="flex flex-col items-center text-center mb-4">
              <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-lg font-bold mb-2">
                {user.name[0]}
              </div>
              <p className="font-semibold text-slate-800 text-sm">{user.name}</p>
              <p className="text-xs text-slate-400 mt-0.5">{user.role}</p>
              <span className={`mt-2 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[status]}`}>
                {status}
              </span>
            </div>
            <div className="space-y-2 text-xs border-t border-slate-100 pt-3">
              {[
                { label: "사용자 ID", value: user.id },
                { label: "이메일", value: user.email },
                { label: "최근 접속", value: user.lastLogin },
                { label: "등록일", value: user.regDate },
                { label: "MFA", value: user.mfa },
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
            <button onClick={() => setStatus("정상")} className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600">계정 활성화</button>
            <button onClick={() => setStatus("잠금")} className="w-full text-left px-3 py-2 rounded text-xs border border-slate-200 hover:bg-slate-50 text-slate-600">계정 잠금</button>
            <button onClick={() => setStatus("비활성")} className="w-full text-left px-3 py-2 rounded text-xs border border-orange-200 hover:bg-orange-50 text-orange-600">비활성 처리</button>
            <button onClick={() => setShowDeleteConfirm(true)} className="w-full text-left px-3 py-2 rounded text-xs border border-red-200 hover:bg-red-50 text-red-500">계정 삭제</button>
          </div>
        </div>

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
                      { label: "사용자 ID", value: user.id },
                      { label: "성명", value: user.name },
                      { label: "이메일", value: user.email },
                      { label: "휴대폰", value: user.phone },
                      { label: "역할", value: user.role },
                      { label: "MFA 설정", value: user.mfa },
                      { label: "최근 접속일", value: user.lastLogin },
                      { label: "등록일", value: user.regDate },
                    ].map((item) => (
                      <div key={item.label} className="bg-slate-50 rounded p-3">
                        <p className="text-xs text-slate-400 mb-1">{item.label}</p>
                        <p className="text-sm text-slate-700 font-medium">{item.value}</p>
                      </div>
                    ))}
                  </div>
                  <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                    <button onClick={handlePasswordReset} className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">비밀번호 초기화</button>
                    <button onClick={() => setShowEditModal(true)} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">정보 수정</button>
                  </div>
                </div>
              )}

              {activeTab === "법인 정보" && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    {[
                      { label: "법인명", value: user.corp.name },
                      { label: "법인 유형", value: user.corp.type },
                      { label: "사업자등록번호", value: user.corp.biz },
                      { label: "법인등록번호", value: user.corp.corp },
                      { label: "대표자명", value: user.corp.rep },
                      { label: "주소", value: user.corp.address },
                    ].map((item) => (
                      <div key={item.label} className="bg-slate-50 rounded p-3">
                        <p className="text-xs text-slate-400 mb-1">{item.label}</p>
                        <p className="text-sm text-slate-700 font-medium">{item.value}</p>
                      </div>
                    ))}
                  </div>
                  <div className="flex justify-end pt-2 border-t border-slate-100">
                    <Link href="/kyc" className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">KYC 신청 목록 보기</Link>
                  </div>
                </div>
              )}

              {activeTab === "KYC 이력" && (
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
                    {user.kyc.map((row) => (
                      <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-3 py-2.5 text-blue-600 font-medium">{row.id}</td>
                        <td className="px-3 py-2.5 text-slate-500">{row.date}</td>
                        <td className="px-3 py-2.5 text-slate-700">{row.ai}</td>
                        <td className="px-3 py-2.5">
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${kycStatusBadge[row.status]}`}>{row.status}</span>
                        </td>
                        <td className="px-3 py-2.5">
                          <Link href={`/kyc/${row.id}`} className="text-blue-600 hover:underline text-xs">상세 →</Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {activeTab === "VC 이력" && (
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
                    {user.vc.map((row) => (
                      <tr key={row.id} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-3 py-2.5 text-blue-600 font-medium text-xs">{row.id}</td>
                        <td className="px-3 py-2.5 text-slate-700">{row.type}</td>
                        <td className="px-3 py-2.5 text-slate-500">{row.issued}</td>
                        <td className="px-3 py-2.5 text-slate-500">{row.expires}</td>
                        <td className="px-3 py-2.5">
                          <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-600">{row.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {activeTab === "대리인" && (
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
                    {user.agents.map((row) => (
                      <tr key={row.name} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-3 py-2.5 text-slate-700 font-medium">{row.name}</td>
                        <td className="px-3 py-2.5 text-slate-500">{row.role}</td>
                        <td className="px-3 py-2.5 text-slate-500">{row.scope}</td>
                        <td className="px-3 py-2.5 text-slate-500">{row.expires}</td>
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

      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">계정 삭제</h2>
            <p className="text-sm text-slate-600">
              <span className="font-medium text-red-600">{user.name} ({user.id})</span> 계정을 삭제하시겠습니까?<br />
              <span className="text-xs text-slate-400">이 작업은 되돌릴 수 없습니다.</span>
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowDeleteConfirm(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleDelete} className="bg-red-500 text-white px-4 py-1.5 rounded text-sm hover:bg-red-600">삭제 확인</button>
            </div>
          </div>
        </div>
      )}

      {showEditModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">정보 수정</h2>
            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">성명</label>
                <input value={editForm.name} onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">이메일</label>
                <input type="email" value={editForm.email} onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">휴대폰</label>
                <input value={editForm.phone} onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowEditModal(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleEditSave} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">저장</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}