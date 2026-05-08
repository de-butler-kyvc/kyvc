"use client";
import { useState } from "react";

const corpTypes = ["주식회사", "유한회사", "합명/합자회사", "비영리법인", "조합", "외국기업"];

const rules: Record<string, { status: string; items: { label: string; value: string; required: boolean; placeholder: string }[] }> = {
  "주식회사": {
    status: "활성",
    items: [
      { label: "KYC 유효기간", value: "2년 (730일)", required: true, placeholder: "예: 2년 (730일)" },
      { label: "AI 신뢰도 기준", value: "80% 이상 → 자동승인", required: true, placeholder: "예: 80% 이상 → 자동승인" },
      { label: "실제소유자 기준", value: "25% 이상 지분 보유자", required: true, placeholder: "예: 25% 이상 지분 보유자" },
      { label: "수동심사 트리거", value: "신뢰도 60~80% 또는 불일치 감지", required: true, placeholder: "예: 신뢰도 60~80% 또는 불일치 감지" },
    ],
  },
  "유한회사": {
    status: "활성",
    items: [
      { label: "KYC 유효기간", value: "2년 (730일)", required: true, placeholder: "예: 2년 (730일)" },
      { label: "AI 신뢰도 기준", value: "75% 이상 → 자동승인", required: true, placeholder: "예: 75% 이상 → 자동승인" },
      { label: "실제소유자 기준", value: "25% 이상 지분 보유자", required: true, placeholder: "예: 25% 이상 지분 보유자" },
      { label: "수동심사 트리거", value: "신뢰도 60~75% 또는 불일치 감지", required: true, placeholder: "예: 신뢰도 60~75% 또는 불일치 감지" },
    ],
  },
  "합명/합자회사": {
    status: "활성",
    items: [
      { label: "KYC 유효기간", value: "1년 (365일)", required: true, placeholder: "예: 1년 (365일)" },
      { label: "AI 신뢰도 기준", value: "85% 이상 → 자동승인", required: true, placeholder: "예: 85% 이상 → 자동승인" },
      { label: "실제소유자 기준", value: "전체 사원 대상", required: true, placeholder: "예: 전체 사원 대상" },
      { label: "수동심사 트리거", value: "항상 수동심사", required: true, placeholder: "예: 항상 수동심사" },
    ],
  },
  "비영리법인": {
    status: "활성",
    items: [
      { label: "KYC 유효기간", value: "3년 (1095일)", required: true, placeholder: "예: 3년 (1095일)" },
      { label: "AI 신뢰도 기준", value: "80% 이상 → 자동승인", required: true, placeholder: "예: 80% 이상 → 자동승인" },
      { label: "실제소유자 기준", value: "이사회 구성원", required: true, placeholder: "예: 이사회 구성원" },
      { label: "수동심사 트리거", value: "신뢰도 70% 미만", required: true, placeholder: "예: 신뢰도 70% 미만" },
    ],
  },
  "조합": {
    status: "활성",
    items: [
      { label: "KYC 유효기간", value: "1년 (365일)", required: true, placeholder: "예: 1년 (365일)" },
      { label: "AI 신뢰도 기준", value: "80% 이상 → 자동승인", required: true, placeholder: "예: 80% 이상 → 자동승인" },
      { label: "실제소유자 기준", value: "조합장 및 임원", required: true, placeholder: "예: 조합장 및 임원" },
      { label: "수동심사 트리거", value: "신뢰도 60% 미만", required: true, placeholder: "예: 신뢰도 60% 미만" },
    ],
  },
  "외국기업": {
    status: "활성",
    items: [
      { label: "KYC 유효기간", value: "1년 (365일)", required: true, placeholder: "예: 1년 (365일)" },
      { label: "AI 신뢰도 기준", value: "90% 이상 → 자동승인", required: true, placeholder: "예: 90% 이상 → 자동승인" },
      { label: "실제소유자 기준", value: "25% 이상 지분 보유자", required: true, placeholder: "예: 25% 이상 지분 보유자" },
      { label: "수동심사 트리거", value: "항상 수동심사 포함", required: true, placeholder: "예: 항상 수동심사 포함" },
    ],
  },
};

const mockHistory: Record<string, { date: string; actor: string; field: string; before: string; after: string }[]> = {
  "주식회사": [
    { date: "2025.04.10 14:22", actor: "admin_park", field: "AI 신뢰도 기준", before: "75% 이상 → 자동승인", after: "80% 이상 → 자동승인" },
    { date: "2025.02.01 09:05", actor: "admin_ops", field: "KYC 유효기간", before: "1년 (365일)", after: "2년 (730일)" },
  ],
  "유한회사": [
    { date: "2025.03.15 11:00", actor: "admin_park", field: "AI 신뢰도 기준", before: "70% 이상 → 자동승인", after: "75% 이상 → 자동승인" },
  ],
  "합명/합자회사": [],
  "비영리법인": [
    { date: "2025.01.20 16:30", actor: "admin_ops", field: "KYC 유효기간", before: "2년 (730일)", after: "3년 (1095일)" },
  ],
  "조합": [],
  "외국기업": [
    { date: "2025.04.22 10:15", actor: "admin_park", field: "AI 신뢰도 기준", before: "85% 이상 → 자동승인", after: "90% 이상 → 자동승인" },
  ],
};

