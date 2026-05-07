import * as React from "react";

type PageShellProps = {
  title: string;
  description?: string;
  module?: string;
  children?: React.ReactNode;
};

export function PageShell({
  title,
  description,
  module,
  children
}: PageShellProps) {
  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-4 px-9 py-7">
      <div className="flex flex-col gap-1 pb-2">
        {module ? (
          <div className="font-mono text-[10px] uppercase tracking-[0.6px] text-subtle-foreground">
            {module}
          </div>
        ) : null}
        <h1 className="text-[20px] font-bold tracking-[-0.4px] text-foreground">
          {title}
        </h1>
        {description ? (
          <p className="max-w-2xl text-[13px] text-muted-foreground">
            {description}
          </p>
        ) : null}
      </div>
      {children}
    </div>
  );
}
