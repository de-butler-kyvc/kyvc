import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";

export const BASE_URL = "https://dev-api-kyvc.khuoo.synology.me";
const REFRESH_TOKEN_PATH = "/api/auth/token/refresh";

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
    message: string,
  ) {
    super(message);
  }
}

type RequestInput = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  query?: Record<string, string | number | boolean | undefined>;
  body?: unknown;
  formData?: FormData;
};

function compactParams(
  query?: Record<string, string | number | boolean | undefined>,
) {
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
  withCredentials: true
});

apiClient.interceptors.request.use((config) => {
  if (config.data instanceof FormData) {
    config.headers.delete("Content-Type");
  }
  return config;
});

type KyvcAxiosRequestConfig = InternalAxiosRequestConfig & {
  _kyvcDidAttemptRefresh?: boolean;
};

/** axios config.url은 상대 경로인 경우가 많아 pathname만 뽑는다 */
function requestPathname(config: InternalAxiosRequestConfig): string {
  const raw = config.url ?? "";
  const pathOnly = raw.split("?")[0] ?? "";
  if (pathOnly.startsWith("/")) return pathOnly;
  try {
    return new URL(pathOnly, config.baseURL || BASE_URL).pathname;
  } catch {
    return pathOnly;
  }
}

/** 비밀번호 실패 등으로 401이 나와도 리프레시를 시도하면 불필요한 요청만 늘어난다 */
function skipRefreshOn401(pathname: string): boolean {
  if (pathname === REFRESH_TOKEN_PATH) return true;
  return (
    pathname.startsWith("/api/auth/login") ||
    pathname.startsWith("/api/auth/signup") ||
    pathname.startsWith("/api/auth/password-reset") ||
    pathname.startsWith("/api/mobile/auth/auto-login") ||
    pathname.startsWith("/api/mobile/auth/login") ||
    pathname.startsWith("/api/auth/dev/")
  );
}

let refreshInFlight: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await apiClient.post<unknown>(
          REFRESH_TOKEN_PATH,
          undefined,
          {
            headers: { Accept: "application/json" },
          },
        );
        const payload = isEnvelope<{ refreshed?: boolean }>(res.data)
          ? res.data
          : null;
        return (
          res.status >= 200 &&
          res.status < 300 &&
          (!payload || payload.success !== false)
        );
      } catch {
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

apiClient.interceptors.response.use((response) => {
  const config = response.config as KyvcAxiosRequestConfig;
  if (
    response.status !== 401 ||
    config._kyvcDidAttemptRefresh ||
    skipRefreshOn401(requestPathname(config))
  ) {
    return response;
  }

  config._kyvcDidAttemptRefresh = true;
  return refreshAccessToken().then((ok) => {
    if (!ok) return response;
    return apiClient.request(config);
  });
});

async function requestApi<T>(
  path: string,
  input: RequestInput,
) {
  const { method = "GET", query, body, formData } = input;

  return apiClient.request<T>({
    url: path,
    method,
    params: compactParams(query),
    data: formData ?? (body !== undefined ? body : undefined),
  });
}

export async function api<T>(
  path: string,
  input: RequestInput = {},
): Promise<T> {
  let res;
  try {
    res = await requestApi<unknown>(path, input);
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
      payload?.message ?? res.statusText,
    );
  }
  return (payload ? payload.data : res.data) as T;
}

// ── Auth ─────────────────────────────────────────────────────────────
/** 로그인 본문(액세스·리프레시 토큰은 HttpOnly 쿠키로만 발급). */
export type LoginResponse = {
  userId: number;
  email: string;
  userType: string;
  roles: string[];
};

export type MobileLoginRequest = {
  email: string;
  password: string;
  autoLogin: boolean;
  deviceId: string;
  deviceName?: string;
  os: string;
  appVersion?: string;
  publicKey?: string;
};

