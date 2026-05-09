import axios, { type AxiosInstance } from "axios";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080";
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

/** HttpOnly 인증 쿠키를 크로스 오리진 요청에 포함하려면 반드시 true여야 합니다. */
const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  validateStatus: () => true,
  // withCredentials: true
});

apiClient.interceptors.request.use((config) => {
  const token = session.token;
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
/** 로그인 본문(액세스·리프레시 토큰은 HttpOnly 쿠키로만 발급). */
export type LoginResponse = {
  userId: number;
  email: string;
  userType: string;
  roles: string[];
};

export type SignupResponse = {
  userId: number;
  email: string;
  userType?: string;
  userStatus?: string;
};

/** GET /api/common/session — 백엔드 SessionResponse */
export type SessionResponse = {
  authenticated: boolean;
  userId?: number;
  email?: string;
  userName?: string;
  userType?: string;
  userStatus?: string;
  roles?: string[];
  corporateId?: number;
  corporateName?: string;
  corporateRegistered?: boolean;
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
  logout: () =>
    api<{ loggedOut: boolean }>("/api/auth/logout", {
      method: "POST"
    }),
  session: () => api<SessionResponse>("/api/common/session")
};

// ── Corporate ────────────────────────────────────────────────────────
/** GET /api/user/corporates/me — 백엔드 CorporateResponse와 대응 */
export type CorporateProfile = {
  corporateId: number;
  userId?: number;
  corporateName: string;
  businessRegistrationNo: string;
  corporateRegistrationNo?: string | null;
  corporateTypeCode?: string | null;
  establishedDate?: string | null;
  corporatePhone?: string | null;
  representativeName: string;
  representativePhone?: string | null;
  representativeEmail?: string | null;
  address?: string | null;
  website?: string | null;
  businessType?: string | null;
  corporateStatusCode?: string;
  createdAt?: string;
  updatedAt?: string;
};

/** GET /api/user/dashboard — 백엔드 UserDashboardResponse */
export type UserDashboardResponse = {
  userId?: number;
  corporateRegistered?: boolean;
  corporateId?: number;
  corporateName?: string;
  activeKycId?: number;
  activeKycStatus?: string;
  needSupplementCount?: number;
  notificationUnreadCount?: number;
  credentialIssued?: boolean;
};

/** GET /api/corporate/kyc/applications, /current — KycApplicationResponse */
export type KycApplicationResponse = {
  kycId: number;
  corporateId?: number;
  applicantUserId?: number;
  corporateTypeCode?: string;
  kycStatus?: string;
  originalDocumentStoreOption?: string;
  submittedAt?: string;
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
  dashboard: () => api<UserDashboardResponse>("/api/user/dashboard"),
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
export type KycStatusResponse = {
  kycId: number;
  kycStatus?: string;
  corporateTypeCode?: string;
  originalDocumentStoreOption?: string;
  submittedAt?: string;
};

export type KycDocument = {
  documentId: number;
  kycId?: number;
  documentTypeCode?: string;
  fileName?: string;
  mimeType?: string;
  fileSize?: number;
  documentHash?: string;
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

export type KycMissingItem = {
  code: string;
  message: string;
  target?: string;
};

export type KycApplicationSummaryResponse = {
  kycId: number;
  kycStatus?: string;
  corporateId?: number;
  corporateName?: string;
  businessRegistrationNo?: string;
  corporateRegistrationNo?: string;
  representativeName?: string;
  representativePhone?: string;
  representativeEmail?: string;
  agentName?: string;
  agentPhone?: string;
  agentEmail?: string;
  agentAuthorityScope?: string;
  corporateTypeCode?: string;
  documentStoreOption?: string;
  documents: KycDocument[];
  requiredDocuments: RequiredDocument[];
  submittable: boolean;
  missingItems: KycMissingItem[];
  createdAt?: string;
  updatedAt?: string;
  submittedAt?: string;
};

export type KycSubmitResponse = {
  kycId: number;
  kycStatus?: string;
  submittedAt: string;
  submittable?: boolean;
  message?: string;
};

export type KycCompletionResponse = {
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

export type KycReviewFinding = {
  findingType?: string;
  result?: string;
  message?: string;
  confidenceScore?: number;
};

export type KycReviewSummaryResponse = {
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

export type Supplement = {
  supplementId: number;
  kycId?: number;
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

export type CredentialOfferResponse = {
  offerId: number;
  credentialId?: number;
  qrToken?: string;
  expiresAt?: string;
  qrPayload?: Record<string, unknown>;
};

export type DocumentPreviewResponse = {
  previewUrl: string;
  expiresAt?: string;
};

export const kyc = {
  /** 현재 진행 중인 KYC 신청 단건. 없으면 ApiError 반환. */
  current: () =>
    api<KycApplicationResponse>("/api/corporate/kyc/applications/current"),
  detail: (kycId: number) =>
    api<KycApplicationResponse>(`/api/corporate/kyc/applications/${kycId}`),
  status: (kycId: number) =>
    api<KycStatusResponse>(`/api/corporate/kyc/applications/${kycId}/status`),
  create: (corporateTypeCode: string) =>
    api<KycApplicationResponse>("/api/corporate/kyc/applications", {
      method: "POST",
      body: { corporateTypeCode }
    }),
  setCorporateType: (kycId: number, corporateTypeCode: string) =>
    api<KycApplicationResponse>(
      `/api/corporate/kyc/applications/${kycId}/corporate-type`,
      { method: "PUT", body: { corporateTypeCode } }
    ),
  setDocumentStoreOption: (kycId: number, storeOption: "STORE" | "DELETE") =>
    api<KycApplicationResponse>(
      `/api/corporate/kyc/applications/${kycId}/document-store-option`,
      { method: "PUT", body: { storeOption } }
    ),
  documentRequirements: (corporateTypeCode: string) =>
    api<RequiredDocument[]>("/api/corporate/kyc/required-documents", {
      query: { corporateTypeCode }
    }),
  requiredDocumentsByKyc: (kycId: number) =>
    api<RequiredDocument[]>(
      `/api/corporate/kyc/applications/${kycId}/required-documents`
    ),
  summary: (kycId: number) =>
    api<KycApplicationSummaryResponse>(
      `/api/corporate/kyc/applications/${kycId}/summary`
    ),
  documents: (kycId: number) =>
    api<KycDocument[]>(`/api/corporate/kyc/applications/${kycId}/documents`),
  uploadDocument: (kycId: number, file: File, documentTypeCode: string) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentTypeCode", documentTypeCode);
    return api<KycDocument>(
      `/api/corporate/kyc/applications/${kycId}/documents`,
      { method: "POST", formData: fd }
    );
  },
  deleteDocument: (kycId: number, documentId: number) =>
    api<void>(
      `/api/corporate/kyc/applications/${kycId}/documents/${documentId}`,
      { method: "DELETE" }
    ),
  documentPreview: (kycId: number, documentId: number) =>
    api<DocumentPreviewResponse>(
      `/api/corporate/kyc/applications/${kycId}/documents/${documentId}/preview`
    ),
  submit: (kycId: number) =>
    api<KycSubmitResponse>(
      `/api/corporate/kyc/applications/${kycId}/submit`,
      { method: "POST" }
    ),
  completion: (kycId: number) =>
    api<KycCompletionResponse>(
      `/api/corporate/kyc/applications/${kycId}/completion`
    ),
  aiReviewSummary: (kycId: number) =>
    api<KycReviewSummaryResponse>(
      `/api/corporate/kyc/applications/${kycId}/ai-review-summary`
    ),
  supplements: (kycId: number) =>
    api<{ supplements: Supplement[] }>(
      `/api/corporate/kyc/applications/${kycId}/supplements`
    ),
  supplementDetail: (kycId: number, supplementId: number) =>
    api<Supplement>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}`
    ),
  uploadSupplement: (
    kycId: number,
    supplementId: number,
    file: File,
    documentTypeCode: string
  ) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentTypeCode", documentTypeCode);
    return api<SupplementDocument>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}/documents`,
      { method: "POST", formData: fd }
    );
  },
  submitSupplement: (
    kycId: number,
    supplementId: number,
    submittedComment?: string
  ) =>
    api<SupplementSubmitResponse>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}/submit`,
      { method: "POST", body: { submittedComment: submittedComment ?? "" } }
    ),
  credentialOffer: (kycId: number) =>
    api<CredentialOfferResponse>(
      `/api/user/kyc/applications/${kycId}/credential-offer`
    )
};

// ── Credentials ──────────────────────────────────────────────────────
export type CredentialSummary = {
  credentialId: number;
  kycId?: number;
  credentialTypeCode?: string;
  credentialStatusCode?: string;
  issuerDid?: string;
  issuedAt?: string;
  expiresAt?: string;
  walletSaved?: boolean;
  walletSavedAt?: string;
};
export type CredentialListResponse = {
  credentials: CredentialSummary[];
  totalCount: number;
};
export const credentials = {
  list: () => api<CredentialListResponse>("/api/user/credentials")
};

// ── Notifications ────────────────────────────────────────────────────
export type Notification = {
  notificationId: number;
  notificationType?: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
};
export type NotificationPageResponse = {
  content: Notification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export const notifications = {
  list: (params?: { page?: number; size?: number }) =>
    api<NotificationPageResponse>("/api/common/notifications", {
      query: params
    }),
  unreadCount: () =>
    api<{ unreadCount: number }>("/api/common/notifications/unread-count"),
  markRead: (id: number) =>
    api<{ read: boolean }>(`/api/common/notifications/${id}/read`, {
      method: "PATCH"
    }),
  markAllRead: () =>
    api<{ updated: number }>("/api/common/notifications/read-all", {
      method: "PATCH"
    })
};
