"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo } from "react";

import { MIcon } from "@/components/m/icons";
import { MCertCard, MTopBar } from "@/components/m/parts";
import { MOCK_CERTS } from "@/lib/m/data";

function MobileVcDetailInner() {
  const router = useRouter();
  const sp = useSearchParams();
  const id = sp.get("id");
  const cert = useMemo(
    () => MOCK_CERTS.find((c) => c.id === id) ?? MOCK_CERTS[0]!,
    [id],
  );
  return (
    <section className="view wash">
      <MTopBar title="VC 상세" back="/m/home" />
      <div className="scroll">
        <MCertCard cert={cert} index={0} extra="flat-card" />
        <div className="list">
          <div className="m-row">
            <div className="m-row-icon">발</div>
            <div className="m-row-body">
              <div className="m-row-title">발급기관</div>
              <div className="m-row-sub">{cert.issuer}</div>
            </div>
          </div>
          <div className="m-row">
            <div className="m-row-icon">기</div>
            <div className="m-row-body">
              <div className="m-row-title">유효기간</div>
              <div className="m-row-sub">{cert.date} - +1년</div>
            </div>
          </div>
          <div className="m-row">
            <div className="m-row-icon green">
              <MIcon.check />
            </div>
            <div className="m-row-body">
              <div className="m-row-title">상태</div>
              <div className="m-row-sub">정상 · 검증 가능</div>
            </div>
          </div>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() =>
            router.push(`/m/vc/qr?id=${encodeURIComponent(cert.id)}`)
          }
        >
          내 QR 보기
        </button>
      </div>
    </section>
  );
}

export default function MobileVcDetailPage() {
  return (
    <Suspense fallback={<div className="m-loading">불러오는 중…</div>}>
      <MobileVcDetailInner />
    </Suspense>
  );
}
