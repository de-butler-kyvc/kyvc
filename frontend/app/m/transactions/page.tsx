"use client";

import { useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MBottomNav, MTopBar } from "@/components/m/parts";

type ActivityTab = "all" | "vc" | "vp" | "warn" | "tx";

type ActivityItem = {
  id: number;
  cat: Exclude<ActivityTab, "all">;
  icon: "check" | "shield" | "bell" | "cert";
  title: string;
  desc: string;
  time: string;
  group: "오늘" | "이번 주";
  unread?: boolean;
};

const TABS: Array<{ key: ActivityTab; label: string }> = [
  { key: "all", label: "전체" },
  { key: "vc", label: "VC 발급" },
  { key: "vp", label: "VP 제출" },
  { key: "warn", label: "보안/경고" },
  { key: "tx", label: "거래 내역" },
];

const ACTIVITY_ITEMS: ActivityItem[] = [
  {
    id: 1,
    cat: "vp",
    icon: "check",
    title: "법인등록증명서 검증 성공",
    desc: "신한은행 기업금융에서 귀하의 증명서를 성공적으로 검증했습니다.",
    time: "오후 2:32",
    group: "오늘",
    unread: true,
  },
  {
    id: 2,
    cat: "warn",
    icon: "shield",
    title: "새로운 로그인 감지",
    desc: "새로운 기기(MacBook Pro)에서 지갑에 로그인했습니다.",
    time: "오전 10:15",
    group: "오늘",
    unread: true,
  },
  {
    id: 3,
    cat: "warn",
    icon: "bell",
    title: "사업자등록증 만료 안내",
    desc: "등록된 사업자등록증 VC가 30일 후 만료됩니다.",
    time: "수요일",
    group: "이번 주",
  },
  {
    id: 4,
    cat: "vc",
    icon: "cert",
    title: "기업금융 인증서 발급",
    desc: "신한은행 기관으로부터 새로운 증명서를 발급받았습니다.",
    time: "월요일",
    group: "이번 주",
  },
];

function ActivityIcon({ name }: { name: ActivityItem["icon"] }) {
  if (name === "check") return <MIcon.check />;
  if (name === "shield") return <MIcon.shield />;
  if (name === "bell") return <MIcon.bell />;
  return <MIcon.cert />;
}

export default function MobileTransactionsPage() {
  const [tab, setTab] = useState<ActivityTab>("all");

  const visible =
    tab === "all"
      ? ACTIVITY_ITEMS
      : ACTIVITY_ITEMS.filter((item) => item.cat === tab);
  const groups = (["오늘", "이번 주"] as const)
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
                    <div className="activity-icon">
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
