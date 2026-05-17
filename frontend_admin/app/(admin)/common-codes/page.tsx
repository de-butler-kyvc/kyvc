"use client";
import { useState, useEffect, useCallback } from "react";
import {
  getCommonCodeGroups,
  getCommonCodeGroup,
  enableCommonCode,
  disableCommonCode,
  type CommonCodeGroup,
  type CommonCode,
} from "@/lib/api/common-codes";

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

export default function CommonCodesPage() {
  // ── 그룹 목록 ────────────────────────────────────────────────
  const [groups, setGroups] = useState<CommonCodeGroup[]>([]);
  const [groupsLoading, setGroupsLoading] = useState(true);
  const [groupsError, setGroupsError] = useState<string | null>(null);

  // ── 선택된 그룹의 코드 목록 ──────────────────────────────────
  const [selectedGroup, setSelectedGroup] = useState<CommonCodeGroup | null>(null);
  const [codes, setCodes] = useState<CommonCode[]>([]);
  const [codesLoading, setCodesLoading] = useState(false);
  const [codesError, setCodesError] = useState<string | null>(null);

  // ── 토글 처리 상태 ───────────────────────────────────────────
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);

  // ── 그룹 목록 초기 로드 ──────────────────────────────────────
  useEffect(() => {
    getCommonCodeGroups()
      .then((data) => {
        setGroups(data);
        if (data.length > 0) setSelectedGroup(data[0]);
      })
      .catch((err) => setGroupsError(err instanceof Error ? err.message : "그룹 목록을 불러오지 못했습니다."))
      .finally(() => setGroupsLoading(false));
  }, []);

  // ── 선택된 그룹의 코드 로드 ──────────────────────────────────
  const loadGroupCodes = useCallback((group: CommonCodeGroup) => {
    setCodesLoading(true);
    setCodesError(null);
    setToggleError(null);
    getCommonCodeGroup(group.codeGroupId)
      .then((detail) => setCodes(detail.codes ?? []))
      .catch((err) => setCodesError(err instanceof Error ? err.message : "코드 목록을 불러오지 못했습니다."))
      .finally(() => setCodesLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedGroup) return;
    loadGroupCodes(selectedGroup);
  }, [selectedGroup, loadGroupCodes]);

  // ── 활성화/비활성화 토글 ─────────────────────────────────────
  const handleToggle = async (code: CommonCode) => {
    setTogglingId(code.codeId);
    setToggleError(null);
    try {
      if (code.enabled) {
        await disableCommonCode(code.codeId);
      } else {
        await enableCommonCode(code.codeId);
      }
      setCodes((prev) =>
        prev.map((c) => (c.codeId === code.codeId ? { ...c, enabled: !c.enabled } : c))
      );
    } catch (err) {
      setToggleError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setTogglingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs text-slate-400">증명서 관리자</p>
        <h1 className="text-xl font-bold text-slate-800">공통코드 관리</h1>
      </div>

      {groupsError && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
          {groupsError}
        </div>
      )}

      <div className="flex gap-4">
        {/* ── 좌측 그룹 목록 ── */}
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200">
          <div className="px-4 py-3 border-b border-slate-100">
            <h2 className="text-xs font-semibold text-slate-500">코드 그룹</h2>
          </div>
          {groupsLoading ? (
            <p className="text-xs text-slate-400 px-4 py-6 text-center">불러오는 중...</p>
          ) : groups.length === 0 ? (
            <p className="text-xs text-slate-400 px-4 py-6 text-center">그룹 없음</p>
          ) : (
            <ul>
              {groups.map((group) => (
                <li key={group.codeGroupId}>
                  <button
                    onClick={() => setSelectedGroup(group)}
                    className={`w-full text-left px-4 py-3 text-sm transition-colors border-b border-slate-50 last:border-0 ${
                      selectedGroup?.codeGroupId === group.codeGroupId
                        ? "bg-blue-50 text-blue-700 font-medium"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    <p className="font-medium truncate">{group.groupName}</p>
                    <p className="text-xs text-slate-400 mt-0.5 truncate">
                      {group.codeGroupId}
                      {group.codeCount !== undefined ? ` · ${group.codeCount}개` : ""}
                    </p>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* ── 우측 코드 목록 ── */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          {/* 헤더 */}
          <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-slate-800">
                {selectedGroup?.groupName ?? "그룹을 선택하세요"}
              </h2>
              {selectedGroup?.description && (
                <p className="text-xs text-slate-400 mt-0.5">{selectedGroup.description}</p>
              )}
            </div>
            {selectedGroup && (
              <span className="text-xs text-slate-400 font-mono">{selectedGroup.codeGroupId}</span>
            )}
          </div>

          <div className="p-6">
            {toggleError && (
              <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
                {toggleError}
              </div>
            )}

            {!selectedGroup ? (
              <p className="text-sm text-slate-400 py-10 text-center">좌측에서 코드 그룹을 선택하세요.</p>
            ) : codesLoading ? (
              <p className="text-sm text-slate-400 py-10 text-center">불러오는 중...</p>
            ) : codesError ? (
              <p className="text-sm text-red-500 py-4">{codesError}</p>
            ) : codes.length === 0 ? (
              <p className="text-sm text-slate-400 py-10 text-center">등록된 코드가 없습니다.</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50">
                    <th className="text-left px-4 py-3 text-slate-500 font-medium">코드 ID</th>
                    <th className="text-left px-4 py-3 text-slate-500 font-medium">코드명</th>
                    <th className="text-left px-4 py-3 text-slate-500 font-medium">값</th>
                    <th className="text-left px-4 py-3 text-slate-500 font-medium">설명</th>
                    <th className="text-left px-4 py-3 text-slate-500 font-medium">수정일시</th>
                    <th className="text-center px-4 py-3 text-slate-500 font-medium">활성</th>
                  </tr>
                </thead>
                <tbody>
                  {codes.map((code) => (
                    <tr key={code.codeId} className="border-b border-slate-50 hover:bg-slate-50">
                      <td className="px-4 py-3 text-xs font-mono text-slate-500">{code.codeId}</td>
                      <td className="px-4 py-3 text-slate-800 font-medium">{code.codeName}</td>
                      <td className="px-4 py-3 text-xs font-mono text-blue-600">{code.codeValue}</td>
                      <td className="px-4 py-3 text-xs text-slate-400">{code.description ?? "-"}</td>
                      <td className="px-4 py-3 text-xs text-slate-400 whitespace-nowrap">
                        {fmtDt(code.updatedAt ?? code.createdAt)}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <button
                          onClick={() => handleToggle(code)}
                          disabled={togglingId === code.codeId}
                          aria-label={code.enabled ? "비활성화" : "활성화"}
                          className={`relative inline-flex h-5 w-9 shrink-0 items-center rounded-full transition-colors focus:outline-none disabled:opacity-50 ${
                            code.enabled ? "bg-blue-600" : "bg-slate-300"
                          }`}
                        >
                          <span
                            className={`inline-block h-3.5 w-3.5 rounded-full bg-white shadow transition-transform ${
                              code.enabled ? "translate-x-[18px]" : "translate-x-[2px]"
                            }`}
                          />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
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
