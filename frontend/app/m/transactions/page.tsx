"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

type Tab = "all" | "in" | "out";
type XrpHistoryRow = {
  type: "in" | "out";
  address: string;
  amount: string;
  date: string;
};

type BridgeTx = {
  direction?: string;
  amountXrp?: string;
  dateUtc?: string;
  counterparty?: string;
  account?: string;
};

const FALLBACK_ROWS: XrpHistoryRow[] = [
  { type: "in", address: "rHb9CJA...tyTh", amount: "+12.50 XRP", date: "2026.05.08 14:32" },
  { type: "out", address: "rPT1Sjq...2M5y", amount: "-1.00 XRP", date: "2026.05.07 09:18" },
  { type: "in", address: "rN7n3ta...Kx8c", amount: "+5.25 XRP", date: "2026.05.06 17:45" },
  { type: "out", address: "rLHzPsX...9wQd", amount: "-2.50 XRP", date: "2026.05.04 11:03" },
  { type: "in", address: "rGWrZyR...M3kL", amount: "+30.00 XRP", date: "2026.05.01 08:55" },
  { type: "out", address: "rBjMVZF...7vNt", amount: "-0.50 XRP", date: "2026.04.29 20:11" },
];

function shorten(value?: string) {
  if (!value) return "rHb9CJA...tyTh";
  if (value.length <= 14) return value;
  return `${value.slice(0, 7)}...${value.slice(-4)}`;
}

export default function MobileTransactionsPage() {
  const [tab, setTab] = useState<Tab>("all");
  const [rows, setRows] = useState<XrpHistoryRow[]>(FALLBACK_ROWS);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    let cancelled = false;
    bridge
      .getWalletTransactions(20)
      .then((r) => {
        if (cancelled || !r.ok) return;
        const txs = (r.transactions as BridgeTx[] | undefined) ?? [];
        if (txs.length === 0) return;
        setRows(
          txs.map((tx) => {
            const outgoing = tx.direction === "outgoing";
            const amount = tx.amountXrp ?? "0";
            return {
              type: outgoing ? "out" : "in",
              address: shorten(tx.counterparty ?? tx.account),
              amount: `${outgoing ? "-" : "+"}${amount} XRP`,
              date: (tx.dateUtc ?? "").replace("T", " ").slice(0, 16),
            };
          }),
        );
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  const visible = rows.filter((row) => tab === "all" || row.type === tab);

  return (
    <section className="view xrp-flow-view">
      <MTopBar title="XRP 거래 내역" back="/m/home" />
      <div className="xrp-history-tabs">
        {[
          ["all", "전체"],
          ["in", "받기"],
          ["out", "보내기"],
        ].map(([key, label]) => (
          <button
            key={key}
            type="button"
            className={tab === key ? "active" : ""}
            onClick={() => setTab(key as Tab)}
          >
            {label}
          </button>
        ))}
      </div>
      <div className="scroll xrp-history-list">
        {visible.map((row, i) => (
          <div key={`${row.address}-${i}`} className="xrp-history-item">
            <div className={`xrp-history-icon ${row.type}`}>
              {row.type === "in" ? <MIcon.arrowDown /> : <MIcon.arrowUpRight />}
            </div>
            <div className="xrp-history-body">
              <strong>{row.type === "in" ? "받기" : "보내기"}</strong>
              <span>{row.address}</span>
            </div>
            <div className="xrp-history-right">
              <strong className={row.type === "in" ? "plus" : ""}>
                {row.amount}
              </strong>
              <span>{row.date}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
