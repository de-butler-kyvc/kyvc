"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MBottomNav, MToggle, MTopBar } from "@/components/m/parts";
import { auth } from "@/lib/api";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

export default function MobileSettingsPage() {
  const router = useRouter();
  const [bio, setBio] = useState(true);
  const [push, setPush] = useState(true);
  const [expiryNotice, setExpiryNotice] = useState(true);
  const [toast, setToast] = useState("");
  const [toastClosing, setToastClosing] = useState(false);

  const showToast = (message: string) => {
    setToastClosing(false);
    setToast(message);
    window.setTimeout(() => setToastClosing(true), 1400);
    window.setTimeout(() => setToast(""), 1600);
  };

  const openNativePinSettings = async () => {
    if (!isBridgeAvailable()) {
      showToast("앱에서만 사용할 수 있는 기능입니다");
      return;
    }
    await bridge
      .requestNativeAuth("pin", "wallet-login")
      .catch(() => showToast("앱 인증을 시작할 수 없습니다"));
  };

  const onLogout = async () => {
    // 동시 호출: 백엔드 세션 종료 + 네이티브 인증 세션 무효화
    const tasks: Promise<unknown>[] = [];
    tasks.push(auth.logout().catch(() => null));
    if (isBridgeAvailable()) {
      tasks.push(bridge.logout().catch(() => null));
    }
    await Promise.all(tasks);
    // 로그아웃 직후 getWalletInfo 자동 호출 금지(가이드 권장).
    router.replace("/m");
  };

  return (
    <section className="view wash">
      <MTopBar title="설정" back="/m/home" />
      <div className="scroll settings-container">
        <div className="settings-profile">
          <div className="sp-avatar">
            <MIcon.building />
          </div>
          <div className="sp-info">
            <h2>테크노바 주식회사</h2>
            <p>123-45-67890</p>
          </div>
          <button type="button" className="sp-edit">
            수정
          </button>
        </div>

        <div className="settings-group">
          <div className="sg-title">보안 및 개인정보</div>
          <div className="sg-card">
            <div
              className="sg-item"
              onClick={openNativePinSettings}
            >
              <div className="sg-icon blue">
                <MIcon.lock />
              </div>
              <div className="sg-text">PIN 설정</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
            <div className="sg-item">
              <div className="sg-icon purple">
                <MIcon.fingerprint />
              </div>
              <div className="sg-text">생체 인증 로그인</div>
              <div className="sg-right">
                <MToggle active={bio} onClick={() => setBio((b) => !b)} />
              </div>
            </div>
            <div
              className="sg-item"
              onClick={() => router.push("/m/settings/recovery")}
            >
              <div className="sg-icon gray">
                <MIcon.globe />
              </div>
              <div className="sg-text">기기 변경 및 지갑 복구</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">기본 설정</div>
          <div className="sg-card">
            <div className="sg-item">
              <div className="sg-icon orange">
                <MIcon.globe />
              </div>
              <div className="sg-text">네트워크</div>
              <div className="sg-right">
                <span className="sg-val">XRP Ledger Testnet</span>
                <MIcon.chevronRight />
              </div>
            </div>
            <div className="sg-item">
              <div className="sg-icon ink">
                <MIcon.bell />
              </div>
              <div className="sg-text">푸시 알림</div>
              <div className="sg-right">
                <MToggle active={push} onClick={() => setPush((b) => !b)} />
              </div>
            </div>
            <div className="sg-item">
              <div className="sg-icon orange">
                <MIcon.bell />
              </div>
              <div className="sg-text">증명서 만료 사전 알림</div>
              <div className="sg-right">
                <MToggle
                  active={expiryNotice}
                  onClick={() => setExpiryNotice((b) => !b)}
                />
              </div>
            </div>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">자산 관리</div>
          <div className="sg-card">
            <div
              className="sg-item"
              onClick={() => router.push("/m/settings/vc-hide")}
            >
              <div className="sg-icon purple">
                <MIcon.cert />
              </div>
              <div className="sg-text">증명서(VC) 숨김 및 관리</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <div className="settings-group">
          <div className="sg-title">지원</div>
          <div className="sg-card">
            <div className="sg-item">
              <div className="sg-icon gray">
                <MIcon.help />
              </div>
              <div className="sg-text">도움말 및 지원센터</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
            <div className="sg-item">
              <div className="sg-icon gray">
                <MIcon.mail />
              </div>
              <div className="sg-text">약관 및 개인정보 처리방침</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <button
          type="button"
          className="settings-logout"
          onClick={onLogout}
        >
          <MIcon.logout />
          <span>로그아웃</span>
        </button>
        <p className="settings-version">KYvC Wallet v1.0.4</p>
      </div>
      <MBottomNav active="settings" />
      {toast ? (
        <div className={`m-toast${toastClosing ? " closing" : ""}`}>
          {toast}
        </div>
      ) : null}
    </section>
  );
}
