import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center">
      <div className="text-center">
        <p className="text-slate-400 text-sm mb-2">404</p>
        <h1 className="text-white text-2xl font-semibold mb-4">페이지를 찾을 수 없습니다</h1>
        <Link href="/dashboard" className="text-sm text-blue-400 hover:text-blue-300 transition-colors">
          대시보드로 돌아가기
        </Link>
      </div>
    </div>
  );
}