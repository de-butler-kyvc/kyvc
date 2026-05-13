import { API_BASE } from "@/lib/api/api-base";

const CORE_ADMIN_BASE = `${API_BASE}/admin`;

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(`Core admin API request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export type CoreHealth = {
  status: string;
  service: string;
  environment: string;
};

export type ProviderCategory = "ocr" | "llm";

export type ProviderSelectionUpdate = {
  provider: string;
  profile?: string;
  changed_by?: string | null;
};

export function getCoreHealth() {
  return requestJson<CoreHealth>(`${API_BASE}/health`);
}

export function getCoreStatus() {
  return requestJson<Record<string, unknown>>(`${CORE_ADMIN_BASE}/core/status`);
}

export function getProviderSelectionOptions() {
  return requestJson<Record<string, unknown>>(`${CORE_ADMIN_BASE}/provider-selections/options`);
}

export function getProviderSelections() {
  return requestJson<Record<string, unknown>>(`${CORE_ADMIN_BASE}/provider-selections`);
}

export function updateProviderSelection(category: ProviderCategory, payload: ProviderSelectionUpdate) {
  return requestJson<Record<string, unknown>>(`${CORE_ADMIN_BASE}/provider-selections/${category}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function getProviderSelectionHistory(limit = 20) {
  const params = new URLSearchParams({ limit: String(limit) });

  return requestJson<Record<string, unknown>>(`${CORE_ADMIN_BASE}/provider-selections/history?${params}`);
}
