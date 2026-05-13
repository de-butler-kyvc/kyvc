"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MBottomNav, MTopBar } from "@/components/m/parts";
import { ApiError, notifications, type Notification } from "@/lib/api";

type Tab = "all" | "vc" | "vp" | "warn";

type NotifItem = {
  id: number;
  type: "check" | "shield" | "bell" | "cert";
  cat: "vc" | "vp" | "warn";
  title: string;
  desc: string;
  time: string;
  unread: boolean;
  color: "green" | "blue" | "orange" | "purple";
  group: string;
};

const ICON: Record<NotifItem["type"], React.ReactNode> = {
  check: <MIcon.check />,
  shield: <MIcon.shield />,
  bell: <MIcon.bell />,
  cert: <MIcon.cert />,
};

function classifyType(t?: string): NotifItem["type"] {
  if (!t) return "bell";
  const u = t.toUpperCase();
  if (u.includes("VC")) return "cert";
  if (u.includes("LOGIN") || u.includes("SECURITY")) return "shield";
  if (u.includes("VP") || u.includes("VERIFY")) return "check";
  return "bell";
}

function classifyCat(t?: string): NotifItem["cat"] {
  const u = (t ?? "").toUpperCase();
  if (u.includes("VP")) return "vp";
  if (u.includes("VC") || u.includes("CRED")) return "vc";
  return "warn";
}

function classifyColor(t?: string): NotifItem["color"] {
  const u = (t ?? "").toUpperCase();
  if (u.includes("VC")) return "purple";
  if (u.includes("VP")) return "green";
  if (u.includes("LOGIN")) return "blue";
  return "orange";
}

function fromApi(n: Notification): NotifItem {
  const created = n.createdAt ? new Date(n.createdAt) : null;
  const today = new Date();
  let group = "이전";
  if (created) {
    const diffDays = Math.floor(
      (today.getTime() - created.getTime()) / (1000 * 60 * 60 * 24),
    );
    if (diffDays < 1) group = "오늘";
    else if (diffDays < 7) group = "이번 주";
  }
  return {
    id: n.notificationId,
    type: classifyType(n.notificationType),
    cat: classifyCat(n.notificationType),
    title: n.title,
    desc: n.message,
    time: created
      ? created.toLocaleTimeString("ko-KR", {
          hour: "2-digit",
          minute: "2-digit",
        })
      : "",
    unread: !n.read,
    color: classifyColor(n.notificationType),
    group,
  };
}

export default function MobileNotificationsPage() {
  const [tab, setTab] = useState<Tab>("all");
  const [items, setItems] = useState<NotifItem[]>([]);
  const [readAll, setReadAll] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await notifications.list({ page: 0, size: 30 });
        if (cancelled) return;
        setItems(res.content.map(fromApi));
      } catch (e) {
        if (cancelled) return;
        setError(
          e instanceof ApiError
            ? `알림 조회 실패: ${e.message}`
            : "알림을 불러오는 중 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered =
    tab === "all" ? items : items.filter((i) => i.cat === tab);
  const groups = filtered.reduce<{ section: string; items: NotifItem[] }[]>(
    (acc, item) => {
      const g = acc.find((x) => x.section === item.group);
      if (g) g.items.push(item);
      else acc.push({ section: item.group, items: [item] });
      return acc;
    },
    [],
  );

  const onReadAll = async () => {
    try {
      await notifications.markAllRead();
      setReadAll(true);
    } catch (e) {
      setError(
        e instanceof ApiError
          ? `읽음 처리 실패: ${e.message}`
          : "읽음 처리 중 오류가 발생했습니다.",
      );
    }
  };

  const tabs: { k: Tab; label: string }[] = [
    { k: "all", label: "전체" },
    { k: "vc", label: "VC 발급" },
    { k: "vp", label: "VP 제출" },
    { k: "warn", label: "보안/경고" },
  ];

  return (
    <section className="view wash">
      <MTopBar
        title="알림"
        back={false}
        right={
          <button
            type="button"
            className="notif-read-all"
            onClick={onReadAll}
            disabled={readAll}
          >
            모두 읽음
          </button>
        }
      />
      <div className="notif-controls">
        <div className="notif-tabs">
          {tabs.map((t) => (
            <button
              key={t.k}
              type="button"
              className={tab === t.k ? "active" : ""}
              onClick={() => setTab(t.k)}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="scroll notif-container">
        {error ? <p className="m-error">{error}</p> : null}
        {loading ? (
          <p className="m-loading">불러오는 중…</p>
        ) : groups.length === 0 ? (
          <p className="notif-empty">
            {error ? "알림을 불러올 수 없습니다." : "해당하는 알림이 없습니다."}
          </p>
        ) : (
          groups.map((g) => (
            <div key={g.section} className="notif-group">
              <div className="notif-header">{g.section}</div>
              <div className="notif-list">
                {g.items.map((n) => (
                  <div
                    key={n.id}
                    className={`notif-item${n.unread && !readAll ? " unread" : ""}`}
                  >
                    <div className={`notif-icon ${n.color}`}>{ICON[n.type]}</div>
                    <div className="notif-content">
                      <div className="notif-title-row">
                        <span className="notif-title">{n.title}</span>
                        <span className="notif-time">{n.time}</span>
                      </div>
                      <p className="notif-desc">{n.desc}</p>
                    </div>
                    {n.unread && !readAll ? <div className="notif-dot" /> : null}
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      <MBottomNav active="notifications" />
    </section>
  );
}
