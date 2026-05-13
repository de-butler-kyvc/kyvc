"use client";
import { useMemo, useState, useEffect } from "react";
import {
  createAdminRole,
  getAllAdminRoles,
  updateAdminRole,
  type AdminRole,
} from "@/lib/api/managers";

interface GroupItem {
  id: string;
  name: string;
  members: number;
  permissions: string[];
}

function roleToGroupItem(role: AdminRole): GroupItem {
  return {
    id: String(role.roleId),
    name: role.roleName,
    members: role.memberCount ?? 0,
    permissions: role.permissions ?? [],
  };
}

export default function ManagerGroupsPage() {
  const [groupList, setGroupList] = useState<GroupItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [selected, setSelected] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [perms, setPerms] = useState<string[]>([]);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");

  useEffect(() => {
    getAllAdminRoles()
      .then((roles) => {
        const items = roles.map(roleToGroupItem);
        setGroupList(items);
        if (items.length > 0) {
          setSelected(items[0].id);
          setPerms(items[0].permissions);
        }
      })
      .catch((err) => {
        setLoadError(err instanceof Error ? err.message : "데이터를 불러오지 못했습니다.");
      })
      .finally(() => setLoading(false));
  }, []);

  const group = groupList.find((g) => g.id === selected);
  const allPermissions = useMemo(
    () => Array.from(new Set(groupList.flatMap((g) => g.permissions))).sort(),
    [groupList]
  );

  const handleSelect = (id: string) => {
    setSelected(id);
    setEditing(false);
    const found = groupList.find((g) => g.id === id);
    setPerms(found?.permissions ?? []);
  };

  const handleAddGroup = async () => {
    if (!newGroupName.trim()) {
      alert("그룹명을 입력해주세요.");
      return;
    }
    setSaving(true);
    setActionError(null);
    try {
      const created = await createAdminRole({
        roleName: newGroupName.trim(),
        permissions: [],
      });
      const newGroup = roleToGroupItem(created);
      setGroupList((prev) => [...prev, newGroup]);
      setSelected(newGroup.id);
      setPerms(newGroup.permissions);
      setEditing(true);
      setNewGroupName("");
      setShowAddModal(false);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "권한 그룹 생성에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleSavePermissions = async () => {
    if (!group) return;
    setSaving(true);
    setActionError(null);
    try {
      const updated = await updateAdminRole(group.id, {
        roleName: group.name,
        permissions: perms,
      });
      const next = roleToGroupItem(updated);
      setGroupList((prev) => prev.map((item) => (item.id === group.id ? next : item)));
      setPerms(next.permissions);
      setSaved(true);
      setEditing(false);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "권한 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · 관리자</p>
          <h1 className="text-xl font-bold text-slate-800">권한 그룹 관리</h1>
        </div>
      </div>

      {loadError && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
          {loadError}
        </div>
      )}
      {actionError && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
          {actionError}
        </div>
      )}

      <div className="flex gap-4">
        {/* 좌측 그룹 목록 */}
        <div className="w-48 shrink-0 bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-500">권한 그룹</p>
            <button onClick={() => setShowAddModal(true)} className="text-xs text-blue-600 hover:underline">+ 추가</button>
          </div>
          {loading ? (
            <p className="px-4 py-6 text-xs text-slate-400 text-center">불러오는 중...</p>
          ) : groupList.length === 0 ? (
            <p className="px-4 py-6 text-xs text-slate-400 text-center">역할 없음</p>
          ) : (
            groupList.map((g) => (
              <button
                key={g.id}
                onClick={() => handleSelect(g.id)}
                className={`w-full text-left px-4 py-3 border-b border-slate-50 transition-colors ${selected === g.id ? "bg-blue-50" : "hover:bg-slate-50"}`}
              >
                <p className={`text-sm font-medium ${selected === g.id ? "text-blue-600" : "text-slate-700"}`}>{g.name}</p>
                <p className="text-xs text-slate-400 mt-0.5">멤버 {g.members}명</p>
              </button>
            ))
          )}
        </div>

        {/* 우측 권한 목록 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          {!group ? (
            <div className="flex items-center justify-center h-40 text-sm text-slate-400">
              {loading ? "불러오는 중..." : "그룹을 선택해주세요."}
            </div>
          ) : (
            <>
              <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
                <div>
                  <h2 className="text-sm font-semibold text-slate-700">{group.name} · 권한 목록</h2>
                  <p className="text-xs text-slate-400 mt-0.5">멤버 {group.members}명</p>
                </div>
                <button
                  onClick={() => setEditing(!editing)}
                  className={`px-4 py-1.5 rounded text-sm ${editing ? "border border-slate-200 text-slate-600" : "bg-blue-600 text-white hover:bg-blue-700"}`}
                >
                  {editing ? "취소" : "편집"}
                </button>
              </div>
              <div className="p-5">
                <div className="grid grid-cols-2 gap-2">
                  {allPermissions.length === 0 && (
                    <div className="col-span-2 rounded border border-slate-100 bg-slate-50 px-4 py-6 text-center text-sm text-slate-400">
                      역할별 권한 목록 API 응답이 없습니다.
                    </div>
                  )}
                  {allPermissions.map((perm) => {
                    const has = perms.includes(perm);
                    return (
                      <label
                        key={perm}
                        className={`flex items-center gap-3 px-3 py-2.5 rounded-lg border transition-colors ${has ? "border-blue-200 bg-blue-50" : "border-slate-100 bg-slate-50"} ${editing ? "cursor-pointer" : "cursor-default"}`}
                      >
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
                    <button
                      onClick={handleSavePermissions}
                      disabled={saving}
                      className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60"
                    >
                      {saving ? "저장 중..." : saved ? "저장됨 ✓" : "저장"}
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
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
                onChange={(e) => setNewGroupName(e.target.value)}
                placeholder="예: 외부감사자"
                className="w-full border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                onKeyDown={(e) => e.key === "Enter" && handleAddGroup()}
              />
              <p className="text-xs text-slate-400 mt-1.5">생성 후 편집 모드로 진입하여 권한을 설정하세요.</p>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => { setShowAddModal(false); setNewGroupName(""); }} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleAddGroup} disabled={saving} className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60">
                {saving ? "추가 중..." : "추가"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
