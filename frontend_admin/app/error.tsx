"use client";

import { useEffect } from "react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("[KYvC Error]", error);
  }, [error]);

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col items-center justify-center px-4">
      <div className="bg-white border border-slate-200 rounded-lg shadow-sm w-full max-w-md p-10 flex flex-col items-center text-center">
        <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center mb-6">
          <svg className="w-8 h-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
          </svg>
        </div>
        <p className="text-xs font-medium text-red-600 bg-red-50 px-2 py-0.5 rounded-full mb-3">500 Internal Error</p>
        <h1 className="text-xl font-bold text-slate-800 mb-2">오류가 발생했습니다</h1>
        <p className="text-sm text-slate-500 leading-relaxed mb-6">예기치 않은 오류가 발생했습니다.<br />잠시 후 다시 시도해 주세요.</p>
        {error.digest && (
          <div className="w-full bg-slate-50 border border-slate-200 rounded-lg px-4 py-3 mb-6 text-left">
            <p className="text-xs text-slate-400 mb-1">오류 코드</p>
            <p className="text-xs font-mono text-slate-600 break-all">{error.digest}</p>
          </div>
        )}
        <div className="flex gap-3 w-full">
          <button onClick={reset} className="flex-1 inline-flex items-center justify-center gap-2 bg-blue-600 text-white text-sm font-medium px-4 py-2.5 rounded-lg hover:bg-blue-700 transition-colors">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
            </svg>
            다시 시도
          </button>
          <a href="/dashboard" className="flex-1 inline-flex items-center justify-center gap-2 border border-slate-200 text-slate-600 text-sm font-medium px-4 py-2.5 rounded-lg hover:bg-slate-50 transition-colors">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
            </svg>
            대시보드로 이동
          </a>
        </div>
      </div>
      <div className="flex justify-between w-full max-w-md mt-6 text-xs text-slate-400">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}