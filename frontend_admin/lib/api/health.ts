const API_BASE = "https://dev-admin-api-kyvc.khuoo.synology.me";

export interface HealthStatus {
  status: string;
  timestamp?: string;
  components?: Record<string, { status: string; details?: Record<string, unknown> }>;
}

/** GET /health */
export async function checkHealth(): Promise<HealthStatus> {
  const response = await fetch(`${API_BASE}/health`, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
  });
  if (!response.ok) {
    return { status: "DOWN" };
  }
  const json = await response.json() as HealthStatus;
  return json;
}
