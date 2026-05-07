const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "";
const TOKEN_KEY = "kyvc.accessToken";
const REFRESH_KEY = "kyvc.refreshToken";

type Envelope<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string
  ) {
    super(message);
  }
}

export const session = {
  get token() {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(TOKEN_KEY);
  },
  set(accessToken: string, refreshToken?: string) {
    if (typeof window === "undefined") return;
    window.localStorage.setItem(TOKEN_KEY, accessToken);
    if (refreshToken) window.localStorage.setItem(REFRESH_KEY, refreshToken);
  },
  clear() {
    if (typeof window === "undefined") return;
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(REFRESH_KEY);
  },
  get refreshToken() {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(REFRESH_KEY);
  }
};

type RequestInput = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  query?: Record<string, string | number | boolean | undefined>;
  body?: unknown;
  formData?: FormData;
};

export async function api<T>(path: string, input: RequestInput = {}): Promise<T> {
  const { method = "GET", query, body, formData } = input;
  const url = new URL(BASE_URL + path, BASE_URL || "http://localhost");
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null) url.searchParams.set(k, String(v));
    }
  }

  const headers: Record<string, string> = {};
  const token = session.token;
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  const res = await fetch(BASE_URL ? url.toString() : path + url.search, {
    method,
    headers,
    body: formData ?? (body !== undefined ? JSON.stringify(body) : undefined)
  });

  let payload: Envelope<T> | null = null;
  try {
    payload = (await res.json()) as Envelope<T>;
  } catch {
    /* 빈 응답 허용 */
  }

  if (!res.ok || (payload && payload.success === false)) {
    throw new ApiError(
      res.status,
      payload?.code ?? `HTTP_${res.status}`,
      payload?.message ?? res.statusText
    );
  }
  return (payload?.data ?? (undefined as T)) as T;
}

// ── Auth ─────────────────────────────────────────────────────────────
export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: { userId: number; email: string; name?: string; userType?: string };
};

export const auth = {
  login: (email: string, password: string) =>
    api<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password }
    }),
  logout: (refreshToken: string) =>
    api<{ loggedOut: boolean }>("/api/auth/logout", {
      method: "POST",
      body: { refreshToken }
    }),
  me: () =>
    api<{
      userId: number;
      email: string;
      name: string;
      phone: string;
      mfaEnabled: boolean;
    }>("/api/user/me")
};

// ── Corporate ────────────────────────────────────────────────────────
export type CorporateBasicInfo = {
  corporateName: string;
  businessNo: string;
  corporateNo: string;
  address: string;
  businessType: string;
};
export type Representative = {
  name: string;
  birthDate: string;
  phone: string;
  email: string;
};
export type Agent = {
  name: string;
  phone: string;
  email: string;
  authorityScope: string;
};
export type AgentListItem = {
  agentId: number;
  name: string;
  authorityScope: string;
  status: string;
};
export type CorporateProfile = {
  corporateId?: number;
  corporate?: CorporateBasicInfo & { corporateId?: number };
  representative?: Representative;
  agent?: Agent;
};

export const corporate = {
  dashboard: () =>
    api<{
      corporate?: { corporateId?: number; corporateName?: string; businessNo?: string };
      kycSummary?: {
        total?: number;
        inReview?: number;
        supplement?: number;
        approved?: number;
      };
      credentialSummary?: { issued?: number };
      notifications?: Array<{ message: string; createdAt: string; severity?: string }>;
      recentApplications?: Array<{
        kycId: number;
        applicationNo?: string;
        corporateType?: string;
        submittedAt?: string;
        status?: string;
      }>;
    }>("/api/user/dashboard"),
  create: (body: CorporateBasicInfo) =>
    api<{ corporateId: number }>("/api/user/corporates", {
      method: "POST",
      body
    }),
  me: () => api<CorporateProfile>("/api/user/corporates/me"),
  updateBasicInfo: (corporateId: number, body: CorporateBasicInfo) =>
    api<{ updated: boolean }>(
      `/api/user/corporates/${corporateId}/basic-info`,
      { method: "PUT", body }
    ),
  updateRepresentative: (corporateId: number, body: Representative) =>
    api<{ updated: boolean }>(
      `/api/user/corporates/${corporateId}/representative`,
      { method: "PUT", body }
    ),
  updateAgent: (corporateId: number, body: Agent) =>
    api<{ updated: boolean }>(
      `/api/user/corporates/${corporateId}/agent`,
      { method: "PUT", body }
    ),
  agents: (corporateId: number) =>
    api<{ items: AgentListItem[] }>(
      `/api/user/corporates/${corporateId}/agents`
    )
};

