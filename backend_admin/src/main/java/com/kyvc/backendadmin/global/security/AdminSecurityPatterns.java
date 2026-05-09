package com.kyvc.backendadmin.global.security;

public final class AdminSecurityPatterns {

    public static final String[] PUBLIC_PATTERNS = {
            "/api/admin/backend/auth/login",
            "/api/admin/backend/auth/refresh",
            "/api/admin/auth/login",
            "/api/admin/auth/token/refresh",
            "/api/admin/auth/password-reset/request",
            "/api/admin/auth/password-reset/confirm"
    };

    public static final String[] PUBLIC_GET_PATTERNS = {
            "/api/admin/auth/session",
            "/health",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    public static final String[] SYSTEM_ADMIN_ONLY_PATTERNS = {
            "/api/admin/backend/reports/operations/**",
            "/api/admin/backend/audit-logs/**",
            "/api/admin/backend/security-events",
            "/api/admin/backend/data-access-logs",
            "/api/admin/backend/admin-users/**",
            "/api/admin/backend/admin-roles/**",
            "/api/admin/backend/kyc-rules/**",
            "/api/admin/backend/document-requirements/**",
            "/api/admin/backend/document-delete-requests/**",
            "/api/admin/backend/reason-codes/**",
            "/api/admin/backend/notification-templates/**",
            "/api/admin/backend/issuer-policies/**",
            "/api/admin/backend/issuer-configs/**",
            "/api/admin/backend/verifiers/**",
            "/api/admin/backend/ai-review-policies/**",
            "/api/admin/backend/verifier-logs",
            "/api/admin/backend/credentials/**"
    };

    public static final String[] BACKEND_ADMIN_ONLY_PATTERNS = {
            "/api/admin/backend/kyc/applications/*/ai-review/retry",
            "/api/admin/backend/kyc/applications/*/manual-review/approve",
            "/api/admin/backend/kyc/applications/*/manual-review/reject",
            "/api/admin/backend/kyc/applications/*/supplements",
            "/api/admin/backend/kyc/applications/*/credentials/issue"
    };

    public static final String[] ADMIN_COMMON_PATTERNS = {
            "/api/admin/backend/auth/logout",
            "/api/admin/backend/auth/me",
            "/api/admin/auth/logout",
            "/api/admin/auth/mfa/**",
            "/api/admin/me",
            "/api/admin/me/**"
    };

    public static final String BACKEND_API_FALLBACK_PATTERN = "/api/admin/backend/**";

    private AdminSecurityPatterns() {
    }
}
