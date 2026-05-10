"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { ApiError, auth } from "@/lib/api";
import { readSignupDraft, writeSignupDraft } from "@/lib/signup-flow";

const OTP_LEN = 6;

export default function MobileSignupVerifyPage() {
  const router = useRouter();
  const [bootstrapping, setBootstrapping] = useState(true);
  const [email, setEmail] = useState("");
  const [maskedTarget, setMaskedTarget] = useState("");
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [digits, setDigits] = useState<string[]>(() => Array(OTP_LEN).fill(""));
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const cells = useRef<Array<HTMLInputElement | null>>([]);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    (async () => {
      const draft = readSignupDraft();
      if (!draft.email || !draft.password) {
        router.replace("/m/signup");
        return;
      }
      setEmail(draft.email);
      try {
        if (!draft.signedUpAt) {
          await auth.signup({
            email: draft.email,
            password: draft.password,
            userName: draft.userName ?? draft.email.split("@")[0]!,
            phone: draft.phone,
            corporateName: draft.corporateName ?? "",
          });
          writeSignupDraft({ signedUpAt: new Date().toISOString() });
        }
        try {
          await auth.login(draft.email, draft.password);
        } catch (loginErr) {
          if (!(loginErr instanceof ApiError)) throw loginErr;
        }
        await sendChallenge();
      } catch (err) {
        setError(
          err instanceof ApiError
            ? err.message
            : "회원가입 처리 중 오류가 발생했습니다.",
        );
      } finally {
        setBootstrapping(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const t = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearInterval(t);
  }, [secondsLeft]);

  const mm = String(Math.floor(secondsLeft / 60));
  const ss = String(secondsLeft % 60).padStart(2, "0");
  const allFilled = digits.every((d) => d !== "");

  const sendChallenge = async () => {
    setError(null);
    const res = await auth.mfaChallenge("EMAIL", "LOGIN");
    setChallengeId(res.challengeId);
    setMaskedTarget(res.maskedTarget);
    const ms = new Date(res.expiresAt).getTime() - Date.now();
    setSecondsLeft(Math.max(0, Math.round(ms / 1000)));
    setDigits(Array(OTP_LEN).fill(""));
    cells.current[0]?.focus();
  };

  const onCellChange = (i: number, v: string) => {
    if (!/^\d?$/.test(v)) return;
    setDigits((p) => {
      const n = [...p];
      n[i] = v;
      return n;
    });
    if (v && i < OTP_LEN - 1) cells.current[i + 1]?.focus();
  };

  const onCellKey = (i: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace" && !digits[i] && i > 0) {
      cells.current[i - 1]?.focus();
    }
  };

  const onPaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    const v = e.clipboardData
      .getData("text")
      .replace(/\D/g, "")
      .slice(0, OTP_LEN);
    if (!v) return;
    e.preventDefault();
    const next = Array(OTP_LEN).fill("");
    for (let i = 0; i < v.length; i++) next[i] = v[i]!;
    setDigits(next);
    cells.current[Math.min(v.length, OTP_LEN - 1)]?.focus();
  };

  const onVerify = async () => {
    if (!challengeId || !allFilled) return;
    setError(null);
    setInfo(null);
    setVerifying(true);
    try {
      const res = await auth.mfaVerify(challengeId, digits.join(""));
      if (!res.verified) {
        setError("인증번호가 올바르지 않습니다.");
        return;
      }
      router.replace("/m/home");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "인증에 실패했습니다.");
    } finally {
      setVerifying(false);
    }
  };

  const onResend = async () => {
    setError(null);
    setInfo(null);
    setResending(true);
    try {
      await sendChallenge();
      setInfo("인증번호를 재전송했습니다.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "재전송에 실패했습니다.");
    } finally {
      setResending(false);
    }
  };

  if (bootstrapping) {
    return (
      <section className="view verify-view">
        <MTopBar title="이메일 인증" back="/m/signup" />
        <div className="m-loading">이메일 인증 준비 중…</div>
      </section>
    );
  }

  return (
    <section className="view verify-view">
      <MTopBar title="이메일 인증" back="/m/signup" />
      <div className="content center scroll verify-content">
        <div className="verify-icon">
          <MIcon.mail />
        </div>
        <h1 className="headline m-auth-title">이메일을 확인해 주세요</h1>
        <p className="subcopy">
          {(maskedTarget || email) + "로 6자리 인증 코드를 발송했습니다."}
        </p>

        <div className="m-otp-row">
          {digits.map((d, i) => (
            <div key={i} className={`otp-box${d ? " filled" : ""}`}>
              <input
                ref={(el) => {
                  cells.current[i] = el;
                }}
                value={d}
                inputMode="numeric"
                maxLength={1}
                onChange={(e) => onCellChange(i, e.target.value)}
                onKeyDown={(e) => onCellKey(i, e)}
                onPaste={onPaste}
              />
            </div>
          ))}
        </div>
        {secondsLeft > 0 ? (
          <p className="otp-timer">
            남은 시간{" "}
            <b>
              {mm}:{ss}
            </b>
          </p>
        ) : null}
        {error ? <p className="m-error">{error}</p> : null}
        {info && !error ? <p className="otp-timer">{info}</p> : null}
        <button
          type="button"
          className="text-link"
          onClick={onResend}
          disabled={resending}
        >
          {resending ? "재전송 중..." : "인증 코드 재전송"}
        </button>
      </div>
      <div className="bottom-action verify-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onVerify}
          disabled={verifying || !allFilled || !challengeId}
        >
          {verifying ? "인증 중..." : "인증 완료"}
        </button>
      </div>
    </section>
  );
}
