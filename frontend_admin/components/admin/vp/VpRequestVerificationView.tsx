"use client";

import { useEffect, useMemo, useState } from "react";

import {
  buildMockAdminVpRequest,
  createMockAdminVpRequest,
  type AdminVpRequestSession,
  type AdminVpRequestStatus,
} from "@/lib/api/admin-vp-request";

import { VpRequestInfoCard } from "./VpRequestInfoCard";
import { VpRequestQrCard } from "./VpRequestQrCard";
import { VpQrModal } from "./VpQrModal";
import { VpVerificationResultView } from "./VpVerificationResultView";

const ACTIVE_STATUSES: AdminVpRequestStatus[] = ["REQUESTED", "PRESENTED"];

function toRemainingSeconds(expiresAt: string, now: number) {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000));
}

export function VpRequestVerificationView() {
  const [session, setSession] = useState<AdminVpRequestSession>(() =>
    buildMockAdminVpRequest()
  );
  const [now, setNow] = useState(() => Date.now());
  const [qrModalOpen, setQrModalOpen] = useState(false);

  const remainingSeconds = useMemo(
    () => toRemainingSeconds(session.payload.expiresAt, now),
    [now, session.payload.expiresAt]
  );

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (
      remainingSeconds > 0 ||
      !ACTIVE_STATUSES.includes(session.status)
    ) {
      return;
    }

    setSession((current) => ({
      ...current,
      status: "EXPIRED",
    }));
  }, [remainingSeconds, session.status]);

  const handleRegenerate = async () => {
    const nextSession = await createMockAdminVpRequest();

    setSession(nextSession);
    setNow(Date.now());
  };

  const handleCancel = () => {
    setSession((current) => ({
      ...current,
      status: "CANCELLED",
    }));
  };

  const handleStatusChange = (nextStatus: AdminVpRequestStatus) => {
    const changedAt = new Date().toISOString();

    setSession((current) => {
      if (nextStatus === "REQUESTED") {
        return {
          ...current,
          status: nextStatus,
          submittedAt: null,
          verifiedAt: null,
        };
      }

      const needsSubmittedAt =
        nextStatus === "PRESENTED" ||
        nextStatus === "VALID" ||
        nextStatus === "INVALID";
      const needsVerifiedAt = nextStatus === "VALID" || nextStatus === "INVALID";

      return {
        ...current,
        status: nextStatus,
        submittedAt: needsSubmittedAt
          ? current.submittedAt ?? changedAt
          : current.submittedAt,
        verifiedAt: needsVerifiedAt ? changedAt : null,
      };
    });
  };

  const isVerificationResult =
    session.status === "VALID" || session.status === "INVALID";
  const title = isVerificationResult ? "VP 검증 결과" : "VP 제출 대기";
  const qrModalStatusText =
    session.status === "EXPIRED"
      ? "QR 코드가 만료되었습니다. 다시 생성해주세요."
      : session.status === "CANCELLED"
        ? "요청이 취소되었습니다. QR을 다시 생성해주세요."
        : "모바일 Wallet에서 QR 코드를 스캔하세요";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민</p>
          <h1 className="text-xl font-bold text-slate-800">{title}</h1>
        </div>
      </div>

      {isVerificationResult ? (
        <VpVerificationResultView
          session={session}
          onStatusChange={handleStatusChange}
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-[230px_1fr]">
          <VpRequestQrCard
            payload={session.payload}
            remainingSeconds={remainingSeconds}
          status={session.status}
          onRegenerate={handleRegenerate}
          onCancel={handleCancel}
          onQrClick={() => setQrModalOpen(true)}
        />
          <VpRequestInfoCard
            session={session}
            onStatusChange={handleStatusChange}
          />
        </div>
      )}

      <VpQrModal
        open={qrModalOpen}
        payload={session.payload}
        statusText={qrModalStatusText}
        onClose={() => setQrModalOpen(false)}
      />
    </div>
  );
}
