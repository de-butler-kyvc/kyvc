import type { Metadata } from "next";

import { SessionProvider } from "@/lib/session-context";

import "./globals.css";

export const metadata: Metadata = {
  title: "KYvC — 법인 KYC 자동 심사 플랫폼",
  description: "KYvC 법인 KYC 플랫폼"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body
        className="min-h-screen bg-background font-sans antialiased"
        suppressHydrationWarning
      >
        <SessionProvider>{children}</SessionProvider>
      </body>
    </html>
  );
}
