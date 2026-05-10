import type { Metadata, Viewport } from "next";

import "./mobile.css";
import MobileBodyMarker from "./mobile-body-marker";

export const metadata: Metadata = {
  title: "KYvC Wallet",
  description: "KYvC Mobile Wallet WebView",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  viewportFit: "cover",
  themeColor: "#0b1d40",
};

const BODY_MARK_SCRIPT = `(function(){try{document.body.setAttribute('data-mobile','true');}catch(e){}})();`;

export default function MobileLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <script dangerouslySetInnerHTML={{ __html: BODY_MARK_SCRIPT }} />
      <MobileBodyMarker />
      <div className="m-shell">{children}</div>
    </>
  );
}
