"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { login } from "@/lib/api/auth";
import { persistAuthToken, persistRefreshToken } from "@/lib/auth-session";

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [pw, setPw] = useState("");
  const [keepLogin, setKeepLogin] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !pw) {
      setError("이메일 또는 비밀번호를 입력해주세요.");
      return;
    }

    setError(null);
    setLoading(true);
    try {
      const result = await login({ email: email.trim(), password: pw });
      const maxAgeSec = keepLogin ? 30 * 24 * 3600 : 86400;
      persistAuthToken(result.accessToken, { maxAgeSec });
      persistRefreshToken(result.refreshToken);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleSkipLogin = () => {
    persistAuthToken("dummy_token", { maxAgeSec: 86400 });
    router.push("/dashboard");
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <img src="/kyvc-wordmark-light%201.png" alt="KYVC" className="h-10 mx-auto mb-3" />
          <p className="text-sm text-slate-400 mt-1">관리자 전용 · 직원 로그인</p>
        </div>

        <div className="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
          <form onSubmit={handleLogin} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-600">이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setError(null);
                }}
                placeholder="admin@kyvc.com"
                autoComplete="email"
                className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${error ? "border-red-300" : "border-slate-200"}`}
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-600">비밀번호</label>
              <input
                type="password"
                value={pw}
                onChange={(e) => {
                  setPw(e.target.value);
                  setError(null);
                }}
                placeholder="••••••••"
                autoComplete="current-password"
                className={`w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 ${error ? "border-red-300" : "border-slate-200"}`}
              />
              {error && (
                <p className="text-xs text-red-500 flex items-center gap-1">
                  <span>!</span> {error}
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

            <button
              type="button"
              onClick={handleSkipLogin}
              className="w-full border border-blue-200 bg-blue-50 text-blue-700 py-2.5 rounded text-sm font-medium hover:bg-blue-100 transition-colors"
            >
              로그인 건너뛰기
            </button>

            <div className="flex items-center gap-3">
              <div className="flex-1 h-px bg-slate-200" />
              <span className="text-xs text-slate-400">또는</span>
              <div className="flex-1 h-px bg-slate-200" />
            </div>

            <button
              type="button"
              className="w-full border border-slate-200 text-slate-600 py-2.5 rounded text-sm hover:bg-slate-50 disabled:opacity-60 transition-colors"
              disabled
            >
              SSO 로그인 (내부 인증 시스템)
            </button>
          </form>
        </div>

        <p className="text-center text-xs text-slate-400">© 2025 KYvC. All rights reserved.</p>
      </div>
    </div>
  );
}
