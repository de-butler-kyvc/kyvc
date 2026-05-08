import { Icon } from "@/components/design/icons";

type HeaderProps = {
  channel: string;
  channelTag?: string;
  initial?: string;
};

export function Header({ channel, initial = "K" }: HeaderProps) {
  return (
    <header className="dash-topbar">
      <div className="dash-topbar-title">{channel}</div>
      <div className="dash-topbar-right">
        <button className="icon-btn" title="Search" type="button">
          <Icon.Search />
        </button>
        <button className="icon-btn" title="Notifications" type="button">
          <Icon.Bell />
        </button>
        <div style={{ width: 1, height: 22, background: "var(--border)" }} />
        <div className="avatar">{initial}</div>
      </div>
    </header>
  );
}
