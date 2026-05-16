const DEFAULT_BACKEND_API_BASE = "https://dev-api-kyvc.khuoo.synology.me";
const rawBackendApiBase =
  process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL?.trim() || DEFAULT_BACKEND_API_BASE;

const BACKEND_API_BASE = rawBackendApiBase.replace(/\/+$/, "");
const FINANCE_VP_BASE = `${BACKEND_API_BASE}/api/finance/verifier/vp-requests`;

export type AdminVpRequestStatus =
  | "REQUESTED"
  | "PRESENTED"
  | "VALID"
  | "INVALID"
  | "REPLAY_SUSPECTED"
  | "EXPIRED"
  | "CANCELLED";

export type AdminVpRequestCheck = {
  checkType: string;
  checkName: string;
  resultCode: "PASSED" | "FAILED" | "UNKNOWN" | "CHECK_REQUIRED" | string;
  message: string;
};

export type AdminVpRequestResult = {
  corporateName: string | null;
  businessRegistrationNo: string | null;
  verifiedAt: string | null;
  corporateRegistrationNo?: string | null;
  representativeName?: string | null;
  kycStatus?: string | null;
  credentialStatus?: string | null;
  credentialIssuedAt?: string | null;
  credentialExpiresAt?: string | null;
};

export type AdminVpRequestDetail = {
  requestId: string;
  status: AdminVpRequestStatus;
  verificationStatus?: AdminVpRequestStatus | string | null;
  purpose: string;
  requestedClaims: string[];
  qrPayload: string;
  corporateId?: number | null;
  corporateName?: string | null;
  result?: AdminVpRequestResult | null;
  checks?: AdminVpRequestCheck[];
  expiresAt: string;
  createdAt?: string | null;
  submittedAt?: string | null;
  verifiedAt?: string | null;
};

export type AdminVpRequestCreateResponse = {
  requestId: string;
  status: AdminVpRequestStatus;
  qrPayload: string;
  expiresAt: string;
};

export type AdminVpRequestCancelResponse = {
  requestId: string;
  status: AdminVpRequestStatus;
};

export type AdminVpSubmittedClaim = {
  label: string;
  value: string;
  source: string;
};

export type AdminVpRequestListParams = {
  status?: AdminVpRequestStatus | string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
};

export type AdminVpRequestSummary = {
  requestId: string;
  status: AdminVpRequestStatus | string;
  purpose?: string | null;
  requestedClaims?: string[] | null;
  corporateId?: number | null;
  corporateName?: string | null;
  requestedAt?: string | null;
  createdAt?: string | null;
  expiresAt?: string | null;
  verifiedAt?: string | null;
};

export type AdminVpRequestListResponse = {
  items: AdminVpRequestSummary[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
};

type CommonResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

type PageLike<T> = {
  content?: T[];
  items?: T[];
  list?: T[];
  requests?: T[];
  totalElements?: number;
  totalPages?: number;
  page?: number;
  size?: number;
};

const DEFAULT_REQUESTED_CLAIMS = [
  "corporateName",
  "businessRegistrationNo",
  "corporateRegistrationNo",
  "representativeName",
  "kycStatus",
  "credentialIssuedAt",
  "credentialExpiresAt",
];

export class AdminVpApiError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "AdminVpApiError";
    this.status = status;
    this.code = code;
  }
}

function buildHeaders() {
  return {
    "Content-Type": "application/json",
  };
}

async function readError(response: Response) {
  try {
    const text = await response.text();
    if (!text.trim()) {
      return {
        message: `API Error: ${response.status} ${response.statusText}`,
      };
    }
    const parsed = JSON.parse(text) as Partial<CommonResponse<unknown>> & {
      error?: string;
    };
    return {
      code: parsed.code,
      message:
        parsed.message ??
        (typeof parsed.error === "string" ? parsed.error : undefined) ??
        `API Error: ${response.status} ${response.statusText}`,
    };
  } catch {
    return {
      message: `API Error: ${response.status} ${response.statusText}`,
    };
  }
}

