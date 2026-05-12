"use client";
import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";

export default function MfaPage() {
  const router = useRouter();
  const [digits, setDigits] = useState(["", "", "", "", "", ""]);
  const [seconds, setSeconds] = useState(180);
  const [verifying, setVerifying] = useState(false);
  const refs = useRef<(HTMLInputElement | null)[]>([]);

  const expired = seconds === 0;

  useEffect(() => {
    if (seconds <= 0) return;
    const id = setInterval(() => setSeconds((s) => Math.max(0, s - 1)), 1000);
    return () => clearInterval(id);
  }, [seconds]);

  const mm = String(Math.floor(seconds / 60)).padStart(1, "0");
  const ss = String(seconds % 60).padStart(2, "0");

  const onChange = (i: number, v: string) => {
    if (expired || !/^\d?$/.test(v)) return;
    const next = [...digits];
    next[i] = v;
    setDigits(next);
    if (v && i < 5) refs.current[i + 1]?.focus();
  };

  const onKey = (i: number, e: React.KeyboardEvent) => {
    if (e.key === "Backspace" && !digits[i] && i > 0) refs.current[i - 1]?.focus();
  };

  const allFilled = digits.every((d) => d !== "");

  const onVerify = () => {
    if (!allFilled || expired) return;
    setVerifying(true);
    setTimeout(() => { setVerifying(false); router.push("/dashboard"); }, 700);
  };

  const onResend = () => {
    setSeconds(180);
    setDigits(["", "", "", "", "", ""]);
    setTimeout(() => refs.current[0]?.focus(), 0);
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">

        <div className="text-center">
          <img src="/kyvc-wordmark-light%201.png" alt="KYVC" className="h-10 mx-auto mb-3" />
          <h1 className="text-xl font-bold text-slate-800">본인인증 / 추가인증</h1>
          <p className="text-sm text-slate-400 mt-1">보안을 위해 추가 인증이 필요합니다.</p>
        </div>

        <div className="bg-white border border-slate-200 rounded-lg p-6 space-y-4">

          <div className="flex items-center gap-3 bg-slate-50 border border-slate-200 rounded-lg px-4 py-3">
            <div className="w-9 h-9 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center text-sm">✉</div>
            <div>
              <p className="text-sm font-medium text-slate-700">이메일 인증</p>
              <p className="text-xs text-slate-400">admin@kyvc.io</p>
            </div>
          </div>

          <div className={`border rounded-lg px-4 py-4 space-y-3 ${expired ? "bg-red-50 border-red-200" : "bg-slate-50 border-slate-200"}`}>
            {expired ? (
              <p className="text-xs text-red-500 text-center font-medium">인증 시간이 만료되었습니다. 재전송해주세요.</p>
            ) : (
              <p className="text-xs text-slate-500 text-center">
                인증번호 6자리를 입력하세요 (유효시간 <span className="font-semibold text-blue-600">{mm}:{ss}</span>)
              </p>
            )}
            <div className="flex gap-2 justify-center">
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => { refs.current[i] = el; }}
                  value={d}
                  inputMode="numeric"
                  maxLength={1}
                  disabled={expired}
                  onChange={(e) => onChange(i, e.target.value)}
                  onKeyDown={(e) => onKey(i, e)}
                  className={`w-11 h-12 border rounded-lg text-center text-lg font-semibold focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500 disabled:bg-slate-100 disabled:text-slate-300 ${
                    expired ? "border-red-200" : d ? "border-blue-400 bg-blue-50" : "border-slate-200"
                  }`}
                />
              ))}
            </div>
          </div>

          <button
            onClick={onVerify}
            disabled={!allFilled || verifying || expired}
            className="w-full bg-blue-600 text-white py-2.5 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-60 transition-colors"
          >
            {verifying ? "인증 중..." : "인증 확인"}
          </button>

          <button
            onClick={onResend}
            className="w-full border border-slate-200 text-slate-600 py-2.5 rounded text-sm hover:bg-slate-50 transition-colors"
          >
            {expired ? "재전송 (만료됨)" : "재전송"}
          </button>

        </div>

        <p className="text-center text-xs text-slate-400">© 2025 KYvC. All rights reserved.</p>
      </div>
    </div>
  );
}
