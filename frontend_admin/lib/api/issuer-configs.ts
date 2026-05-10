const API_BASE = "";
const CONFIG_BASE = `${API_BASE}/api/admin/backend/issuer-configs`;

// ────────────────────────────────────────────────────────────
// 타입 정의
// ────────────────────────────────────────────────────────────

export interface IssuerConfig {
  issuerConfigId: string;
  configName?: string;
  issuerDid?: string;
  credentialType?: string;
  schemaId?: string;
  validityPeriodDays?: number;
  signingAlgorithm?: string;
  revocationEnabled?: boolean;
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface IssuerConfigDetail extends IssuerConfig {
  credentialSchema?: Record<string, unknown>;
  contextUrls?: string[];
  proofType?: string;
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

// ────────────────────────────────────────────────────────────
// Issuer 발급 설정 API
// ────────────────────────────────────────────────────────────

/** GET /api/admin/backend/issuer-configs */
export async function getIssuerConfigs(filters?: {
  enabled?: boolean;
}): Promise<IssuerConfig[]> {
  const params = new URLSearchParams();
  if (filters?.enabled !== undefined) params.set("enabled", String(filters.enabled));
  const url = params.toString() ? `${CONFIG_BASE}?${params}` : CONFIG_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<IssuerConfig[] | PageLike<IssuerConfig>>;
  return unwrapListData(json.data);
}

/** GET /api/admin/backend/issuer-configs/{issuerConfigId} */
export async function getIssuerConfig(issuerConfigId: string): Promise<IssuerConfigDetail> {
  const response = await fetch(`${CONFIG_BASE}/${issuerConfigId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<IssuerConfigDetail>;
  return json.data;
}
