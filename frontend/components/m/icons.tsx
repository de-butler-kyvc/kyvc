import * as React from "react";

type IconProps = React.SVGProps<SVGSVGElement> & { size?: number };

const wrap = (path: React.ReactNode) =>
  function MIcon({ size, ...rest }: IconProps) {
    return (
      <svg
        viewBox="0 0 24 24"
        width={size}
        height={size}
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
        {...rest}
      >
        {path}
      </svg>
    );
  };

export const MIcon = {
  back: wrap(<path d="M15 18l-6-6 6-6" />),
  gear: wrap(
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.04.04a2 2 0 1 1-2.83 2.83l-.04-.04a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.06a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.04.04a2 2 0 1 1-2.83-2.83l.04-.04A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.04-.04a2 2 0 1 1 2.83-2.83l.04.04A1.65 1.65 0 0 0 8.92 4a1.65 1.65 0 0 0 1-1.51V2.4a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.04-.04a2 2 0 1 1 2.83 2.83l-.04.04A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </>,
  ),
  qr: wrap(
    <>
      <path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4z" />
      <path d="M14 14h2v2h-2zM18 14h2v6h-4v-2h2zM14 18h2v2h-2z" />
    </>,
  ),
  wallet: wrap(
    <>
      <path d="M3 7h18v13H3z" />
      <path d="M3 10h18M16 15h2" />
    </>,
  ),
  history: wrap(
    <>
      <path d="M4 12a8 8 0 1 0 2.34-5.66" />
      <path d="M4 4v5h5M12 8v5l3 2" />
    </>,
  ),
  cert: wrap(
    <>
      <path d="M7 3h10l3 3v15H7z" />
      <path d="M17 3v4h4M10 12h7M10 16h5" />
    </>,
  ),
  user: wrap(
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>,
  ),
  lock: wrap(
    <>
      <rect x="4" y="10" width="16" height="10" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </>,
  ),
  bell: wrap(
    <>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.7 21a2 2 0 0 1-3.4 0" />
    </>,
  ),
  check: wrap(<path d="M20 6L9 17l-5-5" />),
  scan: wrap(
    <>
      <path d="M4 7V5a1 1 0 0 1 1-1h2M17 4h2a1 1 0 0 1 1 1v2M20 17v2a1 1 0 0 1-1 1h-2M7 20H5a1 1 0 0 1-1-1v-2" />
      <path d="M7 12h10" />
    </>,
  ),
  mail: wrap(
    <>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M3 7l9 6 9-6" />
    </>,
  ),
  building: wrap(
    <>
      <path d="M4 21V6l8-3 8 3v15" />
      <path d="M9 21v-6h6v6M8 8h.01M12 8h.01M16 8h.01M8 12h.01M16 12h.01" />
    </>,
  ),
  fingerprint: wrap(
    <path d="M12 11v3M8.5 14.5a5 5 0 1 1 7 0M6 17a8 8 0 1 1 12 0M9 20c1-1.5 1.5-3 1.5-6M14 20c1-2 1.5-4 1.5-6" />,
  ),
  chevronRight: wrap(<path d="M9 18l6-6-6-6" />),
  shield: wrap(<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />),
  globe: wrap(
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1 4-10z" />
      <path d="M2 12h20" />
    </>,
  ),
  help: wrap(
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
      <path d="M12 17h.01" />
    </>,
  ),
  logout: wrap(
    <>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5" />
      <path d="M21 12H9" />
    </>,
  ),
  key: wrap(
    <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4" />,
  ),
  eye: wrap(
    <>
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </>,
  ),
  xrp: wrap(
    <path
      d="M14.5 4.5l-3.5 3.5-3.5-3.5L4.5 7.5l5 5-5 5 3.5 3.5 3.5-3.5 3.5 3.5 3.5-3.5-5-5 5-5z"
      fill="currentColor"
      stroke="none"
    />,
  ),
  zap: wrap(<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />),
  link: wrap(
    <>
      <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
      <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
    </>,
  ),
  external: wrap(
    <>
      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
      <path d="M15 3h6v6" />
      <path d="M10 14L21 3" />
    </>,
  ),
  alert: wrap(
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 8v4M12 16h.01" />
    </>,
  ),
  arrowDown: wrap(<path d="M12 5v14M5 12l7 7 7-7" />),
  arrowUp: wrap(<path d="M12 19V5M5 12l7-7 7 7" />),
  arrowUpRight: wrap(<path d="M7 17L17 7M7 7h10v10" />),
  arrowDownLeft: wrap(<path d="M17 7L7 17M17 17H7V7" />),
  x: wrap(<path d="M18 6L6 18M6 6l12 12" />),
};

export type MIconName = keyof typeof MIcon;
