package com.kyvc.backendadmin.global.security;

import com.kyvc.backendadmin.global.jwt.JwtTokenProvider;
import com.kyvc.backendadmin.global.jwt.TokenCookieUtil;
import com.kyvc.backendadmin.global.logging.LogEventLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AdminSecurityPolicyTest.SecurityTestApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "kyvc.cors.allowed-origins[0]=http://localhost:3000",
                "kyvc.cors.allowed-origins[1]=http://localhost:3001"
        }
)
@AutoConfigureMockMvc
class AdminSecurityPolicyTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            AdminSecurityPolicyTest.JwtFilterDependencyConfig.class,
            AdminSecurityPolicyTest.SecurityProbeController.class
})
    static class SecurityTestApplication {
    }

    private static final String REPORTS_OPERATIONS_URI = "/api/admin/backend/reports/operations";
    private static final String ADMIN_USERS_URI = "/api/admin/backend/admin-users";
    private static final String VERIFIERS_URI = "/api/admin/backend/verifiers";
    private static final String CREDENTIALS_URI = "/api/admin/backend/credentials";
    private static final String DOCUMENT_REQUIREMENT_PATCH_URI = "/api/admin/backend/document-requirements/1";
    private static final String DOCUMENT_DELETE_REQUESTS_URI = "/api/admin/backend/document-delete-requests";
    private static final String DOCUMENT_DELETE_REQUEST_APPROVE_URI = "/api/admin/backend/document-delete-requests/1/approve";
    private static final String DOCUMENT_DELETE_REQUEST_REJECT_URI = "/api/admin/backend/document-delete-requests/1/reject";
    private static final String CREDENTIAL_REISSUE_URI = "/api/admin/backend/credentials/1/reissue";
    private static final String CREDENTIAL_REVOKE_URI = "/api/admin/backend/credentials/1/revoke";
    private static final String ISSUER_POLICY_APPROVE_URI = "/api/admin/backend/issuer-policies/1/approve";
    private static final String MANUAL_APPROVE_URI = "/api/admin/backend/kyc/applications/1/manual-review/approve";
    private static final String AUTH_ME_URI = "/api/admin/backend/auth/me";
    private static final String UNKNOWN_BACKEND_URI = "/api/admin/backend/unknown-test-path";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void systemAdminOnlyApiWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(REPORTS_OPERATIONS_URI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void backendAdminCannotAccessSystemAdminOnlyApis() throws Exception {
        mockMvc.perform(get(REPORTS_OPERATIONS_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_USERS_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(VERIFIERS_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(CREDENTIALS_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(DOCUMENT_REQUIREMENT_PATCH_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(DOCUMENT_DELETE_REQUESTS_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(DOCUMENT_DELETE_REQUEST_APPROVE_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(DOCUMENT_DELETE_REQUEST_REJECT_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CREDENTIAL_REISSUE_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CREDENTIAL_REVOKE_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ISSUER_POLICY_APPROVE_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void systemAdminCanAccessSystemAdminOnlyApis() throws Exception {
        mockMvc.perform(get(REPORTS_OPERATIONS_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(get(ADMIN_USERS_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(get(VERIFIERS_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(get(CREDENTIALS_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(DOCUMENT_REQUIREMENT_PATCH_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(get(DOCUMENT_DELETE_REQUESTS_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(post(DOCUMENT_DELETE_REQUEST_APPROVE_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(post(DOCUMENT_DELETE_REQUEST_REJECT_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(post(CREDENTIAL_REISSUE_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(post(CREDENTIAL_REVOKE_URI).with(systemAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(post(ISSUER_POLICY_APPROVE_URI).with(systemAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void backendAdminOnlyApiAllowsBackendAdminAndRejectsSystemAdmin() throws Exception {
        mockMvc.perform(post(MANUAL_APPROVE_URI).with(backendAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(post(MANUAL_APPROVE_URI).with(systemAdmin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCommonApiAllowsBackendAdminAndSystemAdmin() throws Exception {
        mockMvc.perform(get(AUTH_ME_URI).with(backendAdmin()))
                .andExpect(status().isOk());
        mockMvc.perform(get(AUTH_ME_URI).with(systemAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void backendApiFallbackRejectsBackendAdminAndAllowsSystemAdminToRoutingLayer() throws Exception {
        mockMvc.perform(get(UNKNOWN_BACKEND_URI).with(backendAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(UNKNOWN_BACKEND_URI).with(systemAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsPreflightAllowsFrontendAdminOriginAndAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/admin/backend/kyc/applications")
                        .header("Origin", "http://localhost:3001")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,X-Request-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3001"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private RequestPostProcessor backendAdmin() {
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(
                10L,
                "backend-admin@example.com",
                "ADMIN",
                List.of("ROLE_BACKEND_ADMIN"),
                true
        ));
    }

    private RequestPostProcessor systemAdmin() {
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(
                20L,
                "system-admin@example.com",
                "ADMIN",
                List.of("ROLE_SYSTEM_ADMIN"),
                true
        ));
    }

    @TestConfiguration
    static class JwtFilterDependencyConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return Mockito.mock(JwtTokenProvider.class);
        }

        @Bean
        TokenCookieUtil tokenCookieUtil() {
            return Mockito.mock(TokenCookieUtil.class);
        }

        @Bean
        LogEventLogger logEventLogger() {
            return Mockito.mock(LogEventLogger.class);
        }
    }

    @RestController
    @RequestMapping("/api/admin/backend")
    public static class SecurityProbeController {

        @GetMapping("/reports/operations")
        public ResponseEntity<Void> reportsOperations() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/admin-users")
        public ResponseEntity<Void> adminUsers() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/verifiers")
        public ResponseEntity<Void> verifiers() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/credentials")
        public ResponseEntity<Void> credentials() {
            return ResponseEntity.ok().build();
        }

        @org.springframework.web.bind.annotation.PatchMapping("/document-requirements/{requirementId}")
        public ResponseEntity<Void> updateDocumentRequirement(
                @PathVariable Long requirementId
        ) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/document-delete-requests")
        public ResponseEntity<Void> documentDeleteRequests() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/document-delete-requests/{requestId}/approve")
        public ResponseEntity<Void> approveDocumentDeleteRequest(
                @PathVariable Long requestId
        ) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/document-delete-requests/{requestId}/reject")
        public ResponseEntity<Void> rejectDocumentDeleteRequest(
                @PathVariable Long requestId
        ) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/credentials/{credentialId}/reissue")
        public ResponseEntity<Void> reissueCredential(
                @PathVariable Long credentialId
        ) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/credentials/{credentialId}/revoke")
        public ResponseEntity<Void> revokeCredential(
                @PathVariable Long credentialId
        ) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/issuer-policies/{policyId}/approve")
        public ResponseEntity<Void> approveIssuerPolicy(
                @PathVariable Long policyId
        ) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/kyc/applications/{kycId}/manual-review/approve")
        public ResponseEntity<Void> approveManualReview(
                @PathVariable Long kycId
        ) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/auth/me")
        public ResponseEntity<Void> authMe() {
            return ResponseEntity.ok().build();
        }
    }
}
