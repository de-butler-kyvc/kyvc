package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
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

    @Captor
    private ArgumentCaptor<Credential> credentialCaptor;
    @Captor
    private ArgumentCaptor<CoreVcIssuanceRequest> coreVcIssuanceRequestCaptor;
    @Captor
    private ArgumentCaptor<String> coreResponsePayloadCaptor;

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
                new ObjectMapper().findAndRegisterModules(),
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
                CoreMockSeedData.DEV_CREDENTIAL_TYPE,
                CoreMockSeedData.DEV_ISSUER_DID,
                "dc+sd-jwt",
                null,
                "header.payload.signature~disclosure-011~",
                "vc-hash",
                "tx-hash",
                "status-id",
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1),
                null
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
        verify(coreAdapter).requestVcIssuance(coreVcIssuanceRequestCaptor.capture());
        assertThat(coreVcIssuanceRequestCaptor.getValue().format()).isEqualTo("dc+sd-jwt");
    }

    @Test
    void requestReissue_savesFormatOnly_whenCoreReturnsStringCredential() {
        Credential saved = executeReissueAndCaptureCredential(coreIssuanceResponse(
                KyvcEnums.CredentialStatus.VALID.name(),
                "dc+sd-jwt",
                null,
                "header.payload.signature~disclosure-011~"
        ));

        assertThat(saved.getVcFormat()).isEqualTo("dc+sd-jwt");
        assertThat(saved.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID);
    }

    @Test
    void credentialEntity_doesNotHaveRawPayloadFields() {
        assertThat(Arrays.stream(Credential.class.getDeclaredFields()))
                .extracting(field -> field.getName())
                .doesNotContain("vcPayloadJson", "vcJwt");
    }

    @Test
    void requestReissue_storesCoreResponseMetadataOnly() {
        String credentialPayloadJson = "{\"id\":\"vc-001\",\"type\":[\"VerifiableCredential\"]}";

        Credential saved = executeReissueAndCaptureCredential(coreIssuanceResponse(
                KyvcEnums.CredentialStatus.VALID.name(),
                "dc+sd-jwt",
                credentialPayloadJson,
                "header.payload.signature~disclosure-011~",
                Map.of("disclosablePaths", List.of("$.legalEntity.corporateName"))
        ));

        verify(coreRequestService).markSuccess(any(), coreResponsePayloadCaptor.capture());
        String storedPayload = coreResponsePayloadCaptor.getValue();

        assertThat(saved.getVcFormat()).isEqualTo("dc+sd-jwt");
        assertThat(saved.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID);
        assertThat(storedPayload)
                .contains("hasCredentialPayload", "hasSelectiveDisclosure")
                .doesNotContain("header.payload.signature~disclosure-011~", credentialPayloadJson, "$.legalEntity.corporateName", "credentialJwt", "credentialPayloadJson", "\"selectiveDisclosure\"");
    }

    @Test
    void requestReissue_keepsExistingMetadata_whenCredentialPayloadIsMissing() {
        LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime expiresAt = LocalDateTime.now().plusYears(1);
        Credential saved = executeReissueAndCaptureCredential(new CoreVcIssuanceResponse(
                "core-request-id",
                KyvcEnums.CredentialStatus.VALID.name(),
                "issued",
                LocalDateTime.now(),
                "reissued-credential-external-id",
                CoreMockSeedData.DEV_CREDENTIAL_TYPE,
                CoreMockSeedData.DEV_ISSUER_DID,
                null,
                null,
                null,
                "vc-hash",
                "tx-hash",
                "status-id",
                issuedAt,
                expiresAt,
                null
        ));

        assertThat(saved.getVcFormat()).isEqualTo("dc+sd-jwt");
        assertThat(saved.getCredentialExternalId()).isEqualTo("reissued-credential-external-id");
        assertThat(saved.getIssuerDid()).isEqualTo(CoreMockSeedData.DEV_ISSUER_DID);
        assertThat(saved.getVcHash()).isEqualTo("vc-hash");
        assertThat(saved.getXrplTxHash()).isEqualTo("tx-hash");
        assertThat(saved.getCredentialStatusId()).isEqualTo("status-id");
        assertThat(saved.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID);
    }

    @Test
    void requestReissue_doesNotSavePayload_whenCoreIssuanceFails() {
        Credential saved = executeReissueAndCaptureCredential(coreIssuanceResponse(
                KyvcEnums.CredentialStatus.FAILED.name(),
                "dc+sd-jwt",
                "{\"id\":\"failed-vc\"}",
                "header.payload.signature~failed-disclosure~"
        ));

        assertThat(saved.getVcFormat()).isNull();
        assertThat(saved.getCredentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.FAILED);
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

    private Credential executeReissueAndCaptureCredential(
            CoreVcIssuanceResponse coreResponse // Core VC 발급 응답
    ) {
        Credential sourceCredential = createCredential(10L, KyvcEnums.CredentialStatus.VALID);
        KycApplication kycApplication = createKycApplication(30L, 20L);
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_ISSUE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                sourceCredential.getCredentialId(),
                null
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
        if (KyvcEnums.CredentialStatus.VALID.name().equals(coreResponse.status())) {
            when(coreRequestService.markSuccess(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        } else {
            when(coreRequestService.markFailed(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        }
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

        credentialRequestService.requestReissue(
                userDetails(),
                10L,
                new CredentialReissueRequest("정보 갱신", "VC 재발급 요청")
        );

        verify(credentialRepository, atLeastOnce()).save(credentialCaptor.capture());
        List<Credential> savedCredentials = credentialCaptor.getAllValues();
        return savedCredentials.get(savedCredentials.size() - 1);
    }

    private CoreVcIssuanceResponse coreIssuanceResponse(
            String status, // Core 응답 상태
            String format, // VC format
            String credentialPayloadJson, // VC JSON 원문
            String credentialJwt // VC JWT 원문
    ) {
        return coreIssuanceResponse(status, format, credentialPayloadJson, credentialJwt, null);
    }

    private CoreVcIssuanceResponse coreIssuanceResponse(
            String status, // Core 응답 상태
            String format, // VC format
            String credentialPayloadJson, // VC JSON 원문
            String credentialJwt, // VC JWT 원문
            Map<String, Object> selectiveDisclosure // 선택공개 정보
    ) {
        return new CoreVcIssuanceResponse(
                "core-request-id",
                status,
                "issued",
                LocalDateTime.now(),
                "reissued-credential-external-id",
                CoreMockSeedData.DEV_CREDENTIAL_TYPE,
                CoreMockSeedData.DEV_ISSUER_DID,
                format,
                credentialPayloadJson,
                credentialJwt,
                "vc-hash",
                "tx-hash",
                "status-id",
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1),
                selectiveDisclosure
        );
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
                CoreMockSeedData.DEV_CREDENTIAL_TYPE,
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
