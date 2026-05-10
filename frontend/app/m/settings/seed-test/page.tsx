"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import { MOCK_SEED_WORDS } from "@/lib/m/data";

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
  const target = useMemo(() => SLOTS.map((i) => MOCK_SEED_WORDS[i]!), []);
  const options = useMemo(() => {
    const distractors = ["flame", "koala", "candy"];
    return shuffle([...target, ...distractors]);
  }, [target]);
  const [picked, setPicked] = useState<string[]>([]);

  const onPick = (w: string) => {
    if (picked.includes(w)) return;
    setPicked((p) => (p.length < target.length ? [...p, w] : p));
  };

  const ok =
    picked.length === target.length &&
    picked.every((w, i) => w === target[i]);
  const wrong =
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
          <p className="m-error mt-16">순서가 다릅니다. 다시 시도해 주세요.</p>
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
        >
          다시 선택
        </button>
      </div>
    </section>
  );
}
