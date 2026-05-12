import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "KYvC Core Admin",
  description: "KYvC 코어 관리 시스템",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}