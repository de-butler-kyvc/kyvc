"use client";

import { useEffect, useState } from "react";

import { MBottomNav, MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { MOCK_TRANSACTIONS } from "@/lib/m/data";

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
  const [rows, setRows] = useState<readonly Row[]>(MOCK_TRANSACTIONS);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    (async () => {
      try {
        const r = await bridge.getWalletTransactions(20);
        if (!r.ok) {
          setError(r.error ?? "거래내역 조회 실패");
          return;
        }
        const txs = (r.transactions as BridgeTx[] | undefined) ?? [];
        if (txs.length) {
          setRows(
            txs.map(
              (t) =>
                [
                  t.direction === "outgoing"
                    ? "↗"
                    : t.direction === "incoming"
                      ? "↓"
                      : "·",
                  `${t.direction === "outgoing" ? "송금" : t.direction === "incoming" ? "수신" : "기타"} ${t.amountXrp ?? ""} XRP`,
                  `${(t.hash ?? "").slice(0, 16)}... · ${(t.dateUtc ?? "").replace("T", " ").slice(0, 16)}`,
                ] as Row,
            ),
          );
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : "브리지 호출 실패");
      }
    })();
  }, []);

  return (
    <section className="view wash">
      <MTopBar title="거래 내역" back="/m/home" />
      <div className="list scroll">
        {error ? <p className="m-error">{error}</p> : null}
        {rows.map(([mark, title, sub], i) => (
          <div key={i} className="m-row">
            <div className="m-row-icon">{mark}</div>
            <div className="m-row-body">
              <div className="m-row-title">{title}</div>
              <div className="m-row-sub">{sub}</div>
            </div>
          </div>
        ))}
      </div>
      <MBottomNav active="transactions" />
    </section>
  );
}
