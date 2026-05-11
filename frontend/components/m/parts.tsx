"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";

import { MIcon, type MIconName } from "./icons";

export function MLogo({
  className = "",
  alt = "KYvC",
}: {
  className?: string;
  alt?: string;
}) {
  return (
    <img
      src="/assets/kyvc-wordmark-light.png"
      alt={alt}
      className={`logo-wordmark${className ? " " + className : ""}`}
    />
  );
}

export function MLogoDark({ className = "" }: { className?: string }) {
  return (
    <img
      src="/assets/kyvc-wordmark-dark.png"
      alt="KYvC"
      className={`logo-wordmark${className ? " " + className : ""}`}
    />
  );
}

type TopBarProps = {
  title?: React.ReactNode;
  back?: string | (() => void) | false;
  logo?: boolean;
  right?: React.ReactNode;
  glass?: boolean;
  onLogoLongPress?: () => void;
  logoLongPressMs?: number;
};

export function MTopBar({
  title,
  back,
  logo,
  right,
  glass,
  onLogoLongPress,
  logoLongPressMs = 3000,
}: TopBarProps) {
  const router = useRouter();
  const logoTimer = React.useRef<number | null>(null);
  const onBack = () => {
    if (back === false) return;
    if (typeof back === "function") return back();
    if (typeof back === "string") return router.push(back);
    if (typeof window !== "undefined" && window.history.length > 1) router.back();
    else router.push("/m");
  };
  const clearLogoTimer = () => {
    if (!logoTimer.current) return;
    window.clearTimeout(logoTimer.current);
    logoTimer.current = null;
  };
  const startLogoTimer = () => {
    if (!onLogoLongPress) return;
    clearLogoTimer();
    logoTimer.current = window.setTimeout(() => {
      logoTimer.current = null;
      onLogoLongPress();
    }, logoLongPressMs);
  };

  return (
    <header className="m-topbar">
      <div className="top-left">
        {logo && onLogoLongPress ? (
          <button
            type="button"
            className="logo-longpress"
            aria-label="KYvC"
            onPointerDown={startLogoTimer}
            onPointerUp={clearLogoTimer}
            onPointerCancel={clearLogoTimer}
            onPointerLeave={clearLogoTimer}
          >
            <MLogo className="small" />
          </button>
        ) : logo ? (
          <MLogo className="small" />
        ) : back === false ? null : (
          <button
            type="button"
            className={`m-icon-btn${glass ? " glass" : ""}`}
            aria-label="뒤로"
            onClick={onBack}
          >
            <MIcon.back />
          </button>
        )}
        {!logo && title ? <span className="title">{title}</span> : null}
      </div>
      {right}
    </header>
  );
}

type BottomNavProps = {
  active?: "home" | "notifications" | "settings" | "transactions";
  onQrClick?: () => void;
};

export function MBottomNav({ active, onQrClick }: BottomNavProps) {
  const qrControl = onQrClick ? (
    <button type="button" className="fab" aria-label="QR 스캔" onClick={onQrClick}>
      <MIcon.qr />
    </button>
  ) : (
    <Link href="/m/vp/scan" className="fab" aria-label="QR 스캔">
      <MIcon.qr />
    </Link>
  );

  return (
    <nav className="bottom-nav">
      <Link href="/m/home" className={`nav-btn${active === "home" ? " active" : ""}`}>
        <MIcon.wallet />
        <span>지갑</span>
      </Link>
      <div className="fab-slot">
        {qrControl}
        <span>QR</span>
      </div>
      <Link
        href="/m/transactions"
        className={`nav-btn${active === "transactions" ? " active" : ""}`}
      >
        <MIcon.history />
        <span>활동</span>
      </Link>
    </nav>
  );
}

export type CertItem = {
  issuer: string;
  title: string;
  status: string;
  id: string;
  date: string;
  expiresAt?: string;
  gradient: string;
};

export function MCertCard({
  cert,
  index = 0,
  extra = "",
  onClick,
}: {
  cert: CertItem;
  index?: number;
  extra?: string;
  onClick?: () => void;
}) {
  const showDateLabels = extra.split(" ").includes("stacked");
  const style = {
    "--card-bg": cert.gradient,
    "--i": index,
  } as React.CSSProperties;
  return (
    <article
      className={`credential-card ${extra}`}
      style={style}
      onClick={onClick}
    >
      <div className="card-glow" />
      <div className="card-top">
        <div>
          <p>{cert.issuer}</p>
          <h3>{cert.title}</h3>
        </div>
        <span>{cert.status}</span>
      </div>
      <div className="card-chip">
        <MLogoDark className="chip-logo" />
        <small>Verified Credential</small>
      </div>
      <div className="card-bottom">
        <span>{cert.id}</span>
        {showDateLabels ? (
          <span className="card-dates">
            <em>발급일 {cert.date}</em>
            {cert.expiresAt ? <em>만료일 {cert.expiresAt}</em> : null}
          </span>
        ) : (
          <span>{cert.date}</span>
        )}
      </div>
    </article>
  );
}

export function MToggle({
  active,
  onClick,
}: {
  active: boolean;
  onClick?: () => void;
}) {
  return (
    <div
      className={`toggle${active ? " active" : ""}`}
      role="button"
      onClick={onClick}
    >
      <i />
    </div>
  );
}

export function MIconBox({ name, size }: { name: MIconName; size?: number }) {
  const Cmp = MIcon[name];
  return <Cmp size={size} />;
}

export function usePathSearch() {
  const pathname = usePathname();
  return pathname;
}
