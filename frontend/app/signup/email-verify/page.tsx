"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { Logo, SignupStepper } from "@/components/design/primitives";
import { Icon } from "@/components/design/icons";
import { ApiError, auth } from "@/lib/api";
import { useSession } from "@/lib/session-context";
import { SessionGateSplash } from "@/lib/session-gate";
import { readSignupDraft, writeSignupDraft } from "@/lib/signup-flow";

const OTP_LEN = 6;

export default function SignupEmailVerifyPage() {
  const router = useRouter();
  const { refreshSession } = useSession();

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

  const cellRefs = useRef<Array<HTMLInputElement | null>>([]);
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    (async () => {
      const draft = readSignupDraft();
      if (!draft.entityTypeId) {
        router.replace("/signup");
        return;
      }
      if (!draft.email || !draft.password) {
        router.replace("/signup/info");
        return;
      }
      if (!draft.termsAcceptedAt) {
        router.replace("/signup/terms");
        return;
      }
      setEmail(draft.email);

      try {
        // 가입 → 자동 로그인 → MFA 챌린지
        // 이미 가입했고 세션이 살아있으면 가입 단계는 건너뜁니다.
        if (!draft.signedUpAt) {
          await auth.signup({
            email: draft.email,
            password: draft.password,
            userName: draft.userName!,
            phone: draft.phone,
            corporateName: draft.corporateName!
          });
          writeSignupDraft({ signedUpAt: new Date().toISOString() });
        }
        try {
          await auth.login(draft.email, draft.password);
        } catch (loginErr) {
          // 이미 로그인된 상태라면 무시
          if (!(loginErr instanceof ApiError)) throw loginErr;
        }
        await refreshSession();
        await sendChallenge();
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "회원가입 처리 중 오류가 발생했습니다.");
      } finally {
        setBootstrapping(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 카운트다운
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
    cellRefs.current[0]?.focus();
  };

  const onChange = (i: number, v: string) => {
    if (!/^\d?$/.test(v)) return;
    setDigits((prev) => {
      const next = [...prev];
      next[i] = v;
      return next;
    });
    if (v && i < OTP_LEN - 1) cellRefs.current[i + 1]?.focus();
  };

  const onKey = (i: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace" && !digits[i] && i > 0) {
      cellRefs.current[i - 1]?.focus();
    }
  };

  const onPaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LEN);
    if (!pasted) return;
    e.preventDefault();
    const next = Array(OTP_LEN).fill("");
    for (let i = 0; i < pasted.length; i++) next[i] = pasted[i]!;
    setDigits(next);
    cellRefs.current[Math.min(pasted.length, OTP_LEN - 1)]?.focus();
  };

  const onVerify = async () => {
    if (!challengeId || !allFilled) return;
    setError(null);
    setInfo(null);
    setVerifying(true);
    try {
      const res = await auth.mfaVerify(challengeId, digits.join(""));
      if (!res.verified) {
        setError("인증번호가 올바르지 않습니다. 다시 입력해 주세요.");
        return;
      }
      await refreshSession();
      router.push("/signup/complete");
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
    return <SessionGateSplash message="이메일 인증 준비 중…" />;
  }

  return (
    <div className="app-shell page-enter">
      <div className="topbar">
        <div
          className="topbar-logo"
          onClick={() => router.push("/")}
          style={{ cursor: "pointer" }}
        >
          <Logo size={22} />
        </div>
        <Link className="topbar-nav-link" href="/login">
          로그인
        </Link>
      </div>

      <SignupStepper step={4} />

      <div className="center-stage">
        <div className="auth-card">
          <div style={{ textAlign: "center" }}>
            <div className="email-icon-wrap">
              <Icon.Mail size={28} />
            </div>
            <h1 className="auth-title">이메일 인증</h1>
            <p className="auth-subtitle" style={{ whiteSpace: "pre-line" }}>
              {`보안을 위해 인증번호 6자리가 발송되었습니다.\n메일을 확인하고 인증을 완료해 주세요.`}
            </p>
          </div>

          <div className="otp-method">
            <div className="otp-method-icon">
              <Icon.Mail size={18} />
            </div>
            <div>
              <div className="otp-method-title">이메일 인증</div>
              <div className="otp-method-meta">{maskedTarget || email}</div>
            </div>
          </div>

          <div className="otp-input-shell" style={{ marginTop: 14 }}>
            <div className="otp-helper">
              인증번호 6자리를 입력하세요{" "}
              {secondsLeft > 0 && (
                <>
                  (유효시간 <strong>{mm}:{ss}</strong>)
                </>
              )}
            </div>
            <div className="otp-row">
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => {
                    cellRefs.current[i] = el;
                  }}
                  className={`otp-cell ${d ? "filled" : ""}`}
                  value={d}
                  inputMode="numeric"
                  maxLength={1}
                  onChange={(e) => onChange(i, e.target.value)}
                  onKeyDown={(e) => onKey(i, e)}
                  onPaste={onPaste}
                />
              ))}
            </div>
          </div>

          {error && (
            <div className="field-error" style={{ fontSize: 13, marginTop: 12 }}>
              <Icon.X size={12} /> {error}
            </div>
          )}
          {info && !error && (
            <div className="field-success" style={{ fontSize: 13, marginTop: 12 }}>
              <Icon.Check size={12} /> {info}
            </div>
          )}

          <div className="col" style={{ gap: 8, marginTop: 18 }}>
            <button
              type="button"
              className="btn btn-primary btn-block btn-lg"
              onClick={onVerify}
              disabled={verifying || !allFilled || !challengeId}
            >
              {verifying ? "인증 중..." : "인증 확인"}
            </button>
            <button
              type="button"
              className="btn btn-ghost btn-block"
              onClick={onResend}
              disabled={resending}
            >
              {resending ? "재전송 중..." : "인증번호 재전송"}
            </button>
          </div>

          <div style={{ marginTop: 14, textAlign: "center" }}>
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault();
                router.push("/signup/info");
              }}
              style={{ fontSize: 13, color: "var(--accent)" }}
            >
              이메일 주소가 다르다면 이메일 변경
            </a>
          </div>
        </div>
      </div>

      <div className="footer">© 2025 KYvC. All rights reserved.</div>
    </div>
  );
}
