"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef } from "react";

import { scanPresentationQrAndNavigate } from "@/lib/m/qr-bridge";

export default function MobileVpScanPage() {
  const router = useRouter();
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;
    scanPresentationQrAndNavigate(router).catch(() => {
      router.replace("/m/home");
    });
  }, [router]);

  return null;
}
