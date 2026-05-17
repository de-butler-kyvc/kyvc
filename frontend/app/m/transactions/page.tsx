"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MBottomNav, MTopBar } from "@/components/m/parts";
import {
  bridge,
  isBridgeAvailable,
  type WalletActivitySummary,
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
  sortAt: number;
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

function sortTime(value?: string) {
  if (!value) return 0;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
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
    sortAt: sortTime(tx.dateUtc),
  };
}

function walletActivityToItem(
  activity: WalletActivitySummary,
  index: number,
): ActivityItem {
  const issued = activity.type === "VC_ISSUED";
  const credentialType = activity.credentialType ?? "증명서";
  const fallbackTitle = issued
    ? `${credentialType} 발급`
    : `${credentialType} 제출`;
  const fallbackDesc = issued
    ? `${activity.issuerName ?? "발급기관"}으로부터 증명서를 발급받았습니다.`
    : `${activity.verifierName ?? "요청 기관"}에 증명서를 제출했습니다.`;

  return {
    id: activity.id || `${activity.type}-${index}`,
    cat: issued ? "vc" : "vp",
    icon: issued ? "cert" : "check",
    title: activity.title ?? fallbackTitle,
    desc: activity.description ?? fallbackDesc,
    time: formatTxTime(activity.createdAtUtc),
    group: formatTxGroup(activity.createdAtUtc),
    sortAt: sortTime(activity.createdAtUtc),
    unread: activity.unread,
  };
}

export default function MobileTransactionsPage() {
  const [tab, setTab] = useState<ActivityTab>("all");
  const [nativeTx, setNativeTx] = useState<ActivityItem[]>([]);
  const [walletActivities, setWalletActivities] = useState<ActivityItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    let cancelled = false;
    Promise.allSettled([
      bridge.getWalletTransactions(20),
      bridge.getWalletActivityHistory(50, ["VC_ISSUED", "VP_SUBMITTED"]),
    ]).then(([txResult, activityResult]) => {
      if (cancelled) return;
      const errors: string[] = [];

      if (txResult.status === "fulfilled" && txResult.value.ok) {
        setNativeTx((txResult.value.transactions ?? []).map(txToActivity));
      } else {
        const reason =
          txResult.status === "fulfilled"
            ? txResult.value.error
            : txResult.reason instanceof Error
              ? txResult.reason.message
              : null;
        errors.push(reason ?? "거래 내역을 가져올 수 없습니다.");
      }

      if (activityResult.status === "fulfilled" && activityResult.value.ok) {
        const activities = activityResult.value.activities ?? [];
        setWalletActivities(activities.map(walletActivityToItem));
        const unreadIds = activities
          .filter((activity) => activity.unread)
          .map((activity) => activity.id)
          .filter(Boolean);
        if (unreadIds.length > 0) {
          bridge
            .markWalletActivitiesRead(unreadIds)
            .then((result) => {
              if (cancelled || !result.ok) return;
              setWalletActivities((prev) =>
                prev.map((item) =>
                  unreadIds.includes(item.id) ? { ...item, unread: false } : item,
                ),
              );
            })
            .catch(() => null);
        }
      } else {
        const reason =
          activityResult.status === "fulfilled"
            ? activityResult.value.error
            : activityResult.reason instanceof Error
              ? activityResult.reason.message
              : null;
        errors.push(reason ?? "증명서 활동 내역을 가져올 수 없습니다.");
      }

      setError(errors.length ? errors.join(" ") : null);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const allItems = [...walletActivities, ...nativeTx].sort(
    (a, b) => b.sortAt - a.sortAt,
  );
  const visible =
    tab === "all"
      ? allItems
      : tab === "tx"
        ? nativeTx
        : walletActivities.filter((item) => item.cat === tab);
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
