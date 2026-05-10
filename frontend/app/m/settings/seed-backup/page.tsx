"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { MOCK_SEED_WORDS } from "@/lib/m/data";

export default function MobileSeedBackupPage() {
  const router = useRouter();
  const [words, setWords] = useState<readonly string[]>(MOCK_SEED_WORDS);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.exportWalletMnemonic();
        if (r.ok && typeof r.mnemonic === "string") {
          setWords(r.mnemonic.split(/\s+/).filter(Boolean));
        } else {
          setError(r.error ?? "복구 문구를 가져올 수 없습니다.");
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : "브리지 호출 실패");
      }
    })();
  }, []);

  return (
    <section className="view wash">
      <MTopBar title="복구 문구 백업" back="/m/settings/recovery" />
      <div className="scroll content">
        <h1 className="headline m-auth-title">
          12개의 복구 문구를
          <br />
          안전하게 기록하세요
        </h1>
        <p className="subcopy">
          이 문구는 지갑을 복구할 수 있는 유일한 수단입니다. 온라인이나 클라우드에 저장하지 마세요.
        </p>

        {error ? <p className="m-error">{error}</p> : null}

        <div className="seed-grid mt-24">
          {words.map((w, i) => (
            <div key={i} className="seed-word">
              <span>{i + 1}</span> {w}
            </div>
          ))}
        </div>

        <div className="m-info-box info-box mt-24">
          <div className="info-icon">
            <MIcon.shield />
          </div>
          <div className="info-text">
            <strong>절대 공유 금지</strong>
            <p>복구 문구를 묻는 사람은 100% 사기꾼입니다.</p>
          </div>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.push("/m/settings/seed-test")}
        >
          다 기록했습니다
        </button>
      </div>
    </section>
  );
}