export type MobileLoginResponse = {
  userId: number;
  userType?: string;
  email: string;
  name?: string | null;
  deviceId?: string;
  deviceRegistered?: boolean;
  accessTokenExpiresAt?: string;
};

export type MobileAutoLoginResponse = {
  autoLogin: boolean;
  userId?: number;
  corporateId?: number | null;
  email?: string;
  corporateName?: string | null;
  roleCode?: string;
  accessTokenExpiresAt?: string;
  refreshTokenExpiresAt?: string;
};

export type SignupResponse = {
  userId: number;
  email: string;
  userName?: string;
  phone?: string;
  corporateName?: string;
  userType?: string;
  userStatus?: string;
};

export type CorporateSignupBody = {
  email: string;
  password: string;
  userName: string;
  phone?: string;
  corporateName: string;
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

export type MfaChannel = "EMAIL";
export type MfaPurpose =
  | "LOGIN"
  | "IMPORTANT_ACTION"
  | "PASSWORD_RESET"
  | "KYC_APPROVE"
  | "KYC_REJECT"
  | "VC_ISSUE"
  | "POLICY_CHANGE";

export type MfaChallengeResponse = {
  challengeId: string;
  expiresAt: string;
  maskedTarget: string;
};

export type MfaVerifyResponse = {
  verified: boolean;
  mfaToken?: string;
};

export type SignupEmailVerificationRequestResponse = {
  verificationId: number;
  maskedEmail: string;
  expiresAt: string;
  requested: boolean;
};

export type SignupEmailVerificationVerifyResponse = {
  verified: boolean;
  email: string;
};

export type VpLoginQrPayload = {
  type: string;
  requestId: string;
  qrToken: string;
};

export type VpLoginStartResponse = {
  requestId: string;
  qrPayload: VpLoginQrPayload;
  expiresAt: string;
};

export type VpLoginStatusResponse = {
  requestId: string;
  status: string;
  canComplete: boolean;
  expiresAt: string;
};

export type VpLoginCompleteResponse = {
  userId: number;
  corporateId: number;
  email: string;
  userName?: string;
};

export const auth = {
  signup: (body: CorporateSignupBody) =>
    api<SignupResponse>("/api/auth/signup/corporate", {
      method: "POST",
      body,
    }),
  login: (email: string, password: string) =>
    api<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password },
    }),
  mobileLogin: (body: MobileLoginRequest) =>
    api<MobileLoginResponse>("/api/mobile/auth/login", {
      method: "POST",
      body,
    }),
  mobileAutoLogin: () =>
    api<MobileAutoLoginResponse>("/api/mobile/auth/auto-login", {
      method: "POST",
    }),
  logout: () =>
    api<{ loggedOut: boolean }>("/api/auth/logout", {
      method: "POST",
    }),
  session: () => api<SessionResponse>("/api/common/session"),
  startVpLogin: () =>
    api<VpLoginStartResponse>("/api/auth/vp-login-requests", {
      method: "POST",
      body: {},
    }),
  getVpLoginStatus: (requestId: string) =>
    api<VpLoginStatusResponse>(
      `/api/auth/vp-login-requests/${encodeURIComponent(requestId)}/status`,
    ),
  completeVpLogin: (requestId: string) =>
    api<VpLoginCompleteResponse>(
      `/api/auth/vp-login-requests/${encodeURIComponent(requestId)}/complete`,
      {
        method: "POST",
        body: {},
      },
    ),
  mfaChallenge: (channel: MfaChannel, purpose: MfaPurpose) =>
    api<MfaChallengeResponse>("/api/auth/mfa/challenge", {
      method: "POST",
      body: { channel, purpose },
    }),
  mfaVerify: (challengeId: string, verificationCode: string) =>
    api<MfaVerifyResponse>("/api/auth/mfa/verify", {
      method: "POST",
      body: { challengeId, verificationCode },
    }),
  requestSignupEmailVerification: (email: string) =>
    api<SignupEmailVerificationRequestResponse>(
      "/api/auth/email-verifications/request",
      {
        method: "POST",
        body: { email, purpose: "SIGNUP" },
      },
    ),
  verifySignupEmail: (
    verificationId: number,
    email: string,
    verificationCode: string,
  ) =>
    api<SignupEmailVerificationVerifyResponse>(
      "/api/auth/email-verifications/verify",
      {
        method: "POST",
        body: { verificationId, email, verificationCode },
      },
    ),
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
  businessRegistrationNo: string;
  corporateRegistrationNo?: string | null;
  corporateTypeCode?: string | null;
  establishedDate: string;
  corporatePhone?: string | null;
  address?: string | null;
  website?: string | null;
  businessType: string;
};
export type Representative = {
  name: string;
  birthDate: string;
  nationalityCode: string;
  phone: string;
  email: string;
  identityFile?: File;
};
export type Agent = {
  name: string;
  phone: string;
  email: string;
  relationshipOrPosition: string;
  powerOfAttorneyFile?: File;
};
export type RepresentativeResponse = {
  representativeId: number;
  corporateId: number;
  name: string;
  birthDate?: string | null;
  nationalityCode?: string | null;
  phoneNumber?: string | null;
  email?: string | null;
  identityDocumentId?: number | null;
};
export type AgentResponse = {
  agentId: number;
  corporateId: number;
  name: string;
  relationshipOrPosition?: string | null;
  phoneNumber?: string | null;
  email?: string | null;
  delegationDocumentId?: number | null;
  /** @deprecated older API field kept for compatibility with stale responses */
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
    api<CorporateProfile>("/api/user/corporates", {
      method: "POST",
      body,
    }),
  me: () => api<CorporateProfile>("/api/user/corporates/me"),
  updateBasicInfo: (corporateId: number, body: CorporateBasicInfoBody) =>
    api<CorporateProfile>(
      `/api/user/corporates/${corporateId}/basic-info`,
      { method: "PUT", body },
    ),
  updateRepresentative: (corporateId: number, body: Representative) => {
    const fd = new FormData();
    fd.append("name", body.name);
    fd.append("birthDate", body.birthDate);
    fd.append("nationalityCode", body.nationalityCode);
    fd.append("phoneNumber", body.phone);
    fd.append("email", body.email);
    if (body.identityFile) fd.append("identityFile", body.identityFile);

    return api<RepresentativeResponse>(
      `/api/user/corporates/${corporateId}/representatives`,
      { method: "POST", formData: fd },
    );
  },
  representatives: (corporateId: number) =>
    api<RepresentativeResponse[]>(
      `/api/user/corporates/${corporateId}/representatives`,
    ),
  updateAgent: (corporateId: number, body: Agent) => {
    const fd = new FormData();
    fd.append("name", body.name);
    fd.append("relationshipOrPosition", body.relationshipOrPosition);
    fd.append("phoneNumber", body.phone);
    fd.append("email", body.email);
    if (body.powerOfAttorneyFile) {
      fd.append("powerOfAttorneyFile", body.powerOfAttorneyFile);
    }

    return api<AgentResponse>(`/api/user/corporates/${corporateId}/agents`, {
      method: "POST",
      formData: fd,
    });
  },
  agents: (corporateId: number) =>
    api<AgentResponse[]>(`/api/user/corporates/${corporateId}/agents`),
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
  groupCode?: string | null;
  groupName?: string | null;
  minRequiredCount?: number | null;
  groupCandidate?: boolean;
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

export type KycAiReviewDocumentResult = {
  documentId?: number | null;
  documentTypeCode?: string | null;
  documentTypeName?: string | null;
  resultCode?: string | null;
  confidenceScore?: number | null;
  message?: string | null;
};

export type KycAiReviewMismatchResult = {
  fieldName?: string | null;
  sourceDocumentTypeCode?: string | null;
  targetDocumentTypeCode?: string | null;
  severityCode?: string | null;
  message?: string | null;
};

export type KycAiReviewBeneficialOwnerResult = {
  ownerName?: string | null;
  ownershipRatio?: number | null;
  resultCode?: string | null;
  message?: string | null;
};

export type KycAiReviewDelegationResult = {
  resultCode?: string | null;
  message?: string | null;
};

export type KycAiReviewDetailResponse = {
  kycId: number;
  applicationStatusCode?: string | null;
  aiReviewStatusCode?: string | null;
  overallResultCode?: string | null;
  confidenceScore?: number | null;
  reviewedAt?: string | null;
  manualReviewRequired: boolean;
  supplementRequired: boolean;
  summary?: string | null;
  documentResults: KycAiReviewDocumentResult[];
  mismatchResults: KycAiReviewMismatchResult[];
  beneficialOwnerResults: KycAiReviewBeneficialOwnerResult[];
  delegationResult?: KycAiReviewDelegationResult | null;
  reviewReasons: string[];
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

export type CredentialIssueResponse = {
  credentialId: number;
  status?: string;
  txStatus?: string;
  issuedAt?: string;
  failureReason?: string;
};

export type CredentialOfferQrPayload = {
  type: "CREDENTIAL_OFFER" | string;
  offerId: number;
  qrToken: string;
  expiresAt: string;
};

export type CredentialOfferCreateResponse = {
  offerId: number;
  kycId: number;
  qrPayload: CredentialOfferQrPayload;
  expiresAt: string;
  offerStatus: string;
};

export type CredentialOfferStatusResponse = {
  offerId: number;
  kycId: number;
  offerStatus: string;
  credentialId?: number | null;
  credentialStatus?: string | null;
  walletSaved: boolean;
  usedAt?: string | null;
  expiresAt: string;
};

export type DocumentPreviewResponse = {
  previewUrl: string;
  expiresAt?: string;
};
export type DocumentDeleteResponse = {
  deleted?: boolean;
};

export type UserDocumentItem = {
  documentId: number;
  kycId: number;
  corporateId?: number;
  corporateName?: string;
  documentTypeCode?: string;
  documentTypeName?: string;
  fileName?: string;
  mimeType?: string;
  fileSize?: number;
  uploadStatusCode?: string;
  uploadedAt?: string;
  kycStatusCode?: string;
  deleteRequestId?: number | null;
  deleteRequestStatusCode?: string | null;
};

export type UserDocumentListResponse = {
  items: UserDocumentItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type KycApplicationHistoryItem = {
  kycId: number;
  corporateId?: number;
  corporateName?: string;
  businessRegistrationNo?: string;
  corporateTypeCode?: string;
  kycStatusCode?: string;
  aiReviewStatusCode?: string | null;
  aiReviewResultCode?: string | null;
  submittedAt?: string | null;
  approvedAt?: string | null;
  rejectedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  credentialId?: number | null;
  credentialStatusCode?: string | null;
  credentialIssuedAt?: string | null;
};

export type KycApplicationHistoryResponse = {
  items: KycApplicationHistoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export const kyc = {
  list: () =>
    api<KycApplicationResponse | KycApplicationResponse[]>(
      "/api/corporate/kyc/applications",
    ),
  history: (query?: {
    status?: string;
    keyword?: string;
    page?: number;
    size?: number;
  }) =>
    api<KycApplicationHistoryResponse>(
      "/api/corporate/kyc/applications/history",
      { query },
    ),
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
      body: { corporateTypeCode },
    }),
  setCorporateType: (kycId: number, corporateTypeCode: string) =>
    api<KycApplicationResponse>(
      `/api/corporate/kyc/applications/${kycId}/corporate-type`,
      { method: "PUT", body: { corporateTypeCode } },
    ),
  setDocumentStoreOption: (kycId: number, storeOption: "STORE" | "DELETE") =>
    api<KycApplicationResponse>(
      `/api/corporate/kyc/applications/${kycId}/document-store-option`,
      { method: "PUT", body: { storeOption } },
    ),
  documentRequirements: (corporateTypeCode: string) =>
    api<RequiredDocument[]>("/api/corporate/kyc/required-documents", {
      query: { corporateTypeCode },
    }),
  requiredDocumentsByKyc: (kycId: number) =>
    api<RequiredDocument[]>(
      `/api/corporate/kyc/applications/${kycId}/required-documents`,
    ),
  summary: (kycId: number) =>
    api<KycApplicationSummaryResponse>(
      `/api/corporate/kyc/applications/${kycId}/summary`,
    ),
  documents: (kycId: number) =>
    api<KycDocument[]>(`/api/corporate/kyc/applications/${kycId}/documents`),
  uploadDocument: (kycId: number, file: File, documentTypeCode: string) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentTypeCode", documentTypeCode);
    return api<KycDocument>(
      `/api/corporate/kyc/applications/${kycId}/documents`,
      { method: "POST", formData: fd },
    );
  },
  deleteDocument: (kycId: number, documentId: number) =>
    api<DocumentDeleteResponse>(
      `/api/corporate/kyc/applications/${kycId}/documents/${documentId}`,
      { method: "DELETE" },
    ),
  documentPreview: (kycId: number, documentId: number) =>
    api<DocumentPreviewResponse>(
      `/api/corporate/kyc/applications/${kycId}/documents/${documentId}/preview`,
    ),
  submit: (kycId: number) =>
    api<KycSubmitResponse>(`/api/corporate/kyc/applications/${kycId}/submit`, {
      method: "POST",
    }),
  completion: (kycId: number) =>
    api<KycCompletionResponse>(
      `/api/corporate/kyc/applications/${kycId}/completion`,
    ),
  aiReviewSummary: (kycId: number) =>
    api<KycReviewSummaryResponse>(
      `/api/corporate/kyc/applications/${kycId}/ai-review-summary`,
    ),
  aiReviewResult: (kycId: number) =>
    api<KycAiReviewDetailResponse>(
      `/api/user/kyc/applications/${kycId}/ai-review-result`,
    ),
  supplements: (kycId: number) =>
    api<{ supplements: Supplement[] }>(
      `/api/corporate/kyc/applications/${kycId}/supplements`,
    ),
  supplementDetail: (kycId: number, supplementId: number) =>
    api<Supplement>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}`,
    ),
  uploadSupplement: (
    kycId: number,
    supplementId: number,
    file: File,
    documentTypeCode: string,
  ) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentTypeCode", documentTypeCode);
    return api<SupplementDocument>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}/documents`,
      { method: "POST", formData: fd },
    );
  },
  submitSupplement: (
    kycId: number,
    supplementId: number,
    submittedComment?: string,
  ) =>
    api<SupplementSubmitResponse>(
      `/api/corporate/kyc/applications/${kycId}/supplements/${supplementId}/submit`,
      { method: "POST", body: { submittedComment: submittedComment ?? "" } },
    ),
  credentialOffer: (kycId: number) =>
    api<CredentialOfferResponse>(
      `/api/user/kyc/applications/${kycId}/credential-offer`,
    ),
  issueCredential: (kycId: number) =>
    api<CredentialIssueResponse>(
      `/api/user/kyc/applications/${kycId}/credentials`,
      { method: "POST" },
    ),
};

