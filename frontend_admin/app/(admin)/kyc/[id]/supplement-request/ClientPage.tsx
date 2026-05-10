"use client";
import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { createKycSupplement, getKycDetail } from "@/lib/api/kyc";

const docTypes = [
  { label: "사업자등록증", value: "BUSINESS_REGISTRATION" },
  { label: "등기사항전부증명서", value: "CORPORATE_REGISTRATION" },
  { label: "주주명부", value: "SHAREHOLDER_LIST" },
  { label: "위임장", value: "POWER_OF_ATTORNEY" },
  { label: "정관", value: "ARTICLES_OF_INCORPORATION" },
  { label: "대표자 신분확인", value: "REPRESENTATIVE_ID" },
  { label: "대리인 신분확인", value: "AGENT_ID" },
];

function defaultDeadline() {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  return date.toISOString().slice(0, 10);
}

function docTypeLabel(value: string) {
  return docTypes.find((type) => type.value === value)?.label ?? value;
}

function canRequestSupplement(status?: string) {
  return status === "MANUAL_REVIEW" || status === "NEED_MANUAL_REVIEW" || status === "NEEDS_MANUAL_REVIEW";
}

export default function SupplementRequestPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [items, setItems] = useState([{ docType: "SHAREHOLDER_LIST", reason: "" }]);
  const [deadline, setDeadline] = useState(defaultDeadline);
  const [note, setNote] = useState("");
  const [status, setStatus] = useState<string | undefined>();
  const [statusLoading, setStatusLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getKycDetail(id)
      .then((detail) => setStatus(detail.kycStatus ?? detail.status))
      .catch((err) => setError(err instanceof Error ? err.message : "KYC 신청 상태를 불러오지 못했습니다."))
      .finally(() => setStatusLoading(false));
  }, [id]);

  const addItem = () => setItems([...items, { docType: "BUSINESS_REGISTRATION", reason: "" }]);
  const removeItem = (i: number) => setItems(items.filter((_, idx) => idx !== i));
  const updateItem = (i: number, field: string, value: string) => {
    const next = [...items];
    next[i] = { ...next[i], [field]: value };
    setItems(next);
  };

  const handleSubmit = async () => {
    if (!canRequestSupplement(status)) {
      setError("수동심사필요 상태의 KYC 신청만 보완요청을 생성할 수 있습니다.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const message = items.map((i) => `[${docTypeLabel(i.docType)}] ${i.reason}`).join(" / ");
      await createKycSupplement(id, {
        supplementReason: note.trim() ? `${message} / ${note.trim()}` : message,
        requiredDocuments: items.map((i) => i.docType),
        dueDate: deadline,
      });
      router.push(`/kyc/${id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "보완요청 생성에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href={`/kyc/${id}`} className="hover:underline">KYC 신청 상세</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">보완요청 생성</h1>
        </div>
      </div>

      {!statusLoading && !canRequestSupplement(status) && (
        <div className="bg-orange-50 border border-orange-200 rounded-lg px-4 py-3 text-sm text-orange-700">
          현재 상태가 {status ?? "-"}인 KYC 신청은 보완요청을 생성할 수 없습니다.
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      <div className="flex gap-4">
        <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
          <h2 className="text-xs font-semibold text-slate-500">보완요청 생성</h2>
          {[
            { label: "신청번호", value: id },
          ].map((item) => (
            <div key={item.label}>
              <p className="text-xs text-slate-400">{item.label}</p>
              <p className="text-slate-700 text-xs font-medium mt-0.5">{item.value}</p>
            </div>
          ))}
          <div>
            <p className="text-xs text-slate-400">현재 상태</p>
            <span className="bg-red-100 text-red-600 text-xs px-2 py-0.5 rounded-full font-medium">수동심사필요</span>
          </div>
        </div>

        <div className="flex-1 space-y-4">
          <div className="bg-white rounded-lg border border-slate-200 p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-700">보완 요청 항목 <span className="text-red-500">*</span></h2>
              <button onClick={addItem} className="text-xs text-blue-600 border border-blue-200 px-3 py-1 rounded hover:bg-blue-50">+ 항목 추가</button>
            </div>
            <div className="space-y-3">
              {items.map((item, i) => (
                <div key={i} className="bg-slate-50 rounded-lg border border-slate-200 p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500">항목 {i + 1}</span>
                    {items.length > 1 && (
                      <button onClick={() => removeItem(i)} className="text-xs text-red-400 hover:text-red-600">삭제</button>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <label className="text-xs font-medium text-slate-500">서류 유형</label>
                      <select value={item.docType} onChange={(e) => updateItem(i, "docType", e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500">
                        {docTypes.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                      </select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium text-slate-500">보완 사유 <span className="text-red-500">*</span></label>
                      <input type="text" value={item.reason} onChange={(e) => updateItem(i, "reason", e.target.value)} placeholder="예: 해시 불일치, 발급일 초과 등" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white rounded-lg border border-slate-200 p-5 space-y-4">
            <h2 className="text-sm font-semibold text-slate-700">요청 설정</h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-slate-600">제출 기한 <span className="text-red-500">*</span></label>
                <input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-600">추가 안내사항 (선택)</label>
              <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={3} placeholder="사용자에게 추가로 안내할 내용을 입력하세요." className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none" />
            </div>
          </div>

          <div className="flex items-center justify-between">
            <p className="text-xs text-slate-400">처리자: 관리자</p>
            <div className="flex gap-2">
              <Link href={`/kyc/${id}`} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">취소</Link>
              <button
                onClick={handleSubmit}
                disabled={statusLoading || !canRequestSupplement(status) || loading || items.some((i) => !i.reason.trim())}
                className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
              >
                {loading ? "처리 중..." : "보완요청 발송"}
              </button>
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
