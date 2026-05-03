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
    <div className="flex flex-col gap-6 p-8">
      <div className="flex flex-col gap-1">
        {module ? (
          <div className="font-mono text-xs uppercase tracking-widest text-muted-foreground">
            {module}
          </div>
        ) : null}
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {description ? (
          <p className="max-w-2xl text-sm text-muted-foreground">
            {description}
          </p>
        ) : null}
      </div>
      {children}
    </div>
  );
}
