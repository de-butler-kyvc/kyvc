"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable, useBridgeAction } from "@/lib/m/android-bridge";

export default function MobileSeedBackupPage() {
  const [message, setMessage] = useState("네이티브 복구 문구 백업 화면을 여는 중입니다.");

  useEffect(() => {
    if (!isBridgeAvailable()) {
      setMessage("앱에서만 복구 문구를 백업할 수 있습니다.");
      return;
    }
    if (!bridge.requestMnemonicBackup()) {
      setMessage("복구 문구 백업을 시작할 수 없습니다.");
    }
  }, []);

  useBridgeAction("REQUEST_MNEMONIC_BACKUP", (result) => {
    if (!result.ok) {
      setMessage(result.error ?? "복구 문구 백업을 시작할 수 없습니다.");
    }
  });

  return (
    <section className="view wash">
      <MTopBar title="복구 문구 백업" back="/m/settings/recovery" />
      <div className="scroll content">
        <h1 className="headline m-auth-title">
          복구 문구는 앱에서만
          <br />
          확인할 수 있습니다
        </h1>
        <p className="subcopy">
          웹뷰는 복구 문구 원문을 받거나 저장하지 않습니다. 백업은 네이티브 화면에서 진행됩니다.
        </p>
        <div className="m-info-box info-box mt-24">
          <div className="info-icon">
            <MIcon.shield />
          </div>
          <div className="info-text">
            <strong>보안 안내</strong>
            <p>{message}</p>
          </div>
        </div>
      </div>
    </section>
  );
}