// ── KYC ──────────────────────────────────────────────────────────────
export type KycListItem = {
  kycId: number;
  applicationNo?: string;
  corporateType?: string;
  status: string;
  submittedAt?: string;
  completedAt?: string;
};

export type KycStatus = {
  status: string;
  aiReviewStatus?: string;
  vcStatus?: string;
  nextAction?: string;
};

export type KycDocument = {
  documentId: number;
  documentType: string;
  fileName?: string;
  fileHash?: string;
  status?: string;
};

export const kyc = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    api<{ items: KycListItem[]; page?: { totalElements: number } }>(
      "/api/user/kyc/applications",
      { query: params }
    ),
  detail: (kycId: number) =>
    api<{
      application: { kycId: number; status: string; corporateType?: string; submittedAt?: string };
      corporate?: CorporateBasicInfo;
      documents: KycDocument[];
      statusTimeline?: Array<{ status: string; at: string }>;
    }>(`/api/user/kyc/applications/${kycId}`),
  status: (kycId: number) =>
    api<KycStatus>(`/api/user/kyc/applications/${kycId}/status`),
  create: (body: {
    corporateId: number;
    applicationType: string;
    corporateType?: string;
  }) =>
    api<{ kycId: number; status: string }>("/api/user/kyc/applications", {
      method: "POST",
      body
    }),
  setCorporateType: (kycId: number, corporateType: string) =>
    api<{ updated: boolean; requiredDocuments: string[] }>(
      `/api/user/kyc/applications/${kycId}/corporate-type`,
      { method: "PATCH", body: { corporateType } }
    ),
  documentRequirements: (corporateType: string) =>
    api<{ requiredDocuments: string[]; optionalDocuments: string[] }>(
      "/api/user/kyc/document-requirements",
      { query: { corporateType } }
    ),
  submissionSummary: (kycId: number) =>
    api<{
      corporate?: CorporateBasicInfo;
      documents: KycDocument[];
      missingItems: string[];
      storeOption?: string;
    }>(`/api/user/kyc/applications/${kycId}/submission-summary`),
  submit: (kycId: number) =>
    api<{ kycId: number; status: string; submittedAt: string }>(
      `/api/user/kyc/applications/${kycId}/submit`,
      { method: "POST" }
    ),
  documents: (kycId: number) =>
    api<{ items: KycDocument[] }>(
      `/api/user/kyc/applications/${kycId}/documents`
    ),
  uploadDocument: (kycId: number, file: File, documentType: string) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentType", documentType);
    return api<{ documentId: number; fileHash: string; mimeType: string; fileSize: number }>(
      `/api/user/kyc/applications/${kycId}/documents`,
      { method: "POST", formData: fd }
    );
  },
  deleteDocument: (kycId: number, documentId: number) =>
    api<{ deleted: boolean }>(
      `/api/user/kyc/applications/${kycId}/documents/${documentId}`,
      { method: "DELETE" }
    ),
  uploadSupplement: (kycId: number, supplementId: number, file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return api<{ documentId: number; supplementId: number; fileHash: string }>(
      `/api/user/kyc/applications/${kycId}/supplements/${supplementId}/documents`,
      { method: "POST", formData: fd }
    );
  }
};

// ── Credentials ──────────────────────────────────────────────────────
export type CredentialItem = {
  credentialId: number;
  type: string;
  status: string;
  issuedAt: string;
  expiresAt: string;
};
export const credentials = {
  list: (status?: string) =>
    api<{ items: CredentialItem[] }>("/api/user/credentials", {
      query: { status }
    })
};

// ── Notifications ────────────────────────────────────────────────────
export type Notification = {
  notificationId: number;
  title: string;
  message: string;
  readYn: boolean;
  createdAt: string;
};
export const notifications = {
  list: (params?: { page?: number; size?: number; readYn?: boolean }) =>
    api<{ items: Notification[] }>("/api/common/notifications", {
      query: params
    }),
  markRead: (id: number) =>
    api<{ read: boolean }>(`/api/common/notifications/${id}/read`, {
      method: "PATCH"
    })
};
