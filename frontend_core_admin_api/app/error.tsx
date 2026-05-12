"use client";

export default function Error({ reset }: { reset: () => void }) {
  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center">
      <div className="text-center">
        <p className="text-slate-400 text-sm mb-2">오류 발생</p>
        <h1 className="text-white text-2xl font-semibold mb-4">문제가 발생했습니다</h1>
        <button
          onClick={reset}
          className="text-sm text-blue-400 hover:text-blue-300 transition-colors"
        >
          다시 시도
        </button>
      </div>
    </div>
  );
}