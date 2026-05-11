"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { persistAuthToken } from "@/lib/auth-session";
import { requestMfaChallenge, verifyMfa } from "@/lib/api/auth";

function secondsUntil(expiresAt?: string) {
  if (!expiresAt) return null;
  const time = new Date(expiresAt).getTime();
  if (Number.isNaN(time)) return null;
  return Math.max(0, Math.floor((time - Date.now()) / 1000));
}

export default function MfaPage() {
  const router = useRouter();
  const [digits, setDigits] = useState(["", "", "", "", "", ""]);
  const [seconds, setSeconds] = useState(180);
  const [challengeId, setChallengeId] = useState("");
  const [loadingChallenge, setLoadingChallenge] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const refs = useRef<(HTMLInputElement | null)[]>([]);

  const startChallenge = async () => {
    setLoadingChallenge(true);
    setError(null);
    try {
      const challenge = await requestMfaChallenge({ channel: "EMAIL", purpose: "ADMIN_LOGIN" });
      const id = challenge.challengeId ?? challenge.id;
      if (id == null) throw new Error("MFA challengeId가 응답에 없습니다.");
      setChallengeId(String(id));
      setSeconds(challenge.expiresIn ?? secondsUntil(challenge.expiresAt) ?? 180);
      setDigits(["", "", "", "", "", ""]);
      refs.current[0]?.focus();
    } catch (err) {
      setChallengeId("");
      setError(err instanceof Error ? err.message : "인증번호 발송에 실패했습니다.");
    } finally {
      setLoadingChallenge(false);
    }
  };

  useEffect(() => {
    startChallenge();
  }, []);

  useEffect(() => {
    if (seconds <= 0) return;
    const id = setInterval(() => setSeconds((s) => Math.max(0, s - 1)), 1000);
    return () => clearInterval(id);
  }, [seconds]);

  const mm = String(Math.floor(seconds / 60)).padStart(1, "0");
  const ss = String(seconds % 60).padStart(2, "0");

  const onChange = (i: number, v: string) => {
    if (!/^\d?$/.test(v)) return;
    const next = [...digits];
    next[i] = v;
    setDigits(next);
    setError(null);
    if (v && i < 5) refs.current[i + 1]?.focus();
  };

  const onKey = (i: number, e: React.KeyboardEvent) => {
    if (e.key === "Backspace" && !digits[i] && i > 0) refs.current[i - 1]?.focus();
  };

  const allFilled = digits.every((d) => d !== "");

  const onVerify = async () => {
    if (!allFilled || !challengeId) return;
    setVerifying(true);
    setError(null);
    try {
      const result = await verifyMfa({
        challengeId,
        verificationCode: digits.join(""),
      });
      const token = result.mfaToken ?? result.token;
      if (token) persistAuthToken(token, { maxAgeSec: result.expiresIn });
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "인증번호 확인에 실패했습니다.");
    } finally {
      setVerifying(false);
    }
  };

  const onResend = () => {
    startChallenge();
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <img src="/kyvc-wordmark-light%201.png" alt="KYVC" className="h-10 mx-auto mb-3" />
          <h1 className="text-xl font-bold text-slate-800">본인인증 / 추가 인증</h1>
          <p className="text-sm text-slate-400 mt-1">보안을 위해 추가 인증이 필요합니다.</p>
        </div>

        <div className="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
          <div className="flex items-center gap-3 bg-slate-50 border border-slate-200 rounded-lg px-4 py-3">
            <div className="w-9 h-9 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center text-sm">OTP</div>
            <div>
              <p className="text-sm font-medium text-slate-700">이메일 인증</p>
              <p className="text-xs text-slate-400">
                {loadingChallenge ? "인증번호 발송 중" : "발송된 인증번호를 입력하세요."}
              </p>
            </div>
          </div>

          <div className="bg-slate-50 border border-slate-200 rounded-lg px-4 py-4 space-y-3">
            <p className="text-xs text-slate-500 text-center">
              인증번호 6자리를 입력하세요. 유효시간 <span className="font-semibold text-blue-600">{mm}:{ss}</span>
            </p>
            <div className="flex gap-2 justify-center">
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => { refs.current[i] = el; }}
                  value={d}
                  inputMode="numeric"
                  maxLength={1}
                  disabled={loadingChallenge || !challengeId}
                  onChange={(e) => onChange(i, e.target.value)}
                  onKeyDown={(e) => onKey(i, e)}
                  className={`w-11 h-12 border rounded-lg text-center text-lg font-semibold focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 ${d ? "border-blue-400 bg-blue-50" : "border-slate-200"}`}
                />
              ))}
            </div>
            {error && <p className="text-xs text-red-500 text-center">{error}</p>}
          </div>

          <button
            onClick={onVerify}
            disabled={!allFilled || verifying || loadingChallenge || !challengeId || seconds <= 0}
            className="w-full bg-blue-600 text-white py-2.5 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
          >
            {verifying ? "인증 중..." : "인증 확인"}
          </button>

          <button
            onClick={onResend}
            disabled={loadingChallenge}
            className="w-full border border-slate-200 text-slate-600 py-2.5 rounded text-sm hover:bg-slate-50 disabled:opacity-60 transition-colors"
          >
            {loadingChallenge ? "발송 중..." : "재전송"}
          </button>
        </div>

        <p className="text-center text-xs text-slate-400">© 2025 KYvC. All rights reserved.</p>
      </div>
    </div>
  );
}
