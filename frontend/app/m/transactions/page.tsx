"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MBottomNav, MTopBar } from "@/components/m/parts";
import {
  bridge,
  isBridgeAvailable,
  type WalletTransactionSummary,
} from "@/lib/m/android-bridge";

type ActivityTab = "all" | "vc" | "vp" | "warn" | "tx";

type ActivityItem = {
  id: string;
  cat: Exclude<ActivityTab, "all">;
  icon: "check" | "shield" | "bell" | "cert" | "xrpReceive" | "xrpSend";
  title: string;
  desc: string;
  time: string;
  group: string;
  unread?: boolean;
};

const TABS: Array<{ key: ActivityTab; label: string }> = [
  { key: "all", label: "전체" },
  { key: "vc", label: "증명서 발급" },
  { key: "vp", label: "증명서 제출" },
  { key: "warn", label: "보안/경고" },
  { key: "tx", label: "거래 내역" },
];

function ActivityIcon({ name }: { name: ActivityItem["icon"] }) {
  if (name === "check") return <MIcon.check />;
  if (name === "shield") return <MIcon.shield />;
  if (name === "bell") return <MIcon.bell />;
  if (name === "xrpReceive") return <MIcon.arrowDown />;
  if (name === "xrpSend") return <MIcon.arrowUpRight />;
  return <MIcon.cert />;
}

function formatTxTime(dateUtc?: string) {
  if (!dateUtc) return "";
  const date = new Date(dateUtc);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit",
  });
}

function formatTxGroup(dateUtc?: string) {
  if (!dateUtc) return "최근";
  const date = new Date(dateUtc);
  if (Number.isNaN(date.getTime())) return "최근";
  const today = new Date();
  if (date.toDateString() === today.toDateString()) return "오늘";
  return date.toLocaleDateString("ko-KR", {
    month: "long",
    day: "numeric",
  });
}

function txToActivity(tx: WalletTransactionSummary, index: number): ActivityItem {
  const incoming = tx.direction === "incoming";
  const amount = tx.amountXrp ? `${tx.amountXrp} XRP` : "XRP";
  const result = tx.result ? ` · ${tx.result}` : "";
  const fee = tx.feeXrp && !incoming ? ` · 수수료 ${tx.feeXrp} XRP` : "";

  return {
    id: tx.hash ?? `tx-${index}`,
    cat: "tx",
    icon: incoming ? "xrpReceive" : "xrpSend",
    title: incoming ? `${amount} 받음` : `${amount} 보냄`,
    desc: `${tx.transactionType ?? "Transaction"}${result}${fee}`,
    time: formatTxTime(tx.dateUtc),
    group: formatTxGroup(tx.dateUtc),
  };
}

export default function MobileTransactionsPage() {
  const [tab, setTab] = useState<ActivityTab>("all");
  const [nativeTx, setNativeTx] = useState<ActivityItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    bridge
      .getWalletTransactions(20)
      .then((r) => {
        if (!r.ok) {
          setError(r.error ?? "거래 내역을 가져올 수 없습니다.");
          return;
        }
        setNativeTx((r.transactions ?? []).map(txToActivity));
        setError(null);
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : "거래 내역 조회에 실패했습니다.");
      });
  }, []);

  const visible = tab === "all" || tab === "tx" ? nativeTx : [];
  const groupNames = Array.from(new Set(visible.map((item) => item.group)));
  const groups = groupNames
    .map((group) => ({
      group,
      items: visible.filter((item) => item.group === group),
    }))
    .filter((group) => group.items.length > 0);

  return (
    <section className="view wash activity-view">
      <MTopBar title="활동" back={false} />

      <div className="activity-tabs" aria-label="활동 필터">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={tab === item.key ? "active" : ""}
            onClick={() => setTab(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="scroll activity-scroll">
        {error ? <p className="m-error">{error}</p> : null}
        {groups.length === 0 ? (
          <p className="activity-empty">해당하는 활동이 없습니다.</p>
        ) : (
          groups.map((group) => (
            <section key={group.group} className="activity-group">
              <h2>{group.group}</h2>
              <div className="activity-list">
                {group.items.map((item) => (
                  <article
                    key={item.id}
                    className={`activity-item${item.unread ? " unread" : ""}`}
                  >
                    <div className={`activity-icon ${item.icon}`}>
                      <ActivityIcon name={item.icon} />
                    </div>
                    <div className="activity-body">
                      <div className="activity-title-row">
                        <strong>{item.title}</strong>
                        <time>{item.time}</time>
                      </div>
                      <p>{item.desc}</p>
                    </div>
                    {item.unread ? <span className="activity-dot" /> : null}
                  </article>
                ))}
              </div>
            </section>
          ))
        )}
      </div>

      <MBottomNav active="transactions" />
    </section>
  );
}
