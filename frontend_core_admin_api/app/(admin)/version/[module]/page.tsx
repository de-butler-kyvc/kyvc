"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader, StatusBadge } from "@/components/ui/PageHeader";

interface ModuleDetail {
  name: string;
  version: string;
  env: string;
  deployedAt: string;
  deployedBy: string;
  rollback: boolean;
  history: { version: string; deployedAt: string; deployedBy: string; note: string }[];
}

const moduleDetails: Record<string, ModuleDetail> = {
  ai: {
    name: "AI 모듈", version: "2.2.0", env: "전체", deployedAt: "2025.05.01", deployedBy: "admin_core", rollback: false,
    history: [
      { version: "2.2.0", deployedAt: "2025.05.01", deployedBy: "admin_core", note: "GPT-4o 모델 업그레이드" },
      { version: "2.1.3", deployedAt: "2025.04.10", deployedBy: "admin_core", note: "임계치 로직 수정" },
      { version: "2.1.2", deployedAt: "2025.03.20", deployedBy: "admin_dev",  note: "버그 수정" },
    ],
  },
  vc: {
    name: "VC 모듈", version: "1.5.2", env: "mainnet", deployedAt: "2025.04.15", deployedBy: "admin_core", rollback: false,
    history: [
      { version: "1.5.2", deployedAt: "2025.04.15", deployedBy: "admin_core", note: "VC 발급 성능 개선" },
      { version: "1.5.1", deployedAt: "2025.03.30", deployedBy: "admin_core", note: "서명 검증 로직 수정" },
      { version: "1.5.0", deployedAt: "2025.02.14", deployedBy: "admin_dev",  note: "W3C VC 2.0 지원" },
    ],
  },
  xrpl: {
    name: "XRPL 모듈", version: "3.0.9", env: "testnet", deployedAt: "2025.04.10", deployedBy: "admin_core", rollback: true,
    history: [
      { version: "3.0.9", deployedAt: "2025.04.10", deployedBy: "admin_core", note: "mainnet 연동 오류 수정 중 (롤백)" },
      { version: "3.1.0", deployedAt: "2025.04.08", deployedBy: "admin_dev",  note: "mainnet 전환 시도 → 롤백됨" },
      { version: "3.0.8", deployedAt: "2025.03.25", deployedBy: "admin_core", note: "트랜잭션 재처리 로직 추가" },
    ],
  },
};

interface Props {
  params: Promise<{ module: string }>;
}

export default function VersionDetailPage({ params }: Props) {
  const { module } = use(params);
  const router = useRouter();
  const detail = moduleDetails[module] ?? moduleDetails.ai;

  const [deleting, setDeleting]       = useState(false);
  const [rollingBack, setRollingBack] = useState(false);
  const [toast, setToast]             = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const handleRollback = async () => {
    const prev = detail.history[1];
    if (!prev) return;
    if (!window.confirm(`${detail.name}을 ${prev.version}으로 롤백하시겠습니까?`)) return;
    setRollingBack(true);
    try {
      // TODO: await fetch(`/api/version/${module}/rollback`, { method: 'POST' })
      await new Promise((r) => setTimeout(r, 800));
      showToast(`${prev.version}으로 롤백 요청이 전송되었습니다.`);
    } finally {
      setRollingBack(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(`${detail.name} v${detail.version} 배포를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`)) return;
    setDeleting(true);
    try {
      // TODO: await fetch(`/api/version/${module}/${detail.version}`, { method: 'DELETE' })
      await new Promise((r) => setTimeout(r, 700));
      showToast("배포 기록이 삭제되었습니다.");
      setTimeout(() => router.push("/version"), 1200);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="relative">
      {toast && (
        <div className={`fixed top-5 right-5 z-50 px-4 py-2.5 rounded-lg shadow-lg text-sm text-white transition-all ${toast.type === "success" ? "bg-emerald-600" : "bg-red-600"}`}>
          {toast.msg}
        </div>
      )}

      <PageHeader
        breadcrumb={`버전 / 배포 > ${detail.name}`}
        title={`${detail.name} 배포 상세`}
        actions={
          <button onClick={() => router.back()} className="border border-slate-300 text-slate-600 text-xs px-3 py-1.5 rounded-md hover:bg-slate-50 transition-colors">
            ← 목록으로
          </button>
        }
      />

      {/* 현재 배포 정보 */}
      <div className="bg-white rounded-lg border border-slate-200 p-5 mb-4">
        <div className="grid grid-cols-4 gap-6 mb-5">
          <div>
            <p className="text-[11px] text-slate-400 mb-1">모듈명</p>
            <p className="text-sm font-semibold text-slate-700">{detail.name}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">현재 버전</p>
            <p className="text-sm font-mono text-slate-700">{detail.version}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">적용 환경</p>
            <p className="text-sm text-slate-700">{detail.env}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">배포 상태</p>
            <StatusBadge status={detail.rollback ? "지연" : "정상"} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-6 pb-5 border-b border-slate-100 mb-5">
          <div>
            <p className="text-[11px] text-slate-400 mb-1">배포일</p>
            <p className="text-sm text-slate-700">{detail.deployedAt}</p>
          </div>
          <div>
            <p className="text-[11px] text-slate-400 mb-1">배포자</p>
            <p className="text-sm font-mono text-slate-700">{detail.deployedBy}</p>
          </div>
        </div>

        {/* 액션 버튼 */}
        <div className="flex items-center gap-2">
          {detail.history[1] && (
            <button
              onClick={handleRollback}
              disabled={rollingBack || deleting}
              className="border border-yellow-400 text-yellow-600 text-xs px-3 py-1.5 rounded-md hover:bg-yellow-50 transition-colors disabled:opacity-50"
            >
              {rollingBack ? "롤백 중..." : `↩ v${detail.history[1].version}으로 롤백`}
            </button>
          )}
          <button
            onClick={handleDelete}
            disabled={deleting || rollingBack}
            className="border border-red-300 text-red-500 text-xs px-3 py-1.5 rounded-md hover:bg-red-50 transition-colors disabled:opacity-50"
          >
            {deleting ? "삭제 중..." : "배포 기록 삭제"}
          </button>
        </div>
      </div>

      {/* 배포 이력 */}
      <div className="bg-white rounded-lg border border-slate-200">
        <div className="px-5 py-3 border-b border-slate-100">
          <p className="text-xs font-semibold text-slate-600">배포 이력</p>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-100">
              {["버전", "배포일", "배포자", "비고"].map((h) => (
                <th key={h} className="text-left text-[11px] text-slate-400 font-medium px-5 py-2.5">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {detail.history.map((h, i) => (
              <tr key={i} className={`border-b border-slate-50 last:border-0 ${i === 0 ? "bg-blue-50/40" : "hover:bg-slate-50"}`}>
                <td className="px-5 py-3 text-xs font-mono text-slate-700">
                  {h.version}
                  {i === 0 && <span className="ml-2 text-[10px] bg-blue-100 text-blue-600 px-1.5 py-0.5 rounded">현재</span>}
                </td>
                <td className="px-5 py-3 text-xs text-slate-500">{h.deployedAt}</td>
                <td className="px-5 py-3 text-xs font-mono text-slate-500">{h.deployedBy}</td>
                <td className="px-5 py-3 text-xs text-slate-600">{h.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
