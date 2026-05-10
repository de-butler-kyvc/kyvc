const API_BASE = "";
const DOC_DELETE_BASE = `${API_BASE}/api/admin/backend/document-delete-requests`;

// ── 타입 ──────────────────────────────────────────────────────

export interface DocumentDeleteRequest {
  requestId: string;
  applicantName?: string;
  corporationName?: string;
  documentType?: string;
  reason?: string;
  status?: string;
  requestedAt?: string;
  processedAt?: string;
}

// ── 공통 유틸 ─────────────────────────────────────────────────

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
  return { "Content-Type": "application/json" };
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

// ── 문서 삭제 요청 API ─────────────────────────────────────────

/** GET /api/admin/backend/document-delete-requests */
export async function getDocumentDeleteRequests(filters?: {
  status?: string;
  from?: string;
  to?: string;
}): Promise<DocumentDeleteRequest[]> {
  const params = new URLSearchParams();
  if (filters?.status && filters.status !== "전체 상태") params.set("status", filters.status);
  if (filters?.from) params.set("from", filters.from);
  if (filters?.to) params.set("to", filters.to);
  const url = params.toString() ? `${DOC_DELETE_BASE}?${params}` : DOC_DELETE_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<DocumentDeleteRequest[] | PageLike<DocumentDeleteRequest>>;
  return unwrapListData(json.data);
}

/** POST /api/admin/backend/document-delete-requests/{requestId}/approve */
export async function approveDocumentDeleteRequest(
  requestId: string,
  data?: { comment?: string }
): Promise<void> {
  const response = await fetch(`${DOC_DELETE_BASE}/${requestId}/approve`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data ?? {}),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}

/** POST /api/admin/backend/document-delete-requests/{requestId}/reject */
export async function rejectDocumentDeleteRequest(
  requestId: string,
  data: { reason: string }
): Promise<void> {
  const response = await fetch(`${DOC_DELETE_BASE}/${requestId}/reject`, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
