import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

const API_BASE = "";
const REPORTS_BASE = `${API_BASE}/api/admin/backend/reports`;

// ── 타입 ──────────────────────────────────────────────────────

export interface OperationsReport {
  period?: string;
  kycTotal?: number;
  kycApproved?: number;
  kycRejected?: number;
  vcIssued?: number;
  vcRevoked?: number;
  vpVerified?: number;
  vpFailed?: number;
}

// ── 공통 유틸 ─────────────────────────────────────────────────

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

function getAuthHeaders() {
  const token = getAccessTokenForApi();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (!isPlaceholderAccessToken(token)) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function errorMessageFromResponse(response: Response): Promise<string> {
  try {
    const text = await response.text();
    if (!text.trim()) return `API Error: ${response.status} ${response.statusText}`;
    const parsed = JSON.parse(text) as { message?: string; error?: string };
    if (parsed.message) return parsed.message;
    if (typeof parsed.error === "string") return parsed.error;
    return text;
  } catch {
    return `API Error: ${response.status} ${response.statusText}`;
  }
}

// ── 리포트 API ────────────────────────────────────────────────

/** GET /api/admin/backend/reports/operations */
export async function getOperationsReport(filters?: {
  from?: string;
  to?: string;
  granularity?: "daily" | "weekly" | "monthly";
}): Promise<OperationsReport> {
  const params = new URLSearchParams();
  if (filters?.from) params.set("fromDate", filters.from);
  if (filters?.to) params.set("toDate", filters.to);
  if (filters?.granularity) params.set("groupBy", filters.granularity);
  const url = params.toString()
    ? `${REPORTS_BASE}/operations?${params}`
    : `${REPORTS_BASE}/operations`;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<OperationsReport>;
  return json.data;
}

/** GET /api/admin/backend/reports/operations/export */
export async function exportOperationsReport(filters?: {
  from?: string;
  to?: string;
  format?: "csv" | "xlsx";
}): Promise<Blob> {
  const params = new URLSearchParams();
  if (filters?.from) params.set("fromDate", filters.from);
  if (filters?.to) params.set("toDate", filters.to);
  if (filters?.format) params.set("format", filters.format);
  const url = params.toString()
    ? `${REPORTS_BASE}/operations/export?${params}`
    : `${REPORTS_BASE}/operations/export`;
  const token = getAccessTokenForApi();
  const headers: Record<string, string> = {};
  if (!isPlaceholderAccessToken(token)) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(url, { method: "GET", headers, credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  return response.blob();
}
