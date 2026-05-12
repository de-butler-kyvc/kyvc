"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef } from "react";

import {
  mobileVp,
  type EligibleCredentialResponse,
  type VpRequestResponse,
} from "@/lib/api";
import {
  bridge,
  isBridgeAvailable,
  type NativeCredentialSummary,
} from "@/lib/m/android-bridge";
import {
  nativeCredentialIssuer,
  nativeCredentialTitle,
} from "@/lib/m/credential-summaries";
import { mSession, type ScanResult } from "@/lib/m/session";

type SubmitTarget = {
  credentialId: string;
  issuerName?: string;
  credentialTitle?: string;
  expiresAt?: string;
};

function verifierName(scan: ScanResult | null, request: VpRequestResponse | null) {
  if (request?.requesterName) return request.requesterName;
  if (!scan?.endpoint && !scan?.domain) return "신한은행";
  try {
    const url = new URL(scan.endpoint ?? scan.domain ?? "");
    return url.hostname.replace(/^www\./, "");
  } catch {
    return scan?.domain ?? "신한은행";
  }
}

function apiCredentialToTarget(credential: EligibleCredentialResponse): SubmitTarget {
  return {
    credentialId: String(credential.credentialId),
    issuerName: credential.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
    credentialTitle: credential.credentialTypeCode ?? "법인 증명서",
    expiresAt: credential.expiresAt,
  };
}

function nativeCredentialToTarget(credential: NativeCredentialSummary): SubmitTarget {
  return {
    credentialId: credential.credentialId,
    issuerName: nativeCredentialIssuer(credential),
    credentialTitle: nativeCredentialTitle(credential),
    expiresAt: credential.expiresAt,
  };
}

export default function MobileVpSubmitPage() {
  const router = useRouter();
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    (async () => {
      const scan = mSession.readScanResult();
      if (!isBridgeAvailable()) {
        router.replace("/m/home");
        return;
      }

      try {
        let request: VpRequestResponse | null = null;
        let target: SubmitTarget | null = null;

        if (scan?.requestId) {
          const [req, eligible] = await Promise.all([
            mobileVp.request(scan.requestId),
            mobileVp.eligibleCredentials(scan.requestId),
          ]);
          request = req;
          const [first] = eligible.credentials;
          target = first ? apiCredentialToTarget(first) : null;
        } else {
          const summaries = await bridge.getCredentialSummaries();
          const [first] = summaries.credentials ?? [];
          target = first ? nativeCredentialToTarget(first) : null;
        }

        if (!target) {
          mSession.writeScanResult(null);
          router.replace("/m/home");
          return;
        }

        const result = await bridge.requestCredentialSubmit({
          requesterName: verifierName(scan, request),
          credentialId: target.credentialId,
          credentialTitle: target.credentialTitle,
          issuerName: target.issuerName,
          expiresAt: target.expiresAt,
        });

        if (result.ok && result.result === "submit") {
          mSession.writeSelectedVcId(target.credentialId);
          if (request?.requestId) {
            mSession.writeVpRequest({
              nonce: request.nonce ?? request.challenge ?? "",
              aud: scan?.domain ?? scan?.coreBaseUrl ?? "",
              endpoint: scan?.endpoint ?? "",
              credentialId: target.credentialId,
              receivedAt: Date.now(),
            });
          }
          router.replace("/m/vp/submitting");
          return;
        }

        mSession.writeScanResult(null);
        mSession.writeVpRequest(null);
        mSession.writeSelectedVcId(null);
        router.replace("/m/home");
      } catch {
        router.replace("/m/home");
      }
    })();
  }, [router]);

  return null;
}