async function requestJson<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      ...buildHeaders(),
      ...(init.headers ?? {}),
    },
    credentials: "omit",
  });

  if (!response.ok) {
    const error = await readError(response);
    if (response.status === 401 || response.status === 403) {
      throw new AdminVpApiError(
        "backend VP 요청 API 인증 설정을 확인해야 합니다.",
        response.status,
        error.code
      );
    }
    throw new AdminVpApiError(error.message, response.status, error.code);
  }

  const json = (await response.json()) as CommonResponse<T> | T;
  if (
    json &&
    typeof json === "object" &&
    "success" in json &&
    json.success === false
  ) {
    throw new AdminVpApiError(
      json.message || "요청 처리에 실패했습니다.",
      response.status,
      json.code
    );
  }

  if (json && typeof json === "object" && "data" in json) {
    return json.data as T;
  }

  return json as T;
}

function unwrapListData<T>(data: T[] | PageLike<T> | null | undefined): {
  items: T[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
} {
  if (Array.isArray(data)) {
    return { items: data };
  }
  if (!data || typeof data !== "object") {
    return { items: [] };
  }
  return {
    items:
      data.content ??
      data.items ??
      data.list ??
      data.requests ??
      [],
    page: data.page,
    size: data.size,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
  };
}

function toDetailFromCreate(
  created: AdminVpRequestCreateResponse
): AdminVpRequestDetail {
  return {
    requestId: created.requestId,
    status: created.status,
    verificationStatus: created.status,
    purpose: "ACCOUNT_OPENING",
    requestedClaims: DEFAULT_REQUESTED_CLAIMS,
    qrPayload: created.qrPayload,
    result: null,
    checks: [],
    expiresAt: created.expiresAt,
    createdAt: null,
    submittedAt: null,
    verifiedAt: null,
  };
}

export async function createFinanceVpRequest(): Promise<AdminVpRequestDetail> {
  const created = await requestJson<AdminVpRequestCreateResponse>(
    FINANCE_VP_BASE,
    {
      method: "POST",
      body: JSON.stringify({
        purpose: "ACCOUNT_OPENING",
        requestedClaims: DEFAULT_REQUESTED_CLAIMS,
        expiresInSeconds: 300,
      }),
    }
  );

  return toDetailFromCreate(created);
}

export async function getFinanceVpRequestDetail(
  requestId: string
): Promise<AdminVpRequestDetail> {
  return requestJson<AdminVpRequestDetail>(
    `${FINANCE_VP_BASE}/${encodeURIComponent(requestId)}`,
    { method: "GET" }
  );
}

export async function getFinanceVpRequestList(
  params?: AdminVpRequestListParams
): Promise<AdminVpRequestListResponse> {
  const searchParams = new URLSearchParams();
  if (params?.status) searchParams.set("status", params.status);
  if (params?.from) searchParams.set("from", params.from);
  if (params?.to) searchParams.set("to", params.to);
  if (params?.page !== undefined) searchParams.set("page", String(params.page));
  if (params?.size !== undefined) searchParams.set("size", String(params.size));

  const url = searchParams.toString()
    ? `${FINANCE_VP_BASE}?${searchParams}`
    : FINANCE_VP_BASE;
  const data = await requestJson<
    AdminVpRequestSummary[] | PageLike<AdminVpRequestSummary>
  >(url, { method: "GET" });
  const listData = unwrapListData(data);

  return {
    ...listData,
    totalElements: listData.totalElements ?? listData.items.length,
  };
}

export async function cancelFinanceVpRequest(
  requestId: string
): Promise<AdminVpRequestCancelResponse> {
  return requestJson<AdminVpRequestCancelResponse>(
    `${FINANCE_VP_BASE}/${encodeURIComponent(requestId)}/cancel`,
    { method: "POST" }
  );
}
