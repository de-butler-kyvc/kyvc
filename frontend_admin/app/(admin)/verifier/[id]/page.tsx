"use client";
import { use, useState } from "react";
import Link from "next/link";

const mockVerifierMap: Record<string, {
  id: string; name: string; domain: string; type: string;
  callbackUrl: string; webhookUrl: string; credentials: string[];
  contactEmail: string; regDate: string; regBy: string; status: string;
}> = {
  "VER-001": { id: "VER-001", name: "파이낸셜 파트너스", domain: "financial-partners.co.kr", type: "코어 도입형", callbackUrl: "https://api.financial-partners.co.kr/vp/callback", webhookUrl: "https://api.financial-partners.co.kr/webhook", credentials: ["KYC VC", "위임권한 VC"], contactEmail: "admin@financial-partners.co.kr", regDate: "2024.09.01", regBy: "admin_park", status: "활성" },
  "VER-002": { id: "VER-002", name: "비즈파트너 포털", domain: "bizpartner.com", type: "SDK-only", callbackUrl: "https://api.bizpartner.com/vp/callback", webhookUrl: "https://api.bizpartner.com/webhook", credentials: ["KYC VC"], contactEmail: "admin@bizpartner.com", regDate: "2025.01.15", regBy: "admin_lee", status: "활성" },
  "VER-003": { id: "VER-003", name: "마켓플레이스 A", domain: "marketplace-a.kr", type: "SDK-only", callbackUrl: "", webhookUrl: "", credentials: ["KYC VC"], contactEmail: "admin@partner.com", regDate: "2025.03.10", regBy: "admin_kim", status: "심사중" },
  "VER-004": { id: "VER-004", name: "파트너 포털 B", domain: "partner-b.co.kr", type: "코어 도입형", callbackUrl: "https://api.partner-b.co.kr/callback", webhookUrl: "", credentials: ["KYC VC"], contactEmail: "admin@partner-b.co.kr", regDate: "2025.04.20", regBy: "admin_ops", status: "비활성" },
};

const statusBadge: Record<string, string> = {
  활성: "bg-green-100 text-green-600",
  심사중: "bg-orange-100 text-orange-600",
  비활성: "bg-slate-100 text-slate-500",
};

const allCredentials = ["KYC VC", "위임권한 VC"];

export default function VerifierDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const raw = mockVerifierMap[id] ?? mockVerifierMap["VER-001"];

  const [name, setName] = useState(raw.name);
  const [domain, setDomain] = useState(raw.domain);
  const [type, setType] = useState(raw.type);
  const [callbackUrl, setCallbackUrl] = useState(raw.callbackUrl);
  const [webhookUrl, setWebhookUrl] = useState(raw.webhookUrl);
  const [credentials, setCredentials] = useState<string[]>(raw.credentials);
  const [contactEmail, setContactEmail] = useState(raw.contactEmail);
  const [saved, setSaved] = useState(false);

  const toggleCredential = (cred: string) => setCredentials((prev) => prev.includes(cred) ? prev.filter((c) => c !== cred) : [...prev, cred]);
  const handleSave = () => { setSaved(true); setTimeout(() => setSaved(false), 3000); };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">백엔드 어드민 · <Link href="/verifier" className="hover:underline">Verifier 플랫폼 목록</Link></p>
          <h1 className="text-xl font-bold text-slate-800">Verifier 플랫폼 상세</h1>
        </div>
      </div>

      <div className="flex gap-4">
        <div className="w-52 shrink-0">
          <div className="bg-white rounded-lg border border-slate-200 p-4">
            <p className="text-xs font-semibold text-slate-500 mb-3">Verifier 플랫폼 등록</p>
            <div className="space-y-3 text-xs">
              <div><p className="text-slate-400">상태</p><span className={`inline-block mt-0.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusBadge[raw.status]}`}>{raw.status}</span></div>
              <div><p className="text-slate-400">등록 요청자</p><p className="text-slate-700 font-medium mt-0.5">{raw.regBy}</p></div>
              <div><p className="text-slate-400">요청일시</p><p className="text-slate-700 mt-0.5">{raw.regDate}</p></div>
              <div><p className="text-slate-400">플랫폼 ID</p><p className="text-blue-600 font-medium mt-0.5">{raw.id}</p></div>
              <div><p className="text-slate-400">연동 유형</p><p className="text-slate-700 mt-0.5">{raw.type}</p></div>
            </div>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-slate-200">
            <div className="p-4 border-b border-slate-100"><p className="text-sm font-semibold text-slate-700">플랫폼 기본 정보</p></div>
            <div className="p-5 space-y-4">
              {saved && (
                <div className="flex items-center gap-2 bg-green-50 border border-green-200 rounded-lg px-4 py-3 text-green-700 text-sm font-medium">
                  <span>✓</span> 변경사항이 저장되었습니다.
                </div>
              )}
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">플랫폼명</label>
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="예: 마켓플레이스 A" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">도메인</label>
                <input type="text" value={domain} onChange={(e) => setDomain(e.target.value)} placeholder="예: marketplace-a.kr" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">연동 유형</label>
                <select value={type} onChange={(e) => setType(e.target.value)} className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50">
                  <option>SDK-only</option>
                  <option>코어 도입형</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">Callback URL</label>
                <input type="text" value={callbackUrl} onChange={(e) => setCallbackUrl(e.target.value)} placeholder="https://..." className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">Webhook URL</label>
                <input type="text" value={webhookUrl} onChange={(e) => setWebhookUrl(e.target.value)} placeholder="https://..." className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">허용 Credential</label>
                <div className="flex gap-4">
                  {allCredentials.map((cred) => (
                    <label key={cred} className="flex items-center gap-2 cursor-pointer">
                      <input type="checkbox" checked={credentials.includes(cred)} onChange={() => toggleCredential(cred)} className="accent-blue-600" />
                      <span className="text-sm text-slate-700">{cred}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-sm text-slate-600 font-medium">담당자 이메일</label>
                <input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} placeholder="admin@partner.com" className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Link href="/verifier" className="px-4 py-1.5 border border-slate-200 text-slate-600 rounded text-sm hover:bg-slate-50">취소</Link>
                <button onClick={handleSave} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">플랫폼 등록</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between text-xs text-slate-400 pt-2">
        <span>KYvC Backend Admin · 백엔드 관리 시스템</span>
        <span>© 2025 KYvC. All rights reserved.</span>
      </div>
    </div>
  );
}