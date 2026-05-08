"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, Suspense } from "react";

function IssuerNewForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const typeParam = searchParams.get("type");
  const isBlacklist = typeParam === "blacklist";

  const [form, setForm] = useState({
    did: "",
    type: isBlacklist ? "블랙리스트" : "화이트리스트",
    credential: "",
    scope: "플랫폼 전체",
    startDate: "",
    endDate: "",
  });
  const [errors, setErrors] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors: Record<string, boolean> = {};
    if (!form.did.trim()) newErrors.did = true;
    if (!form.credential.trim()) newErrors.credential = true;
    if (!form.startDate) newErrors.startDate = true;
    if (!form.endDate) newErrors.endDate = true;
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    setLoading(true);
    setTimeout(() => {
      router.push(isBlacklist ? "/issuer/blacklist" : "/issuer/whitelist");
    }, 600);
  };

  const backHref = isBlacklist ? "/issuer/blacklist" : "/issuer/whitelist";
  const title = isBlacklist ? "블랙리스트 등록" : "화이트리스트 등록";

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs text-slate-400">
          백엔드 어드민 · <Link href="/issuer" className="hover:underline">Issuer 신뢰정책</Link> · <Link href={backHref} className="hover:underline">{isBlacklist ? "블랙리스트" : "화이트리스트"}</Link>
        </p>
        <h1 className="text-xl font-bold text-slate-800">{title}</h1>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="bg-white rounded-lg border border-slate-200 divide-y divide-slate-100">

          {/* DID */}
          <div className="flex items-start px-6 py-4 gap-6">
            <label className="w-40 shrink-0 text-sm font-medium text-slate-600 pt-1.5">
              Issuer DID
            </label>
            <div className="flex-1">
              <input
                type="text"
                value={form.did}
                onChange={e => { setForm(f => ({ ...f, did: e.target.value })); setErrors(v => ({ ...v, did: false })); }}
                placeholder="did:kyvc:issuer:xxx"
                className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${errors.did ? "border-red-400 bg-red-50" : "border-slate-200"}`}
              />
              {errors.did && <p className="text-xs text-red-500 mt-1">DID를 입력해주세요.</p>}
            </div>
          </div>

          {/* 정책 유형 */}
          <div className="flex items-center px-6 py-4 gap-6">
            <label className="w-40 shrink-0 text-sm font-medium text-slate-600">정책 유형</label>
            <div className="flex gap-3">
              {["화이트리스트", "블랙리스트"].map(t => (
                <label key={t} className="flex items-center gap-2 cursor-pointer text-sm text-slate-600">
                  <input
                    type="radio"
                    name="type"
                    value={t}
                    checked={form.type === t}
                    onChange={() => setForm(f => ({ ...f, type: t }))}
                    className="accent-blue-600"
                  />
                  <span className={t === "블랙리스트" ? "text-red-600 font-medium" : "text-green-600 font-medium"}>{t}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Credential Type */}
          <div className="flex items-start px-6 py-4 gap-6">
            <label className="w-40 shrink-0 text-sm font-medium text-slate-600 pt-1.5">Credential Type</label>
            <div className="flex-1">
              <select
                value={form.credential}
                onChange={e => { setForm(f => ({ ...f, credential: e.target.value })); setErrors(v => ({ ...v, credential: false })); }}
                className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${errors.credential ? "border-red-400 bg-red-50" : "border-slate-200"}`}
              >
                <option value="">선택</option>
                <option>KYC VC</option>
                <option>위임권한 VC</option>
                <option>전체</option>
              </select>
              {errors.credential && <p className="text-xs text-red-500 mt-1">Credential Type을 선택해주세요.</p>}
            </div>
          </div>

          {/* 적용 범위 */}
          <div className="flex items-center px-6 py-4 gap-6">
            <label className="w-40 shrink-0 text-sm font-medium text-slate-600">적용 범위</label>
            <select
              value={form.scope}
              onChange={e => setForm(f => ({ ...f, scope: e.target.value }))}
              className="border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 w-64"
            >
              <option>플랫폼 전체</option>
              <option>파이낸셜 파트너스</option>
              <option>비즈파트너 포털</option>
            </select>
          </div>

          {/* 적용 기간 */}
          <div className="flex items-start px-6 py-4 gap-6">
            <label className="w-40 shrink-0 text-sm font-medium text-slate-600 pt-1.5">적용 기간</label>
            <div className="flex-1 space-y-1">
              <div className="flex items-center gap-2">
                <input
                  type="date"
                  value={form.startDate}
                  onChange={e => { setForm(f => ({ ...f, startDate: e.target.value })); setErrors(v => ({ ...v, startDate: false })); }}
                  className={`border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${errors.startDate ? "border-red-400 bg-red-50" : "border-slate-200"}`}
                />
                <span className="text-slate-400 text-sm">~</span>
                <input
                  type="date"
                  value={form.endDate}
                  onChange={e => { setForm(f => ({ ...f, endDate: e.target.value })); setErrors(v => ({ ...v, endDate: false })); }}
                  className={`border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${errors.endDate ? "border-red-400 bg-red-50" : "border-slate-200"}`}
                />
              </div>
              {(errors.startDate || errors.endDate) && <p className="text-xs text-red-500">시작일과 종료일을 모두 입력해주세요.</p>}
            </div>
          </div>

        </div>

        {/* 하단 버튼 */}
        <div className="flex justify-end gap-2 mt-4">
          <Link href={backHref} className="border border-slate-200 text-slate-600 px-5 py-2 rounded text-sm hover:bg-slate-50">
            취소
          </Link>
          <button
            type="submit"
            disabled={loading}
            className="bg-blue-600 text-white px-5 py-2 rounded text-sm hover:bg-blue-700 disabled:opacity-60 transition-colors"
          >
            {loading ? "등록 중..." : "등록"}
          </button>
        </div>
      </form>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}

export default function IssuerNewPage() {
  return (
    <Suspense>
      <IssuerNewForm />
    </Suspense>
  );
}
