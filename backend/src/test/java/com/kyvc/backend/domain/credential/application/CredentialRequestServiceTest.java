package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backend.domain.credential.dto.CredentialRequestResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backend.domain.credential.repository.CredentialQueryRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestQueryRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryRepository;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialRequestServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CredentialQueryRepository credentialQueryRepository;

    @Mock
    private CredentialRequestRepository credentialRequestRepository;

    @Mock
    private CredentialRequestQueryRepository credentialRequestQueryRepository;

    @Mock
    private CredentialStatusHistoryRepository credentialStatusHistoryRepository;

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private CoreRequestService coreRequestService;

    @Mock
    private CoreAdapter coreAdapter;

    @Mock
    private LogEventLogger logEventLogger;

    @Captor
    private ArgumentCaptor<CredentialRequest> credentialRequestCaptor;

    @Captor
    private ArgumentCaptor<CredentialStatusHistory> statusHistoryCaptor;

    private CredentialRequestService service;

    @BeforeEach
    void setUp() {
        service = new CredentialRequestService(
                credentialRepository,
                credentialQueryRepository,
                credentialRequestRepository,
                credentialRequestQueryRepository,
                credentialStatusHistoryRepository,
                corporateRepository,
                coreRequestService,
                coreAdapter,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    @Test
    void requestReissue_marksUnsupportedWithoutCoreEndpointCall() {
        Credential credential = createValidCredential();
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_REISSUE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                100L,
                null
        );
        mockOwnedCredential(credential);
        when(credentialRequestRepository.existsInProgress(100L, KyvcEnums.CredentialRequestType.REISSUE)).thenReturn(false);
        when(credentialRequestRepository.save(any(CredentialRequest.class))).thenAnswer(invocation -> {
            CredentialRequest request = invocation.getArgument(0);
            if (request.getCredentialRequestId() == null) {
                ReflectionTestUtils.setField(request, "credentialRequestId", 300L);
            }
            return request;
        });
        when(coreRequestService.createVcReissueRequest(eq(100L), any())).thenReturn(coreRequest);

        CredentialRequestResponse response = service.requestReissue(
                userDetails(),
                100L,
                new CredentialReissueRequest("정보 갱신", "재발급 요청")
        );

        verify(coreAdapter, never()).requestVcIssuance(any());
        verify(coreAdapter, never()).revokeCredential(any());
        verify(coreRequestService).markRunning(coreRequest.getCoreRequestId());
        verify(coreRequestService).markFailed(eq(coreRequest.getCoreRequestId()), any());
        verify(credentialRequestRepository, org.mockito.Mockito.atLeastOnce()).save(credentialRequestCaptor.capture());

        CredentialRequest saved = credentialRequestCaptor.getAllValues().get(credentialRequestCaptor.getAllValues().size() - 1);
        assertThat(saved.getRequestStatus()).isEqualTo(KyvcEnums.CredentialRequestStatus.FAILED);
        assertThat(saved.getReasonCode()).isEqualTo(ErrorCode.CORE_UNSUPPORTED_OPERATION.getCode());
        assertThat(response.status()).isEqualTo(KyvcEnums.CredentialRequestStatus.FAILED.name());
        assertThat(response.failureReason()).isEqualTo(ErrorCode.CORE_UNSUPPORTED_OPERATION.getCode());
    }

    @Test
    void requestRevoke_callsCoreAndStoresRevokedStatusHistory() {
        Credential credential = createValidCredential();
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_REVOKE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                100L,
                null
        );
        mockOwnedCredential(credential);
        when(credentialRequestRepository.existsInProgress(100L, KyvcEnums.CredentialRequestType.REVOKE)).thenReturn(false);
        when(credentialRequestRepository.save(any(CredentialRequest.class))).thenAnswer(invocation -> {
            CredentialRequest request = invocation.getArgument(0);
            if (request.getCredentialRequestId() == null) {
                ReflectionTestUtils.setField(request, "credentialRequestId", 301L);
            }
            return request;
        });
        when(coreRequestService.createVcRevocationRequest(100L, null)).thenReturn(coreRequest);
        when(coreAdapter.revokeCredential(any(CoreRevokeCredentialRequest.class)))
                .thenReturn(new CoreRevokeCredentialResponse(true, "xrpl", "revoked"));
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(credentialStatusHistoryRepository.save(any(CredentialStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CredentialRequestResponse response = service.requestRevoke(
                userDetails(),
                100L,
                new CredentialRevokeRequest("사용자 요청 폐기")
        );

        verify(coreRequestService).markRunning(coreRequest.getCoreRequestId());
        verify(coreAdapter).revokeCredential(any(CoreRevokeCredentialRequest.class));
        verify(coreRequestService).markSuccess(eq(coreRequest.getCoreRequestId()), any());
        verify(credentialStatusHistoryRepository).save(statusHistoryCaptor.capture());

        assertThat(credential.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.REVOKED);
        assertThat(statusHistoryCaptor.getValue().getAfterStatus()).isEqualTo(KyvcEnums.CredentialStatus.REVOKED);
        assertThat(response.status()).isEqualTo(KyvcEnums.CredentialRequestStatus.COMPLETED.name());
        assertThat(response.credentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.REVOKED.name());
        assertThat(response.failureReason()).isNull();
    }

    private void mockOwnedCredential(Credential credential) {
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(createCorporate()));
        when(credentialRepository.getById(100L)).thenReturn(credential);
    }

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(
                1L,
                "user@test.com",
                KyvcEnums.UserType.CORPORATE_USER.name(),
                List.of("ROLE_CORPORATE_USER"),
                true
        );
    }

    private Corporate createCorporate() {
        Corporate corporate = Corporate.create(
                1L,
                "KYVC Corp",
                "123-45-67890",
                "110111-1234567",
                "CORPORATION",
                null,
                null,
                "대표자",
                "010-0000-0000",
                "rep@test.com",
                "서울",
                null,
                "IT",
                KyvcEnums.CorporateStatus.ACTIVE
        );
        ReflectionTestUtils.setField(corporate, "corporateId", 10L);
        return corporate;
    }

    private Credential createValidCredential() {
        Credential credential = newInstance(Credential.class);
        ReflectionTestUtils.setField(credential, "credentialId", 100L);
        ReflectionTestUtils.setField(credential, "corporateId", 10L);
        ReflectionTestUtils.setField(credential, "kycId", 200L);
        ReflectionTestUtils.setField(credential, "credentialExternalId", "cred-ext-100");
        ReflectionTestUtils.setField(credential, "credentialTypeCode", KyvcEnums.CredentialType.KYC_CREDENTIAL.name());
        ReflectionTestUtils.setField(credential, "issuerDid", "did:xrpl:1:rIssuerAccount");
        ReflectionTestUtils.setField(credential, "holderXrplAddress", "rHolderAccount");
        ReflectionTestUtils.setField(credential, "credentialStatusId", "status-id");
        ReflectionTestUtils.setField(credential, "credentialStatus", KyvcEnums.CredentialStatus.VALID);
        ReflectionTestUtils.setField(credential, "issuedAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(credential, "expiresAt", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(credential, "walletSavedYn", KyvcEnums.Yn.Y.name());
        return credential;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
