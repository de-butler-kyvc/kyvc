"use client";

import { useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable, useBridgeAction } from "@/lib/m/android-bridge";

function ShieldSearchIcon() {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" className="recovery-row-svg shield-search">
      <path d="M25.1567 14.1533V9.58C25.1567 7.94 23.9033 6.12667 22.37 5.55333L15.7167 3.06C14.61 2.64667 12.7967 2.64667 11.69 3.06L5.03667 5.56667C3.50333 6.14 2.25 7.95333 2.25 9.58V19.4867C2.25 21.06 3.29 23.1267 4.55667 24.0733L10.29 28.3533C11.2233 29.0733 12.4633 29.42 13.7033 29.42" />
      <circle cx="23.08" cy="23.08" r="5.33" />
      <path d="M26.85 26.85L30 30" />
      <path d="M23.08 23.08H23.092" />
    </svg>
  );
}

function WalletRecoveryIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="recovery-row-svg">
      <path d="M18.04 13.55C17.62 13.96 17.38 14.55 17.44 15.18C17.53 16.26 18.52 17.05 19.6 17.05H21.5V18.24C21.5 20.31 19.81 22 17.74 22H6.26C4.19 22 2.5 20.31 2.5 18.24V11.51C2.5 9.44001 4.19 7.75 6.26 7.75H17.74C19.81 7.75 21.5 9.44001 21.5 11.51V12.95H19.48C18.92 12.95 18.41 13.17 18.04 13.55Z" />
      <path d="M2.5 12.41V7.84004C2.5 6.65004 3.23 5.59 4.34 5.17L12.28 2.17C13.52 1.7 14.85 2.62003 14.85 3.95003V7.75002" opacity=".4" />
      <path d="M22.56 13.97V16.03C22.56 16.58 22.12 17.03 21.56 17.05H19.6C18.52 17.05 17.53 16.26 17.44 15.18C17.38 14.55 17.62 13.96 18.04 13.55C18.41 13.17 18.92 12.95 19.48 12.95H21.56C22.12 12.97 22.56 13.42 22.56 13.97Z" />
      <path d="M7 12H14" opacity=".4" />
    </svg>
  );
}

function NoticeIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true" className="recovery-notice-svg">
      <circle cx="10" cy="10" r="8.25" />
      <path d="M7.95 7.75C8.22 6.82 8.96 6.25 10.05 6.25C11.27 6.25 12.05 6.95 12.05 7.95C12.05 8.77 11.62 9.22 10.93 9.66C10.37 10.02 10.15 10.31 10.15 10.92V11.3" />
      <path d="M10.12 13.3H10.13" />
    </svg>
  );
}

export default function MobileSettingsRecoveryPage() {
  const [toast, setToast] = useState("");
  const [toastClosing, setToastClosing] = useState(false);

  const showToast = (message: string) => {
    setToastClosing(false);
    setToast(message);
    window.setTimeout(() => setToastClosing(true), 1400);
    window.setTimeout(() => setToast(""), 1600);
  };

  useBridgeAction("REQUEST_MNEMONIC_BACKUP", (result) => {
    if (!result.ok) showToast(result.error ?? "복구 문구 백업을 시작할 수 없습니다.");
  });

  useBridgeAction("REQUEST_WALLET_RESTORE", (result) => {
    if (result.ok) {
      showToast("지갑 복구가 완료되었습니다.");
      return;
    }
    showToast(result.error ?? "지갑 복구를 시작할 수 없습니다.");
  });

  const requestBackup = () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 복구 문구를 백업할 수 있습니다.");
      return;
    }
    if (!bridge.requestMnemonicBackup()) {
      showToast("복구 문구 백업을 시작할 수 없습니다.");
    }
  };

  const requestRestore = () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 지갑을 복구할 수 있습니다.");
      return;
    }
    if (!bridge.requestWalletRestore({ overwrite: true, autoRegisterDidSet: true })) {
      showToast("지갑 복구를 시작할 수 없습니다.");
    }
  };

  return (
    <section className="view wash settings-recovery-view">
      <MTopBar title="기기 변경 및 복구" back="/m/settings" />
      <div className="scroll content settings-recovery-content">
        <h1 className="recovery-title">
          안전한 지갑 복구를 위한
          <br />
          안내입니다
        </h1>
        <p className="recovery-copy">
          기기 변경 또는 앱 삭제 후 복구하려면 백업된 복구 문구(Seed Phrase)가 필요합니다.
        </p>

        <div className="recovery-card">
          <button
            type="button"
            className="recovery-row"
            onClick={requestBackup}
          >
            <span className="recovery-row-icon plain">
              <ShieldSearchIcon />
            </span>
            <span className="recovery-row-text">복구 문구 확인 및 백업</span>
            <span className="recovery-row-arrow">
              <MIcon.chevronRight />
            </span>
          </button>
          <button
            type="button"
            className="recovery-row"
            onClick={requestRestore}
          >
            <span className="recovery-row-icon plain">
              <WalletRecoveryIcon />
            </span>
            <span className="recovery-row-text">지갑 복구</span>
            <span className="recovery-row-arrow">
                <MIcon.chevronRight />
            </span>
          </button>
        </div>

        <div className="recovery-notice">
          <div className="recovery-notice-icon">
            <NoticeIcon />
          </div>
          <div className="recovery-notice-text">
            <strong>주의사항</strong>
            <p>
              복구 문구를 타인에게 노출하지 마세요. KYvC는 어떠한 경우에도 복구
              문구를 요구하지 않습니다.
            </p>
          </div>
        </div>
      </div>
      {toast ? (
        <div className={`m-toast${toastClosing ? " closing" : ""}`}>
          {toast}
        </div>
      ) : null}
    </section>
  );
}
