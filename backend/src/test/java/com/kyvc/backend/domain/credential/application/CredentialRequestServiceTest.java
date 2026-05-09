package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.dto.CredentialOperationResponse;
import com.kyvc.backend.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backend.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestQueryRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialRequestServiceTest {

    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private CredentialRequestRepository credentialRequestRepository;
    @Mock
    private CredentialRequestQueryRepository credentialRequestQueryRepository;
    @Mock
    private CredentialStatusHistoryRepository credentialStatusHistoryRepository;
    @Mock
    private CorporateRepository corporateRepository;
    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private CoreRequestService coreRequestService;
    @Mock
    private CoreAdapter coreAdapter;
    @Mock
    private LogEventLogger logEventLogger;

    private CredentialRequestService credentialRequestService;
    private CoreProperties coreProperties;

    @BeforeEach
    void setUp() {
        coreProperties = new CoreProperties();
        coreProperties.setDevSeedEnabled(true);
        credentialRequestService = new CredentialRequestService(
                credentialRepository,
                credentialRequestRepository,
                credentialRequestQueryRepository,
                credentialStatusHistoryRepository,
                corporateRepository,
                kycApplicationRepository,
                coreRequestService,
                coreAdapter,
                coreProperties,
                new ObjectMapper(),
                logEventLogger
        );
    }

    @Test
    void requestReissue_callsCoreIssuanceAndCreatesNewCredential() {
        Credential sourceCredential = createCredential(10L, KyvcEnums.CredentialStatus.VALID);
        KycApplication kycApplication = createKycApplication(30L, 20L);
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_ISSUE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                sourceCredential.getCredentialId(),
                null
        );
        CoreVcIssuanceResponse coreResponse = new CoreVcIssuanceResponse(
                coreRequest.getCoreRequestId(),
                KyvcEnums.CredentialStatus.VALID.name(),
                "issued",
                LocalDateTime.now(),
                "reissued-credential-external-id",
                CoreMockSeedData.DEV_ISSUER_DID,
                "vc-hash",
                "tx-hash",
                "status-id",
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1)
        );

        mockBaseOwnership(sourceCredential);
        when(credentialRequestRepository.existsInProgressByCredentialIdAndType(
                10L,
                KyvcEnums.CredentialRequestType.REISSUE
        )).thenReturn(false);
        when(credentialRequestRepository.save(any(CredentialRequest.class))).thenAnswer(invocation -> {
            CredentialRequest request = invocation.getArgument(0);
            if (request.getCredentialRequestId() == null) {
                ReflectionTestUtils.setField(request, "credentialRequestId", 100L);
            }
            return request;
        });
        when(coreRequestService.createVcIssuanceRequest(eq(10L), isNull())).thenReturn(coreRequest);
        when(coreRequestService.updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreRequestService.markProcessing(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markSuccess(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(kycApplicationRepository.findById(30L)).thenReturn(Optional.of(kycApplication));
        when(coreAdapter.requestVcIssuance(any())).thenReturn(coreResponse);
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> {
            Credential credential = invocation.getArgument(0);
            if (credential.getCredentialId() == null) {
                ReflectionTestUtils.setField(credential, "credentialId", 11L);
            }
            return credential;
        });
        when(credentialStatusHistoryRepository.save(any(CredentialStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CredentialOperationResponse response = credentialRequestService.requestReissue(
                userDetails(),
                10L,
                new CredentialReissueRequest("정보 갱신", "VC 재발급 요청")
        );

        assertThat(response.credentialRequestId()).isEqualTo(100L);
        assertThat(response.credentialId()).isEqualTo(11L);
        assertThat(response.status()).isEqualTo(KyvcEnums.CredentialRequestStatus.COMPLETED.name());
        assertThat(response.credentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID.name());
        assertThat(response.failureReason()).isNull();
        assertThat(List.of(CredentialOperationResponse.class.getRecordComponents()))
                .extracting(component -> component.getName())
                .doesNotContain("coreRequestId");
        verify(coreAdapter).requestVcIssuance(any());
    }

    @Test
    void requestRevoke_coreFailureDoesNotRevokeCredential() {
        Credential credential = createCredential(10L, KyvcEnums.CredentialStatus.VALID);
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_REVOKE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credential.getCredentialId(),
                null
        );

        mockBaseOwnership(credential);
        when(credentialRequestRepository.existsInProgressByCredentialIdAndType(
                10L,
                KyvcEnums.CredentialRequestType.REVOKE
        )).thenReturn(false);
        when(credentialRequestRepository.save(any(CredentialRequest.class))).thenAnswer(invocation -> {
            CredentialRequest request = invocation.getArgument(0);
            if (request.getCredentialRequestId() == null) {
                ReflectionTestUtils.setField(request, "credentialRequestId", 101L);
            }
            return request;
        });
        when(coreRequestService.createVcRevokeRequest(eq(10L), isNull())).thenReturn(coreRequest);
        when(coreRequestService.updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreRequestService.markProcessing(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markFailed(coreRequest.getCoreRequestId(), ErrorCode.CORE_API_CALL_FAILED.getCode())).thenReturn(coreRequest);
        when(coreAdapter.revokeCredential(any())).thenThrow(new ApiException(ErrorCode.CORE_API_CALL_FAILED));

        CredentialOperationResponse response = credentialRequestService.requestRevoke(
                userDetails(),
                10L,
                new CredentialRevokeRequest("사용자 요청", "VC 폐기 요청")
        );

        assertThat(response.status()).isEqualTo(KyvcEnums.CredentialRequestStatus.FAILED.name());
        assertThat(response.credentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID.name());
        assertThat(response.failureReason()).isEqualTo(ErrorCode.CORE_API_CALL_FAILED.getCode());
        assertThat(credential.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID);
    }

    @Test
    void requestRevoke_successMarksCredentialRevoked() {
        Credential credential = createCredential(10L, KyvcEnums.CredentialStatus.VALID);
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_REVOKE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credential.getCredentialId(),
                null
        );

        mockBaseOwnership(credential);
        when(credentialRequestRepository.existsInProgressByCredentialIdAndType(
                10L,
                KyvcEnums.CredentialRequestType.REVOKE
        )).thenReturn(false);
        when(credentialRequestRepository.save(any(CredentialRequest.class))).thenAnswer(invocation -> {
            CredentialRequest request = invocation.getArgument(0);
            if (request.getCredentialRequestId() == null) {
                ReflectionTestUtils.setField(request, "credentialRequestId", 102L);
            }
            return request;
        });
        when(coreRequestService.createVcRevokeRequest(eq(10L), isNull())).thenReturn(coreRequest);
        when(coreRequestService.updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreRequestService.markProcessing(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markSuccess(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreAdapter.revokeCredential(any())).thenReturn(new CoreRevokeCredentialResponse(true, "xrpl", "revoked"));
        when(credentialRepository.save(credential)).thenReturn(credential);
        when(credentialStatusHistoryRepository.save(any(CredentialStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CredentialOperationResponse response = credentialRequestService.requestRevoke(
                userDetails(),
                10L,
                new CredentialRevokeRequest("사용자 요청", "VC 폐기 요청")
        );

        assertThat(response.status()).isEqualTo(KyvcEnums.CredentialRequestStatus.COMPLETED.name());
        assertThat(response.credentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.REVOKED.name());
        assertThat(credential.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.REVOKED);
        assertThat(credential.getRevokedAt()).isNotNull();
    }

    private void mockBaseOwnership(Credential credential) {
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
        ReflectionTestUtils.setField(corporate, "corporateId", 20L);
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.getById(credential.getCredentialId())).thenReturn(credential);
    }

    private Credential createCredential(
            Long credentialId,
            KyvcEnums.CredentialStatus status
    ) {
        Credential credential = Credential.createIssuing(
                20L,
                30L,
                "credential-external-id-" + credentialId,
                KyvcEnums.CredentialType.KYC_CREDENTIAL.name(),
                CoreMockSeedData.DEV_ISSUER_DID,
                CoreMockSeedData.DEV_CREDENTIAL_STATUS_PURPOSE,
                CoreMockSeedData.DEV_KYC_LEVEL,
                CoreMockSeedData.DEV_JURISDICTION,
                CoreMockSeedData.DEV_HOLDER_DID,
                CoreMockSeedData.DEV_HOLDER_ACCOUNT
        );
        ReflectionTestUtils.setField(credential, "credentialId", credentialId);
        credential.applyIssuanceMetadata(
                "credential-external-id-" + credentialId,
                CoreMockSeedData.DEV_ISSUER_DID,
                status,
                "vc-hash-" + credentialId,
                "tx-hash-" + credentialId,
                "status-id-" + credentialId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusYears(1)
        );
        return credential;
    }

    private KycApplication createKycApplication(
            Long kycId,
            Long corporateId
    ) {
        KycApplication kycApplication = KycApplication.createDraft(corporateId, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", kycId);
        return kycApplication;
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
}
