import axios, { type AxiosInstance } from "axios";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ??
  "https://dev-api-kyvc.khuoo.synology.me";
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

function compactParams(query?: Record<string, string | number | boolean | undefined>) {
  if (!query) return undefined;
  const out: Record<string, string | number | boolean> = {};
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null) out[k] = v;
  }
  return Object.keys(out).length ? out : undefined;
}

function isEnvelope<T>(data: unknown): data is Envelope<T> {
  return (
    typeof data === "object" &&
    data !== null &&
    "success" in data &&
    typeof (data as Envelope<T>).success === "boolean"
  );
}

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  validateStatus: () => true,
  // withCredentials: true
});

apiClient.interceptors.request.use((config) => {
  const token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzIiwiZW1haWwiOiJ0ZXN0QGFiYy5jb20iLCJ1c2VyVHlwZSI6IkNPUlBPUkFURV9VU0VSIiwicm9sZXMiOlsiUk9MRV9DT1JQT1JBVEVfVVNFUiJdLCJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJpc3MiOiJreXZjLWJhY2tlbmQtZGV2IiwiaWF0IjoxNzc4MzEwODA5LCJleHAiOjE3NzgzMTI2MDksImp0aSI6IjE5Yjk3NjUyLWNjMDMtNGQwZC04NGU3LTNjMWE4YTkzNDlmNyJ9.paaLSpZCAcwijztuxRwaFpriebM3cJeyYNFHASrveD4';
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  if (config.data instanceof FormData) {
    config.headers.delete("Content-Type");
  }
  return config;
});

