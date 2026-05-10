package com.kyvc.backendadmin.domain.kyc.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminKycAccessCheckerTest {

    @Mock
    private AuditLogWriter auditLogWriter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void backendAdminCanAccessKycAction() {
        authenticate(3L, List.of("ROLE_BACKEND_ADMIN"));
        AdminKycAccessChecker accessChecker = new AdminKycAccessChecker(auditLogWriter);

        assertDoesNotThrow(() -> accessChecker.validateActionAccess(10L, "KYC_MANUAL_APPROVE"));

        verify(auditLogWriter, never()).write(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void systemAdminCanAccessKycAction() {
        authenticate(3L, List.of("ROLE_SYSTEM_ADMIN"));
        AdminKycAccessChecker accessChecker = new AdminKycAccessChecker(auditLogWriter);

        assertDoesNotThrow(() -> accessChecker.validateActionAccess(10L, "KYC_MANUAL_REJECT"));

        verify(auditLogWriter, never()).write(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void otherRoleCannotAccessKycAction() {
        authenticate(3L, List.of("ROLE_VIEWER"));
        AdminKycAccessChecker accessChecker = new AdminKycAccessChecker(auditLogWriter);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> accessChecker.validateActionAccess(10L, "KYC_MANUAL_REJECT")
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(auditLogWriter).write(any(), eq(3L), eq("KYC_MANUAL_REJECT"), any(), eq(10L), any(), any(), eq("FAILURE"));
    }

    private void authenticate(Long adminId, List<String> roles) {
        CustomUserDetails userDetails = new CustomUserDetails(
                adminId,
                "admin@example.com",
                "ADMIN",
                roles,
                true
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        ));
    }
}
