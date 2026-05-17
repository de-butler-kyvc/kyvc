package com.kyvc.backend.domain.verifier.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.finance.application.FinanceContextService;
import com.kyvc.backend.domain.finance.repository.FinanceCorporateCustomerRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.verifier.domain.Verifier;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCancelResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestDetailResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpVerificationCheckResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationQueryRepository;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifierVpServiceTest {

    @Mock
    private VpVerificationRepository vpVerificationRepository;

    @Mock
    private VpVerificationQueryRepository vpVerificationQueryRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private KycApplicationRepository kycApplicationRepository;

    @Mock
    private FinanceContextService financeContextService;

    @Mock
    private FinanceCorporateCustomerRepository financeCorporateCustomerRepository;

    @Mock
    private VerifierRepository verifierRepository;

    @Mock
    private CoreRequestService coreRequestService;

    @Mock
    private CoreAdapter coreAdapter;

    @Mock
    private LogEventLogger logEventLogger;

    @Captor
    private ArgumentCaptor<VpVerification> vpVerificationCaptor;

    @Captor
    private ArgumentCaptor<String> requestPayloadCaptor;

    @Captor
    private ArgumentCaptor<String> formatCaptor;

    @Captor
    private ArgumentCaptor<Object> presentationCaptor;

    private VerifierVpService service;

    @BeforeEach
    void setUp() {
        service = new VerifierVpService(
                vpVerificationRepository,
                vpVerificationQueryRepository,
                credentialRepository,
                corporateRepository,
                kycApplicationRepository,
                financeContextService,
                financeCorporateCustomerRepository,
                verifierRepository,
                coreRequestService,
                coreAdapter,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    @Test
    void createFinanceVpRequest_createsRequestWithoutCoreCall() throws Exception {
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coreAdapter.issuePresentationChallenge(any())).thenReturn(new CorePresentationChallengeResponse(
                "challenge-core",
                "nonce-core",
                "kyvc-backend",
                "https://aud.example",
                LocalDateTime.now().plusMinutes(5),
                java.util.Map.of("id", "definition")
        ));

        FinanceVpRequestCreateResponse response = service.createFinanceVpRequest(
                new FinanceVpRequestCreateRequest(
                        "ACCOUNT_OPENING",
                        List.of(
                                "corporateName",
                                "businessRegistrationNo",
                                "corporateRegistrationNo",
                                "representativeName",
                                "kycStatus",
                                "credentialIssuedAt",
                                "credentialExpiresAt"
                        ),
                        600L
                )
        );

        verify(coreAdapter, never()).requestVpVerification(any(CoreVpVerificationRequest.class), any(), any());
        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());

        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.REQUESTED.name());
        assertThat(response.requestId()).isEqualTo(saved.getVpRequestId());
        JsonNode qrPayload = new ObjectMapper().findAndRegisterModules().readTree(response.qrPayload());
        assertThat(qrPayload.path("type").asText()).isEqualTo(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(qrPayload.path("requestType").asText()).isEqualTo("FINANCIAL_KYC_CHECK");
        assertThat(qrPayload.path("requestId").asText()).isEqualTo(saved.getVpRequestId());
        assertThat(qrPayload.path("qrToken").asText()).isNotBlank();
        assertThat(saved.matchesQrTokenHash(TokenHashUtil.sha256(qrPayload.path("qrToken").asText()))).isTrue();
        assertThat(response.qrPayload()).doesNotContain("VP_LOGIN_REQUEST").doesNotContain("LOGIN");
        assertThat(qrPayload.has("corporateId")).isFalse();
        assertThat(qrPayload.has("kycId")).isFalse();
        assertThat(qrPayload.has("corporateName")).isFalse();
        assertThat(qrPayload.has("businessNumber")).isFalse();
        assertThat(qrPayload.has("visitorName")).isFalse();
        assertThat(saved.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.REQUESTED);
        assertThat(saved.getRequestTypeCode()).isEqualTo(KyvcEnums.VpRequestType.FINANCE_VERIFY);
        assertThat(saved.getFinanceInstitutionCode()).isEqualTo("FINANCE_PUBLIC");
        assertThat(saved.getCorporateId()).isNull();

        ArgumentCaptor<CorePresentationChallengeRequest> challengeRequestCaptor =
                ArgumentCaptor.forClass(CorePresentationChallengeRequest.class);
        verify(coreAdapter).issuePresentationChallenge(challengeRequestCaptor.capture());
        Map<String, Object> presentationDefinition = challengeRequestCaptor.getValue().presentationDefinition();
        assertThat(presentationDefinition.get("requiredDisclosures"))
                .isEqualTo(List.of("legalEntity.name", "legalEntity.registrationNumber", "representative.name"));
        assertThat(presentationDefinition.get("requiredClaims"))
                .isEqualTo(List.of("legalEntity.name", "legalEntity.registrationNumber", "representative.name"));
    }

    @Test
    void testVpVerification_callsCoreAndStoresVpJwtHashOnly() {
        String vpJwt = "vp.jwt.value";
        Corporate corporate = createCorporate(10L, 1L);
        Credential credential = createCredential(100L, 10L);
        Verifier verifier = createVerifier(3L, "user@test.com");
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                21L,
                null
        );

        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.findLatestByCorporateId(10L)).thenReturn(Optional.of(credential));
        when(verifierRepository.findLatestByContactEmail("user@test.com")).thenReturn(Optional.of(verifier));
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> {
            VpVerification vpVerification = invocation.getArgument(0);
            if (vpVerification.getVpVerificationId() == null) {
                ReflectionTestUtils.setField(vpVerification, "vpVerificationId", 21L);
            }
            return vpVerification;
        });
        when(coreRequestService.createVpVerificationRequest(21L, null)).thenReturn(coreRequest);
        when(coreAdapter.requestVpVerification(any(CoreVpVerificationRequest.class), eq("vp+jwt"), eq(vpJwt)))
                .thenAnswer(invocation -> {
                    CoreVpVerificationRequest request = invocation.getArgument(0);
                    return new CoreVpVerificationResponse(
                            request.coreRequestId(),
                            KyvcEnums.VpVerificationStatus.VALID.name(),
                            "valid",
                            LocalDateTime.now(),
                            true,
                            true,
                            false,
                            "VP 검증 성공"
                    );
                });

        VerifierTestVpVerificationResponse response = service.testVpVerification(
                userDetails(),
                new VerifierTestVpVerificationRequest(vpJwt, "test-nonce", List.of("corporateName"), true)
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        verify(coreRequestService).markRunning(coreRequest.getCoreRequestId());
        verify(coreRequestService).markSuccess(eq(coreRequest.getCoreRequestId()), any());
        verify(coreRequestService).updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), requestPayloadCaptor.capture());
        verify(vpVerificationRepository, org.mockito.Mockito.atLeastOnce()).save(vpVerificationCaptor.capture());

        VpVerification completed = vpVerificationCaptor.getAllValues().get(vpVerificationCaptor.getAllValues().size() - 1);
        assertThat(formatCaptor.getValue()).isEqualTo("vp+jwt");
        assertThat(presentationCaptor.getValue()).isEqualTo(vpJwt);
        assertThat(requestPayloadCaptor.getValue()).doesNotContain(vpJwt);
        assertThat(completed.getVpJwtHash()).isEqualTo(TokenHashUtil.sha256(vpJwt));
        assertThat(completed.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID);
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID.name());
        assertThat(response.result().signatureValid()).isTrue();
    }

    @Test
    void verifyTestPresentation_usesVpJwtFallback_whenFormatMissing() {
        String vpJwt = "vp.jwt.value";
        mockVerifierTestSubmit(coreResponse(true, false));

        service.testVpVerification(
                userDetails(),
                new VerifierTestVpVerificationRequest(vpJwt, null, null, "test-nonce", List.of("corporateName"), true)
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        assertThat(formatCaptor.getValue()).isEqualTo("vp+jwt");
        assertThat(presentationCaptor.getValue()).isEqualTo(vpJwt);
    }

    @Test
    void verifyTestPresentation_usesPresentationObject_whenSdJwtFormat() {
        Map<String, Object> presentation = new LinkedHashMap<>();
        presentation.put("format", "kyvc-sd-jwt-presentation-v1");
        presentation.put("aud", "https://dev-api-kyvc.khuoo.synology.me");
        presentation.put("nonce", "test-nonce");
        presentation.put("sdJwtKb", "sd-jwt-kb");
        mockVerifierTestSubmit(coreResponse(true, false));

        service.testVpVerification(
                userDetails(),
                new VerifierTestVpVerificationRequest(
                        null,
                        "kyvc-sd-jwt-presentation-v1",
                        presentation,
                        "test-nonce",
                        List.of("corporateName"),
                        true
                )
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        assertThat(formatCaptor.getValue()).isEqualTo("kyvc-sd-jwt-presentation-v1");
        assertThat(presentationCaptor.getValue()).isEqualTo(presentation);
    }

    @Test
    void verifyTestPresentation_rejectsJsonLdVpFormat() {
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(createCorporate(10L, 1L)));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.testVpVerification(
                        userDetails(),
                        new VerifierTestVpVerificationRequest(
                                "vp.jwt.value",
                                "kyvc-jsonld-vp-v1",
                                null,
                                "test-nonce",
                                List.of("corporateName"),
                                true
                        )
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(coreAdapter, never()).requestVpVerification(any(CoreVpVerificationRequest.class), any(), any());
    }

    @Test
    void verifyTestPresentation_rejectsMissingPresentation() {
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(createCorporate(10L, 1L)));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.testVpVerification(
                        userDetails(),
                        new VerifierTestVpVerificationRequest(null, null, null, "test-nonce", List.of("corporateName"), true)
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(coreAdapter, never()).requestVpVerification(any(CoreVpVerificationRequest.class), any(), any());
    }

    @Test
    void createVpRequest_usesCoreChallenge_whenCoreAvailable() {
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coreAdapter.issuePresentationChallenge(any())).thenReturn(new CorePresentationChallengeResponse(
                "challenge-core",
                "nonce-core",
                "kyvc-backend",
                "https://aud.example",
                LocalDateTime.now().plusMinutes(5),
                Map.of("id", "definition")
        ));

        FinanceVpRequestCreateResponse response = service.createFinanceVpRequest(
                new FinanceVpRequestCreateRequest("ACCOUNT_OPENING", List.of("corporateName"), 600L)
        );

        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());
        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(saved.getRequestNonce()).isEqualTo("nonce-core");
        assertThat(saved.getChallenge()).isEqualTo("challenge-core");
        assertThat(saved.getPermissionResultJson()).contains("https://aud.example");
        assertThat(response.qrPayload()).contains(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(response.qrPayload()).doesNotContain("VP_LOGIN_REQUEST").doesNotContain("LOGIN");
    }

    @Test
    void createVpRequest_fallsBackToLocalChallenge_whenCoreChallengeFails() {
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coreAdapter.issuePresentationChallenge(any())).thenThrow(new ApiException(ErrorCode.CORE_API_CALL_FAILED));

        FinanceVpRequestCreateResponse response = service.createFinanceVpRequest(
                new FinanceVpRequestCreateRequest("ACCOUNT_OPENING", List.of("corporateName"), 600L)
        );

        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());
        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(saved.getRequestNonce()).isNotBlank();
        assertThat(saved.getChallenge()).isNotBlank();
        assertThat(response.qrPayload()).contains(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(response.qrPayload()).doesNotContain("VP_LOGIN_REQUEST").doesNotContain("LOGIN");
    }

    @Test
    void getFinanceVpRequest_returnsExtendedDetailFields() {
        Corporate corporate = createCorporate(10L, 1L);
        Credential credential = createCredential(100L, 10L);
        KycApplication kycApplication = createKycApplication(300L, 10L, KyvcEnums.KycStatus.APPROVED);
        VpVerification vpVerification = createFinanceVpVerification(KyvcEnums.VpVerificationStatus.REQUESTED);
        vpVerification.markPresentedForCorporate(
                10L,
                100L,
                TokenHashUtil.sha256("vp.jwt.value"),
                "core-request-id",
                LocalDateTime.now().minusSeconds(5)
        );
        vpVerification.markValid("VP 검증 성공", LocalDateTime.now());
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));
        when(kycApplicationRepository.findById(300L)).thenReturn(Optional.of(kycApplication));

        FinanceVpRequestDetailResponse response = service.getFinanceVpRequest("vp-req-001");

        assertThat(response.requestId()).isEqualTo("vp-req-001");
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID.name());
        assertThat(response.verificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID.name());
        assertThat(response.requestedClaims()).contains("corporateName");
        assertThat(response.qrPayload()).contains(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(response.qrPayload()).doesNotContain("qrToken");
        assertThat(response.corporateId()).isEqualTo(10L);
        assertThat(response.corporateName()).isEqualTo("KYVC Corp");
        assertThat(response.submittedAt()).isNotNull();
        assertThat(response.checks()).hasSize(5).allMatch(check -> "PASSED".equals(check.resultCode()));
        assertThat(response.result().corporateName()).isEqualTo("KYVC Corp");
        assertThat(response.result().businessRegistrationNo()).isEqualTo("123-45-67890");
        assertThat(response.result().corporateRegistrationNo()).isEqualTo("110111-1234567");
        assertThat(response.result().representativeName()).isNotBlank();
        assertThat(response.result().kycStatus()).isEqualTo(KyvcEnums.KycStatus.APPROVED.name());
        assertThat(response.result().credentialStatus()).isEqualTo(KyvcEnums.CredentialStatus.VALID.name());
        assertThat(response.result().credentialIssuedAt()).isNotNull();
        assertThat(response.result().credentialExpiresAt()).isNotNull();
    }

    @Test
    void getFinanceVpRequest_mapsInvalidCoreResultToChecks() throws Exception {
        Corporate corporate = createCorporate(10L, 1L);
        Credential credential = createCredential(100L, 10L);
        KycApplication kycApplication = createKycApplication(300L, 10L, KyvcEnums.KycStatus.APPROVED);
        VpVerification vpVerification = createFinanceVpVerification(KyvcEnums.VpVerificationStatus.REQUESTED);
        vpVerification.markPresentedForCorporate(
                10L,
                100L,
                TokenHashUtil.sha256("vp.jwt.value"),
                "core-request-id",
                LocalDateTime.now().minusSeconds(5)
        );
        vpVerification.markInvalid("VP 검증 결과가 유효하지 않습니다.", LocalDateTime.now());
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                21L,
                null
        );
        coreRequest.markFailed(
                "VP 검증 결과가 유효하지 않습니다.",
                new ObjectMapper().findAndRegisterModules().writeValueAsString(new CoreVpVerificationResponse(
                        "core-request-id",
                        KyvcEnums.VpVerificationStatus.INVALID.name(),
                        "Core VP 검증 실패",
                        LocalDateTime.now(),
                        true,
                        false,
                        false,
                        "VP 검증 결과가 유효하지 않습니다.",
                        List.of(),
                        Map.of(
                                "signatureValid", false,
                                "issuerTrusted", false,
                                "credentialStatus", KyvcEnums.VpVerificationStatus.INVALID.name(),
                                "replayDetected", false
                        )
                ))
        );

        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));
        when(kycApplicationRepository.findById(300L)).thenReturn(Optional.of(kycApplication));
        when(coreRequestService.getCoreRequest("core-request-id")).thenReturn(coreRequest);

        FinanceVpRequestDetailResponse response = service.getFinanceVpRequest("vp-req-001");

        assertThat(checkCode(response, "VP_FORMAT")).isEqualTo("CHECK_REQUIRED");
        assertThat(checkCode(response, "VC_SIGNATURE")).isEqualTo("FAILED");
        assertThat(checkCode(response, "VC_STATUS")).isEqualTo("FAILED");
        assertThat(checkCode(response, "ISSUER_TRUST")).isEqualTo("FAILED");
        assertThat(checkCode(response, "NONCE")).isEqualTo("PASSED");
        assertThat(response.checks())
                .extracting(FinanceVpVerificationCheckResponse::message)
                .noneMatch(message -> String.valueOf(message).contains("Credential 매핑"));
    }

    @Test
    void cancelFinanceVpRequest_cancelsRequestedRequest() {
        VpVerification vpVerification = createFinanceVpVerification(KyvcEnums.VpVerificationStatus.REQUESTED);
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceVpRequestCancelResponse response = service.cancelFinanceVpRequest("vp-req-001");

        assertThat(response.requestId()).isEqualTo("vp-req-001");
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.CANCELLED.name());
        assertThat(vpVerification.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.CANCELLED);
    }

    @Test
    void cancelFinanceVpRequest_throwsConflictWhenAlreadyCompleted() {
        VpVerification vpVerification = createFinanceVpVerification(KyvcEnums.VpVerificationStatus.VALID);
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cancelFinanceVpRequest("vp-req-001")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_REQUEST_INVALID_STATUS);
    }

    @Test
    void cancelFinanceVpRequest_throwsNotFoundWhenMissing() {
        when(vpVerificationRepository.getByRequestId("missing"))
                .thenThrow(new ApiException(ErrorCode.VP_REQUEST_NOT_FOUND));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cancelFinanceVpRequest("missing")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_REQUEST_NOT_FOUND);
    }

    private void mockVerifierTestSubmit(
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        Corporate corporate = createCorporate(10L, 1L);
        Credential credential = createCredential(100L, 10L);
        Verifier verifier = createVerifier(3L, "user@test.com");
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                21L,
                null
        );

        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.findLatestByCorporateId(10L)).thenReturn(Optional.of(credential));
        when(verifierRepository.findLatestByContactEmail("user@test.com")).thenReturn(Optional.of(verifier));
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> {
            VpVerification vpVerification = invocation.getArgument(0);
            if (vpVerification.getVpVerificationId() == null) {
                ReflectionTestUtils.setField(vpVerification, "vpVerificationId", 21L);
            }
            return vpVerification;
        });
        when(coreRequestService.createVpVerificationRequest(21L, null)).thenReturn(coreRequest);
        when(coreRequestService.updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreRequestService.markRunning(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        if (Boolean.TRUE.equals(coreResponse.valid())) {
            when(coreRequestService.markSuccess(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        } else {
            when(coreRequestService.markFailed(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        }
        when(coreAdapter.requestVpVerification(any(CoreVpVerificationRequest.class), anyString(), any()))
                .thenReturn(coreResponse);
    }

    private CoreVpVerificationResponse coreResponse(
            boolean valid, // Core 검증 성공 여부
            boolean replaySuspected // Replay 의심 여부
    ) {
        return new CoreVpVerificationResponse(
                "core-request-id",
                valid ? KyvcEnums.VpVerificationStatus.VALID.name() : KyvcEnums.VpVerificationStatus.INVALID.name(),
                valid ? "valid" : "invalid",
                LocalDateTime.now(),
                true,
                valid,
                replaySuspected,
                replaySuspected ? "VP Replay 의심" : (valid ? "VP 검증 성공" : "VP 검증 실패")
        );
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

    private void mockFinanceContext() {
        when(financeContextService.requireFinanceStaff(any(CustomUserDetails.class))).thenReturn(
                new FinanceContextService.FinanceContext(
                        1L,
                        "FINANCE_USER_1",
                        null,
                        List.of("ROLE_FINANCE_STAFF")
                )
        );
        lenient().when(financeCorporateCustomerRepository.findLatestByLinkedByUserId(1L)).thenReturn(Optional.empty());
    }

    private Corporate createCorporate(Long corporateId, Long userId) {
        Corporate corporate = Corporate.create(
                userId,
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
        ReflectionTestUtils.setField(corporate, "corporateId", corporateId);
        return corporate;
    }

    private Credential createCredential(Long credentialId, Long corporateId) {
        Credential credential = newInstance(Credential.class);
        ReflectionTestUtils.setField(credential, "credentialId", credentialId);
        ReflectionTestUtils.setField(credential, "corporateId", corporateId);
        ReflectionTestUtils.setField(credential, "kycId", 300L);
        ReflectionTestUtils.setField(credential, "credentialExternalId", "cred-ext-" + credentialId);
        ReflectionTestUtils.setField(credential, "credentialTypeCode", KyvcEnums.CredentialType.KYC_CREDENTIAL.name());
        ReflectionTestUtils.setField(credential, "issuerDid", "did:kyvc:issuer");
        ReflectionTestUtils.setField(credential, "credentialStatus", KyvcEnums.CredentialStatus.VALID);
        ReflectionTestUtils.setField(credential, "issuedAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(credential, "expiresAt", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(credential, "walletSavedYn", KyvcEnums.Yn.Y.name());
        return credential;
    }

    private Verifier createVerifier(Long verifierId, String contactEmail) {
        Verifier verifier = Verifier.createForAuthenticatedUser("Verifier", contactEmail);
        ReflectionTestUtils.setField(verifier, "verifierId", verifierId);
        return verifier;
    }

    private VpVerification createFinanceVpVerification(
            KyvcEnums.VpVerificationStatus status // VP 검증 상태
    ) {
        VpVerification vpVerification = VpVerification.createRequest(
                null,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                "ACCOUNT_OPENING",
                "user@test.com",
                "[\"corporateName\",\"businessRegistrationNo\"]",
                LocalDateTime.now().plusMinutes(5),
                null,
                "FINANCE_USER_1",
                KyvcEnums.VpRequestType.FINANCE_VERIFY,
                KyvcEnums.Yn.N,
                KyvcEnums.Yn.N,
                "{\"coreChallenge\":{\"domain\":\"kyvc-finance-vp\",\"aud\":\"kyvc-finance-vp\",\"presentationDefinition\":{}}}"
        );
        ReflectionTestUtils.setField(vpVerification, "vpVerificationId", 21L);
        if (KyvcEnums.VpVerificationStatus.VALID == status) {
            vpVerification.markPresentedForCorporate(
                    10L,
                    100L,
                    TokenHashUtil.sha256("vp.jwt.value"),
                    "core-request-id",
                    LocalDateTime.now().minusSeconds(5)
            );
            vpVerification.markValid("VP 검증 성공", LocalDateTime.now());
        }
        return vpVerification;
    }

    private KycApplication createKycApplication(
            Long kycId, // KYC ID
            Long corporateId, // 법인 ID
            KyvcEnums.KycStatus status // KYC 상태
    ) {
        KycApplication kycApplication = newInstance(KycApplication.class);
        ReflectionTestUtils.setField(kycApplication, "kycId", kycId);
        ReflectionTestUtils.setField(kycApplication, "corporateId", corporateId);
        ReflectionTestUtils.setField(kycApplication, "kycStatus", status);
        return kycApplication;
    }

    private String checkCode(
            FinanceVpRequestDetailResponse response, // 금융사 VP 요청 상세 응답
            String checkType // 검증 항목 유형
    ) {
        return response.checks().stream()
                .filter(check -> checkType.equals(check.checkType()))
                .map(FinanceVpVerificationCheckResponse::resultCode)
                .findFirst()
                .orElse(null);
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
