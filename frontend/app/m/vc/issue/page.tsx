"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import {
  ApiError,
  mobileDevice,
  mobileVp,
  mobileWallet,
  type WalletCredentialOfferResponse,
  type WalletCredentialPayload,
  type WalletCredentialPrepareRequest,
  type WalletCredentialPrepareResponse,
} from "@/lib/api";
import { bridge, isBridgeAvailable, type SaveVcPayload } from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

const QR_TYPE = {
  CREDENTIAL_OFFER: "CREDENTIAL_OFFER",
} as const;
const SCAN_RESULT_KEY = "kyvc.m.scanResult";
const PAYLOAD_NOT_REPLAYABLE_MESSAGE =
  "증명서 발급 응답은 재전송할 수 없습니다. PC 화면에서 QR을 다시 생성해 주세요.";
const preparedCredentialByOffer = new Map<number, WalletCredentialPrepareResponse>();
const preparingCredentialByOffer = new Map<
  number,
  Promise<WalletCredentialPrepareResponse>
>();

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
  if (error.code === "WALLET_CREDENTIAL_PAYLOAD_NOT_REPLAYABLE") {
    return PAYLOAD_NOT_REPLAYABLE_MESSAGE;
  }
  if (error.code === "WALLET_DEVICE_NOT_REGISTERED") {
    return "Wallet 기기 등록 정보를 찾을 수 없습니다. 앱을 다시 실행한 뒤 재시도해주세요.";
  }
  return error.message;
}

