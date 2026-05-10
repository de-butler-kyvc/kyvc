package com.kyvc.backendadmin.global.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

public final class AdminSecurityPatterns {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String[] PUBLIC_PATTERNS = {
            "/api/admin/auth/login",
            "/api/admin/auth/logout",
            "/api/admin/auth/refresh",
            "/api/admin/auth/token/refresh",
            "/api/admin/auth/mfa/challenge",
            "/api/admin/auth/mfa/verify",
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

    public static final String[] FORBIDDEN_PATTERNS = {
            "/api/admin/core/**"
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
            "/api/admin/backend/verifier-logs"
    };

    public static final String[] SYSTEM_ADMIN_ONLY_POST_PATTERNS = {
            "/api/admin/backend/credentials/*/reissue",
            "/api/admin/backend/credentials/*/revoke"
    };

    public static final String[] ADMIN_WORK_GET_PATTERNS = {
            "/api/admin/backend/kyc/applications/**",
            "/api/admin/backend/credentials/**",
            "/api/admin/backend/vp-verifications/**"
    };

    public static final String[] ADMIN_WORK_POST_PATTERNS = {
            "/api/admin/backend/kyc/applications/*/ai-review/retry",
            "/api/admin/backend/kyc/applications/*/manual-review/approve",
            "/api/admin/backend/kyc/applications/*/manual-review/reject",
            "/api/admin/backend/kyc/applications/*/supplements",
            "/api/admin/backend/kyc/applications/*/credentials/issue"
    };

    public static final String[] ADMIN_COMMON_PATTERNS = {
            "/api/admin/auth/me",
            "/api/admin/auth/change-password",
            "/api/admin/me",
            "/api/admin/me/**"
    };

    public static final String BACKEND_API_FALLBACK_PATTERN = "/api/admin/backend/**";

    public static boolean isPublicRequest(
            String method, // HTTP 메서드
            String path // 요청 경로
    ) {
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        if (HttpMethod.POST.matches(method) && matchesAny(path, PUBLIC_PATTERNS)) {
            return true;
        }
        return HttpMethod.GET.matches(method) && matchesAny(path, PUBLIC_GET_PATTERNS);
    }

    private static boolean matchesAny(
            String path, // 요청 경로
            String[] patterns // 비교 대상 경로 패턴
    ) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private AdminSecurityPatterns() {
    }
}