function getInitialValues(type: string): Record<string, string> {
  return Object.fromEntries(rules[type].items.map(item => [item.label, item.value]));
}

export default function PolicyPage() {
  const [selected, setSelected] = useState("주식회사");
  const [editValues, setEditValues] = useState<Record<string, Record<string, string>>>(
    Object.fromEntries(corpTypes.map(t => [t, getInitialValues(t)]))
  );
  const [saved, setSaved] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [errors, setErrors] = useState<Record<string, boolean>>({});

  const rule = rules[selected];
  const history = mockHistory[selected] ?? [];
  const currentValues = editValues[selected];

  const handleChange = (label: string, value: string) => {
    setEditValues(prev => ({
      ...prev,
      [selected]: { ...prev[selected], [label]: value },
    }));
    if (value.trim()) {
      setErrors(prev => ({ ...prev, [label]: false }));
    }
  };

  const handleSave = () => {
    const newErrors: Record<string, boolean> = {};
    let hasError = false;
    rule.items.forEach(item => {
      if (item.required && !currentValues[item.label]?.trim()) {
        newErrors[item.label] = true;
        hasError = true;
      }
    });
    if (hasError) {
      setErrors(newErrors);
      return;
    }
    setErrors({});
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const handleSelectType = (type: string) => {
    setSelected(type);
    setSaved(false);
    setErrors({});
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">KYC 규칙 관리</h1>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200 flex">
        {/* 좌측 법인 유형 선택 */}
        <div className="w-48 border-r border-slate-200 p-4 shrink-0">
          <p className="text-xs text-slate-400 mb-3 font-medium">KYC 규칙 관리</p>
          <div className="space-y-1">
            {corpTypes.map((type) => (
              <button
                key={type}
                onClick={() => handleSelectType(type)}
                className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${
                  type === selected
                    ? "bg-blue-50 text-blue-600 font-medium border border-blue-200"
                    : "text-slate-600 hover:bg-slate-50"
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>

        {/* 우측 규칙 편집 */}
        <div className="flex-1 p-6">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <h2 className="text-base font-semibold text-slate-800">{selected} KYC 규칙</h2>
              <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">{rule.status}</span>
            </div>
            <div className="flex gap-2">
              <button onClick={() => setShowHistory(true)} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">변경 이력</button>
              <button onClick={handleSave} className={`px-4 py-1.5 rounded text-sm transition-colors ${saved ? "bg-green-600 text-white" : "bg-blue-600 text-white hover:bg-blue-700"}`}>
                {saved ? "저장됨 ✓" : "규칙 저장"}
              </button>
            </div>
          </div>

          <div className="border border-slate-200 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <tbody>
                {rule.items.map((item) => (
                  <tr key={item.label} className="border-b border-slate-100 last:border-0">
                    <td className="px-5 py-4 bg-slate-50 w-52 align-top pt-[18px]">
                      <span className="text-slate-600 font-medium">{item.label}</span>
                    </td>
                    <td className="px-5 py-3">
                      <input
                        type="text"
                        value={currentValues[item.label] ?? ""}
                        onChange={e => handleChange(item.label, e.target.value)}
                        placeholder={item.placeholder}
                        className={`w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 ${
                          errors[item.label]
                            ? "border-red-400 focus:ring-red-400 bg-red-50"
                            : "border-slate-200 focus:ring-blue-500"
                        }`}
                      />
                      {errors[item.label] && (
                        <p className="text-xs text-red-500 mt-1">필수 항목입니다.</p>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>

      {showHistory && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg border border-slate-200 w-full max-w-2xl p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-slate-800">{selected} · 규칙 변경 이력</h2>
              <button onClick={() => setShowHistory(false)} className="text-slate-400 hover:text-slate-600 text-lg leading-none">✕</button>
            </div>
            {history.length === 0 ? (
              <p className="text-sm text-slate-400 py-6 text-center">변경 이력이 없습니다.</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50">
                    <th className="text-left px-4 py-2.5 text-slate-500 font-medium">일시</th>
                    <th className="text-left px-4 py-2.5 text-slate-500 font-medium">처리자</th>
                    <th className="text-left px-4 py-2.5 text-slate-500 font-medium">항목</th>
                    <th className="text-left px-4 py-2.5 text-slate-500 font-medium">변경 전</th>
                    <th className="text-left px-4 py-2.5 text-slate-500 font-medium">변경 후</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h, i) => (
                    <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                      <td className="px-4 py-2.5 text-slate-400 text-xs">{h.date}</td>
                      <td className="px-4 py-2.5 text-slate-600">{h.actor}</td>
                      <td className="px-4 py-2.5 text-slate-700 font-medium">{h.field}</td>
                      <td className="px-4 py-2.5 text-red-400 text-xs">{h.before}</td>
                      <td className="px-4 py-2.5 text-blue-600 text-xs">{h.after}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <div className="flex justify-end pt-2">
              <button onClick={() => setShowHistory(false)} className="bg-slate-100 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-200">닫기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
