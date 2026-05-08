import * as React from "react";

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
    <div className="stepper">
      {STEPS.map((s, i) => {
        const idx = i + 1;
        const done = idx < current;
        const active = idx === current;
        const cls = active ? "active" : done ? "done" : "";
        return (
          <React.Fragment key={s.label}>
            <div className={`stepper-item ${cls}`}>
              <div className="stepper-num">{done ? "✓" : idx}</div>
              <div className="stepper-label">{s.label}</div>
            </div>
            {idx < STEPS.length ? <div className="stepper-line" /> : null}
          </React.Fragment>
        );
      })}
    </div>
  );
}
