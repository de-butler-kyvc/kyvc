"use client";
import { useState, useEffect } from "react";
import {
  getNotificationTemplates, updateNotificationTemplate,
  type NotificationTemplate,
} from "@/lib/api/notification-templates";

export default function NotificationsPage() {
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [editSubject, setEditSubject] = useState("");
  const [editBody, setEditBody] = useState("");
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getNotificationTemplates()
      .then(setTemplates)
      .catch((err) => setError(err instanceof Error ? err.message : "불러오기 실패"))
      .finally(() => setLoading(false));
  }, []);

  const selectedTpl = templates.find((t) => t.templateId === selected);

  const handleSelect = (tpl: NotificationTemplate) => {
    setSelected(tpl.templateId);
    setEditSubject(tpl.subject ?? "");
    setEditBody(tpl.body ?? "안녕하세요, {{법인명}} 담당자님.\n\n{{내용}}\n\nKYvC 드림.");
    setSaved(false);
  };

  const handleSave = async () => {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateNotificationTemplate(selected, { subject: editSubject, body: editBody });
      setTemplates((prev) => prev.map((t) => t.templateId === selected ? { ...t, ...updated } : t));
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleToggleEnabled = async (tpl: NotificationTemplate) => {
    try {
      await updateNotificationTemplate(tpl.templateId, { enabled: !tpl.enabled });
      setTemplates((prev) => prev.map((t) => t.templateId === tpl.templateId ? { ...t, enabled: !t.enabled } : t));
    } catch {
      // ignore
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · 설정</p>
          <h1 className="text-xl font-bold text-slate-800">알림 템플릿 관리</h1>
        </div>
      </div>

      {error && <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="flex gap-4">
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">알림 템플릿 목록</h2>
          </div>
          {loading ? (
            <div className="p-8 text-center text-slate-400 text-sm">로딩 중...</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">ID</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">이벤트</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">채널</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">제목</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">활성</th>
                  <th className="text-left px-4 py-3 text-slate-500 font-medium">편집</th>
                </tr>
              </thead>
              <tbody>
                {templates.length === 0 ? (
                  <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400 text-sm">데이터가 없습니다.</td></tr>
                ) : templates.map((tpl) => (
                  <tr key={tpl.templateId} className={`border-b border-slate-50 hover:bg-slate-50 cursor-pointer ${selected === tpl.templateId ? "bg-blue-50" : ""}`} onClick={() => handleSelect(tpl)}>
                    <td className="px-4 py-3 text-slate-400 text-xs">{tpl.templateId}</td>
                    <td className="px-4 py-3 text-slate-700 font-medium">{tpl.eventType ?? tpl.templateName}</td>
                    <td className="px-4 py-3"><span className="bg-blue-100 text-blue-600 text-xs px-2 py-0.5 rounded">{tpl.channel ?? "이메일"}</span></td>
                    <td className="px-4 py-3 text-slate-500 text-xs">{tpl.subject}</td>
                    <td className="px-4 py-3">
                      <button onClick={(e) => { e.stopPropagation(); handleToggleEnabled(tpl); }} className={`px-2 py-0.5 rounded-full text-xs font-medium transition-colors ${tpl.enabled ? "bg-green-100 text-green-600" : "bg-slate-100 text-slate-400"}`}>
                        {tpl.enabled ? "활성" : "비활성"}
                      </button>
                    </td>
                    <td className="px-4 py-3"><button onClick={(e) => { e.stopPropagation(); handleSelect(tpl); }} className="text-xs text-blue-600 hover:underline">편집</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {selected && selectedTpl && (
          <div className="w-80 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-4 h-fit">
            <h2 className="text-sm font-semibold text-slate-700">템플릿 편집</h2>
            <div><p className="text-xs text-slate-400 mb-1">이벤트</p><p className="text-sm text-slate-700 font-medium">{selectedTpl.eventType ?? selectedTpl.templateName}</p></div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">제목</label>
              <input type="text" value={editSubject} onChange={(e) => setEditSubject(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">본문</label>
              <textarea value={editBody} onChange={(e) => setEditBody(e.target.value)} rows={6} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none" />
              <p className="text-xs text-slate-400">변수: {`{{법인명}}, {{내용}}, {{링크}}`}</p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => setSelected(null)} className="flex-1 border border-slate-200 text-slate-600 py-2 rounded text-sm hover:bg-slate-50">취소</button>
              <button onClick={handleSave} disabled={saving} className="flex-1 bg-blue-600 text-white py-2 rounded text-sm hover:bg-blue-700 disabled:opacity-60">{saving ? "저장 중..." : saved ? "저장됨 ✓" : "저장"}</button>
            </div>
          </div>
        )}
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
