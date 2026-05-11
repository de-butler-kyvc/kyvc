"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { ApiError, auth } from "@/lib/api";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { writeSignupDraft } from "@/lib/signup-flow";

type Step = 1 | 2;
type TermKey = "service" | "privacy" | "marketing";

type FormState = {
  corporateName: string;
  bizNo: string;
  userName: string;
  email: string;
  password: string;
  passwordConfirm: string;
  terms: { all: boolean } & Record<TermKey, boolean>;
};

type EmailVerificationProps = {
  verificationId: number | null;
  maskedTarget: string;
  secondsLeft: number;
  verificationCode: string;
  emailVerified: boolean;
  sendingCode: boolean;
  verifyingCode: boolean;
  canRequestEmailCode: boolean;
  canVerifyEmailCode: boolean;
  sendEmailCode: () => void;
  verifyEmailCode: () => void;
  setVerificationCode: (value: string) => void;
};

const TERMS_LABELS: Record<TermKey, string> = {
  service: "(필수) 서비스 이용약관 동의",
  privacy: "(필수) 개인정보 처리방침 동의",
  marketing: "(선택) 마케팅 정보 수신 동의",
};

const TERMS_DESCRIPTIONS: Record<TermKey, string> = {
  service:
    "지갑을 활성화하려면 1 XRP를 예치해야 합니다.",
  privacy:
    "회원가입 및 서비스 제공을 위해 담당자 정보, 회사 정보, 인증 이력 등 개인정보 처리 항목과 보관 기준을 확인합니다.",
  marketing:
    "서비스 안내, 업데이트, 이벤트 등 선택적 정보 수신에 대한 동의 내용을 확인합니다. 동의하지 않아도 서비스 이용은 가능합니다.",
};

const STEP_LABELS = ["기본 정보", "PIN 등록"] as const;
const SIGNUP_EMAIL_CHALLENGE_KEY = "kyvc.signupEmailChallenge";
const OTP_LEN = 6;
const DEFAULT_EMAIL_CODE_SECONDS = 300;

function secondsUntil(expiresAt: string) {
  const expires = new Date(expiresAt).getTime();
  if (!Number.isFinite(expires)) return DEFAULT_EMAIL_CODE_SECONDS;
  const seconds = Math.round((expires - Date.now()) / 1000);
  return seconds > 0 ? seconds : DEFAULT_EMAIL_CODE_SECONDS;
}

