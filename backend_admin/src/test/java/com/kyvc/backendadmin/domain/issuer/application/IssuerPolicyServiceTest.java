package com.kyvc.backendadmin.domain.issuer.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicyApproveRequest;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicyRejectRequest;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicyResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicySubmitApprovalRequest;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerPolicyQueryRepository;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerPolicyRepository;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.CustomUserDetails;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuerPolicyServiceTest {

    private static final Long ADMIN_ID = 10L;
    private static final Long POLICY_ID = 1L;
    private static final String MFA_TOKEN = "mfa-session-token";

    @Mock
    private IssuerPolicyRepository issuerPolicyRepository;
    @Mock
    private IssuerPolicyQueryRepository issuerPolicyQueryRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private IssuerPolicyService issuerPolicyService;

    @BeforeEach
    void setUp() {
        issuerPolicyService = new IssuerPolicyService(
                issuerPolicyRepository,
                issuerPolicyQueryRepository,
                authTokenRepository,
                auditLogWriter
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ADMIN_ID, "admin@example.com", "ADMIN", List.of("ROLE_SYSTEM_ADMIN"), true),
                null,
                List.of()
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitApprovalChangesRejectedPolicyToPending() {
        IssuerPolicy policy = policy(KyvcEnums.IssuerPolicyStatus.REJECTED, "기존 반려 사유");
        when(issuerPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(issuerPolicyQueryRepository.findDetailById(POLICY_ID)).thenReturn(Optional.of(response(KyvcEnums.IssuerPolicyStatus.PENDING)));

        issuerPolicyService.submitApproval(POLICY_ID, new IssuerPolicySubmitApprovalRequest("재승인요청"));

        assertEquals(KyvcEnums.IssuerPolicyStatus.PENDING, policy.getStatus());
        assertEquals("재승인요청", policy.getReason());
        verify(auditLogWriter).write(eq(KyvcEnums.ActorType.ADMIN), eq(ADMIN_ID), eq("ISSUER_POLICY_APPROVAL_SUBMITTED"),
                eq(KyvcEnums.AuditTargetType.ISSUER_POLICY), eq(POLICY_ID), eq("ISSUER_POLICY_APPROVAL_SUBMITTED"), any(), any());
    }

    @Test
    void approveChangesPendingPolicyToActiveAndUsesMfaToken() {
        IssuerPolicy policy = policy(KyvcEnums.IssuerPolicyStatus.PENDING, "승인 대기");
        AuthToken authToken = mfaToken();
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.of(authToken));
        when(issuerPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(issuerPolicyQueryRepository.findDetailById(POLICY_ID)).thenReturn(Optional.of(response(KyvcEnums.IssuerPolicyStatus.ACTIVE)));

        issuerPolicyService.approve(POLICY_ID, new IssuerPolicyApproveRequest(MFA_TOKEN, "승인"));

        assertEquals(KyvcEnums.IssuerPolicyStatus.ACTIVE, policy.getStatus());
        assertEquals(KyvcEnums.TokenStatus.USED, authToken.getStatus());
        verify(auditLogWriter).write(eq(KyvcEnums.ActorType.ADMIN), eq(ADMIN_ID), eq("ISSUER_POLICY_APPROVED"),
                eq(KyvcEnums.AuditTargetType.ISSUER_POLICY), eq(POLICY_ID), eq("ISSUER_POLICY_APPROVED"), any(), any());
    }

    @Test
    void rejectChangesPendingPolicyToRejectedAndStoresReason() {
        IssuerPolicy policy = policy(KyvcEnums.IssuerPolicyStatus.PENDING, "승인 대기");
        AuthToken authToken = mfaToken();
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.of(authToken));
        when(issuerPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(issuerPolicyQueryRepository.findDetailById(POLICY_ID)).thenReturn(Optional.of(response(KyvcEnums.IssuerPolicyStatus.REJECTED)));

        issuerPolicyService.reject(POLICY_ID, new IssuerPolicyRejectRequest(MFA_TOKEN, "검증 기준 미충족"));

        assertEquals(KyvcEnums.IssuerPolicyStatus.REJECTED, policy.getStatus());
        assertEquals("검증 기준 미충족", policy.getReason());
        assertEquals(KyvcEnums.TokenStatus.USED, authToken.getStatus());
        verify(auditLogWriter).write(eq(KyvcEnums.ActorType.ADMIN), eq(ADMIN_ID), eq("ISSUER_POLICY_REJECTED"),
                eq(KyvcEnums.AuditTargetType.ISSUER_POLICY), eq(POLICY_ID), eq("ISSUER_POLICY_REJECTED"), any(), any());
    }

    private IssuerPolicy policy(KyvcEnums.IssuerPolicyStatus status, String reason) {
        return IssuerPolicy.create(
                "did:xrpl:1:rIssuer",
                "KYvC Issuer",
                KyvcEnums.IssuerPolicyType.WHITELIST,
                KyvcEnums.CredentialType.KYC_CREDENTIAL.name(),
                status,
                reason
        );
    }

    private AuthToken mfaToken() {
        return AuthToken.create(
                KyvcEnums.ActorType.ADMIN,
                ADMIN_ID,
                KyvcEnums.TokenType.MFA_SESSION,
                TokenHashUtil.sha256(MFA_TOKEN),
                LocalDateTime.now().plusMinutes(5)
        );
    }

    private IssuerPolicyResponse response(KyvcEnums.IssuerPolicyStatus status) {
        return new IssuerPolicyResponse(
                POLICY_ID,
                "did:xrpl:1:rIssuer",
                "KYvC Issuer",
                KyvcEnums.IssuerPolicyType.WHITELIST.name(),
                List.of(KyvcEnums.CredentialType.KYC_CREDENTIAL.name()),
                status.name(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
