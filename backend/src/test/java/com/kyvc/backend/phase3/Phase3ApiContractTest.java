package com.kyvc.backend.phase3;

import com.kyvc.backend.domain.core.controller.CoreInternalHealthController;
import com.kyvc.backend.domain.credential.controller.CredentialController;
import com.kyvc.backend.domain.kyc.controller.DevKycApprovalController;
import com.kyvc.backend.domain.kyc.controller.KycSubmissionController;
import com.kyvc.backend.domain.verifier.controller.FinanceVerifierController;
import com.kyvc.backend.domain.verifier.controller.VerifierController;
import com.kyvc.backend.domain.vp.controller.MobileVpController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Phase3ApiContractTest {

    private static final Class<?>[] PHASE3_CONTROLLERS = {
            KycSubmissionController.class,
            DevKycApprovalController.class,
            MobileVpController.class,
            CredentialController.class,
            FinanceVerifierController.class,
            VerifierController.class,
            CoreInternalHealthController.class
    };

    @Test
    void phase3ApiMappings_areExposedOnCanonicalPathsOnly() {
        Set<String> mappings = collectMappings();

        assertThat(mappings).contains(
                "POST /api/user/kyc/applications/{kycId}/submit",
                "POST /api/internal/dev/kyc/applications/{kycId}/approve",
                "POST /api/mobile/vp/presentations",
                "GET /api/mobile/vp/requests/{requestId}",
                "GET /api/mobile/vp/presentations/{presentationId}",
                "GET /api/internal/core/health",
                "POST /api/user/kyc/applications/{kycId}/credentials",
                "POST /api/user/credentials/{credentialId}/reissue-requests",
                "POST /api/user/credentials/{credentialId}/revoke-requests",
                "GET /api/user/credentials",
                "GET /api/user/credentials/{credentialId}",
                "GET /api/user/credential-requests",
                "GET /api/user/credential-requests/{credentialRequestId}",
                "POST /api/finance/verifier/vp-requests",
                "GET /api/finance/verifier/vp-requests",
                "GET /api/finance/verifier/vp-requests/{requestId}",
                "POST /api/verifier/test-vp-verifications",
                "GET /api/verifier/test-vp-verifications/{testId}",
                "POST /api/verifier/re-auth-requests"
        );

        assertThat(mappings).doesNotContain(
                "POST /api/corporate/kyc/applications/{kycId}/submit",
                "POST /api/mobile/vp-presentations",
                "GET /api/finance/verifier/vp-requests/{requestId}/result"
        );
        assertThat(mappings).noneMatch(mapping -> mapping.contains("/api/internal/core/")
                && mapping.endsWith("/callback"));
    }

    private Set<String> collectMappings() {
        Set<String> mappings = new LinkedHashSet<>();
        for (Class<?> controller : PHASE3_CONTROLLERS) {
            String[] classPaths = classPaths(controller);
            for (Method method : controller.getDeclaredMethods()) {
                addMappings(mappings, classPaths, method);
            }
        }
        return mappings;
    }

    private String[] classPaths(Class<?> controller) {
        RequestMapping requestMapping = controller.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return new String[]{""};
        }
        String[] paths = paths(requestMapping.value(), requestMapping.path());
        return paths.length == 0 ? new String[]{""} : paths;
    }

    private void addMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            Method method // Controller 메서드
    ) {
        addMethodMappings(mappings, classPaths, "GET", method.getAnnotation(GetMapping.class));
        addMethodMappings(mappings, classPaths, "POST", method.getAnnotation(PostMapping.class));
        addMethodMappings(mappings, classPaths, "PUT", method.getAnnotation(PutMapping.class));
        addMethodMappings(mappings, classPaths, "PATCH", method.getAnnotation(PatchMapping.class));
        addMethodMappings(mappings, classPaths, "DELETE", method.getAnnotation(DeleteMapping.class));
    }

    private void addMethodMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            String httpMethod, // HTTP Method
            GetMapping mapping // GET 매핑
    ) {
        if (mapping != null) {
            addResolvedMappings(mappings, classPaths, httpMethod, paths(mapping.value(), mapping.path()));
        }
    }

    private void addMethodMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            String httpMethod, // HTTP Method
            PostMapping mapping // POST 매핑
    ) {
        if (mapping != null) {
            addResolvedMappings(mappings, classPaths, httpMethod, paths(mapping.value(), mapping.path()));
        }
    }

    private void addMethodMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            String httpMethod, // HTTP Method
            PutMapping mapping // PUT 매핑
    ) {
        if (mapping != null) {
            addResolvedMappings(mappings, classPaths, httpMethod, paths(mapping.value(), mapping.path()));
        }
    }

    private void addMethodMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            String httpMethod, // HTTP Method
            PatchMapping mapping // PATCH 매핑
    ) {
        if (mapping != null) {
            addResolvedMappings(mappings, classPaths, httpMethod, paths(mapping.value(), mapping.path()));
        }
    }

    private void addMethodMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            String httpMethod, // HTTP Method
            DeleteMapping mapping // DELETE 매핑
    ) {
        if (mapping != null) {
            addResolvedMappings(mappings, classPaths, httpMethod, paths(mapping.value(), mapping.path()));
        }
    }

    private void addResolvedMappings(
            Set<String> mappings, // 매핑 목록
            String[] classPaths, // 클래스 경로 목록
            String httpMethod, // HTTP Method
            String[] methodPaths // 메서드 경로 목록
    ) {
        String[] resolvedMethodPaths = methodPaths.length == 0 ? new String[]{""} : methodPaths;
        for (String classPath : classPaths) {
            for (String methodPath : resolvedMethodPaths) {
                mappings.add(httpMethod + " " + combinePath(classPath, methodPath));
            }
        }
    }

    private String[] paths(String[] valuePaths, String[] pathPaths) {
        if (pathPaths != null && pathPaths.length > 0) {
            return pathPaths;
        }
        return valuePaths == null ? new String[0] : valuePaths;
    }

    private String combinePath(String classPath, String methodPath) {
        if (methodPath != null && methodPath.startsWith("/api/")) {
            return methodPath;
        }
        String prefix = normalizePath(classPath);
        String suffix = normalizePath(methodPath);
        if (prefix.isBlank()) {
            return suffix.isBlank() ? "/" : suffix;
        }
        if (suffix.isBlank()) {
            return prefix;
        }
        return prefix + suffix;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return Arrays.stream(normalized.split("/"))
                .filter(part -> !part.isBlank())
                .reduce("", (current, part) -> current + "/" + part);
    }
}
