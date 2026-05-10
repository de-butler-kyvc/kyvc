"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

/**
 * 복구 테스트 화면.
 * 백업한 12단어 중 일부 인덱스를 사용자가 골라 검증한다.
 *
 * 보안: 정답 단어는 브리지에서 받아온 실제 mnemonic에서 추출한다.
 * 브리지 실패 시 mock으로 진행하지 않는다 (가짜 검증으로 사용자에게 오인 위험).
 */

const SLOTS = [3, 7, 11] as const;

function shuffle<T>(arr: T[]): T[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j]!, a[i]!];
  }
  return a;
}

export default function MobileSeedTestPage() {
  const router = useRouter();
  const [target, setTarget] = useState<string[] | null>(null);
  const [allWords, setAllWords] = useState<string[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [picked, setPicked] = useState<string[]>([]);

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
        if (!r.ok || typeof r.mnemonic !== "string" || !r.mnemonic.trim()) {
          setError(r.error ?? "복구 문구를 가져올 수 없습니다.");
          return;
        }
        const words = r.mnemonic.split(/\s+/).filter(Boolean);
        if (words.length < 12) {
          setError("복구 문구가 12 단어 미만입니다.");
          return;
        }
        setAllWords(words);
        setTarget(SLOTS.map((i) => words[i]!));
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
      // 메모리에서 제거
      setAllWords(null);
      setTarget(null);
    };
  }, []);

  // 보기: 정답 3개 + 무작위 distractor 3개
  const options = useMemo(() => {
    if (!target || !allWords) return [] as string[];
    const distractors = allWords
      .filter((w) => !target.includes(w))
      .sort(() => Math.random() - 0.5)
      .slice(0, 3);
    return shuffle([...target, ...distractors]);
  }, [target, allWords]);

  const onPick = (w: string) => {
    if (!target) return;
    if (picked.includes(w)) return;
    setPicked((p) => (p.length < target.length ? [...p, w] : p));
  };

  const ok =
    !!target &&
    picked.length === target.length &&
    picked.every((w, i) => w === target[i]);
  const wrong =
    !!target &&
    picked.length === target.length &&
    !ok;

  return (
    <section className="view wash">
      <MTopBar title="복구 테스트" back="/m/settings/recovery" />
      <div className="scroll content">
        <h1 className="headline m-auth-title">
          단어를 순서대로
          <br />
          선택해주세요
        </h1>
        <p className="subcopy">
          {SLOTS.map((s) => `${s + 1}번`).join(", ")} 단어를 차례대로 선택합니다.
        </p>

        {loading ? <p className="m-loading mt-24">불러오는 중…</p> : null}
        {error ? <p className="m-error mt-16">{error}</p> : null}

        {target ? (
          <>
            <div className="seed-input-area mt-24">
              {SLOTS.map((slot, i) => (
                <div
                  key={slot}
                  className={`seed-slot ${picked[i] ? "filled" : "empty"}`}
                >
                  {picked[i] ?? slot + 1}
                </div>
              ))}
            </div>

            <div className="seed-options mt-24">
              {options.map((w) => (
                <button
                  key={w}
                  type="button"
                  className="seed-opt"
                  onClick={() => onPick(w)}
                  disabled={picked.includes(w)}
                >
                  {w}
                </button>
              ))}
            </div>

            {wrong ? (
              <p className="m-error mt-16">
                순서가 다릅니다. 다시 시도해 주세요.
              </p>
            ) : null}
          </>
        ) : null}
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary dark"
          disabled={!ok}
          onClick={() => router.push("/m/settings")}
        >
          테스트 완료
        </button>
        <button
          type="button"
          className="ghost"
          onClick={() => setPicked([])}
          disabled={!target}
        >
          다시 선택
        </button>
      </div>
    </section>
  );
}
