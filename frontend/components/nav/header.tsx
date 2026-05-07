type HeaderProps = {
  channel: string;
  channelTag: string;
};

export function Header({ channel }: HeaderProps) {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-border bg-card px-10">
      <div className="flex items-center gap-2.5">
        <div className="flex size-[30px] items-center justify-center rounded-lg bg-primary text-[14px] font-bold text-primary-foreground">
          K
        </div>
        <div className="text-[16px] font-bold tracking-[-0.4px] text-foreground">
          KYvC
        </div>
        <div className="ml-3 hidden text-[12px] text-muted-foreground sm:block">
          {channel}
        </div>
      </div>
      <div className="flex size-8 items-center justify-center rounded-full border border-accent-border bg-accent text-[12px] font-bold text-accent-foreground">
        K
      </div>
    </header>
  );
}
