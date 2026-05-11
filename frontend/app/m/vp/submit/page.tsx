"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import {
  ApiError,
  mobileVp,
  type VpRequestResponse,
} from "@/lib/api";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import {
  nativeCredentialIssuer,
  nativeCredentialStatus,
  nativeCredentialTitle,
} from "@/lib/m/credential-summaries";
import { mSession, type ScanResult } from "@/lib/m/session";

type SubmitCredential = {
  credentialId: string;
  title: string;
  issuer: string;
  status: string;
  gradient: string;
};

const PALETTES = [
  "linear-gradient(135deg, #a5b4fc, #818cf8)",
  "linear-gradient(135deg, #d8b4fe, #c084fc)",
  "linear-gradient(135deg, #99f6e4, #38bdf8)",
];

export default function MobileVpSubmitPage() {
  const router = useRouter();
  const [scan, setScan] = useState<ScanResult | null>(null);
  const [request, setRequest] = useState<VpRequestResponse | null>(null);
  const [vcList, setVcList] = useState<SubmitCredential[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const nextScan = mSession.readScanResult();
    setScan(nextScan);
    let cancelled = false;
    (async () => {
      try {
        if (nextScan?.requestId) {
          const [req, eligible] = await Promise.all([
            mobileVp.request(nextScan.requestId),
            mobileVp.eligibleCredentials(nextScan.requestId),
          ]);
          if (cancelled) return;
          setRequest(req);
          const list = eligible.credentials.map((c, i) => ({
            credentialId: String(c.credentialId),
            title: c.credentialTypeCode ?? "법인 증명서",
            issuer: c.issuerDid?.split(":").slice(-1)[0] ?? "Issuer",
            status: "유효",
            gradient: PALETTES[i % PALETTES.length]!,
          }));
          setVcList(list);
          setSelected(new Set(list.map((c) => c.credentialId)));
          return;
        }

        if (!isBridgeAvailable()) {
          if (!cancelled) {
            setVcList([]);
            setError("제출할 증명서는 KYvC 앱 지갑에서 확인할 수 있습니다.");
          }
          return;
        }

        const list = await bridge.getCredentialSummaries();
        if (cancelled) return;
        const mapped = (list.credentials ?? []).map((c, i) => ({
          credentialId: c.credentialId,
          title: nativeCredentialTitle(c),
          issuer: nativeCredentialIssuer(c),
          status: nativeCredentialStatus(c),
          gradient: PALETTES[i % PALETTES.length]!,
        }));
        setVcList(mapped);
        setSelected(new Set(mapped.map((c) => c.credentialId)));
      } catch (e) {
        if (cancelled) return;
        setError(
          e instanceof ApiError
            ? `제출 가능 증명서 조회 실패: ${e.message}`
            : "제출 가능 증명서를 불러오지 못했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const toggle = (credentialId: string) =>
    setSelected((p) => {
      const n = new Set(p);
      if (n.has(credentialId)) n.delete(credentialId);
      else n.add(credentialId);
      return n;
    });

  const verifierName = (() => {
    if (request?.requesterName) return request.requesterName;
    if (!scan?.endpoint && !scan?.domain) return "신한은행";
    try {
      const url = new URL(scan.endpoint ?? scan.domain ?? "");
      return url.hostname.replace(/^www\./, "");
    } catch {
      return scan.domain ?? "신한은행";
    }
  })();

  const onSubmit = () => {
    const [first] = Array.from(selected);
    if (!first) return;
    mSession.writeSelectedVcId(String(first));
    if (request?.requestId) {
      mSession.writeVpRequest({
        nonce: request.nonce ?? request.challenge ?? "",
        aud: scan?.domain ?? scan?.coreBaseUrl ?? "",
        endpoint: scan?.endpoint ?? "",
        credentialId: String(first),
        receivedAt: Date.now(),
      });
    }
    router.push("/m/vp/submitting");
  };

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
                {request?.purpose ??
                  (scan?.actionType === "VP_REQUEST"
                    ? "법인계좌 개설용 인증"
                    : "VP 제출 요청")}
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
                {request?.requestId ? (
                  <>
                    <br />
                    request: {request.requestId}
                  </>
                ) : null}
              </p>
            </div>
          </div>
        ) : null}

        <div className="sb-title mt-24">
          제출할 증명서 ({vcList.length}건)
        </div>
        <div className="submit-vc-list">
          {loading ? <p className="m-loading">불러오는 중...</p> : null}
          {error ? <p className="m-error">{error}</p> : null}
          {!loading && !error && vcList.length === 0 ? (
            <p className="subcopy">제출 가능한 증명서가 없습니다.</p>
          ) : null}
          {vcList.map((vc) => (
            <div
              key={vc.credentialId}
              className="submit-vc-card"
              onClick={() => toggle(vc.credentialId)}
            >
              <div className="sv-icon" style={{ background: vc.gradient }}>
                <div className="sv-badge">{vc.status}</div>
              </div>
              <div className="sv-info">
                <h4>{vc.title}</h4>
                <p>{vc.issuer}</p>
              </div>
              <div className={`sv-check${selected.has(vc.credentialId) ? " active" : ""}`}>
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
            <span>앱 지갑 기준</span>
          </div>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          disabled={selected.size === 0}
          onClick={onSubmit}
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
