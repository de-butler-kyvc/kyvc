import { CorporateSidebar } from "@/components/corporate/sidebar";

export default function CorporateLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col bg-[#fafaf9]">
      <header className="flex h-14 shrink-0 items-center justify-between border-b border-[#e5e5e2] bg-white px-10">
        <div className="flex items-center gap-[9px]">
          <div className="flex size-[30px] items-center justify-center rounded-lg bg-[#2563eb] text-[14px] font-bold text-white">
            K
          </div>
          <span className="text-[16px] font-bold tracking-[-0.4px] text-[#111110]">
            KYvC
          </span>
        </div>
        <div className="flex size-8 items-center justify-center rounded-2xl border border-[#bfdbfe] bg-[#eff6ff] text-[12px] font-bold text-[#2563eb]">
          김
        </div>
      </header>
      <div className="flex min-h-0 flex-1">
        <CorporateSidebar />
        <main className="min-w-0 flex-1 overflow-y-auto">{children}</main>
      </div>
      <footer className="flex h-12 shrink-0 items-center border-t border-[#e5e5e2] bg-white px-10">
        <span className="text-[12px] text-[#a1a19d]">
          © 2025 KYvC. All rights reserved.
        </span>
      </footer>
    </div>
  );
}
