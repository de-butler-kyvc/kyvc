import Link from "next/link";

const menus = [
  {
    title: "알림 템플릿 관리",
    desc: "이메일, SMS 등 알림 템플릿을 관리합니다.",
    href: "/settings/notifications",
    badge: "BADM-040",
  },
];

export default function SettingsPage() {
  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div>
        <p className="text-xs text-slate-400 mb-1">시스템</p>
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-slate-800">설정</h1>
          <span className="text-xs text-slate-400">BADM-설정</span>
        </div>
      </div>

      {/* 메뉴 카드 */}
      <div className="grid grid-cols-3 gap-4">
        {menus.map((menu) => (
          <Link key={menu.href} href={menu.href}>
            <div className="bg-white border border-slate-200 rounded-lg p-5 hover:border-blue-300 hover:shadow-sm transition-all cursor-pointer">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-sm font-semibold text-slate-800">
                  {menu.title}
                </h2>
                <span className="text-xs text-slate-400">{menu.badge}</span>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">{menu.desc}</p>
              <p className="text-xs text-blue-500 mt-3">바로가기 →</p>
            </div>
          </Link>
        ))}
      </div>

      {/* 푸터 */}
      <div className="flex justify-between text-xs text-slate-400 pt-4">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}