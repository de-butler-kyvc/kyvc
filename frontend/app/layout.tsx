import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: "KYvC",
  description: "KYvC 법인 KYC 플랫폼"
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
