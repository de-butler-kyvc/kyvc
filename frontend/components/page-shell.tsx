import * as React from "react";

type PageShellProps = {
  title: string;
  description?: string;
  module?: string;
  actions?: React.ReactNode;
  contentClassName?: string;
  children?: React.ReactNode;
};

export function PageShell({
  title,
  description,
  module,
  actions,
  contentClassName,
  children
}: PageShellProps) {
  const content = (
    <>
      <div className="page-head">
        <div>
          {module ? (
            <div
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 10,
                letterSpacing: "0.08em",
                textTransform: "uppercase",
                color: "var(--text-muted)",
                marginBottom: 4
              }}
            >
              {module}
            </div>
          ) : null}
          <h1 className="page-head-title">{title}</h1>
          {description ? (
            <p className="page-head-desc">{description}</p>
          ) : null}
        </div>
        {actions ? <div className="page-head-actions">{actions}</div> : null}
      </div>
      {children}
    </>
  );

  if (contentClassName) {
    return <div className={contentClassName}>{content}</div>;
  }

  return content;
}
