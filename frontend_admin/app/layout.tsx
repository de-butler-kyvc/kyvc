import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: "KYvC · 백엔드 어드민",
  description: "업무 운영 · 심사 · 정책 관리"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body className="min-h-screen bg-background font-sans antialiased">
        {children}
      </body>
    </html>
  );
}
