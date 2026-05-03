import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: "KYvC · 코어 어드민",
  description: "AI · Schema · XRPL 운영"
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
