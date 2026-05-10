"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { mSession, type ScanResult } from "@/lib/m/session";

const VC_LIST = [
  {
    title: "법인등록증명서",
    issuer: "법원행정처",
    gradient: "linear-gradient(135deg, #a5b4fc, #818cf8)",
  },
  {
    title: "사업자등록증",
    issuer: "국세청",
    gradient: "linear-gradient(135deg, #d8b4fe, #c084fc)",
  },
];

export default function MobileVpSubmitPage() {
  const router = useRouter();
  const [scan, setScan] = useState<ScanResult | null>(null);
  const [selected, setSelected] = useState<Set<number>>(
    () => new Set(VC_LIST.map((_, i) => i)),
  );

  useEffect(() => {
    setScan(mSession.readScanResult());
  }, []);

  const toggle = (i: number) =>
    setSelected((p) => {
      const n = new Set(p);
      if (n.has(i)) n.delete(i);
      else n.add(i);
      return n;
    });

  const verifierName = (() => {
    if (!scan?.endpoint && !scan?.domain) return "신한은행";
    try {
      const url = new URL(scan.endpoint ?? scan.domain ?? "");
      return url.hostname.replace(/^www\./, "");
    } catch {
      return scan.domain ?? "신한은행";
    }
  })();

  return (
    <section className="view wash">
      <MTopBar title="증명서 제출" back="/m/vp/scan" />
      <div className="content scroll">
        <div className="submit-box mt-16">
          <div className="sb-label">요청 기관</div>
          <div className="sb-org">
            <div className="sb-org-icon">
              {verifierName.charAt(0).toUpperCase()}
            </div>
            <div className="sb-org-info">
              <h3>{verifierName}</h3>
              <p>
                {scan?.actionType === "VP_REQUEST"
                  ? "법인계좌 개설용 인증"
                  : "VP 제출 요청"}
              </p>
            </div>
            <div className="sb-badge green">
              <MIcon.check /> 검증됨
            </div>
          </div>
        </div>

        {scan ? (
          <div className="m-info-box info-box mt-16">
            <div className="info-icon">
              <MIcon.shield />
            </div>
            <div className="info-text">
              <strong>요청 정보</strong>
              <p>
                challenge: {scan.challenge ?? "(서버에서 발급 예정)"}
                <br />
                aud/domain: {scan.domain ?? scan.coreBaseUrl ?? "-"}
              </p>
            </div>
          </div>
        ) : null}

        <div className="sb-title mt-24">
          제출할 증명서 ({VC_LIST.length}건)
        </div>
        <div className="submit-vc-list">
          {VC_LIST.map((vc, i) => (
            <div
              key={vc.title}
              className="submit-vc-card"
              onClick={() => toggle(i)}
            >
              <div className="sv-icon" style={{ background: vc.gradient }}>
                <div className="sv-badge">유효</div>
              </div>
              <div className="sv-info">
                <h4>{vc.title}</h4>
                <p>{vc.issuer}</p>
              </div>
              <div className={`sv-check${selected.has(i) ? " active" : ""}`}>
                <MIcon.check />
              </div>
            </div>
          ))}
        </div>

        <div className="consent-box mt-24">
          <div className="cb-icon">
            <MIcon.lock />
          </div>
          <div className="cb-info">
            <strong>제출 시 동의 사항</strong>
            <p>
              선택한 증명서가 {verifierName}에 제출되며, 블록체인에 제출 기록이 남습니다.
            </p>
          </div>
        </div>

        <div className="fee-box mt-16">
          <div className="fee-row">
            <span>수수료</span>
            <span>0.000012 XRP</span>
          </div>
          <div className="fee-row">
            <span>잔액</span>
            <span>12.48 XRP</span>
          </div>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          disabled={selected.size === 0}
          onClick={() => router.push("/m/vp/submitting")}
        >
          증명서 제출하기
        </button>
        <button
          type="button"
          className="ghost"
          onClick={() => {
            mSession.writeScanResult(null);
            router.replace("/m/home");
          }}
        >
          거부
        </button>
      </div>
    </section>
  );
}
