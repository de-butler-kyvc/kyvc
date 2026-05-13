export default function Loading() {
  return (
    <div className="p-6 space-y-6">
      {/* 헤더 스켈레톤 */}
      <div className="space-y-2">
        <div className="h-3 w-32 bg-slate-200 rounded animate-pulse" />
        <div className="h-7 w-56 bg-slate-200 rounded animate-pulse" />
      </div>

      {/* 카드 4개 */}
      <div className="grid grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
            <div className="h-3 w-20 bg-slate-200 rounded animate-pulse" />
            <div className="h-8 w-16 bg-slate-200 rounded animate-pulse" />
            <div className="h-3 w-24 bg-slate-200 rounded animate-pulse" />
          </div>
        ))}
      </div>

      {/* 테이블 카드 */}
      <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
        {/* 테이블 헤더 */}
        <div className="bg-slate-50 border-b border-slate-200 px-6 py-3 flex gap-6">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-3 bg-slate-200 rounded animate-pulse" style={{ width: `${[80, 120, 60, 90, 70][i]}px` }} />
          ))}
        </div>

        {/* 테이블 행 */}
        {[...Array(8)].map((_, i) => (
          <div key={i} className="px-6 py-4 border-b border-slate-50 flex gap-6 items-center">
            <div className="h-3 w-20 bg-slate-200 rounded animate-pulse" />
            <div className="h-3 w-28 bg-slate-200 rounded animate-pulse" />
            <div className="h-5 w-14 bg-slate-200 rounded-full animate-pulse" />
            <div className="h-3 w-24 bg-slate-200 rounded animate-pulse" />
            <div className="h-3 w-16 bg-slate-200 rounded animate-pulse" />
          </div>
        ))}
      </div>

      {/* 푸터 */}
      <div className="flex justify-between pt-2">
        <div className="h-3 w-48 bg-slate-200 rounded animate-pulse" />
        <div className="h-3 w-36 bg-slate-200 rounded animate-pulse" />
      </div>
    </div>
  );
}