"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import {
  ApiError,
  mobileDevice,
  mobileVp,
  mobileWallet,
  type WalletCredentialOfferResponse,
  type WalletCredentialPayload,
} from "@/lib/api";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

const QR_TYPE = {
  CREDENTIAL_OFFER: "CREDENTIAL_OFFER",
} as const;
const SCAN_RESULT_KEY = "kyvc.m.scanResult";

type IssueStep =
  | "qr"
  | "offer"
  | "wallet"
  | "device"
  | "prepare"
  | "save"
  | "xrpl"
  | "status"
  | "confirm";

const STEP_TEXT: Record<IssueStep, string> = {
  qr: "QR 정보를 확인하고 있어요",
  offer: "발급 정보를 불러오고 있어요",
  wallet: "Wallet 정보를 확인하고 있어요",
  device: "기기를 등록하고 있어요",
  prepare: "증명서를 발급하고 있어요",
  save: "지갑에 저장하고 있어요",
  xrpl: "블록체인에 기록하고 있어요",
  status: "기록 상태를 확인하고 있어요",
  confirm: "완료 처리 중이에요",
};

type ParsedCredentialOfferQr = {
  qrToken: string;
};

function parseCredentialOfferQr(qrPayloadText: string): ParsedCredentialOfferQr {
  let parsed: unknown;
  try {
    parsed = JSON.parse(qrPayloadText);
  } catch {
    throw new Error("지원하지 않는 QR입니다.");
  }

  if (typeof parsed !== "object" || parsed === null) {
    throw new Error("지원하지 않는 QR입니다.");
  }

  const payload = parsed as Record<string, unknown>;
  if (
    payload.type !== QR_TYPE.CREDENTIAL_OFFER ||
    typeof payload.qrToken !== "string" ||
    !payload.qrToken
  ) {
    throw new Error("지원하지 않는 QR입니다.");
  }

  return { qrToken: payload.qrToken };
}

function resolveOfferId(offerId?: number, targetId?: string) {
  if (typeof offerId === "number" && Number.isFinite(offerId)) return offerId;
  if (!targetId) return null;
  const parsed = Number(targetId);
  return Number.isFinite(parsed) ? parsed : null;
}

function apiErrorMessage(error: ApiError) {
  if (error.code === "CREDENTIAL_OFFER_EXPIRED") {
    return "만료된 발급 QR입니다. PC 화면에서 QR을 다시 생성해주세요.";
  }
  if (error.code === "CREDENTIAL_OFFER_ALREADY_USED") {
    return "이미 사용된 발급 QR입니다.";
  }
  if (error.code === "CREDENTIAL_OFFER_INVALID_TOKEN") {
    return "지원하지 않는 QR입니다.";
  }
  if (error.code === "WALLET_DEVICE_NOT_REGISTERED") {
    return "Wallet 기기 등록 정보를 찾을 수 없습니다. 앱을 다시 실행한 뒤 재시도해주세요.";
  }
  return error.message;
}