export default function MobileSignupPage() {
  const router = useRouter();
  const [step, setStep] = useState<Step>(1);
  const [activeTerm, setActiveTerm] = useState<TermKey | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [accountCreated, setAccountCreated] = useState(false);
  const [verificationId, setVerificationId] = useState<number | null>(null);
  const [maskedTarget, setMaskedTarget] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [verificationCode, setVerificationCodeState] = useState("");
  const [toast, setToast] = useState("");
  const [toastClosing, setToastClosing] = useState(false);
  const [form, setForm] = useState<FormState>({
    corporateName: "",
    bizNo: "",
    userName: "",
    email: "",
    password: "",
    passwordConfirm: "",
    terms: { all: false, service: false, privacy: false, marketing: false },
  });

  const setField = <K extends keyof FormState>(k: K, v: FormState[K]) => {
    if (
      k === "email" ||
      k === "password" ||
      k === "passwordConfirm" ||
      k === "userName" ||
      k === "corporateName" ||
      k === "bizNo"
    ) {
      setEmailVerified(false);
      setAccountCreated(false);
      setVerificationId(null);
      setMaskedTarget("");
      setSecondsLeft(0);
      setVerificationCodeState("");
      window.sessionStorage.removeItem(SIGNUP_EMAIL_CHALLENGE_KEY);
    }
    setForm((p) => ({ ...p, [k]: v }));
  };

  const toggleTerm = (key: TermKey | "all") => {
    setForm((p) => {
      if (key === "all") {
        const next = !p.terms.all;
        return {
          ...p,
          terms: { all: next, service: next, privacy: next, marketing: next },
        };
      }
      const t = { ...p.terms, [key]: !p.terms[key] };
      t.all = t.service && t.privacy && t.marketing;
      return { ...p, terms: t };
    });
  };

  const back = () => {
    if (step === 2) setStep(1);
    else router.push("/m/login");
  };

  const canGoNext =
    !!form.corporateName.trim() &&
    !!form.bizNo.trim() &&
    !!form.userName.trim() &&
    /\S+@\S+\.\S+/.test(form.email) &&
    form.password.length >= 8 &&
    !!form.passwordConfirm &&
    form.password === form.passwordConfirm &&
    emailVerified &&
    form.terms.service &&
    form.terms.privacy;
  const canRequestEmailCode =
    !!form.corporateName.trim() &&
    !!form.bizNo.trim() &&
    !!form.userName.trim() &&
    /\S+@\S+\.\S+/.test(form.email) &&
    form.password.length >= 8 &&
    form.password === form.passwordConfirm;
  const canVerifyEmailCode =
    verificationCode.replace(/\D/g, "").length === OTP_LEN && secondsLeft > 0;

  const showToast = (message: string) => {
    setToastClosing(false);
    setToast(message);
    window.setTimeout(() => setToastClosing(true), 1400);
    window.setTimeout(() => setToast(""), 1600);
  };

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = window.setInterval(
      () => setSecondsLeft((s) => Math.max(0, s - 1)),
      1000,
    );
    return () => window.clearInterval(timer);
  }, [secondsLeft]);

  const writeDraft = (signedUpAt?: string) =>
    writeSignupDraft({
      corporateName: form.corporateName.trim(),
      userName: form.userName.trim(),
      email: form.email.trim(),
      password: form.password,
      phone: "",
      termsAcceptedAt: new Date().toISOString(),
      marketingAccepted: form.terms.marketing,
      signedUpAt,
    });

  const sendEmailCode = async () => {
    if (!canRequestEmailCode) {
      showToast("사업자 정보, 이메일, 비밀번호를 먼저 입력해주세요.");
      return;
    }
    if (emailVerified) return;
    setSendingCode(true);
    try {
      window.sessionStorage.removeItem(SIGNUP_EMAIL_CHALLENGE_KEY);
      writeDraft(accountCreated ? new Date().toISOString() : undefined);
      const challenge = await auth.requestSignupEmailVerification(
        form.email.trim(),
      );
      setVerificationId(challenge.verificationId);
      setMaskedTarget(challenge.maskedEmail);
      setSecondsLeft(secondsUntil(challenge.expiresAt));
      setVerificationCodeState("");
      window.sessionStorage.setItem(
        SIGNUP_EMAIL_CHALLENGE_KEY,
        JSON.stringify({ email: form.email.trim(), ...challenge }),
      );
      showToast("인증코드를 발송했습니다.");
    } catch (err) {
      showToast(
        err instanceof ApiError
          ? err.message
          : "이메일 인증코드 발송에 실패했습니다.",
      );
    } finally {
      setSendingCode(false);
    }
  };

  const setVerificationCode = (value: string) => {
    setVerificationCodeState(value.replace(/\D/g, "").slice(0, OTP_LEN));
  };

  const verifyEmailCode = async () => {
    if (!verificationId || !canVerifyEmailCode) return;
    setVerifyingCode(true);
    try {
      const res = await auth.verifySignupEmail(
        verificationId,
        form.email.trim(),
        verificationCode,
      );
      if (!res.verified) {
        showToast("인증번호가 올바르지 않습니다.");
        return;
      }
      setEmailVerified(true);
      window.sessionStorage.removeItem(SIGNUP_EMAIL_CHALLENGE_KEY);
      showToast("이메일 인증이 완료되었습니다.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "인증에 실패했습니다.");
    } finally {
      setVerifyingCode(false);
    }
  };

  const onNext = async () => {
    if (step === 1) {
      if (!canGoNext) return;
      if (!isBridgeAvailable()) {
        showToast("앱에서만 PIN을 등록할 수 있어 이 단계는 건너뜁니다.");
        setStep(2);
        return;
      }
      const requested = bridge.requestPinReset("user-request");
      if (!requested) {
        showToast("PIN 등록을 시작할 수 없습니다.");
        return;
      }
      setStep(2);
      return;
    }
    if (submitting) return;
    setSubmitting(true);
    try {
      const signedUpAt = new Date().toISOString();
      if (!accountCreated) {
        await auth.signup({
          email: form.email.trim(),
          password: form.password,
          userName: form.userName.trim(),
          phone: "",
          corporateName: form.corporateName.trim(),
        });
        setAccountCreated(true);
      }
      writeDraft(signedUpAt);
      router.push("/m/home");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "회원가입에 실패했습니다.");
      setSubmitting(false);
    }
  };

  return (
    <section className="view signup-view">
      <MTopBar title="회원가입" back={back} />
      <div className="signup-steps">
        {STEP_LABELS.map((label, i) => {
          const num = (i + 1) as Step;
          const cls = step === num ? "active" : num < step ? "done" : "";
          return (
            <div key={label} style={{ display: "contents" }}>
              <div className={`ss-item${cls ? " " + cls : ""}`}>
                <div className="ss-dot">
                  {num < step ? <MIcon.check /> : num}
                </div>
                <span>{label}</span>
              </div>
              {i < STEP_LABELS.length - 1 ? <div className="ss-line" /> : null}
            </div>
          );
        })}
      </div>

      <div className="content scroll signup-content">
        {step === 1 ? (
          <BasicInfoStep
            form={form}
            setField={setField}
            toggleTerm={toggleTerm}
            openTerm={setActiveTerm}
            emailVerification={{
              verificationId,
              maskedTarget,
              secondsLeft,
              verificationCode,
              emailVerified,
              sendingCode,
              verifyingCode,
              canRequestEmailCode,
              canVerifyEmailCode,
              sendEmailCode,
              verifyEmailCode,
              setVerificationCode,
            }}
          />
        ) : (
          <PinStep />
        )}
      </div>

      <div className="bottom-action signup-bottom-action">
        <button
          type="button"
          className="primary"
          onClick={onNext}
          disabled={(step === 1 && !canGoNext) || submitting}
        >
          {step === 1 ? "다음 단계" : submitting ? "처리 중..." : "가입 완료"}
        </button>
      </div>
      {toast ? (
        <div className={`m-toast${toastClosing ? " closing" : ""}`}>
          {toast}
        </div>
      ) : null}
      {activeTerm ? (
        <TermsBottomSheet
          termKey={activeTerm}
          checked={form.terms[activeTerm]}
          onClose={() => setActiveTerm(null)}
          onAgree={() => {
            if (!form.terms[activeTerm]) toggleTerm(activeTerm);
            setActiveTerm(null);
          }}
        />
      ) : null}
    </section>
  );
}

