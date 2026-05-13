"use client";

import { useState } from "react";
import { requestMfaChallenge, verifyMfa } from "@/lib/api/auth";

interface MfaModalProps {
  purpose?: string;
  onConfirm: (mfaToken: string) => void;
  onClose: () => void;
}

const MFA_PURPOSE_MAP: Record<string, string> = {
  ADMIN_ACTION: "IMPORTANT_ACTION",
  KYC_MANUAL_REVIEW: "IMPORTANT_ACTION",
  CREDENTIAL_ISSUE: "VC_ISSUE",
};

function normalizeMfaPurpose(purpose?: string) {
  const value = purpose ?? "IMPORTANT_ACTION";
  return MFA_PURPOSE_MAP[value] ?? value;
}

export default function MfaModal({ purpose, onConfirm, onClose }: MfaModalProps) {
  const [code, setCode] = useState("");
  const [challengeId, setChallengeId] = useState<string | number | null>(null);
  const [step, setStep] = useState<"idle" | "sent" | "verifying">("idle");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSendOtp = async () => {
    setSending(true);
    setError(null);
    try {
      const result = await requestMfaChallenge({
        channel: "EMAIL",
        purpose: normalizeMfaPurpose(purpose),
      });
      const cid = result.challengeId ?? result.id ?? null;
      setChallengeId(cid);
      setStep("sent");
    } catch (e) {
      setError((e as Error).message ?? "OTP 발송에 실패했습니다.");
    } finally {
      setSending(false);
    }
  };

  const handleVerify = async () => {
    if (!code.trim()) { setError("인증 코드를 입력해주세요."); return; }
    if (!challengeId) { setError("먼저 OTP를 발송해주세요."); return; }
    setStep("verifying");
    setError(null);
    try {
      const result = await verifyMfa({
        challengeId: String(challengeId),
        verificationCode: code.trim(),
      });
      const mfaToken = result.mfaToken ?? result.token ?? "";
      if (!mfaToken) throw new Error("MFA 토큰을 받지 못했습니다.");
      onConfirm(mfaToken);
    } catch (e) {
      setError((e as Error).message ?? "인증에 실패했습니다.");
      setStep("sent");
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg border border-slate-200 w-full max-w-sm p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-slate-800">MFA 인증</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-lg leading-none">✕</button>
        </div>

        <p className="text-sm text-slate-500">
          민감한 작업을 수행하기 위해 추가 인증이 필요합니다.<br />
          등록된 이메일로 OTP를 발송합니다.
        </p>

        {error && (
          <p className="text-xs text-red-500 bg-red-50 border border-red-200 rounded px-3 py-2">{error}</p>
        )}

        <div className="space-y-3">
          <button
            type="button"
            onClick={handleSendOtp}
            disabled={sending || step === "verifying"}
            className="w-full border border-blue-300 text-blue-600 py-2 rounded text-sm hover:bg-blue-50 disabled:opacity-60 transition-colors"
          >
            {sending ? "발송 중..." : step === "sent" ? "OTP 재발송" : "OTP 발송"}
          </button>

          {step !== "idle" && (
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-slate-500">인증 코드</label>
              <input
                type="text"
                inputMode="numeric"
                maxLength={8}
                value={code}
                onChange={(e) => { setCode(e.target.value); setError(null); }}
                onKeyDown={(e) => { if (e.key === "Enter") handleVerify(); }}
                placeholder="123456"
                className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 tracking-widest text-center"
                autoFocus
              />
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button onClick={onClose} className="border border-slate-200 text-slate-600 px-4 py-1.5 rounded text-sm hover:bg-slate-50">
            취소
          </button>
          <button
            onClick={handleVerify}
            disabled={step !== "sent" || !code.trim()}
            className="bg-blue-600 text-white px-4 py-1.5 rounded text-sm hover:bg-blue-700 disabled:opacity-60 transition-colors"
          >
            {step === "verifying" ? "확인 중..." : "확인"}
          </button>
        </div>
      </div>
    </div>
  );
}
