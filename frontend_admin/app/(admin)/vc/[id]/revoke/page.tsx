"use client";
import { use, useState } from "react";
import Link from "next/link";

const revokeReasons = ["법인 정보 변경", "부정 발급 의심", "사용자 요청", "만료 전 폐기", "기타"];

export default function VcRevokePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [reason, setReason] = useState("법인 정보 변경");
  const [detail, setDetail] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = () => {
    if (!confirmed) return;
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
    }, 800);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">
            백엔드 어드민 · <Link href="/vc" className="hover:underline">VC 관리</Link>
          </p>
          <h1 className="text-xl font-bold text-slate-800">VC 폐기 요청</h1>
        </div>
      </div>

      {success ? (
        <div className="flex items-center justify-center py-20">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-12 text-center max-w-md w-full">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-5">
              <span className="text-green-600 text-3xl leading-none">✓</span>
            </div>
            <h2 className="text-lg font-bold text-slate-800 mb-2">폐기 요청 완료</h2>
            <p className="text-sm text-slate-500 mb-1">
              VC 폐기 요청이 정상적으로 처리되었습니다.
            </p>
            <p className="text-sm text-slate-400 mb-1">사유: <span className="font-medium">{reason}</span></p>
            <p className="text-xs text-slate-400 mb-8">
              해당 VC는 즉시 무효 처리되며 XRPL에 폐기 상태가 기록됩니다.
            </p>
            <Link
              href={`/vc/${id}`}
              className="bg-slate-700 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-slate-800 inline-block transition-colors"
            >
              VC 상세로 돌아가기
            </Link>
          </div>
        </div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">VC 폐기 요청</h2>
            {[
              { label: "Credential ID", value: id },
              { label: "법인명", value: "주식회사 케이원" },
              { label: "Credential Type", value: "KYC VC" },
              { label: "발급일", value: "2025.05.02 14:00" },
            ].map((item) => (
              <div key={item.label}>
                <p className="text-xs text-slate-400">{item.label}</p>
                <p className="text-slate-700 text-xs font-medium mt-0.5 break-all">{item.value}</p>
              </div>
            ))}
            <div>
              <p className="text-xs text-slate-400">현재 상태</p>
              <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">활성</span>
            </div>
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-6">
            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">
                폐기 사유 <span className="text-red-500">*</span>
              </h2>
              <div className="flex flex-wrap gap-3 mb-3">
                {revokeReasons.map((r) => (
                  <label key={r} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="reason"
                      value={r}
                      checked={reason === r}
                      onChange={() => setReason(r)}
                      className="accent-blue-600"
                    />
                    <span className="text-sm text-slate-700">{r}</span>
                  </label>
                ))}
              </div>
              <textarea
                value={detail}
                onChange={(e) => setDetail(e.target.value)}
                rows={3}
                placeholder="상세 사유를 입력해주세요. (선택)"
                className="w-full border border-slate-200 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 resize-none"
              />
            </div>

            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
              <p className="font-medium mb-1">⚠ 폐기 주의사항</p>
              <ul className="text-xs leading-relaxed space-y-1 list-disc list-inside">
                <li>폐기된 VC는 복구할 수 없습니다.</li>
                <li>폐기 즉시 모든 VP 검증에서 무효 처리됩니다.</li>
                <li>XRPL에 폐기 상태가 기록됩니다.</li>
              </ul>
            </div>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={confirmed}
                onChange={(e) => setConfirmed(e.target.checked)}
                className="accent-red-600 w-4 h-4"
              />
              <span className="text-sm text-slate-700">위 내용을 확인했으며, VC 폐기에 동의합니다.</span>
            </label>

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <p className="text-xs text-slate-400">요청자: 김심사 (admin@kyvc.kr)</p>
              <div className="flex gap-2">
                <Link
                  href={`/vc/${id}`}
                  className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50"
                >
                  취소
                </Link>
                <button
                  onClick={handleSubmit}
                  disabled={loading || !confirmed}
                  className="bg-red-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-red-700 disabled:opacity-60 transition-colors"
                >
                  {loading ? "처리 중..." : "VC 폐기"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}