export async function api<T>(path: string, input: RequestInput = {}): Promise<T> {
  const { method = "GET", query, body, formData } = input;
  let res;
  try {
    res = await apiClient.request<unknown>({
      url: path,
      method,
      params: compactParams(query),
      data: formData ?? (body !== undefined ? body : undefined)
    });
  } catch (err) {
    if (axios.isAxiosError(err)) {
      throw new ApiError(0, "NETWORK", err.message || "네트워크 오류");
    }
    throw err;
  }

  let payload: Envelope<T> | null = null;
  if (isEnvelope<T>(res.data)) {
    payload = res.data;
  }

  const status = res.status;
  if (status < 200 || status >= 300 || (payload && payload.success === false)) {
    throw new ApiError(
      status,
      payload?.code ?? `HTTP_${status}`,
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

export type SignupResponse = {
  userId: number;
  email: string;
  userType?: string;
  userStatus?: string;
};

export const auth = {
  signup: (email: string, password: string) =>
    api<SignupResponse>("/api/auth/signup/corporate", {
      method: "POST",
      body: { email, password }
    }),
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
/** GET /api/user/corporates/me — 백엔드 CorporateResponse와 대응 */
export type CorporateProfile = {
  corporateId: number;
  userId?: number;
  corporateName: string;
  businessRegistrationNo: string;
  corporateRegistrationNo?: string | null;
  representativeName: string;
  representativePhone?: string | null;
  representativeEmail?: string | null;
  address?: string | null;
  businessType?: string | null;
  corporateStatusCode?: string;
  createdAt?: string;
  updatedAt?: string;
};

/** POST /api/user/corporates, PUT .../basic-info 요청 본문 */
export type CorporateBasicInfoBody = {
  corporateName: string;
  representativeName: string;
  businessRegistrationNo: string;
  corporateRegistrationNo: string;
  address: string;
  businessType?: string;
  representativePhone?: string;
  representativeEmail?: string;
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
export type RepresentativeResponse = {
  representativeId: number;
  corporateId: number;
  name: string;
  phoneNumber?: string | null;
  email?: string | null;
};
export type AgentResponse = {
  agentId: number;
  corporateId: number;
  name: string;
  phoneNumber?: string | null;
  email?: string | null;
  authorityScope?: string | null;
};
export type AgentListItem = {
  agentId: number;
  name: string;
  authorityScope: string;
  status: string;
};

export const corporate = {
  dashboard: () =>
    api<{
      corporate?: {
        corporateId?: number;
        corporateName?: string;
        businessNo?: string;
      };
      kycSummary?: {
        total?: number;
        inReview?: number;
        supplement?: number;
        approved?: number;
      };
      credentialSummary?: { issued?: number };
      notifications?: Array<{
        message: string;
        createdAt: string;
        severity?: string;
      }>;
      recentApplications?: Array<{
        kycId: number;
        applicationNo?: string;
        corporateType?: string;
        submittedAt?: string;
        status?: string;
      }>;
    }>("/api/user/dashboard"),
  create: (body: CorporateBasicInfoBody) =>
    api<{ corporateId: number }>("/api/user/corporates", {
      method: "POST",
      body,
    }),
  me: () => api<CorporateProfile>("/api/user/corporates/me"),
  updateBasicInfo: (corporateId: number, body: CorporateBasicInfoBody) =>
    api<{ updated: boolean }>(
      `/api/user/corporates/${corporateId}/basic-info`,
      { method: "PUT", body },
    ),
  updateRepresentative: (corporateId: number, body: Representative) =>
    api<RepresentativeResponse>(
      `/api/user/corporates/${corporateId}/representatives`,
      {
        method: "POST",
        body: {
          name: body.name,
          phoneNumber: body.phone,
          email: body.email
        }
      },
    ),
  representatives: (corporateId: number) =>
    api<RepresentativeResponse[]>(
      `/api/user/corporates/${corporateId}/representatives`,
    ),
  updateAgent: (corporateId: number, body: Agent) =>
    api<AgentResponse>(`/api/user/corporates/${corporateId}/agents`, {
      method: "POST",
      body: {
        name: body.name,
        phoneNumber: body.phone,
        email: body.email,
        authorityScope: body.authorityScope
      },
    }),
  agents: (corporateId: number) =>
    api<AgentResponse[]>(
      `/api/user/corporates/${corporateId}/agents`,
    ),
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
  kycStatus?: string;
  corporateTypeCode?: string;
  originalDocumentStoreOption?: string;
  submittedAt?: string;
  aiReviewStatus?: string;
  vcStatus?: string;
  nextAction?: string;
};

export type KycDocument = {
  documentId: number;
  kycId?: number;
  documentType?: string;
  documentTypeCode?: string;
  fileName?: string;
  mimeType?: string;
  fileSize?: number;
  fileHash?: string;
  documentHash?: string;
  status?: string;
  uploadStatus?: string;
  uploadedAt?: string;
};

export type RequiredDocument = {
  documentTypeCode: string;
  documentTypeName: string;
  required: boolean;
  uploaded: boolean;
  description?: string;
  allowedExtensions?: string[];
  maxFileSizeMb?: number;
};

export type KycReviewFinding = {
  findingType: string;
  result: string;
  message?: string;
  confidenceScore?: number;
};

export type KycReviewSummary = {
  kycId: number;
  kycStatus?: string;
  aiReviewStatus?: string;
  aiReviewResult?: string;
  confidenceScore?: number;
  summaryMessage?: string;
  findings?: KycReviewFinding[];
  manualReviewRequired?: boolean;
  reviewedAt?: string;
};

export type KycCompletion = {
  kycId: number;
  corporateId?: number;
  corporateName?: string;
  status?: string;
  approvedAt?: string;
  credentialIssued?: boolean;
  credentialId?: number;
  nextActionCode?: string;
  message?: string;
};

export type SupplementDocument = {
  supplementDocumentId: number;
  documentId?: number;
  documentTypeCode?: string;
  fileName?: string;
  mimeType?: string;
  fileSize?: number;
  documentHash?: string;
  uploadedAt?: string;
};

export type SupplementDetail = {
  supplementId: number;
  kycId: number;
  supplementStatus?: string;
  supplementReasonCode?: string;
  title?: string;
  message?: string;
  requestReason?: string;
  requestedDocumentTypeCodes?: string[];
  uploadedDocuments?: SupplementDocument[];
  requestedAt?: string;
  dueAt?: string;
  completedAt?: string;
  submittedComment?: string;
};

export type SupplementSubmitResponse = {
  kycId: number;
  supplementId: number;
  kycStatus?: string;
  supplementStatus?: string;
  submittedAt?: string;
  message?: string;
};

export const kyc = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    api<{ items: KycListItem[]; page?: { totalElements: number } }>(
      "/api/user/kyc/applications",
      { query: params }
    ),
  detail: (kycId: number) =>
    api<{
      application?: { kycId: number; status?: string; kycStatus?: string; corporateType?: string; corporateTypeCode?: string; submittedAt?: string };
      kycId?: number;
      kycStatus?: string;
      corporateTypeCode?: string;
      submittedAt?: string;
      corporate?: CorporateProfile;
      documents: KycDocument[];
      statusTimeline?: Array<{ status: string; at: string }>;
    }>(`/api/corporate/kyc/applications/${kycId}`),
  status: (kycId: number) =>
    api<KycStatus>(`/api/corporate/kyc/applications/${kycId}/status`),
  create: (body: {
    corporateId?: number;
    applicationType?: string;
    corporateType?: string;
    corporateTypeCode?: string;
  }) =>
    api<{ kycId: number; kycStatus?: string; status?: string; corporateTypeCode?: string }>("/api/corporate/kyc/applications", {
      method: "POST",
      body: { corporateTypeCode: body.corporateTypeCode ?? body.corporateType ?? "CORPORATION" }
    }),
  setCorporateType: (kycId: number, corporateType: string) =>
    api<{ kycId: number; kycStatus?: string; corporateTypeCode?: string; requiredDocuments?: string[] }>(
      `/api/corporate/kyc/applications/${kycId}/corporate-type`,
      { method: "PUT", body: { corporateTypeCode: corporateType } }
    ),
  documentRequirements: (corporateType: string) =>
    api<RequiredDocument[]>("/api/corporate/kyc/required-documents", {
      query: { corporateTypeCode: corporateType }
    }),
  requiredDocumentsByKyc: (kycId: number) =>
    api<RequiredDocument[]>(`/api/corporate/kyc/applications/${kycId}/required-documents`),
  setDocumentStoreOption: (kycId: number, storeOption: "STORE" | "DELETE") =>
    api<{ kycId: number; kycStatus?: string; originalDocumentStoreOption?: string }>(
      `/api/corporate/kyc/applications/${kycId}/document-store-option`,
      { method: "PUT", body: { storeOption } }
    ),
  submissionSummary: (kycId: number) =>
    api<{
      corporate?: CorporateProfile;
      documents: KycDocument[];
      missingItems: string[];
      storeOption?: string;
      canSubmit?: boolean;
    }>(`/api/corporate/kyc/applications/${kycId}/summary`),
  submit: (kycId: number) =>
    api<{ kycId: number; status?: string; kycStatus?: string; submittedAt: string; submittable?: boolean; message?: string }>(
      `/api/corporate/kyc/applications/${kycId}/submit`,
      { method: "POST" }
    ),
  documents: (kycId: number) =>
    api<KycDocument[]>(`/api/corporate/kyc/applications/${kycId}/documents`),
  uploadDocument: (kycId: number, file: File, documentType: string) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentTypeCode", documentType);
    return api<KycDocument>(
      `/api/corporate/kyc/applications/${kycId}/documents`,
      { method: "POST", formData: fd }
    );
  },
  deleteDocument: (kycId: number, documentId: number) =>
    api<{ deleted: boolean }>(
      `/api/corporate/kyc/applications/${kycId}/documents/${documentId}`,
      { method: "DELETE" }
    ),
  uploadSupplement: (kycId: number, supplementId: number, file: File, documentTypeCode?: string) => {
    const fd = new FormData();
    fd.append("file", file);
    if (documentTypeCode) fd.append("documentTypeCode", documentTypeCode);
    return api<SupplementDocument>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}/documents`,
      { method: "POST", formData: fd }
    );
  },
  reviewSummary: (kycId: number) =>
    api<KycReviewSummary>(`/api/corporate/kyc/applications/${kycId}/ai-review-summary`),
  completion: (kycId: number) =>
    api<KycCompletion>(`/api/corporate/kyc/applications/${kycId}/completion`),
  current: () =>
    api<{ kycId: number; kycStatus?: string; corporateTypeCode?: string; submittedAt?: string }>(
      "/api/corporate/kyc/applications/current"
    ),
  supplements: (kycId: number) =>
    api<{ supplements: SupplementDetail[] }>(
      `/api/corporate/kyc/applications/${kycId}/supplements`
    ),
  supplement: (kycId: number, supplementId: number) =>
    api<SupplementDetail>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}`
    ),
  submitSupplement: (kycId: number, supplementId: number, submittedComment?: string) =>
    api<SupplementSubmitResponse>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}/submit`,
      { method: "POST", body: { submittedComment } }
    )
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
