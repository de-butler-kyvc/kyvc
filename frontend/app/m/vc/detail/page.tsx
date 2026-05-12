"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef } from "react";

import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import {
  nativeCredentialIssuer,
  nativeCredentialTitle,
} from "@/lib/m/credential-summaries";

function MobileVcDetailInner() {
  const router = useRouter();
  const sp = useSearchParams();
  const id = sp.get("id");
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    (async () => {
      if (!id || !isBridgeAvailable()) {
        router.replace("/m/home");
        return;
      }

      try {
        const list = await bridge.getCredentialSummaries();
        const credential = (list.credentials ?? []).find(
          (item) => item.credentialId === id,
        );
        if (!credential) {
          router.replace("/m/home");
          return;
        }

        const result = await bridge.requestCredentialDetail({
          credentialId: credential.credentialId,
          issuerName: nativeCredentialIssuer(credential),
          credentialTitle: nativeCredentialTitle(credential),
          expiresAt: credential.expiresAt,
        });

        if (result.ok && result.result === "showQr") {
          router.replace(`/m/vc/qr?id=${encodeURIComponent(id)}`);
          return;
        }

        router.replace("/m/home");
      } catch {
        router.replace("/m/home");
      }
    })();
  }, [id, router]);

  return null;
}

export default function MobileVcDetailPage() {
  return (
    <Suspense fallback={null}>
      <MobileVcDetailInner />
    </Suspense>
  );
}
