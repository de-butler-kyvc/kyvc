package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialActionResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backendadmin.domain.credential.repository.CredentialQueryRepository;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository.CredentialRequestSaveResult;
import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierSecuritySupport;
import com.kyvc.backendadmin.global.exception.ApiException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCredentialLifecycleServiceTest {

    private static final Long ADMIN_ID = 10L;
    private static final Long CREDENTIAL_ID = 1L;
    private static final Long CREDENTIAL_REQUEST_ID = 100L;
    private static final String MFA_TOKEN = "mfa-session-token";

    @Mock
    private CredentialQueryRepository credentialQueryRepository;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private AdminCredentialLifecycleService service;

    @BeforeEach
    void setUp() {
        AdminVerifierSecuritySupport securitySupport = new AdminVerifierSecuritySupport(authTokenRepository);
        service = new AdminCredentialLifecycleService(
                credentialQueryRepository,
                credentialRepository,
                securitySupport,
                auditLogWriter
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(ADMIN_ID, "system-admin@example.com", "ADMIN", List.of("ROLE_SYSTEM_ADMIN"), true),
                null,
                List.of()
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reissueCreatesCredentialRequestAndUsesMfaToken() {
        AuthToken authToken = mfaToken();
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("VALID")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REISSUE")).thenReturn(false);
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.of(authToken));
        when(credentialRepository.saveCredentialRequest(
                eq(CREDENTIAL_ID),
                eq("REISSUE"),
                eq("REQUESTED"),
                eq("ADMIN"),
                eq(ADMIN_ID),
                isNull(),
                contains("reissue reason"),
                isNull(),
                any(LocalDateTime.class)
        )).thenReturn(saveResult());

        CredentialActionResponse response = service.reissue(
                CREDENTIAL_ID,
                new CredentialReissueRequest(MFA_TOKEN, "reissue reason")
        );

        assertEquals("REISSUE", response.requestType());
        assertEquals("REQUESTED", response.requestStatus());
        assertEquals(CREDENTIAL_REQUEST_ID, response.credentialRequestId());
        assertEquals(KyvcEnums.TokenStatus.USED, authToken.getStatus());
        verify(auditLogWriter).write(
                eq(KyvcEnums.ActorType.ADMIN),
                eq(ADMIN_ID),
                eq("VC_REISSUE_REQUESTED"),
                eq(KyvcEnums.AuditTargetType.CREDENTIAL),
                eq(CREDENTIAL_ID),
                contains("credentialRequestId=100"),
                eq("VALID"),
                eq("REQUESTED")
        );
    }

    @Test
    void revokeCreatesCredentialRequestAndUsesMfaToken() {
        AuthToken authToken = mfaToken();
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("SUSPENDED")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REVOKE")).thenReturn(false);
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.of(authToken));
        when(credentialRepository.saveCredentialRequest(
                eq(CREDENTIAL_ID),
                eq("REVOKE"),
                eq("REQUESTED"),
                eq("ADMIN"),
                eq(ADMIN_ID),
                isNull(),
                contains("revoke reason"),
                isNull(),
                any(LocalDateTime.class)
        )).thenReturn(saveResult());

        CredentialActionResponse response = service.revoke(
                CREDENTIAL_ID,
                new CredentialRevokeRequest(MFA_TOKEN, "revoke reason")
        );

        assertEquals("REVOKE", response.requestType());
        assertEquals("REQUESTED", response.requestStatus());
        assertEquals(CREDENTIAL_REQUEST_ID, response.credentialRequestId());
        assertEquals(KyvcEnums.TokenStatus.USED, authToken.getStatus());
        verify(auditLogWriter).write(
                eq(KyvcEnums.ActorType.ADMIN),
                eq(ADMIN_ID),
                eq("VC_REVOKE_REQUESTED"),
                eq(KyvcEnums.AuditTargetType.CREDENTIAL),
                eq(CREDENTIAL_ID),
                contains("credentialRequestId=100"),
                eq("SUSPENDED"),
                eq("REQUESTED")
        );
    }

    @Test
    void reissueRejectsDuplicateInProgressRequest() {
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("VALID")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REISSUE")).thenReturn(true);

        assertThrows(ApiException.class, () -> service.reissue(
                CREDENTIAL_ID,
                new CredentialReissueRequest(MFA_TOKEN, "reissue reason")
        ));
        verifyNoInteractions(authTokenRepository, auditLogWriter);
    }

    @Test
    void revokeRejectsDuplicateInProgressRequest() {
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("VALID")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REVOKE")).thenReturn(true);

        assertThrows(ApiException.class, () -> service.revoke(
                CREDENTIAL_ID,
                new CredentialRevokeRequest(MFA_TOKEN, "revoke reason")
        ));
        verifyNoInteractions(authTokenRepository, auditLogWriter);
    }

    @Test
    void revokeRejectsAlreadyRevokedCredential() {
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("REVOKED")));

        assertThrows(ApiException.class, () -> service.revoke(
                CREDENTIAL_ID,
                new CredentialRevokeRequest(MFA_TOKEN, "revoke reason")
        ));
        verifyNoInteractions(authTokenRepository, credentialRepository, auditLogWriter);
    }

    @Test
    void reissueRejectsInvalidMfaToken() {
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("VALID")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REISSUE")).thenReturn(false);
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.reissue(
                CREDENTIAL_ID,
                new CredentialReissueRequest(MFA_TOKEN, "reissue reason")
        ));
        verifyNoInteractions(auditLogWriter);
    }

    @Test
    void reissueRejectsExpiredMfaToken() {
        AuthToken authToken = mfaToken(LocalDateTime.now().minusMinutes(1));
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("VALID")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REISSUE")).thenReturn(false);
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.of(authToken));

        assertThrows(ApiException.class, () -> service.reissue(
                CREDENTIAL_ID,
                new CredentialReissueRequest(MFA_TOKEN, "reissue reason")
        ));
        verifyNoInteractions(auditLogWriter);
    }

    @Test
    void reissueRejectsUsedMfaToken() {
        AuthToken authToken = mfaToken();
        authToken.markUsed(LocalDateTime.now());
        when(credentialQueryRepository.findDetailById(CREDENTIAL_ID)).thenReturn(Optional.of(credential("VALID")));
        when(credentialRepository.existsInProgressCredentialRequest(CREDENTIAL_ID, "REISSUE")).thenReturn(false);
        when(authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(MFA_TOKEN), KyvcEnums.TokenType.MFA_SESSION))
                .thenReturn(Optional.of(authToken));

        assertThrows(ApiException.class, () -> service.reissue(
                CREDENTIAL_ID,
                new CredentialReissueRequest(MFA_TOKEN, "reissue reason")
        ));
        verifyNoInteractions(auditLogWriter);
    }

    private CredentialRequestSaveResult saveResult() {
        return new CredentialRequestSaveResult(CREDENTIAL_REQUEST_ID, LocalDateTime.now());
    }

    private AuthToken mfaToken() {
        return mfaToken(LocalDateTime.now().plusMinutes(5));
    }

    private AuthToken mfaToken(LocalDateTime expiresAt) {
        return AuthToken.create(
                KyvcEnums.ActorType.ADMIN,
                ADMIN_ID,
                KyvcEnums.TokenType.MFA_SESSION,
                TokenHashUtil.sha256(MFA_TOKEN),
                expiresAt
        );
    }

    private AdminCredentialDetailResponse credential(String status) {
        return new AdminCredentialDetailResponse(
                CREDENTIAL_ID,
                "kyvc-credential-1",
                KyvcEnums.CredentialType.KYC_CREDENTIAL.name(),
                status,
                "did:kyvc:issuer",
                null,
                null,
                null,
                null,
                "N",
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                10L,
                "Test Corp",
                "999-88-77777",
                "02-9999-8888",
                20L,
                KyvcEnums.KycStatus.APPROVED.name(),
                null,
                null,
                null,
                null,
                List.of()
        );
    }
}
