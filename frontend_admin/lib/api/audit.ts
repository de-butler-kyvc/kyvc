import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import { API_BASE } from "@/lib/api/api-base";
const AUDIT_BASE = `${API_BASE}/api/admin/backend/audit-logs`;

// ────────────────────────────────────────────────────────────
// 타입 정의
// ────────────────────────────────────────────────────────────

export interface AuditLog {
  auditId: string;
  actorId?: string;
  actorName?: string;
  actionType?: string;
  targetId?: string;
  targetType?: string;
  description?: string;
  ipAddress?: string;
  result?: string;
  createdAt?: string;
}

export interface AuditLogDetail extends AuditLog {
  requestBody?: Record<string, unknown>;
  responseBody?: Record<string, unknown>;
  userAgent?: string;
  sessionId?: string;
}

// ────────────────────────────────────────────────────────────
// 공통 유틸
// ────────────────────────────────────────────────────────────

interface CommonResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

type PageLike<T> = { content?: T[]; items?: T[]; list?: T[] };

function unwrapListData<T>(data: T[] | PageLike<T> | null | undefined): T[] {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];
  const o = data as PageLike<T>;
  if (Array.isArray(o.content)) return o.content;
  if (Array.isArray(o.items)) return o.items;
  if (Array.isArray(o.list)) return o.list;
  return [];
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

function fmtDt(iso?: string) {
  if (!iso) return "-";
  return iso.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

// ────────────────────────────────────────────────────────────
// 감사로그 API
// ────────────────────────────────────────────────────────────

/** GET /api/admin/backend/audit-logs */
export async function getAuditLogs(filters?: {
  search?: string;
  action?: string;
  actorId?: string;
  from?: string;
  to?: string;
}): Promise<{
  id: string;
  date: string;
  actor: string;
  action: string;
  target: string;
  content: string;
  ip: string;
  result: string;
}[]> {
  const params = new URLSearchParams();
  if (filters?.search?.trim()) params.set("keyword", filters.search.trim());
  if (filters?.action && filters.action !== "전체 액션 유형") params.set("actionType", filters.action);
  if (filters?.actorId) params.set("actorId", filters.actorId);
  if (filters?.from) params.set("from", filters.from);
  if (filters?.to) params.set("to", filters.to);
  const url = params.toString() ? `${AUDIT_BASE}?${params}` : AUDIT_BASE;

  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));

  const json = (await response.json()) as CommonResponse<AuditLog[] | PageLike<AuditLog>>;
  return unwrapListData(json.data).map((log) => ({
    id: log.auditId,
    date: fmtDt(log.createdAt),
    actor: log.actorName ?? log.actorId ?? "-",
    action: log.actionType ?? "-",
    target: log.targetId ? `${log.targetType ? `${log.targetType}-` : ""}${log.targetId}` : "-",
    content: log.description ?? "-",
    ip: log.ipAddress ?? "-",
    result: log.result ?? "-",
  }));
}

/** GET /api/admin/backend/audit-logs/{auditId} */
export async function getAuditLog(auditId: string): Promise<AuditLogDetail> {
  const response = await fetch(`${AUDIT_BASE}/${auditId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<AuditLogDetail>;
  return json.data;
}

export interface SecurityEvent {
  eventId: string;
  eventType?: string;
  actorId?: string;
  actorName?: string;
  description?: string;
  severity?: string;
  ipAddress?: string;
  createdAt?: string;
}

export interface DataAccessLog {
  logId: string;
  accessorId?: string;
  accessorName?: string;
  dataType?: string;
  targetId?: string;
  accessReason?: string;
  ipAddress?: string;
  accessedAt?: string;
}

/** GET /api/admin/backend/security-events */
export async function getSecurityEvents(filters?: {
  eventType?: string;
  severity?: string;
  from?: string;
  to?: string;
}): Promise<SecurityEvent[]> {
  const params = new URLSearchParams();
  if (filters?.eventType) params.set("eventType", filters.eventType);
  if (filters?.severity) params.set("severity", filters.severity);
  if (filters?.from) params.set("from", filters.from);
  if (filters?.to) params.set("to", filters.to);
  const url = params.toString()
    ? `${API_BASE}/api/admin/backend/security-events?${params}`
    : `${API_BASE}/api/admin/backend/security-events`;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<SecurityEvent[] | PageLike<SecurityEvent>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/data-access-logs */
export async function getDataAccessLogs(filters?: {
  accessorId?: string;
  dataType?: string;
  from?: string;
  to?: string;
}): Promise<DataAccessLog[]> {
  const params = new URLSearchParams();
  if (filters?.accessorId) params.set("accessorId", filters.accessorId);
  if (filters?.dataType) params.set("dataType", filters.dataType);
  if (filters?.from) params.set("from", filters.from);
  if (filters?.to) params.set("to", filters.to);
  const url = params.toString()
    ? `${API_BASE}/api/admin/backend/data-access-logs?${params}`
    : `${API_BASE}/api/admin/backend/data-access-logs`;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<DataAccessLog[] | PageLike<DataAccessLog>>;
  return unwrapListData(json.data);
}
