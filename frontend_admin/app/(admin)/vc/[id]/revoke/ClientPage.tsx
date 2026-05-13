"use client";

import { getVcDetail, requestVcRevoke, type VcDetail } from "@/lib/api/vc";
import { getCommonCodes } from "@/lib/api/common-codes";
import { vcDetailPath } from "@/lib/navigation/admin-routes";
import Link from "next/link";
import { use, useEffect, useState } from "react";

const DEFAULT_REVOKE_REASONS = ["법인 정보 변경", "부정 발급 의심", "사용자 요청", "만료 전 폐기", "기타"];

export default function VcRevokePage({ params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = use(params);
  const id = decodeURIComponent(rawId);
  const [vc, setVc] = useState<VcDetail | null>(null);
  const [revokeReasons, setRevokeReasons] = useState(DEFAULT_REVOKE_REASONS);
  const [reason, setReason] = useState(DEFAULT_REVOKE_REASONS[0]);
  const [detail, setDetail] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;

    getCommonCodes({ codeGroupId: "REJECT_REASON", enabled: true })
      .then((codes) => {
        if (!alive) return;
        const reasons = codes.map((code) => code.codeName).filter(Boolean);
        if (reasons.length > 0) {
          setRevokeReasons(reasons);
          setReason((prev) => reasons.includes(prev) ? prev : reasons[0]);
        }
      })
      .catch(() => undefined);

    const fetchDetail = async () => {
      setDetailLoading(true);
      setError("");
      try {
        const data = await getVcDetail(id);
        if (alive) setVc(data);
      } catch (err) {
        if (alive) setError(err instanceof Error ? err.message : "VC 정보를 불러오지 못했습니다.");
      } finally {
        if (alive) setDetailLoading(false);
      }
    };

    fetchDetail();
    return () => {
      alive = false;
    };
  }, [id]);

  const handleSubmit = async () => {
    if (!confirmed) return;
    setLoading(true);
    setError("");
    try {
      await requestVcRevoke(id, { reason, detail: detail.trim() || undefined });
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "폐기 요청 처리에 실패했습니다.");
    } finally {
      setLoading(false);
    }
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
              <span className="text-green-600 text-2xl font-bold leading-none">OK</span>
            </div>
            <h2 className="text-lg font-bold text-slate-800 mb-2">폐기 요청 완료</h2>
            <p className="text-sm text-slate-500 mb-1">VC 폐기 요청이 정상적으로 처리되었습니다.</p>
            <p className="text-sm text-slate-400 mb-1">사유: <span className="font-medium">{reason}</span></p>
            <p className="text-xs text-slate-400 mb-8">해당 VC는 무효 처리되며 XRPL에 폐기 상태가 기록됩니다.</p>
            <Link
              href={vcDetailPath(id)}
              className="bg-slate-700 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-slate-800 inline-block transition-colors"
            >
              VC 상세로 돌아가기
            </Link>
          </div>
        </div>
      ) : detailLoading ? (
        <div className="bg-white rounded-lg border border-slate-200 p-8 text-center text-slate-500">로딩 중...</div>
      ) : (
        <div className="flex gap-4">
          <div className="w-56 shrink-0 bg-white rounded-lg border border-slate-200 p-4 space-y-3 h-fit">
            <h2 className="text-xs font-semibold text-slate-500">VC 폐기 요청</h2>
            {[
              { label: "Credential ID", value: vc?.credentialId ?? id },
              { label: "법인명", value: vc?.corp ?? "-" },
              { label: "Credential Type", value: vc?.credentialType ?? "-" },
              { label: "발급일", value: vc?.issuedAt ?? "-" },
            ].map((item) => (
              <div key={item.label}>
                <p className="text-xs text-slate-400">{item.label}</p>
                <p className="text-slate-700 text-xs font-medium mt-0.5 break-all">{item.value}</p>
              </div>
            ))}
            <div>
              <p className="text-xs text-slate-400">현재 상태</p>
              <span className="bg-green-100 text-green-600 text-xs px-2 py-0.5 rounded-full font-medium">{vc?.status ?? "-"}</span>
            </div>
          </div>

          <div className="flex-1 bg-white rounded-lg border border-slate-200 p-6 space-y-6">
            {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

            <div>
              <h2 className="text-sm font-semibold text-slate-700 mb-3">폐기 사유 <span className="text-red-500">*</span></h2>
              <div className="flex flex-wrap gap-3 mb-3">
                {revokeReasons.map((item) => (
                  <label key={item} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="reason"
                      value={item}
                      checked={reason === item}
                      onChange={() => setReason(item)}
                      className="accent-blue-600"
                    />
                    <span className="text-sm text-slate-700">{item}</span>
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
              <p className="font-medium mb-1">폐기 주의사항</p>
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
              <p className="text-xs text-slate-400">요청자: 현재 로그인 관리자</p>
              <div className="flex gap-2">
                <Link href={vcDetailPath(id)} className="border border-slate-200 text-slate-600 px-4 py-2 rounded text-sm hover:bg-slate-50">
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
    </div>
  );
}
