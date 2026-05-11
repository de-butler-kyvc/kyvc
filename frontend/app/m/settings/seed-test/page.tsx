"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable, useBridgeAction } from "@/lib/m/android-bridge";

export default function MobileSeedTestPage() {
  const [message, setMessage] = useState("네이티브 지갑 복구 화면을 여는 중입니다.");

  useEffect(() => {
    if (!isBridgeAvailable()) {
      setMessage("앱에서만 지갑을 복구할 수 있습니다.");
      return;
    }
    if (!bridge.requestWalletRestore({ overwrite: true, autoRegisterDidSet: true })) {
      setMessage("지갑 복구를 시작할 수 없습니다.");
    }
  }, []);

  useBridgeAction("REQUEST_WALLET_RESTORE", (result) => {
    if (result.ok) {
      setMessage("지갑 복구가 완료되었습니다.");
      return;
    }
    setMessage(result.error ?? "지갑 복구를 시작할 수 없습니다.");
  });

  return (
    <section className="view wash">
      <MTopBar title="지갑 복구" back="/m/settings/recovery" />
      <div className="scroll content">
        <h1 className="headline m-auth-title">
          지갑 복구는 앱에서
          <br />
          진행됩니다
        </h1>
        <p className="subcopy">
          웹뷰는 복구 문구 원문을 받거나 전달하지 않습니다. 복구 문구 입력은 네이티브 화면에서만 처리됩니다.
        </p>
        <div className="m-info-box info-box mt-24">
          <div className="info-icon">
            <MIcon.wallet />
          </div>
          <div className="info-text">
            <strong>복구 상태</strong>
            <p>{message}</p>
          </div>
        </div>
      </div>
    </section>
  );
}
