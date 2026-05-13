"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef } from "react";

import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

export default function MobileVcCelebrationPage() {
  const router = useRouter();
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    (async () => {
      const result = mSession.readVcIssueResult();
      if (!isBridgeAvailable()) {
        router.replace("/m/home");
        return;
      }

      try {
        const nativeResult = await bridge.requestCredentialIssueComplete({
          credentialId: result?.credentialId,
          issuerName: result?.issuerName,
          credentialTitle: result?.credentialTitle,
          expiresAt: result?.expiresAt,
          transactionHash: result?.txHash,
        });

        if (
          nativeResult.ok &&
          nativeResult.result === "viewCredential" &&
          result?.credentialId
        ) {
          router.replace(
            `/m/vc/detail?id=${encodeURIComponent(String(result.credentialId))}`,
          );
          return;
        }

        mSession.writeVcIssueResult(null);
        router.replace("/m/home");
      } catch {
        router.replace("/m/home");
      }
    })();
  }, [router]);

  return null;
}
