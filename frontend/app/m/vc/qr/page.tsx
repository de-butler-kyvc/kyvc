"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { MTopBar } from "@/components/m/parts";
import { ApiError, credentials } from "@/lib/api";

function MobileVcQrInner() {
  const sp = useSearchParams();
  const id = sp.get("id");
  const credentialId = (() => {
    if (!id) return null;
    const m = id.match(/^urn:cred:(\d+)$/);
    return m ? Number(m[1]) : null;
  })();

  const [title, setTitle] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) {
      setError("증명서 ID가 없습니다.");
      setLoading(false);
      return;
    }
    if (credentialId == null) {
      setError("증명서 ID 형식이 잘못되었습니다.");
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const d = await credentials.detail(credentialId);
        if (cancelled) return;
        setTitle(d.credentialTypeCode ?? "법인 증명서");
      } catch (e) {
        if (cancelled) return;
        setError(
          e instanceof ApiError
            ? `VC 조회 실패: ${e.message}`
            : "VC 조회 중 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, credentialId]);

  return (
    <section className="view wash">
      <MTopBar
        title="내 QR 코드"
        back={
          id ? `/m/vc/detail?id=${encodeURIComponent(id)}` : "/m/home"
        }
      />
      <div className="content center">
        {loading ? (
          <p className="m-loading">불러오는 중…</p>
        ) : error ? (
          <p className="m-error">{error}</p>
        ) : (
          <div className="qr-panel">
            <h2>{title}</h2>
            <div className="qr-box" />
            <p>3분간 유효한 일회용 QR</p>
          </div>
        )}
      </div>
      <div className="bottom-action">
        <button type="button" className="secondary" disabled={!title}>
          이미지 저장
        </button>
        <button type="button" className="primary" disabled={!title}>
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
