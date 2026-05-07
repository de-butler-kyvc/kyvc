import { Check } from "lucide-react";

import { cn } from "@/lib/utils";

const STEPS = [
  { label: "시작" },
  { label: "법인 유형" },
  { label: "서류 안내" },
  { label: "업로드" },
  { label: "확인/제출" }
];

export type KycStepNumber = 1 | 2 | 3 | 4 | 5;

export function StepIndicator({ current }: { current: KycStepNumber }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      {STEPS.map((s, i) => {
        const idx = i + 1;
        const done = idx < current;
        const active = idx === current;
        return (
          <div key={s.label} className="flex items-center gap-2">
            <div
              className={cn(
                "flex size-6 items-center justify-center rounded-full text-[11px] font-bold",
                done && "bg-success text-white",
                active && "bg-primary text-white",
                !done && !active && "bg-secondary text-muted-foreground"
              )}
            >
              {done ? <Check className="size-3.5" /> : idx}
            </div>
            <span
              className={cn(
                "text-[13px]",
                active ? "font-semibold text-foreground" : "text-muted-foreground"
              )}
            >
              {s.label}
            </span>
            {idx < STEPS.length ? (
              <div className="hidden h-px w-10 bg-border md:block" />
            ) : null}
          </div>
        );
      })}
    </div>
  );
}
