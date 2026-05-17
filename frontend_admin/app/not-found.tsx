"use client";

import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center px-4">
      <div className="bg-white border border-slate-200 rounded-lg shadow-sm w-full max-w-md p-10 flex flex-col items-center text-center">
        {/* 아이콘 */}
        <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center mb-6">
          <svg
            className="w-8 h-8 text-slate-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={1.5}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 5.25h.008v.008H12v-.008Z"
            />
          </svg>
        </div>

        <p className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full mb-3">
          404 Not Found
        </p>

        <h1 className="text-xl font-bold text-slate-800 mb-2">
          페이지를 찾을 수 없습니다
        </h1>

        <p className="text-sm text-slate-500 leading-relaxed mb-8">
          요청하신 페이지가 존재하지 않거나,
          <br />
          이동되었거나, 삭제되었을 수 있습니다.
        </p>

        <div className="flex gap-3 w-full">
          <Link
            href="/dashboard"
            className="flex-1 inline-flex items-center justify-center gap-2 bg-blue-600 text-white text-sm font-medium px-4 py-2.5 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
            </svg>
            대시보드로 이동
          </Link>
          <button
            onClick={() => history.back()}
            className="flex-1 inline-flex items-center justify-center gap-2 border border-slate-200 text-slate-600 text-sm font-medium px-4 py-2.5 rounded-lg hover:bg-slate-50 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
            </svg>
            이전 페이지
          </button>
        </div>
      </div>

      <div className="flex justify-between w-full max-w-md mt-6 text-xs text-slate-400">
        <span>KYvC 증명서 관리자 · 증명서 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}