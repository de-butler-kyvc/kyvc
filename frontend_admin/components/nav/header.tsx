import { Badge } from "@/components/ui/badge";

type HeaderProps = {
  channel: string;
  channelTag: string;
};

export function Header({ channel, channelTag }: HeaderProps) {
  return (
    <header className="flex h-16 items-center justify-between border-b bg-background px-6">
      <div className="flex items-center gap-3">
        <div className="text-sm font-medium">{channel}</div>
        <Badge variant="outline" className="font-mono">
          {channelTag}
        </Badge>
      </div>
      <div className="text-xs text-muted-foreground">
        KYvC · KYC Platform
      </div>
    </header>
  );
}
