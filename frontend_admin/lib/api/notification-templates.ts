import { getAccessTokenForApi, isPlaceholderAccessToken } from "@/lib/auth-session";

import { API_BASE } from "@/lib/api/api-base";
const NOTIF_BASE = `${API_BASE}/api/admin/backend/notification-templates`;

// ── 타입 ──────────────────────────────────────────────────────

export interface NotificationTemplate {
  templateId: string;
  templateCode?: string;
  templateName: string;
  eventType?: string;
  channel?: string;
  channelCode?: string;
  subject?: string;
  titleTemplate?: string;
  body?: string;
  messageTemplate?: string;
  enabled?: boolean;
  enabledYn?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateNotificationTemplateBody {
  templateName: string;
  eventType?: string;
  channel?: string;
  subject?: string;
  body?: string;
}

export interface UpdateNotificationTemplateBody {
  templateName?: string;
  subject?: string;
  body?: string;
  titleTemplate?: string;
  messageTemplate?: string;
  enabled?: boolean;
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

function normalizeTemplate(template: NotificationTemplate): NotificationTemplate {
  return {
    ...template,
    templateId: String(template.templateId),
    eventType: template.eventType ?? template.templateCode,
    channel: template.channel ?? template.channelCode,
    subject: template.subject ?? template.titleTemplate,
    body: template.body ?? template.messageTemplate,
    enabled: template.enabled ?? template.enabledYn === "Y",
  };
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

// ── 알림 템플릿 API ────────────────────────────────────────────

/** GET /api/admin/backend/notification-templates */
export async function getNotificationTemplates(filters?: {
  eventType?: string;
  channel?: string;
}): Promise<NotificationTemplate[]> {
  const params = new URLSearchParams();
  if (filters?.eventType) params.set("keyword", filters.eventType);
  if (filters?.channel) params.set("channelCode", filters.channel);
  const url = params.toString() ? `${NOTIF_BASE}?${params}` : NOTIF_BASE;
  const response = await fetch(url, { method: "GET", headers: getAuthHeaders(), credentials: "include" });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<NotificationTemplate[] | PageLike<NotificationTemplate>>;
  return unwrapListData(json.data).map(normalizeTemplate);
}

/** POST /api/admin/backend/notification-templates */
export async function createNotificationTemplate(
  data: CreateNotificationTemplateBody
): Promise<NotificationTemplate> {
  const response = await fetch(NOTIF_BASE, {
    method: "POST",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({
      templateCode: data.eventType ?? data.templateName,
      templateName: data.templateName,
      channelCode: data.channel ?? "EMAIL",
      titleTemplate: data.subject,
      messageTemplate: data.body,
      enabledYn: "Y",
    }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<NotificationTemplate>;
  return normalizeTemplate(json.data);
}

/** GET /api/admin/backend/notification-templates/{templateId} */
export async function getNotificationTemplate(templateId: string): Promise<NotificationTemplate> {
  const response = await fetch(`${NOTIF_BASE}/${templateId}`, {
    method: "GET",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<NotificationTemplate>;
  return normalizeTemplate(json.data);
}

/** PATCH /api/admin/backend/notification-templates/{templateId} */
export async function updateNotificationTemplate(
  templateId: string,
  data: UpdateNotificationTemplateBody
): Promise<NotificationTemplate> {
  const response = await fetch(`${NOTIF_BASE}/${templateId}`, {
    method: "PATCH",
    headers: getAuthHeaders(),
    credentials: "include",
    body: JSON.stringify({
      templateName: data.templateName,
      titleTemplate: data.subject ?? data.titleTemplate,
      messageTemplate: data.body ?? data.messageTemplate,
      enabledYn: data.enabled == null ? undefined : data.enabled ? "Y" : "N",
    }),
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
  const json = (await response.json()) as CommonResponse<NotificationTemplate>;
  return normalizeTemplate(json.data);
}

/** DELETE /api/admin/backend/notification-templates/{templateId} */
export async function deleteNotificationTemplate(templateId: string): Promise<void> {
  const response = await fetch(`${NOTIF_BASE}/${templateId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
    credentials: "include",
  });
  if (!response.ok) throw new Error(await errorMessageFromResponse(response));
}