function BasicInfoStep({
  form,
  setField,
  toggleTerm,
  openTerm,
  emailVerification,
}: {
  form: FormState;
  setField: <K extends keyof FormState>(k: K, v: FormState[K]) => void;
  toggleTerm: (k: TermKey | "all") => void;
  openTerm: (k: TermKey) => void;
  emailVerification: EmailVerificationProps;
}) {
  const t = form.terms;
  const emailAuth = emailVerification;
  const mm = String(Math.floor(emailAuth.secondsLeft / 60));
  const ss = String(emailAuth.secondsLeft % 60).padStart(2, "0");
  const basicFilled =
    !!form.corporateName.trim() &&
    !!form.bizNo.trim() &&
    !!form.userName.trim();
  const passwordFilled =
    form.password.length >= 8 &&
    !!form.passwordConfirm &&
    form.password === form.passwordConfirm;

  return (
    <>
      <h1 className="headline m-auth-title">사업자 정보를 입력하세요</h1>
      <p className="subcopy">법인 VC 발급을 위해 기본 정보를 확인합니다.</p>

      <label className="m-field-label">회사명</label>
      <div className="input-box">
        <input
          placeholder="회사명을 입력하세요"
          value={form.corporateName}
          onChange={(e) => setField("corporateName", e.target.value)}
        />
      </div>

      <label className="m-field-label">사업자번호</label>
      <div className={`input-box${form.bizNo ? " focus" : ""}`}>
        <input
          inputMode="numeric"
          placeholder="123-45-67890"
          value={form.bizNo}
          onChange={(e) => setField("bizNo", e.target.value)}
        />
        {form.bizNo ? (
          <span className="ok">
            <MIcon.check />
          </span>
        ) : null}
      </div>

      <label className="m-field-label">담당자 이름</label>
      <div className="input-box">
        <input
          placeholder="담당자 이름을 입력하세요"
          value={form.userName}
          onChange={(e) => setField("userName", e.target.value)}
        />
      </div>

      {basicFilled ? (
        <div className="signup-progressive-group">
          <PasswordFields form={form} setField={setField} />
        </div>
      ) : null}

      {passwordFilled ? (
        <div className="signup-progressive-group">
          <label className="m-field-label">담당자 이메일</label>
          <div
            className={`input-box signup-email-input${emailAuth.emailVerified ? " verified" : ""}`}
          >
            <input
              type="email"
              inputMode="email"
              placeholder="example@company.com"
              value={form.email}
              onChange={(e) => setField("email", e.target.value)}
              disabled={emailAuth.emailVerified}
            />
            <button
              type="button"
              className="signup-email-code-btn"
              onClick={emailAuth.sendEmailCode}
              disabled={emailAuth.emailVerified || emailAuth.sendingCode}
            >
              {emailAuth.emailVerified
                ? "인증완료"
                : emailAuth.sendingCode
                  ? "발송중"
                  : emailAuth.verificationId
                    ? "재전송"
                    : "코드받기"}
            </button>
          </div>
        </div>
      ) : null}

      {emailAuth.verificationId && !emailAuth.emailVerified ? (
        <div className="signup-progressive-group signup-email-verify">
          <p>
            {(emailAuth.maskedTarget || form.email) +
              "로 발송된 인증코드를 입력하세요."}
          </p>
          <div
            className={`input-box signup-code-input${emailAuth.verificationCode ? " focus" : ""}`}
          >
            <input
              value={emailAuth.verificationCode}
              inputMode="numeric"
              maxLength={OTP_LEN}
              placeholder="인증코드 6자리"
              onChange={(e) => emailAuth.setVerificationCode(e.target.value)}
            />
            <button
              type="button"
              className="signup-code-verify-btn"
              onClick={emailAuth.verifyEmailCode}
              disabled={emailAuth.verifyingCode || !emailAuth.canVerifyEmailCode}
            >
              {emailAuth.verifyingCode ? "확인 중" : "인증하기"}
            </button>
          </div>
          <div className="signup-email-verify-actions">
            {emailAuth.secondsLeft > 0 ? (
              <span>
                남은 시간 <b>{mm}:{ss}</b>
              </span>
            ) : (
              <span>인증 시간이 만료되었습니다.</span>
            )}
          </div>
        </div>
      ) : null}

      {emailAuth.emailVerified ? (
        <div className="signup-progressive-group">
          <p className="signup-field-hint ok-text">
            이메일 인증이 완료되었습니다.
          </p>
          <p className="signup-terms-guide">
            서비스 이용을 위해 약관 동의가 필요합니다.
          </p>

          <div className="terms-box">
            <label
              className="m-terms-row terms-all"
              onClick={() => toggleTerm("all")}
            >
              <div className={`chk${t.all ? " on" : ""}`}>
                {t.all ? <MIcon.check /> : null}
              </div>
              <span className="terms-all-label">전체 동의</span>
            </label>

            <div className="terms-divider" />

            {(Object.keys(TERMS_LABELS) as TermKey[]).map((k) => (
              <div key={k} className="checkbox-item">
                <label className="m-terms-row" onClick={() => toggleTerm(k)}>
                  <div className={`chk${t[k] ? " on" : ""}`}>
                    {t[k] ? <MIcon.check /> : null}
                  </div>
                  <div className="terms-text">
                    <span>{TERMS_LABELS[k]}</span>
                  </div>
                </label>
                <button
                  type="button"
                  className="terms-link"
                  onClick={() => openTerm(k)}
                >
                  보기
                </button>
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </>
  );
}

function TermsBottomSheet({
  termKey,
  checked,
  onClose,
  onAgree,
}: {
  termKey: TermKey;
  checked: boolean;
  onClose: () => void;
  onAgree: () => void;
}) {
  const [dragY, setDragY] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [closing, setClosing] = useState(false);
  const dragStart = useRef<number | null>(null);
  const title = TERMS_LABELS[termKey].replace(/^\([^)]+\)\s*/, "");

  const closeWithAnimation = () => {
    setClosing(true);
    setDragY(window.innerHeight);
    window.setTimeout(onClose, 180);
  };

  const onDragStart = (clientY: number) => {
    dragStart.current = clientY;
    setDragging(true);
  };

  const onDragMove = (clientY: number) => {
    if (dragStart.current == null) return;
    setDragY(Math.max(0, clientY - dragStart.current));
  };

  const onDragEnd = () => {
    if (dragY > 120) closeWithAnimation();
    else setDragY(0);
    dragStart.current = null;
    setDragging(false);
  };

  return (
    <div className="terms-sheet-layer" role="dialog" aria-modal="true">
      <button
        type="button"
        className="terms-sheet-dim"
        aria-label="약관 닫기"
        onClick={closeWithAnimation}
      />
      <div
        className={`terms-sheet${dragging ? " dragging" : ""}${closing ? " closing" : ""}`}
        style={{ transform: `translateY(${dragY}px)` }}
      >
        <div
          className="terms-sheet-handle"
          role="button"
          aria-label="약관 시트 이동"
          tabIndex={0}
          onPointerDown={(e) => {
            e.currentTarget.setPointerCapture(e.pointerId);
            onDragStart(e.clientY);
          }}
          onPointerMove={(e) => onDragMove(e.clientY)}
          onPointerUp={onDragEnd}
          onPointerCancel={onDragEnd}
        />
        <div className="terms-sheet-body">
          <h2>{title}</h2>
          <p>{TERMS_DESCRIPTIONS[termKey]}</p>
        </div>
        <button type="button" className="terms-sheet-agree" onClick={onAgree}>
          {checked ? "확인" : "동의"}
        </button>
      </div>
    </div>
  );
}

function PasswordFields({
  form,
  setField,
}: {
  form: FormState;
  setField: <K extends keyof FormState>(k: K, v: FormState[K]) => void;
}) {
  const passwordValid = form.password.length >= 8;
  const passwordMatches =
    !!form.passwordConfirm && form.password === form.passwordConfirm;

  return (
    <>
      <label className="m-field-label">비밀번호</label>
      <div className={`input-box${form.password ? " focus" : ""}`}>
        <input
          type="password"
          placeholder="8자 이상 입력"
          value={form.password}
          autoComplete="new-password"
          onChange={(e) => setField("password", e.target.value)}
        />
      </div>
      {form.password ? (
        <p className={`signup-field-hint${passwordValid ? " ok-text" : ""}`}>
          {passwordValid ? "사용 가능한 비밀번호입니다." : "8자 이상 입력해주세요."}
        </p>
      ) : null}

      <label className="m-field-label">비밀번호 확인</label>
      <div className={`input-box${form.passwordConfirm ? " focus" : ""}`}>
        <input
          type="password"
          placeholder="비밀번호를 다시 입력"
          value={form.passwordConfirm}
          autoComplete="new-password"
          onChange={(e) => setField("passwordConfirm", e.target.value)}
        />
      </div>
      {form.passwordConfirm ? (
        <p className={`signup-field-hint${passwordMatches ? " ok-text" : ""}`}>
          {passwordMatches ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다."}
        </p>
      ) : null}
    </>
  );
}

function PinStep() {
  return (
    <div className="signup-pin-panel">
      <div className="success-circle">
        <MIcon.lock />
      </div>
      <h1 className="headline m-auth-title">PIN 등록</h1>
      <p className="subcopy">
        다음 단계에서 지갑 로그인에 사용할 PIN을 등록합니다.
      </p>
    </div>
  );
}