function requiredString(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

async function registerMobileDevice(device: {
  deviceId: string;
  deviceName?: string;
  os?: string;
  appVersion?: string;
  publicKey?: string;
}) {
  const publicKey = requiredString(device.publicKey);
  try {
    await mobileDevice.register({
      deviceId: device.deviceId,
      deviceName: requiredString(device.deviceName) ?? "Android Device",
      os: requiredString(device.os) ?? "Android",
      appVersion: requiredString(device.appVersion) ?? "1.0",
      ...(publicKey ? { publicKey } : {}),
    });
  } catch {
    throw new Error("Wallet 기기 등록에 실패했습니다. 앱을 다시 실행한 뒤 재시도해주세요.");
  }
}

function debugMissingScanResult() {
  if (process.env.NODE_ENV === "production") return;

  const hasWindow = typeof window !== "undefined";
  let hasSessionStorage = false;
  let storageKeys = 0;
  let hasScanResultKey = false;

  if (hasWindow) {
    try {
      const keys = Object.keys(window.sessionStorage);
      hasSessionStorage = true;
      storageKeys = keys.filter((key) => key.includes("kyvc")).length;
      hasScanResultKey = keys.includes(SCAN_RESULT_KEY);
    } catch {
      hasSessionStorage = false;
    }
  }

  // eslint-disable-next-line no-console
  console.debug("[vc-issue] missing scan result", {
    hasWindow,
    hasSessionStorage,
    storageKeys,
    hasScanResultKey,
  });
}

function buildSavePayload(
  credentialId: number,
  credentialPayload: WalletCredentialPayload,
) {
  const metadata = credentialPayload.metadata ?? {};
  const credentialJson = credentialPayload.credential
    ? JSON.stringify(credentialPayload.credential)
    : undefined;

  return {
    credentialId: String(credentialId),
    credential: credentialPayload.credentialJwt ?? credentialPayload.credential,
    sdJwt: credentialPayload.credentialJwt,
    vcJwt: credentialPayload.credentialJwt,
    vcJson: credentialJson,
    metadata,
  };
}

function buildIssueDisplayPayload(
  offer: WalletCredentialOfferResponse,
  extras?: Record<string, unknown>,
) {
  const profile = mSession.readCorporateProfile();
  return {
    issuerName: "KYvC 인증기관",
    issuerDid: offer.issuerDid,
    credentialTitle: credentialTitle(offer),
    holderName: offer.corporateName ?? profile?.corporateName,
    registrationNumber:
      offer.businessNumber ?? profile?.businessRegistrationNo,
    expiresAt: offer.expiresAt,
    ...extras,
  };
}

function isNativeReject(result: unknown) {
  return result === "reject";
}

function isNativeCancel(result: unknown, ok?: boolean) {
  return ok === false || result === "cancel";
}

export default function MobileVcIssuePage() {
  const router = useRouter();
  const [step, setStep] = useState<IssueStep>("qr");
  const [error, setError] = useState<string | null>(null);
  const [runKey, setRunKey] = useState(0);
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const run = async () => {
      try {
        if (!isBridgeAvailable()) {
          throw new Error("앱 내부 발급 모듈에 연결할 수 없습니다.");
        }

        const scan = mSession.readScanResult();
        if (!scan?.qrData) {
          debugMissingScanResult();
          throw new Error("스캔한 QR 정보가 없습니다. 다시 스캔해주세요.");
        }

        setStep("qr");
        const parsedQr = parseCredentialOfferQr(scan.qrData);
        const resolved = await mobileVp.resolveQr(scan.qrData);
        if (resolved.type !== QR_TYPE.CREDENTIAL_OFFER) {
          throw new Error("지원하지 않는 QR입니다.");
        }

        const offerId = resolveOfferId(resolved.offerId, resolved.targetId);
        if (!offerId) {
          throw new Error("지원하지 않는 QR입니다.");
        }

        setStep("offer");
        const offer = await mobileWallet.offer(offerId);
        if (offer.alreadySaved) {
          throw new Error("이미 지갑에 저장된 증명서입니다.");
        }

        setStep("wallet");
        const wallet = await bridge.getWalletInfo();
        const holderDid = requiredString(wallet.did);
        const holderXrplAddress = requiredString(wallet.account);
        if (!wallet.ok || !holderDid || !holderXrplAddress) {
          throw new Error("활성화된 지갑을 찾을 수 없습니다. 지갑을 먼저 생성해주세요.");
        }

        const device = await bridge.getDeviceInfo();
        const deviceId = requiredString(device.deviceId);
        if (!device.ok || !deviceId) {
          throw new Error("기기 정보를 확인할 수 없습니다. 앱을 업데이트한 뒤 다시 시도해주세요.");
        }

        setStep("device");
        await registerMobileDevice({
          deviceId,
          deviceName: device.deviceName,
          os: device.os,
          appVersion: device.appVersion,
          publicKey: device.publicKey,
        });

        const confirm = await bridge.requestCredentialIssueConfirm(
          buildIssueDisplayPayload(offer, {
            offerId,
            did: holderDid,
            holderXrplAddress,
          }),
        );
        if (isNativeReject(confirm.result) || isNativeCancel(confirm.result, confirm.ok)) {
          mSession.writeScanResult(null);
          router.replace("/m/home");
          return;
        }
        if (confirm.result !== "confirm") {
          throw new Error(confirm.error ?? "증명서 발급 확인이 취소되었습니다.");
        }

        setStep("prepare");
        const prepared = await mobileWallet.prepare(offerId, {
          qrToken: parsedQr.qrToken,
          deviceId,
          holderDid,
          holderXrplAddress,
          accepted: true,
        });
        if (!prepared.prepared || !prepared.credentialPayload) {
          throw new Error("증명서 발급 준비에 실패했습니다.");
        }

        const metadata = prepared.credentialPayload.metadata ?? {};
        const issuerAccount = requiredString(metadata.issuerAccount);
        const credentialType = requiredString(metadata.credentialType) ?? "KYC_CREDENTIAL";
        const holderAccount =
          requiredString(metadata.holderXrplAddress) ?? holderXrplAddress;
        if (!issuerAccount) {
          throw new Error("블록체인 기록에 필요한 발급자 정보가 없습니다.");
        }

        setStep("save");
        const saveResult = await bridge.saveVC(
          buildSavePayload(prepared.credentialId, prepared.credentialPayload),
        );
        if (!saveResult.ok) {
          throw new Error(saveResult.error ?? "증명서를 지갑에 저장하지 못했습니다.");
        }

        setStep("xrpl");
        const submitResult = await bridge.submitToXRPL({
          credentialId: String(prepared.credentialId),
          issuerAccount,
          holderAccount,
          credentialType,
        });
        if (!submitResult.ok) {
          throw new Error(submitResult.error ?? "블록체인 기록에 실패했습니다.");
        }

        setStep("status");
        const statusResult = await bridge.checkCredentialStatus({
          credentialId: String(prepared.credentialId),
          issuerAccount,
          holderAccount,
          credentialType,
        });
        if (!statusResult.ok || statusResult.accepted === false) {
          throw new Error("블록체인 기록 상태를 확인하지 못했습니다.");
        }

        const walletSavedAt = new Date().toISOString();
        const txHash =
          submitResult.txHash ??
          submitResult.credentialAcceptHash ??
          statusResult.txHash ??
          statusResult.credentialAcceptHash;

        setStep("confirm");
        const confirmed = await mobileWallet.confirm(offerId, {
          credentialId: prepared.credentialId,
          deviceId,
          walletSaved: true,
          walletSavedAt,
          ...(txHash ? { credentialAcceptHash: txHash } : {}),
        });
        if (!confirmed.walletSaved) {
          throw new Error("저장 완료 처리에 실패했습니다.");
        }

        void bridge.getCredentialSummaries().catch(() => null);

        mSession.writeVcIssueResult({
          credentialId: prepared.credentialId,
          offerId,
          issuerName: "KYvC 인증기관",
          issuerDid: metadata.issuerDid ?? offer.issuerDid ?? undefined,
          credentialType,
          credentialTitle: credentialTitle(offer),
          issuedAt: metadata.issuedAt ?? confirmed.walletSavedAt ?? walletSavedAt,
          expiresAt: metadata.expiresAt ?? offer.expiresAt ?? undefined,
          txHash,
          credentialStatus: confirmed.credentialStatus,
          savedAt: confirmed.walletSavedAt ?? walletSavedAt,
          receivedAt: Date.now(),
        });
        mSession.writeScanResult(null);
        router.replace("/m/vc/celebration");
      } catch (e) {
        if (e instanceof ApiError) {
          setError(apiErrorMessage(e));
          return;
        }
        setError(e instanceof Error ? e.message : "증명서 발급에 실패했습니다.");
      }
    };

    run();
  }, [router, runKey]);

  const retry = () => {
    startedRef.current = false;
    setError(null);
    setStep("qr");
    setRunKey((value) => value + 1);
  };

  return (
    <section className="view wash">
      <MTopBar title="증명서 발급" back="/m/home" />
      <div className="content scroll center">
        {error ? (
          <>
            <h1 className="headline m-auth-title mt-24">증명서 발급에 실패했습니다.</h1>
            <p className="m-error mt-16">{error}</p>
          </>
        ) : (
          <>
            <div className="m-loading mt-24">{STEP_TEXT[step]}</div>
            <h1 className="headline m-auth-title mt-24">증명서를 발급하고 있어요...</h1>
            <p className="subcopy">잠시만 기다려주세요.</p>
          </>
        )}
      </div>
      {error ? (
        <div className="bottom-action">
          <button type="button" className="primary" onClick={() => router.replace("/m/home")}>
            홈으로
          </button>
          <button type="button" className="ghost" onClick={retry}>
            다시 시도
          </button>
        </div>
      ) : null}
    </section>
  );
}

function credentialTitle(offer: WalletCredentialOfferResponse) {
  if (offer.credentialTypeCode === "KYC_CREDENTIAL") return "법인등록증명서";
  return offer.credentialTypeCode ?? "법인등록증명서";
}
