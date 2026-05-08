"use client";
import { useState } from "react";

const verifiers = ["파이낸셜 파트너스", "비즈파트너 포털", "마켓플레이스 A"];

const policyByVerifier: Record<string, { credential: string; maxAge: string; xrpl: boolean; reuse: boolean; purpose: string }> = {
  "파이낸셜 파트너스": { credential: "KYC VC, 위임권한 VC", maxAge: "365일", xrpl: true, reuse: false, purpose: "기업금융 KYC 확인" },
  "비즈파트너 포털": { credential: "KYC VC", maxAge: "180일", xrpl: false, reuse: true, purpose: "법인 서비스 가입 확인" },
  "마켓플레이스 A": { credential: "KYC VC", maxAge: "90일", xrpl: false, reuse: true, purpose: "마켓플레이스 입점 심사" },
};

export default function VerifierTrustPolicyPage() {
  const [selected, setSelected] = useState("파이낸셜 파트너스");
  const [editing, setEditing] = useState(false);
  const policy = policyByVerifier[selected];
  const [saved, setSaved] = useState(false);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · Verifier</p>
          <h1 className="text-xl font-bold text-slate-800">Verifier별 신뢰정책 설정</h1>
        </div>
      </div>

      <div className="flex gap-4">
        {/* 좌측 Verifier 선택 */}
        <div className="w-48 shrink-0 bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100 bg-slate-50">
            <p className="text-xs font-semibold text-slate-500">Verifier 선택</p>
          </div>
          {verifiers.map((v) => (
            <button
              key={v}
              onClick={() => { setSelected(v); setEditing(false); }}
              className={`w-full text-left px-4 py-2.5 text-sm border-b border-slate-50 transition-colors ${selected === v ? "bg-blue-50 text-blue-600 font-medium" : "text-slate-600 hover:bg-slate-50"}`}
            >
              {v}
            </button>
          ))}
        </div>

        {/* 우측 정책 */}
        <div className="flex-1 bg-white rounded-lg border border-slate-200">
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">{selected} · 신뢰정책</h2>
            <button onClick={() => setEditing(!editing)} className={`px-4 py-1.5 rounded text-sm ${editing ? "border border-slate-200 text-slate-600" : "bg-blue-600 text-white hover:bg-blue-700"}`}>
              {editing ? "취소" : "편집"}
            </button>
          </div>
          <div className="p-5 space-y-4">
            {[
              { label: "허용 Credential Type", value: policy.credential },
              { label: "VP 최대 유효기간", value: policy.maxAge },
              { label: "XRPL 상태 검증", value: policy.xrpl ? "필수" : "선택" },
              { label: "VP 재사용 허용", value: policy.reuse ? "허용" : "불허" },
              { label: "제출 목적", value: policy.purpose },
            ].map((item) => (
              <div key={item.label} className="flex items-center gap-4 border-b border-slate-50 pb-3 last:border-0 last:pb-0">
                <span className="text-sm text-slate-500 w-40 shrink-0">{item.label}</span>
                {editing ? (
                  <input defaultValue={item.value} className="flex-1 border border-slate-200 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500" />
                ) : (
                  <span className="text-sm text-slate-700 font-medium">{item.value}</span>
                )}
              </div>
            ))}
            {editing && (
              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
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
    </div>
  );
}