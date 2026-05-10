"use client";

import { useEffect, useState } from "react";

import { MBottomNav, MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

type Row = readonly [string, string, string];

type BridgeTx = {
  hash?: string;
  direction?: string;
  amountXrp?: string;
  feeXrp?: string;
  result?: string;
  dateUtc?: string;
};

export default function MobileTransactionsPage() {
  const [rows, setRows] = useState<readonly Row[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isBridgeAvailable()) {
      setError(
        "앱 내부 지갑 모듈에 연결할 수 없습니다. KYvC 앱에서 다시 열어 주세요.",
      );
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const r = await bridge.getWalletTransactions(20);
        if (cancelled) return;
        if (!r.ok) {
          setError(r.error ?? "거래내역 조회 실패");
          return;
        }
        const txs = (r.transactions as BridgeTx[] | undefined) ?? [];
        setRows(
          txs.map(
            (t) =>
              [
                t.direction === "outgoing"
                  ? "↗"
                  : t.direction === "incoming"
                    ? "↓"
                    : "·",
                `${
                  t.direction === "outgoing"
                    ? "송금"
                    : t.direction === "incoming"
                      ? "수신"
                      : "기타"
                } ${t.amountXrp ?? ""} XRP`,
                `${(t.hash ?? "").slice(0, 16)}... · ${(t.dateUtc ?? "").replace("T", " ").slice(0, 16)}`,
              ] as Row,
          ),
        );
      } catch (e) {
        if (!cancelled)
          setError(
            e instanceof Error ? e.message : "거래내역 호출에 실패했습니다.",
          );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="view wash">
      <MTopBar title="거래 내역" back="/m/home" />
      <div className="list scroll">
        {error ? <p className="m-error">{error}</p> : null}
        {loading ? (
          <p className="m-loading">불러오는 중…</p>
        ) : rows.length === 0 && !error ? (
          <p
            className="subcopy"
            style={{ textAlign: "center", padding: "32px 0" }}
          >
            거래내역이 없습니다.
          </p>
        ) : (
          rows.map(([mark, title, sub], i) => (
            <div key={i} className="m-row">
              <div className="m-row-icon">{mark}</div>
              <div className="m-row-body">
                <div className="m-row-title">{title}</div>
                <div className="m-row-sub">{sub}</div>
              </div>
            </div>
          ))
        )}
      </div>
      <MBottomNav active="transactions" />
    </section>
  );
}
