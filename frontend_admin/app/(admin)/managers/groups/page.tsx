"use client";
import { useState } from "react";

const groups = [
  { id: "GRP-001", name: "심사자", members: 3, permissions: ["KYC 신청 조회", "KYC 심사 처리", "AI 결과 조회", "보완요청 생성"] },
  { id: "GRP-002", name: "승인권자", members: 1, permissions: ["KYC 신청 조회", "KYC 심사 처리", "최종 승인/반려", "정책 관리", "사용자 관리"] },
  { id: "GRP-003", name: "운영자", members: 1, permissions: ["시스템 설정 조회", "SDK 관리", "감사로그 조회", "리포트 조회"] },
  { id: "GRP-004", name: "조회자", members: 2, permissions: ["KYC 신청 조회", "VC 관리 조회", "VP 이력 조회"] },
];

const allPermissions = [
  "KYC 신청 조회", "KYC 심사 처리", "AI 결과 조회", "보완요청 생성",
  "최종 승인/반려", "정책 관리", "사용자 관리", "시스템 설정 조회",
  "SDK 관리", "감사로그 조회", "리포트 조회", "VC 관리 조회", "VP 이력 조회",
];

export default function ManagerGroupsPage() {
  const [groupList, setGroupList] = useState(groups);
  const [selected, setSelected] = useState("GRP-001");
  const group = groupList.find((g) => g.id === selected)!;
  const [editing, setEditing] = useState(false);
  const [perms, setPerms] = useState<string[]>(group.permissions);
  const [saved, setSaved] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");

  const handleSelect = (id: string) => {
    setSelected(id);
    setEditing(false);
    setPerms(groupList.find((g) => g.id === id)!.permissions);
  };

  const handleAddGroup = () => {
    if (!newGroupName.trim()) {
      alert("그룹명을 입력해주세요.");
      return;
    }
    const newId = `GRP-${String(groupList.length + 1).padStart(3, "0")}`;
    const newGroup = { id: newId, name: newGroupName.trim(), members: 0, permissions: [] };
    setGroupList(prev => [...prev, newGroup]);
    setSelected(newId);
    setPerms([]);
    setEditing(true);
    setNewGroupName("");
    setShowAddModal(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · 관리자</p>
          <h1 className="text-xl font-bold text-slate-800">권한 그룹 관리</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 그룹 목록 */}
        <div className="w-48 shrink-0 bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-500">권한 그룹</p>
            <button onClick={() => setShowAddModal(true)} className="text-xs text-blue-600 hover:underline">+ 추가</button>
          </div>
          {groups.map((g) => (
            <button
              key={g.id}
              onClick={() => handleSelect(g.id)}
              className={`w-full text-left px-4 py-3 border-b border-slate-50 transition-colors ${selected === g.id ? "bg-blue-50" : "hover:bg-slate-50"}`}
            >
              <p className={`text-sm font-medium ${selected === g.id ? "text-blue-600" : "text-slate-700"}`}>{g.name}</p>
              <p className="text-xs text-slate-400 mt-0.5">멤버 {g.members}명</p>
            </button>
          ))}
        </div>

        {/* 우측 권한 목록 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
            <div>
              <h2 className="text-sm font-semibold text-slate-700">{group.name} · 권한 목록</h2>
              <p className="text-xs text-slate-400 mt-0.5">멤버 {group.members}명</p>
            </div>
            <button onClick={() => setEditing(!editing)} className={`px-4 py-1.5 rounded text-sm ${editing ? "border border-slate-200 text-slate-600" : "bg-blue-600 text-white hover:bg-blue-700"}`}>
              {editing ? "취소" : "편집"}
            </button>
          </div>
          <div className="p-5">
            <div className="grid grid-cols-2 gap-2">
              {allPermissions.map((perm) => {
                const has = perms.includes(perm);
                return (
                  <label key={perm} className={`flex items-center gap-3 px-3 py-2.5 rounded-lg border cursor-pointer transition-colors ${has ? "border-blue-200 bg-blue-50" : "border-slate-100 bg-slate-50"} ${editing ? "cursor-pointer" : "cursor-default"}`}>
                    <input
                      type="checkbox"
                      checked={has}
                      disabled={!editing}
                      onChange={() => {
                        if (!editing) return;
                        setPerms(has ? perms.filter((p) => p !== perm) : [...perms, perm]);
                      }}
                      className="accent-blue-600"
                    />
                    <span className={`text-sm ${has ? "text-blue-700 font-medium" : "text-slate-500"}`}>{perm}</span>
                  </label>
                );
              })}
            </div>
            {editing && (
              <div className="flex justify-end gap-2 mt-4 pt-4 border-t border-slate-100">
                <button onClick={() => setEditing(false)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
                <button onClick={() => { setSaved(true); setEditing(false); setTimeout(() => setSaved(false), 2000); }} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">{saved ? "저장됨 ✓" : "저장"}</button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showAddModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
            <h2 className="text-base font-semibold text-slate-800">권한 그룹 추가</h2>
            <div>
              <label className="text-xs text-slate-500 mb-1 block">그룹명</label>
              <input
                value={newGroupName}
                onChange={e => setNewGroupName(e.target.value)}
                placeholder="예: 외부감사자"
                className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                onKeyDown={e => e.key === "Enter" && handleAddGroup()}
              />
              <p className="text-xs text-slate-400 mt-1.5">생성 후 편집 모드로 진입하여 권한을 설정하세요.</p>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => { setShowAddModal(false); setNewGroupName(""); }} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleAddGroup} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700">추가</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}