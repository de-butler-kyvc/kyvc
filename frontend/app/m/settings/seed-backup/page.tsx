"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

/**
 * 복구 문구(mnemonic) 백업 화면.
 *
 * 보안 정책:
 * - mnemonic은 반드시 브리지 `exportWalletMnemonic`에서 받아온 실제 값만 노출.
 * - 브리지 실패/미연결 시 절대 mock 값을 보여주지 않는다 (사용자가 가짜 단어를 적어둘 위험).
 * - 화면을 떠날 때 메모리에서 즉시 제거. 로그/원격 전송 금지.
 */
export default function MobileSeedBackupPage() {
  const router = useRouter();
  const [words, setWords] = useState<string[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isBridgeAvailable()) {
      setError(
        "앱 내부 지갑 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.",
      );
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const r = await bridge.exportWalletMnemonic();
        if (cancelled) return;
        if (!r.ok) {
          setError(r.error ?? "복구 문구를 가져올 수 없습니다.");
          return;
        }
        if (typeof r.mnemonic !== "string" || r.mnemonic.trim().length === 0) {
          setError(
            "현재 지갑에 저장된 복구 문구가 없습니다. (시드로 복구된 지갑 또는 구버전)",
          );
          return;
        }
        setWords(r.mnemonic.split(/\s+/).filter(Boolean));
      } catch (e) {
        if (!cancelled) {
          setError(
            e instanceof Error
              ? e.message
              : "복구 문구 조회 호출에 실패했습니다.",
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
      // 컴포넌트 언마운트 시 메모리에서 단어 제거
      setWords(null);
    };
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

        {loading ? <p className="m-loading mt-24">불러오는 중…</p> : null}
        {error ? <p className="m-error mt-16">{error}</p> : null}

        {words ? (
          <div className="seed-grid mt-24">
            {words.map((w, i) => (
              <div key={i} className="seed-word">
                <span>{i + 1}</span> {w}
              </div>
            ))}
          </div>
        ) : null}

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
          disabled={!words}
          onClick={() => router.push("/m/settings/seed-test")}
        >
          다 기록했습니다
        </button>
      </div>
    </section>
  );
}