function requiredString(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function isXrplClassicAddress(value: string | null) {
  return Boolean(value && /^r[1-9A-HJ-NP-Za-km-z]{24,34}$/.test(value));
}

function xrplClassicAddressFrom(value: unknown) {
  const text = requiredString(value);
  if (!text) return null;
  if (isXrplClassicAddress(text)) return text;

  const lastDidPart = text.startsWith("did:xrpl:")
    ? text.split(":").at(-1) ?? null
    : null;
  return isXrplClassicAddress(lastDidPart) ? lastDidPart : null;
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function preferredNativeAuthMethod(methods?: ("pin" | "pattern" | "biometric")[]) {
  if (methods?.includes("biometric")) return "biometric";
  if (methods?.includes("pin")) return "pin";
  if (methods?.includes("pattern")) return "pattern";
  return null;
}

async function ensureWalletSession() {
  const status = await bridge.getAuthStatus();
  if (!status.ok) {
    throw new Error(status.error ?? "지갑 인증 상태를 확인할 수 없습니다.");
  }
  if (status.emailVerificationRequired) {
    throw new Error("이메일 인증이 필요합니다. 인증을 완료한 뒤 다시 시도해주세요.");
  }
  if (status.sessionUnlocked) return;

  const method = preferredNativeAuthMethod(status.availableMethods);
  if (!method) {
    throw new Error("사용 가능한 네이티브 인증 수단이 없습니다.");
  }

  const auth = await bridge.requestNativeAuth(method, "wallet-login");
  if (!auth.ok || !auth.authenticated) {
    throw new Error(auth.error ?? "네이티브 인증에 실패했습니다.");
  }
}

async function waitForCredentialAccepted(params: {
  credentialId: string;
  issuerAccount: string;
  holderAccount: string;
  credentialType: string;
}) {
  let lastResult: Awaited<ReturnType<typeof bridge.checkCredentialStatus>> | null = null;

  for (let attempt = 0; attempt < 6; attempt += 1) {
    if (attempt > 0) await wait(1500);
    lastResult = await bridge.checkCredentialStatus(params);
    if (lastResult.ok && lastResult.accepted !== false) return lastResult;
  }

  const hint = lastResult?.error ? ` ${lastResult.error}` : "";
  throw new Error(`블록체인 기록 상태를 확인하지 못했습니다.${hint}`);
}

function credentialStringFromPayload(payload: WalletCredentialPayload) {
  const record = payload as Record<string, unknown>;
  const format = requiredString(record.format);

  if (format === "dc+sd-jwt") {
    return (
      requiredString(record.sdJwt) ??
      requiredString(record.credentialJwt) ??
      requiredString(record.credential) ??
      ""
    );
  }

  if (format === "vc+jwt") {
    return (
      requiredString(record.credentialJwt) ??
      requiredString(record.vcJwt) ??
      requiredString(record.credential) ??
      ""
    );
  }

  return (
    requiredString(record.sdJwt) ??
    requiredString(record.credentialJwt) ??
    requiredString(record.credential) ??
    ""
  );
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
): SaveVcPayload {
  const metadata = credentialPayload.metadata ?? {};
  const format = requiredString(credentialPayload.format);
  const credential = credentialStringFromPayload(credentialPayload);

  if (!format) {
    throw new Error("Credential format이 없습니다.");
  }

  if (!credential) {
    throw new Error("저장할 Credential payload가 없습니다.");
  }

  return {
    credentialId: String(credentialId),
    format,
    credential,
    sdJwt: format === "dc+sd-jwt" ? credential : undefined,
    vcJwt: format === "vc+jwt" ? credential : undefined,
    metadata,
    selectiveDisclosure: credentialPayload.selectiveDisclosure,
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

async function prepareCredentialOnce(
  offerId: number,
  payload: WalletCredentialPrepareRequest,
) {
  const cached = preparedCredentialByOffer.get(offerId);
  if (cached) return cached;

  const pending = preparingCredentialByOffer.get(offerId);
  if (pending) return pending;

  const pendingPrepare = mobileWallet
    .prepare(offerId, payload)
    .then((prepared) => {
      if (!prepared.prepared || !prepared.credentialPayload) {
        throw new Error("증명서 발급 준비에 실패했습니다.");
      }

      preparedCredentialByOffer.set(offerId, prepared);
      return prepared;
    })
    .finally(() => {
      preparingCredentialByOffer.delete(offerId);
    });

  preparingCredentialByOffer.set(offerId, pendingPrepare);
  return pendingPrepare;
}

export default function MobileVcIssuePage() {
  const router = useRouter();
  const [step, setStep] = useState<IssueStep>("qr");
  const [error, setError] = useState<string | null>(null);
  const [payloadDelivered, setPayloadDelivered] = useState(false);
  const [runKey, setRunKey] = useState(0);
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const run = async () => {
      let deliveredPayload = false;
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
        await ensureWalletSession();
        const wallet = await bridge.getWalletInfo();
        if (!wallet.ok) {
          throw new Error(wallet.error ?? "지갑 정보를 확인할 수 없습니다. 다시 인증한 뒤 시도해주세요.");
        }

        let holderDid = requiredString(wallet.holderDid ?? wallet.did);
        let holderXrplAddress = requiredString(
          wallet.holderAccount ?? wallet.account,
        );

        if (!holderDid || !holderXrplAddress) {
          const did = await bridge.checkHolderDidSet().catch(() => null);
          if (did?.ok) {
            holderDid = holderDid ?? requiredString(did.holderDid ?? did.did);
            holderXrplAddress =
              holderXrplAddress ??
              requiredString(did.holderAccount ?? did.account);
          }
        }

        if (!holderXrplAddress) {
          throw new Error("지갑을 찾을 수 없습니다. 지갑을 먼저 생성해주세요.");
        }
        if (!holderDid) {
          throw new Error("DID 등록이 필요합니다. 홈에서 DID 등록을 완료한 뒤 다시 시도해주세요.");
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
        const prepared = await prepareCredentialOnce(offerId, {
          qrToken: parsedQr.qrToken,
          deviceId,
          holderDid,
          holderXrplAddress,
          accepted: true,
        });

        deliveredPayload = true;
        setPayloadDelivered(true);

        const metadata = prepared.credentialPayload.metadata ?? {};
        const credentialType = requiredString(metadata.credentialType) ?? "KYC_CREDENTIAL";
        const holderAccount =
          requiredString(metadata.holderXrplAddress) ?? holderXrplAddress;

        setStep("save");
        const savePayload = buildSavePayload(
          prepared.credentialId,
          prepared.credentialPayload,
        );
        const saveResult = await bridge.saveVC(savePayload);
        if (!saveResult.ok) {
          throw new Error(saveResult.error ?? "증명서를 지갑에 저장하지 못했습니다.");
        }
        const issuerAccount =
          xrplClassicAddressFrom(saveResult.issuerAccount) ??
          xrplClassicAddressFrom(metadata.issuerAccount) ??
          xrplClassicAddressFrom(metadata.issuerDid) ??
          xrplClassicAddressFrom(offer.issuerDid);
        if (!issuerAccount) {
          throw new Error("블록체인 기록에 필요한 실제 발급자 XRPL 주소가 없습니다.");
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
        const statusResult = await waitForCredentialAccepted({
          credentialId: String(prepared.credentialId),
          issuerAccount,
          holderAccount,
          credentialType,
        });

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
        preparedCredentialByOffer.delete(offerId);
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
    setPayloadDelivered(false);
    setStep("qr");
    setRunKey((value) => value + 1);
  };

  return (
    <section className="view wash mobile-process-view">
      <MTopBar title="증명서 발급" back="/m/home" />
      <div className="content scroll mobile-process-content">
        {error ? (
          <section className="process-panel error">
            <div className="process-icon error">
              <MIcon.x />
            </div>
            <span className="process-eyebrow">발급 실패</span>
            <h1>증명서 발급에 실패했습니다</h1>
            <p>{error}</p>
          </section>
        ) : (
          <section className="process-panel">
            <div className="process-orbit" aria-hidden="true">
              <span />
              <MIcon.cert />
            </div>
            <span className="process-eyebrow">KYvC Wallet</span>
            <h1>증명서를 발급하고 있어요</h1>
            <p>{STEP_TEXT[step]}</p>
            <div className="process-steps" aria-label="증명서 발급 진행 상태">
              {(["qr", "offer", "wallet", "device", "prepare", "save", "xrpl", "status", "confirm"] as IssueStep[]).map((item) => (
                <span
                  key={item}
                  className={item === step ? "active" : ""}
                  aria-current={item === step ? "step" : undefined}
                />
              ))}
            </div>
          </section>
        )}
      </div>
      {error ? (
        <div className="bottom-action">
          <button type="button" className="primary" onClick={() => router.replace("/m/home")}>
            홈으로
          </button>
          {!payloadDelivered ? (
            <button type="button" className="ghost" onClick={retry}>
              다시 시도
            </button>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function credentialTitle(offer: WalletCredentialOfferResponse) {
  if (offer.credentialTypeCode === "KYC_CREDENTIAL") return "법인등록증명서";
  return offer.credentialTypeCode ?? "법인등록증명서";
}
