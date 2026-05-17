"use client";

import * as React from "react";

import { Icon } from "@/components/design/icons";

type TimelineStep = {
  id: string;
  label: string;
  done: boolean;
  active: boolean;
  at?: string;
};

const STATUS_ORDER = [
  "DRAFT",
  "SUBMITTED",
  "AI_REVIEWING",
  "NEED_SUPPLEMENT",
  "MANUAL_REVIEW",
  "APPROVED",
  "VC_ISSUED",
  "REJECTED"
] as const;

export type KycStatusCode = (typeof STATUS_ORDER)[number];

const DEFAULT_STEPS = [
  { id: "recv", label: "서류 접수" },
  { id: "review", label: "담당자 검토" },
  { id: "ai", label: "AI 심사" },
  { id: "result", label: "심사 결과" }
];

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "임시 저장",
  SUBMITTED: "심사 대기",
  AI_REVIEWING: "AI 심사 중",
  NEED_SUPPLEMENT: "보완 필요",
  MANUAL_REVIEW: "수동 심사 중",
  APPROVED: "심사 완료",
  REJECTED: "반려",
  VC_ISSUED: "VC 발급 완료"
};

export function statusLabel(status?: string | null) {
  if (!status) return "-";
  return STATUS_DISPLAY[status] ?? STATUS_LABELS[status] ?? status;
}

const STATUS_DISPLAY: Record<string, string> = {
  DRAFT: "임시 저장",
  SUBMITTED: "접수완료",
  AI_REVIEWING: "AI 심사중",
  NEED_SUPPLEMENT: "보완 필요",
  MANUAL_REVIEW: "수동 심사중",
  APPROVED: "승인",
  REJECTED: "반려",
  VC_ISSUED: "VC 발급 완료"
};

export function statusBadgeVariant(
  status?: string | null
): "default" | "secondary" | "warning" | "success" | "destructive" {
  switch (status) {
    case "APPROVED":
    case "VC_ISSUED":
      return "success";
    case "NEED_SUPPLEMENT":
      return "warning";
    case "REJECTED":
      return "destructive";
    case "AI_REVIEWING":
    case "MANUAL_REVIEW":
    case "SUBMITTED":
      return "default";
    default:
      return "secondary";
  }
}

export function progressFor(status?: string | null) {
  switch (status) {
    case "DRAFT":
      return { stepIndex: 0, percent: 15 };
    case "SUBMITTED":
      return { stepIndex: 1, percent: 35 };
    case "AI_REVIEWING":
      return { stepIndex: 2, percent: 60 };
    case "NEED_SUPPLEMENT":
      return { stepIndex: 2, percent: 60 };
    case "MANUAL_REVIEW":
      return { stepIndex: 2, percent: 80 };
    case "APPROVED":
    case "VC_ISSUED":
      return { stepIndex: 3, percent: 100 };
    case "REJECTED":
      return { stepIndex: 3, percent: 100 };
    default:
      return { stepIndex: 0, percent: 5 };
  }
}

export function StatusTimeline({
  status,
  submittedAt,
  reviewedAt
}: {
  status?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
}) {
  const { stepIndex, percent } = progressFor(status);
  const steps: TimelineStep[] = DEFAULT_STEPS.map((s, i) => ({
    id: s.id,
    label: s.label,
    done: i < stepIndex,
    active: i === stepIndex,
    at:
      i === 0 && submittedAt
        ? formatDateTime(submittedAt)
        : i === 1 && submittedAt
          ? formatDateTime(submittedAt)
          : i === 3 && reviewedAt
            ? formatDateTime(reviewedAt)
            : undefined
  }));

  const isWarning = status === "NEED_SUPPLEMENT";

  return (
    <>
      <div style={{ display: "flex", flexDirection: "column", gap: 0 }}>
        {steps.map((s, i) => (
          <div
            key={s.id}
            style={{
              display: "flex",
              gap: 16,
              alignItems: "flex-start",
              paddingBottom: i < steps.length - 1 ? 24 : 0,
              position: "relative"
            }}
          >
            {i < steps.length - 1 ? (
              <div
                style={{
                  position: "absolute",
                  left: 13,
                  top: 28,
                  width: 2,
                  height: "calc(100% - 4px)",
                  background: s.done ? "var(--success)" : "var(--border)"
                }}
              />
            ) : null}
            <div
              style={{
                width: 28,
                height: 28,
                borderRadius: "50%",
                flexShrink: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 12,
                fontWeight: 700,
                zIndex: 1,
                background: s.done
                  ? "var(--success)"
                  : s.active
                    ? isWarning
                      ? "var(--warning)"
                      : "var(--accent)"
                    : "var(--surface-2)",
                color: s.done || s.active ? "#fff" : "var(--text-muted)",
                border: `2px solid ${
                  s.done
                    ? "var(--success)"
                    : s.active
                      ? isWarning
                        ? "var(--warning)"
                        : "var(--accent)"
                      : "var(--border)"
                }`
              }}
            >
              {s.done ? <Icon.Check size={13} /> : i + 1}
            </div>
            <div style={{ paddingTop: 4 }}>
              <div
                style={{
                  fontSize: 14,
                  fontWeight: s.active ? 700 : 500,
                  color: s.active
                    ? "var(--text-primary)"
                    : s.done
                      ? "var(--text-secondary)"
                      : "var(--text-muted)"
                }}
              >
                {s.label}
              </div>
              {s.done && s.at ? (
                <div style={{ fontSize: 12, color: "var(--text-muted)", marginTop: 2 }}>
                  {s.at}
                </div>
              ) : null}
              {s.active ? (
                <div
                  style={{
                    fontSize: 12,
                    color: isWarning ? "var(--warning)" : "var(--accent)",
                    marginTop: 2
                  }}
                >
                  {isWarning ? "보완 요청 중" : "진행 중..."}
                </div>
              ) : null}
            </div>
          </div>
        ))}
      </div>
      <div
        style={{
          marginTop: 20,
          height: 8,
          background: "var(--surface-2)",
          borderRadius: 999,
          overflow: "hidden",
          border: "1px solid var(--border)"
        }}
      >
        <div
          style={{
            height: "100%",
            width: `${percent}%`,
            background: isWarning ? "var(--warning)" : "var(--accent)",
            borderRadius: 999,
            transition: "width 0.5s ease"
          }}
        />
      </div>
      <div
        style={{
          fontSize: 12,
          color: "var(--text-muted)",
          marginTop: 6,
          textAlign: "right"
        }}
      >
        {percent}%
      </div>
    </>
  );
}

function formatDateTime(input?: string | null) {
  if (!input) return "";
  try {
    const d = new Date(input);
    if (Number.isNaN(d.getTime())) return input;
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return input;
  }
}

export function formatDate(input?: string | null) {
  if (!input) return "-";
  try {
    const d = new Date(input);
    if (Number.isNaN(d.getTime())) return input;
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`;
  } catch {
    return input;
  }
}
