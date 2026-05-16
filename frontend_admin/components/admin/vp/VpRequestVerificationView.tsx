"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import {
  AdminVpApiError,
  cancelFinanceVpRequest,
  createFinanceVpRequest,
  getFinanceVpRequestDetail,
  type AdminVpRequestDetail,
  type AdminVpRequestStatus,
} from "@/lib/api/admin-vp-request";

import { VpRequestInfoCard } from "./VpRequestInfoCard";
import { VpRequestQrCard } from "./VpRequestQrCard";
import { VpQrModal } from "./VpQrModal";
import { VpVerificationResultView } from "./VpVerificationResultView";

const ACTIVE_STATUSES: AdminVpRequestStatus[] = ["REQUESTED", "PRESENTED"];
const RESULT_STATUSES: AdminVpRequestStatus[] = [
  "VALID",
  "INVALID",
  "REPLAY_SUSPECTED",
];
const KNOWN_STATUSES: AdminVpRequestStatus[] = [
  "REQUESTED",
  "PRESENTED",
  "VALID",
  "INVALID",
  "REPLAY_SUSPECTED",
  "EXPIRED",
  "CANCELLED",
];

function toRemainingSeconds(expiresAt: string, now: number) {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000));
}

function getUiStatus(
  backendStatus: AdminVpRequestStatus,
  remainingSeconds: number
): AdminVpRequestStatus {
  if (remainingSeconds === 0 && ACTIVE_STATUSES.includes(backendStatus)) {
    return "EXPIRED";
  }

  return backendStatus;
}

function normalizeStatus(value?: string | null) {
  return KNOWN_STATUSES.find((status) => status === value) ?? null;
}

function getBackendStatus(detail: AdminVpRequestDetail): AdminVpRequestStatus {
  const verificationStatus = normalizeStatus(detail.verificationStatus);

  if (verificationStatus && RESULT_STATUSES.includes(verificationStatus)) {
    return verificationStatus;
  }

  return detail.status;
}

function toErrorMessage(error: unknown) {
  if (error instanceof AdminVpApiError) {
    if (error.status === 401 || error.status === 403) {
      return "backend VP 요청 API 인증 설정을 확인해야 합니다.";
    }
    if (error.status === 409) {
      return "이미 제출 또는 검증 완료된 요청은 취소할 수 없습니다.";
    }
    if (error.status === 410) {
      return "만료된 VP 요청입니다. QR을 재생성해 주세요.";
    }
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return "요청 처리 중 오류가 발생했습니다.";
}

export function VpRequestVerificationView() {
  const [detail, setDetail] = useState<AdminVpRequestDetail | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const [qrModalOpen, setQrModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [pollingWarning, setPollingWarning] = useState<string | null>(null);
  const initializedRef = useRef(false);

  const createRequest = useCallback(async () => {
    setCreating(true);
    setErrorMessage(null);
    setPollingWarning(null);

    try {
      const nextDetail = await createFinanceVpRequest();
      setDetail(nextDetail);
      setNow(Date.now());
      setQrModalOpen(false);
    } catch (error) {
      setErrorMessage(toErrorMessage(error));
    } finally {
      setCreating(false);
    }
  }, []);

  useEffect(() => {
    if (initializedRef.current) return;
    initializedRef.current = true;
    void createRequest();
  }, [createRequest]);

  const remainingSeconds = useMemo(
    () => (detail ? toRemainingSeconds(detail.expiresAt, now) : 0),
    [detail, now]
  );
  const backendStatus = detail ? getBackendStatus(detail) : "REQUESTED";
  const status = detail ? getUiStatus(backendStatus, remainingSeconds) : "REQUESTED";
  const isVerificationResult = detail
    ? RESULT_STATUSES.includes(backendStatus)
    : false;
  const displayDetail =
    detail && backendStatus !== detail.status
      ? { ...detail, status: backendStatus }
      : detail;
  const title = isVerificationResult ? "VP 검증 결과" : "VP 제출 대기";

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!detail?.requestId || !ACTIVE_STATUSES.includes(getBackendStatus(detail))) {
      return;
    }

    const pollDetail = async () => {
      try {
        const nextDetail = await getFinanceVpRequestDetail(detail.requestId);
        setDetail((current) =>
          current?.requestId === nextDetail.requestId && current.qrPayload
            ? { ...nextDetail, qrPayload: current.qrPayload }
            : nextDetail
        );
        setPollingWarning(null);
      } catch (error) {
        setPollingWarning(toErrorMessage(error));
      }
    };

    const timer = window.setInterval(() => {
      void pollDetail();
    }, 3000);

    return () => window.clearInterval(timer);
  }, [detail]);

  const handleCancel = async () => {
    if (!detail || !ACTIVE_STATUSES.includes(getBackendStatus(detail))) return;

    setCancelling(true);
    setErrorMessage(null);

    try {
      const response = await cancelFinanceVpRequest(detail.requestId);
      setDetail((current) =>
        current
          ? {
              ...current,
              status: response.status,
              verificationStatus: response.status,
            }
          : current
      );
      setQrModalOpen(false);
    } catch (error) {
      setErrorMessage(toErrorMessage(error));
    } finally {
      setCancelling(false);
    }
  };

  const qrModalStatusText =
    status === "EXPIRED"
      ? "QR 코드가 만료되었습니다. 다시 생성해주세요."
      : status === "CANCELLED"
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

      {errorMessage ? (
        <div className="rounded-lg border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      ) : null}

      {pollingWarning ? (
        <div className="rounded-lg border border-orange-100 bg-orange-50 px-4 py-3 text-sm text-orange-700">
          {pollingWarning}
        </div>
      ) : null}

      {!detail ? (
        <section className="rounded-lg border border-slate-200 bg-white p-8 text-center">
          <p className="text-sm font-medium text-slate-700">
            {creating ? "QR 생성 중..." : "VP 요청 QR을 생성할 수 없습니다."}
          </p>
          {!creating ? (
            <button
              type="button"
              onClick={createRequest}
              className="mt-4 rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              다시 시도
            </button>
          ) : null}
        </section>
      ) : isVerificationResult && displayDetail ? (
        <VpVerificationResultView detail={displayDetail} />
      ) : (
        <div className="grid gap-4 lg:grid-cols-[230px_1fr]">
          <VpRequestQrCard
            detail={detail}
            remainingSeconds={remainingSeconds}
            status={status}
            onRegenerate={createRequest}
            onCancel={handleCancel}
            onQrClick={() => setQrModalOpen(true)}
            regenerating={creating}
            cancelling={cancelling}
          />
          <VpRequestInfoCard detail={detail} status={status} />
        </div>
      )}

      <VpQrModal
        open={qrModalOpen}
        qrPayload={detail?.qrPayload ?? ""}
        statusText={qrModalStatusText}
        onClose={() => setQrModalOpen(false)}
      />
    </div>
  );
}
