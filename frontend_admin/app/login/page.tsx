"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function LoginPage() {
  const router = useRouter();
  const [id, setId] = useState("");
  const [pw, setPw] = useState("");
  const [keepLogin, setKeepLogin] = useState(false);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(false);
  const [ssoLoading, setSsoLoading] = useState(false);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !pw || pw.length < 4) { setError(true); return; }
    setError(false);
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      // 더미 토큰 설정 (실제로는 백엔드에서 받아와야 함)
      document.cookie = "auth_token=dummy_token; path=/; max-age=86400"; // 24시간
      router.push("/dashboard");
    }, 700);
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">

        {/* 타이틀 */}
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-11 h-11 rounded-xl bg-blue-600 text-white font-bold text-lg mb-3">K</div>
          <h1 className="text-xl font-bold text-slate-800">KYvC 백엔드 어드민</h1>
          <p className="text-sm text-slate-400 mt-1">관리자 전용 · 직원 로그인</p>
        </div>

        {/* 카드 */}
        <div className="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
          <form onSubmit={handleLogin} className="space-y-4">

            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-600">직원 ID</label>
              <input
                type="text"
                value={id}
                onChange={(e) => { setId(e.target.value); setError(false); }}
                placeholder="admin@kyvc.io"
                autoComplete="username"
                className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${error ? "border-red-300" : "border-slate-200"}`}
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-600">비밀번호</label>
              <input
                type="password"
                value={pw}
                onChange={(e) => { setPw(e.target.value); setError(false); }}
                placeholder="••••••••"
                autoComplete="current-password"
                className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${error ? "border-red-300" : "border-slate-200"}`}
              />
              {error && (
                <p className="text-xs text-red-500 flex items-center gap-1">
                  <span>✕</span> 아이디 또는 비밀번호를 확인해주세요.
                </p>
              )}
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 cursor-pointer select-none text-sm text-slate-500">
                <input
                  type="checkbox"
                  checked={keepLogin}
                  onChange={(e) => setKeepLogin(e.target.checked)}
                  className="accent-blue-600"
                />
                로그인 상태 유지
              </label>
              <Link href="/login/reset" className="text-sm text-blue-600 hover:underline">
                비밀번호 찾기
              </Link>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 text-white py-2.5 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>

            <div className="flex items-center gap-3">
              <div className="flex-1 h-px bg-slate-200" />
              <span className="text-xs text-slate-400">또는</span>
              <div className="flex-1 h-px bg-slate-200" />
            </div>

            <button
              type="button"
              disabled={ssoLoading}
              onClick={() => {
                setSsoLoading(true);
                setTimeout(() => {
                  document.cookie = "auth_token=sso_token; path=/; max-age=86400";
                  router.push("/dashboard");
                }, 900);
              }}
              className="w-full border border-slate-200 text-slate-600 py-2.5 rounded text-sm hover:bg-slate-50 disabled:opacity-60 transition-colors"
            >
              {ssoLoading ? "SSO 인증 중..." : "🏢 SSO 로그인 (내부 인증 시스템)"}
            </button>

          </form>
        </div>

        <p className="text-center text-xs text-slate-400">© 2025 KYvC. All rights reserved.</p>
      </div>
    </div>
  );
}