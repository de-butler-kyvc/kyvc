"use client";

import * as React from "react";
import { useT, type Lang } from "@/lib/i18n";
import { Icon } from "./icons";

export type FieldProps = {
  label?: React.ReactNode;
  required?: boolean;
  hint?: React.ReactNode;
  error?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
};

export function Field({ label, required, hint, error, children, className }: FieldProps) {
  return (
    <label className={`field ${className || ""}`}>
      {label !== undefined && (
        <span className="field-label">
          {label}
          {required && <span style={{ color: "var(--danger)" }}> *</span>}
        </span>
      )}
      {children}
      {hint && !error && <span className="field-help">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

export function Logo({
  size = 22,
  theme = "light",
}: {
  size?: number;
  theme?: "light" | "dark";
}) {
  const src =
    theme === "dark"
      ? "/assets/kyvc-wordmark-dark.png"
      : "/assets/kyvc-wordmark-light.png";
  return (
    <img
      src={src}
      alt="KYvC"
      style={{ height: size, width: "auto", display: "block" }}
    />
  );
}

export function SignupStepper({ step, lang = "ko" }: { step: number; lang?: Lang }) {
  const t = useT(lang);
  const steps = [
    t("step_type"),
    t("step_info"),
    t("step_terms"),
    t("step_email_verify"),
    t("step_complete"),
  ];
  return (
    <div className="signup-stepper-wrap">
      <div className="signup-stepper">
        {steps.map((label, i) => {
          const num = i + 1;
          const isDone = num < step;
          const isActive = num === step;
          return (
            <React.Fragment key={num}>
              {i > 0 && <div className={`signup-step-line${isDone ? " done" : ""}`} />}
              <div className={`signup-step${isDone ? " done" : isActive ? " active" : ""}`}>
                <div className="signup-step-circle">
                  {isDone ? <Icon.Check size={12} /> : num}
                </div>
                <span className="signup-step-label">{label}</span>
              </div>
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}

export function TopBar({
  minimal = true,
  theme = "light",
  onLogo,
  onLogin,
  onSignup,
  lang = "ko",
}: {
  minimal?: boolean;
  theme?: "light" | "dark";
  onLogo?: () => void;
  onLogin?: () => void;
  onSignup?: () => void;
  lang?: Lang;
}) {
  const t = useT(lang);
  return (
    <div className="topbar">
      <div
        className="topbar-logo"
        onClick={onLogo}
        style={{ cursor: onLogo ? "pointer" : "default" }}
      >
        <Logo theme={theme} size={22} />
      </div>
      {!minimal && (
        <div className="topbar-right">
          <a
            className="topbar-nav-link"
            onClick={(e) => {
              e.preventDefault();
              onLogin?.();
            }}
            href="#"
          >
            {t("login")}
          </a>
          <a
            className="topbar-nav-link active"
            onClick={(e) => {
              e.preventDefault();
              onSignup?.();
            }}
            href="#"
          >
            {t("signup")}
          </a>
        </div>
      )}
    </div>
  );
}

export function Footer({ lang = "ko" }: { lang?: Lang }) {
  const t = useT(lang);
  return <div className="footer">{t("footer")}</div>;
}

export function Checkbox({
  checked,
  onChange,
  children,
}: {
  checked: boolean;
  onChange: (next: boolean) => void;
  children?: React.ReactNode;
}) {
  return (
    <label
      className={`checkbox-row ${checked ? "checked" : ""}`}
      onClick={() => onChange(!checked)}
    >
      <span className="checkbox-box">
        {checked && <Icon.Check size={11} style={{ color: "#fff" }} />}
      </span>
      {children !== undefined && <span>{children}</span>}
    </label>
  );
}

export type ToastItem = {
  id: number | string;
  kind?: "success" | "warning" | "danger" | "";
  title: React.ReactNode;
  desc?: React.ReactNode;
};

export function ToastStack({ toasts }: { toasts: ToastItem[] }) {
  return (
    <div className="toast-stack">
      {toasts.map((t) => (
        <div key={t.id} className={`toast ${t.kind || ""}`}>
          <div className="toast-title">{t.title}</div>
          {t.desc && <div className="toast-desc">{t.desc}</div>}
        </div>
      ))}
    </div>
  );
}

export function useToasts() {
  const [toasts, setToasts] = React.useState<ToastItem[]>([]);
  const addToast = React.useCallback((t: Omit<ToastItem, "id">) => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, ...t }]);
    setTimeout(
      () => setToasts((prev) => prev.filter((x) => x.id !== id)),
      3500,
    );
  }, []);
  return { toasts, addToast };
}
