const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

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
