"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";

export default function ResetPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState<string | null>(null);
  const [emailSent, setEmailSent] = useState(false);
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");
  const [done, setDone] = useState(false);

  const matches = pw && pw2 && pw === pw2;
  const mismatch = pw2 && pw !== pw2;
  const strong = pw.length >= 8 && /[A-Z]/.test(pw) && /[0-9]/.test(pw);

  const onSendEmail = () => {
    if (!email.trim()) { setEmailError("이메일을 입력해주세요."); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) { setEmailError("올바른 이메일 형식이 아닙니다."); return; }
    setEmailError(null);
    setEmailSent(true);
  };

  const onReset = () => {
    if (!matches || !strong) return;
    setDone(true);
    setTimeout(() => router.push("/login"), 2000);
  };

  if (done) {
    return (
      <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center p-4">
        <div className="w-full max-w-sm space-y-6">
          <div className="text-center">
            <img src="/kyvc-wordmark-light%201.png" alt="KYVC" className="h-10 mx-auto mb-3" />
          </div>
          <div className="bg-white border border-slate-200 rounded-lg p-6 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xl mx-auto">✓</div>
            <p className="text-sm font-medium text-slate-700">비밀번호가 재설정되었습니다.</p>
            <p className="text-xs text-slate-400">잠시 후 로그인 페이지로 이동합니다...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">

        <div className="text-center">
          <img src="/kyvc-wordmark-light%201.png" alt="KYVC" className="h-10 mx-auto mb-3" />
          <h1 className="text-xl font-bold text-slate-800">비밀번호 재설정</h1>
          <p className="text-sm text-slate-400 mt-1">가입 시 등록한 이메일로 재설정 링크를 보내드립니다.</p>
        </div>

        <div className="bg-white border border-slate-200 rounded-lg p-6 space-y-4">

          {/* Step 1: 이메일 입력 및 발송 */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-600">가입 이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setEmailError(null); }}
              placeholder="admin@kyvc.io"
              disabled={emailSent}
              className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-slate-50 disabled:text-slate-400 ${emailError ? "border-red-300" : "border-slate-200"}`}
            />
            {emailError && (
              <p className="text-xs text-red-500 flex items-center gap-1"><span>✕</span> {emailError}</p>
            )}
          </div>

          <button
            onClick={onSendEmail}
            disabled={emailSent}
            className={`w-full py-2.5 rounded text-sm font-medium transition-colors ${
              emailSent
                ? "border border-slate-200 text-slate-500 bg-slate-50"
                : "bg-blue-600 text-white hover:bg-blue-700"
            }`}
          >
            {emailSent ? "✓ 발송 완료 — 이메일을 확인하세요" : "이메일로 찾기"}
          </button>

          {/* Step 2: 새 비밀번호 (이메일 발송 후 표시) */}
          {emailSent && (
            <>
              <div className="h-px bg-slate-100" />

              <div className="space-y-1.5">
                <label className="text-sm font-medium text-slate-600">새 비밀번호</label>
                <input
                  type="password"
                  value={pw}
                  onChange={(e) => setPw(e.target.value)}
                  placeholder="••••••••"
                  className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
                <p className="text-xs text-slate-400">영문 대소문자, 숫자, 특수문자를 포함해 8자 이상</p>
              </div>

              <div className="space-y-1.5">
                <label className="text-sm font-medium text-slate-600">새 비밀번호 확인</label>
                <input
                  type="password"
                  value={pw2}
                  onChange={(e) => setPw2(e.target.value)}
                  placeholder="••••••••"
                  className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${mismatch ? "border-red-300" : "border-slate-200"}`}
                />
                {matches && <p className="text-xs text-green-600 flex items-center gap-1"><span>✓</span> 비밀번호가 일치합니다</p>}
                {mismatch && <p className="text-xs text-red-500 flex items-center gap-1"><span>✕</span> 비밀번호가 일치하지 않습니다</p>}
              </div>

              <button
                onClick={onReset}
                disabled={!matches || !strong}
                className="w-full bg-blue-600 text-white py-2.5 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
              >
                비밀번호 재설정
              </button>
            </>
          )}

          <button
            onClick={() => router.push("/login")}
            className="w-full border border-slate-200 text-slate-600 py-2.5 rounded text-sm hover:bg-slate-50 transition-colors"
          >
            로그인으로 돌아가기
          </button>

        </div>

        <p className="text-center text-xs text-slate-400">© 2025 KYvC. All rights reserved.</p>
      </div>
    </div>
  );
}
