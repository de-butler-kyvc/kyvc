"use client";

import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MBottomNav, MTopBar } from "@/components/m/parts";
import { notifications, type Notification } from "@/lib/api";
import { MOCK_NOTIFS, type MNotifItem } from "@/lib/m/data";

type Tab = "all" | "vc" | "vp" | "warn";

const ICON: Record<MNotifItem["type"], React.ReactNode> = {
  check: <MIcon.check />,
  shield: <MIcon.shield />,
  bell: <MIcon.bell />,
  cert: <MIcon.cert />,
};

function classifyNotificationType(t?: string): MNotifItem["type"] {
  if (!t) return "bell";
  const u = t.toUpperCase();
  if (u.includes("VC")) return "cert";
  if (u.includes("LOGIN") || u.includes("SECURITY")) return "shield";
  if (u.includes("VP") || u.includes("VERIFY")) return "check";
  return "bell";
}

function classifyNotificationCat(t?: string): MNotifItem["cat"] {
  const u = (t ?? "").toUpperCase();
  if (u.includes("VP")) return "vp";
  if (u.includes("VC") || u.includes("CRED")) return "vc";
  return "warn";
}

function classifyNotificationColor(t?: string): MNotifItem["color"] {
  const u = (t ?? "").toUpperCase();
  if (u.includes("VC")) return "purple";
  if (u.includes("VP")) return "green";
  if (u.includes("LOGIN")) return "blue";
  return "orange";
}

function fromApi(n: Notification): MNotifItem {
  return {
    id: n.notificationId,
    type: classifyNotificationType(n.notificationType),
    cat: classifyNotificationCat(n.notificationType),
    title: n.title,
    desc: n.message,
    time: (n.createdAt ?? "").slice(11, 16) || "방금",
    unread: !n.read,
    color: classifyNotificationColor(n.notificationType),
    group: "오늘",
  };
}

export default function MobileNotificationsPage() {
  const [tab, setTab] = useState<Tab>("all");
  const [items, setItems] = useState<MNotifItem[]>(MOCK_NOTIFS);
  const [readAll, setReadAll] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const res = await notifications.list({ page: 0, size: 30 });
        if (res.content.length) {
          setItems(res.content.map(fromApi));
        }
      } catch {
        /* mock 유지 */
      }
    })();
  }, []);

  const filtered =
    tab === "all" ? items : items.filter((i) => i.cat === tab);
  const groups = filtered.reduce<{ section: string; items: MNotifItem[] }[]>(
    (acc, item) => {
      const g = acc.find((x) => x.section === item.group);
      if (g) g.items.push(item);
      else acc.push({ section: item.group, items: [item] });
      return acc;
    },
    [],
  );

  const onReadAll = async () => {
    setReadAll(true);
    try {
      await notifications.markAllRead();
    } catch {
      /* ignore */
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
        {groups.length === 0 ? (
          <p className="notif-empty">해당하는 알림이 없습니다.</p>
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
