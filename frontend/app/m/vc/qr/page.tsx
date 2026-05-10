"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useMemo } from "react";

import { MTopBar } from "@/components/m/parts";
import { MOCK_CERTS } from "@/lib/m/data";

function MobileVcQrInner() {
  const sp = useSearchParams();
  const id = sp.get("id");
  const cert = useMemo(
    () => MOCK_CERTS.find((c) => c.id === id) ?? MOCK_CERTS[0]!,
    [id],
  );
  return (
    <section className="view wash">
      <MTopBar
        title="내 QR 코드"
        back={`/m/vc/detail?id=${encodeURIComponent(cert.id)}`}
      />
      <div className="content center">
        <div className="qr-panel">
          <h2>{cert.title}</h2>
          <div className="qr-box" />
          <p>3분간 유효한 일회용 QR</p>
        </div>
      </div>
      <div className="bottom-action">
        <button type="button" className="secondary">
          이미지 저장
        </button>
        <button type="button" className="primary">
          공유하기
        </button>
      </div>
    </section>
  );
}

export default function MobileVcQrPage() {
  return (
    <Suspense fallback={<div className="m-loading">불러오는 중…</div>}>
      <MobileVcQrInner />
    </Suspense>
  );
}