export const userDocuments = {
  list: (query?: {
    documentTypeCode?: string;
    status?: string;
    page?: number;
    size?: number;
  }) =>
    api<UserDocumentListResponse>("/api/user/documents", {
      query,
    }),
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
export type CredentialDetailResponse = CredentialSummary & {
  corporateId?: number;
  credentialExternalId?: string;
  vcHash?: string;
  xrplTxHash?: string;
  walletSavedYn?: "Y" | "N" | string;
  holderDid?: string;
  holderXrplAddress?: string;
  credentialStatusId?: string;
  credentialStatusPurposeCode?: string;
  kycLevelCode?: string;
  jurisdictionCode?: string;
};
export type CredentialListResponse = {
  credentials: CredentialSummary[];
  totalCount: number;
};
export type CredentialIssueGuideResponse = {
  corporateId?: number;
  latestKycId?: number;
  kycStatus?: string;
  credentialIssued?: boolean;
  credentialStatus?: string;
  issueAvailable?: boolean;
  nextActionCode?: string;
  guideTitle?: string;
  guideMessage?: string;
};
export const credentials = {
  list: () => api<CredentialListResponse>("/api/user/credentials"),
  detail: (credentialId: number) =>
    api<CredentialDetailResponse>(`/api/user/credentials/${credentialId}`),
  offerForKyc: (kycId: number) =>
    api<CredentialOfferResponse>(
      `/api/user/kyc/applications/${kycId}/credential-offer`,
    ),
  issueGuide: () =>
    api<CredentialIssueGuideResponse>("/api/corporate/credentials/issue-guide"),
};

export const credentialOffers = {
  createForKyc: (kycId: number) =>
    api<CredentialOfferCreateResponse>(
      `/api/user/kyc/applications/${kycId}/credential-offers`,
      { method: "POST" },
    ),
  status: (offerId: number) =>
    api<CredentialOfferStatusResponse>(
      `/api/user/credential-offers/${offerId}/status`,
    ),
};

// ── User VP Presentations ────────────────────────────────────────────
export type UserVpPresentationSummary = {
  presentationId: number;
  requestId?: string;
  verifierName?: string;
  purpose?: string;
  verificationStatus?: string;
  presentedAt?: string;
};

export type UserVpPresentationListResponse = {
  items: UserVpPresentationSummary[];
  page: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
};

export const userVpPresentations = {
  list: (query?: {
    page?: number;
    size?: number;
    status?: string;
    verifierName?: string;
  }) =>
    api<UserVpPresentationListResponse>("/api/user/vp-presentations", {
      query,
    }),
};

// ── Common DID Institution ───────────────────────────────────────────
export type DidInstitutionResponse = {
  did: string;
  institutionName: string;
  status: string;
};

export const didInstitutions = {
  get: (did: string) =>
    api<DidInstitutionResponse>(
      `/api/common/dids/${encodeURIComponent(did)}/institution`,
    ),
};

// ── Mobile VP / QR ───────────────────────────────────────────────────
export type QrResolveResponse = {
  type: string;
  targetId?: string;
  offerId?: number;
  requestId?: string;
  nextAction?: string;
  message?: string;
};

export type WalletCredentialOfferResponse = {
  offerId: number;
  kycId: number;
  credentialId?: number | null;
  credentialTypeCode?: string | null;
  issuerDid?: string | null;
  corporateName?: string | null;
  businessNumber?: string | null;
  expiresAt?: string | null;
  alreadySaved: boolean;
};

export type MobileDeviceRegisterRequest = {
  deviceId: string;
  deviceName?: string;
  os?: string;
  appVersion?: string;
  publicKey?: string;
};

export type MobileDeviceRegisterResponse = {
  deviceId?: string;
  registered?: boolean;
  status?: string;
  deviceStatusCode?: string;
  active?: boolean;
};

export type WalletCredentialPrepareRequest = {
  qrToken: string;
  deviceId: string;
  holderDid: string;
  holderXrplAddress: string;
  accepted: true;
};

export type WalletCredentialPayload = {
  format: "dc+sd-jwt" | "vc+jwt" | string;
  sdJwt?: string;
  credentialJwt?: string;
  vcJwt?: string;
  credential?: unknown;
  selectiveDisclosure?: {
    disclosablePaths?: string[];
    [key: string]: unknown;
  };
  metadata?: {
    credentialId?: number | string;
    credentialType?: string;
    issuerDid?: string;
    issuerAccount?: string;
    holderDid?: string;
    holderXrplAddress?: string;
    vcHash?: string;
    xrplTxHash?: string;
    credentialStatusId?: string;
    issuedAt?: string;
    expiresAt?: string;
    format?: string;
    [key: string]: unknown;
  };
  [key: string]: unknown;
};

export type WalletCredentialPrepareResponse = {
  offerId: number;
  credentialId: number;
  prepared: boolean;
  credentialPayload: WalletCredentialPayload;
  documentAttachments?: Record<string, unknown>[];
  documentAttachmentManifest?: unknown;
};

export type WalletCredentialConfirmRequest = {
  credentialId: number;
  deviceId: string;
  walletSaved: true;
  walletSavedAt?: string;
  credentialAcceptHash?: string;
};

export type WalletCredentialConfirmResponse = {
  offerId: number;
  credentialId: number;
  walletSaved: boolean;
  offerStatus: string;
  credentialStatus: string;
  walletSavedAt?: string | null;
};

export type VpRequestResponse = {
  requestId: string;
  requesterName?: string;
  purpose?: string;
  requiredClaims?: string;
  challenge?: string;
  nonce?: string;
  expiresAt?: string;
  expired?: boolean;
  submitted?: boolean;
  status?: string;
};

export type EligibleCredentialResponse = {
  credentialId: number;
  credentialTypeCode?: string;
  issuerDid?: string;
  issuedAt?: string;
  expiresAt?: string;
  matchReason?: string;
};

export type EligibleCredentialListResponse = {
  requestId: string;
  credentials: EligibleCredentialResponse[];
  totalCount: number;
};

export type VpPresentationResponse = {
  presentationId: number;
  requestId: string;
  credentialId: number;
  status?: string;
  submittedAt?: string;
  verifiedAt?: string;
  message?: string;
};

export const mobileVp = {
  resolveQr: (qrPayload: string) =>
    api<QrResolveResponse>("/api/mobile/qr/resolve", {
      method: "POST",
      body: { qrPayload },
    }),
  request: (requestId: string) =>
    api<VpRequestResponse>(`/api/mobile/vp/requests/${requestId}`),
  eligibleCredentials: (requestId: string) =>
    api<EligibleCredentialListResponse>(
      `/api/mobile/vp/requests/${requestId}/eligible-credentials`,
    ),
  submitPresentation: (body: {
    requestId: string;
    credentialId: number;
    nonce?: string;
    challenge?: string;
    vpJwt?: string;
    format?: string;
    presentation?: unknown;
    deviceId?: string;
  }) =>
    api<VpPresentationResponse>("/api/mobile/vp/presentations", {
      method: "POST",
      body,
    }),
};

export const mobileDevice = {
  register: (body: MobileDeviceRegisterRequest) =>
    api<MobileDeviceRegisterResponse>("/api/mobile/device/register", {
      method: "POST",
      body,
    }),
};

export const mobileWallet = {
  offer: (offerId: number) =>
    api<WalletCredentialOfferResponse>(
      `/api/mobile/wallet/credential-offers/${offerId}`,
    ),
  prepare: (offerId: number, body: WalletCredentialPrepareRequest) =>
    api<WalletCredentialPrepareResponse>(
      `/api/mobile/wallet/credential-offers/${offerId}/prepare`,
      { method: "POST", body },
    ),
  confirm: (offerId: number, body: WalletCredentialConfirmRequest) =>
    api<WalletCredentialConfirmResponse>(
      `/api/mobile/wallet/credential-offers/${offerId}/confirm`,
      { method: "POST", body },
    ),
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
      query: params,
    }),
  unreadCount: () =>
    api<{ unreadCount: number }>("/api/common/notifications/unread-count"),
  markRead: (id: number) =>
    api<{ read: boolean }>(`/api/common/notifications/${id}/read`, {
      method: "PATCH",
    }),
  markAllRead: () =>
    api<{ updated: number }>("/api/common/notifications/read-all", {
      method: "PATCH",
    }),
};
