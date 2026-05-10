package com.kyvc.backend.domain.vp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.dto.EligibleCredentialListResponse;
import com.kyvc.backend.domain.vp.dto.QrResolveRequest;
import com.kyvc.backend.domain.vp.dto.QrResolveResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationRequest;
import com.kyvc.backend.domain.vp.dto.VpPresentationResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationResultResponse;
import com.kyvc.backend.domain.vp.dto.VpRequestResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VpVerificationServiceTest {

    @Mock
    private VpVerificationRepository vpVerificationRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CorporateRepository corporateRepository;

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
    private ArgumentCaptor<String> responsePayloadCaptor;

    @Captor
    private ArgumentCaptor<String> formatCaptor;

    @Captor
    private ArgumentCaptor<Object> presentationCaptor;

    private VpVerificationService service;

    @BeforeEach
    void setUp() {
        service = new VpVerificationService(
                vpVerificationRepository,
                credentialRepository,
                corporateRepository,
                coreRequestService,
                coreAdapter,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );

        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(createCorporate(10L, 1L)));
    }

    @Test
    void resolveQr_parsesCredentialOfferQr() {
        QrResolveResponse response = service.resolveQr(
                userDetails(),
                new QrResolveRequest("{\"type\":\"CREDENTIAL_OFFER\",\"offerId\":1,\"qrToken\":\"token\"}")
        );

        assertThat(response.type()).isEqualTo(KyvcEnums.QrType.CREDENTIAL_OFFER.name());
        assertThat(response.offerId()).isEqualTo(1L);
        assertThat(response.requestId()).isNull();
        assertThat(response.nextAction()).isEqualTo(KyvcEnums.QrNextAction.OPEN_CREDENTIAL_OFFER.name());
    }

    @Test
    void resolveQr_parsesVpRequestQr() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce",
                "challenge",
                LocalDateTime.now().plusMinutes(30)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);

        QrResolveResponse response = service.resolveQr(
                userDetails(),
                new QrResolveRequest("{\"type\":\"VP_REQUEST\",\"requestId\":\"vp-req-001\",\"nonce\":\"nonce\",\"challenge\":\"challenge\"}")
        );

        assertThat(response.type()).isEqualTo(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(response.requestId()).isEqualTo("vp-req-001");
        assertThat(response.offerId()).isNull();
        assertThat(response.nextAction()).isEqualTo(KyvcEnums.QrNextAction.OPEN_VP_REQUEST.name());
    }

    @Test
    void resolveQr_throwsWhenPayloadIsInvalidJson() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.resolveQr(userDetails(), new QrResolveRequest("{invalid-json"))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QR_PAYLOAD_INVALID);
    }

    @Test
    void resolveQr_throwsWhenQrTypeIsUnsupported() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.resolveQr(userDetails(), new QrResolveRequest("{\"type\":\"UNKNOWN\",\"requestId\":\"vp-req-001\"}"))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QR_TYPE_NOT_SUPPORTED);
    }

    @Test
    void getVpRequest_returnsRequest() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);

        VpRequestResponse response = service.getVpRequest(userDetails(), "vp-req-001");

        assertThat(response.requestId()).isEqualTo("vp-req-001");
        assertThat(response.nonce()).isEqualTo("nonce-001");
        assertThat(response.challenge()).isEqualTo("challenge-001");
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.REQUESTED.name());
    }

    @Test
    void getVpRequest_throwsWhenRequestDoesNotExist() {
        when(vpVerificationRepository.getByRequestId("missing"))
                .thenThrow(new ApiException(ErrorCode.VP_REQUEST_NOT_FOUND));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.getVpRequest(userDetails(), "missing")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_REQUEST_NOT_FOUND);
    }

    @Test
    void getVpRequest_returnsExpiredFlagWhenRequestExpired() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().minusSeconds(1)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);

        VpRequestResponse response = service.getVpRequest(userDetails(), "vp-req-001");

        assertThat(response.requestId()).isEqualTo("vp-req-001");
        assertThat(response.expired()).isTrue();
        assertThat(response.submitted()).isFalse();
    }

    @Test
    void getEligibleCredentials_returnsRepositoryFilteredCredentials() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.findVpEligibleCredentialsByCorporateId(10L)).thenReturn(List.of(
                createCredential(100L, 10L, KyvcEnums.Yn.Y.name(), KyvcEnums.CredentialStatus.VALID, LocalDateTime.now().plusDays(1)),
                createCredential(101L, 10L, KyvcEnums.Yn.Y.name(), KyvcEnums.CredentialStatus.VALID, LocalDateTime.now().plusDays(2))
        ));

        EligibleCredentialListResponse response = service.getEligibleCredentials(userDetails(), "vp-req-001");

        assertThat(response.requestId()).isEqualTo("vp-req-001");
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.credentials()).extracting("credentialId").containsExactly(100L, 101L);
        verify(credentialRepository).findVpEligibleCredentialsByCorporateId(10L);
    }

    @Test
    void submitPresentation_savesHashOnlyAndReturnsVerificationResult() {
        String vpJwt = "vp.jwt.value";
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                21L,
                null
        );

        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.getById(100L)).thenReturn(credential);
        when(vpVerificationRepository.existsReplayCandidate("nonce-001", TokenHashUtil.sha256(vpJwt))).thenReturn(false);
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
        when(vpVerificationRepository.save(any(VpVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VpPresentationResponse response = service.submitPresentation(
                userDetails(),
                new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-001", vpJwt)
        );

        verify(coreRequestService).createVpVerificationRequest(21L, null);
        verify(coreRequestService).updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), requestPayloadCaptor.capture());
        verify(coreRequestService).markSuccess(eq(coreRequest.getCoreRequestId()), responsePayloadCaptor.capture());
        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());

        String requestPayload = requestPayloadCaptor.getValue();
        String responsePayload = responsePayloadCaptor.getValue();
        VpVerification savedVpVerification = vpVerificationCaptor.getValue();

        assertThat(requestPayload).doesNotContain(vpJwt);
        assertThat(responsePayload).doesNotContain(vpJwt);
        assertThat(savedVpVerification.getVpJwtHash()).isEqualTo(TokenHashUtil.sha256(vpJwt));
        assertThat(savedVpVerification.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID);
        assertThat(savedVpVerification.getCoreRequestId()).isEqualTo(coreRequest.getCoreRequestId());
        assertThat(response.presentationId()).isEqualTo(21L);
        assertThat(response.credentialId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID.name());
        assertThat(response.result().signatureValid()).isTrue();
        assertThat(response.result().replayDetected()).isFalse();
    }

    @Test
    void submitPresentation_usesVpJwtAsVpJwtPresentation_whenFormatMissing() {
        String vpJwt = "vp.jwt.value";
        mockSuccessfulSubmit(vpJwt, coreResponse(true, false));

        service.submitPresentation(
                userDetails(),
                new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-001", vpJwt)
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        assertThat(formatCaptor.getValue()).isEqualTo("vp+jwt");
        assertThat(presentationCaptor.getValue()).isEqualTo(vpJwt);
    }

    @Test
    void submitPresentation_usesPresentationString_whenFormatVpJwt() {
        String presentation = "compact-vp-jwt";
        mockSuccessfulSubmit(presentation, coreResponse(true, false));

        service.submitPresentation(
                userDetails(),
                new VpPresentationRequest(
                        "vp-req-001",
                        100L,
                        "nonce-001",
                        "challenge-001",
                        null,
                        "vp+jwt",
                        presentation,
                        null
                )
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        assertThat(formatCaptor.getValue()).isEqualTo("vp+jwt");
        assertThat(presentationCaptor.getValue()).isEqualTo(presentation);
    }

    @Test
    void submitPresentation_usesSdJwtPresentationObject_whenFormatSdJwt() {
        Map<String, Object> presentation = new LinkedHashMap<>();
        presentation.put("format", "kyvc-sd-jwt-presentation-v1");
        presentation.put("aud", "https://dev-api-kyvc.khuoo.synology.me");
        presentation.put("nonce", "nonce-001");
        presentation.put("sdJwtKb", "sd-jwt-kb");
        mockSuccessfulSubmit(null, coreResponse(true, false));

        service.submitPresentation(
                userDetails(),
                new VpPresentationRequest(
                        "vp-req-001",
                        100L,
                        "nonce-001",
                        "challenge-001",
                        null,
                        "kyvc-sd-jwt-presentation-v1",
                        presentation,
                        null
                )
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        assertThat(formatCaptor.getValue()).isEqualTo("kyvc-sd-jwt-presentation-v1");
        assertThat(presentationCaptor.getValue()).isEqualTo(presentation);
    }

    @Test
    void submitPresentation_fallsBackToVpJwt_whenPresentationMissing() {
        String sdJwtKb = "sd-jwt-kb";
        mockSuccessfulSubmit(sdJwtKb, coreResponse(true, false));

        service.submitPresentation(
                userDetails(),
                new VpPresentationRequest(
                        "vp-req-001",
                        100L,
                        "nonce-001",
                        "challenge-001",
                        sdJwtKb,
                        "kyvc-sd-jwt-presentation-v1",
                        null,
                        null
                )
        );

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), formatCaptor.capture(), presentationCaptor.capture());
        assertThat(formatCaptor.getValue()).isEqualTo("kyvc-sd-jwt-presentation-v1");
        assertThat(presentationCaptor.getValue()).isEqualTo(sdJwtKb);
    }

    @Test
    void submitPresentation_marksInvalid_whenCoreVerificationFails() {
        String vpJwt = "vp.jwt.invalid";
        mockSuccessfulSubmit(vpJwt, coreResponse(false, false));

        VpPresentationResponse response = service.submitPresentation(
                userDetails(),
                new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-001", vpJwt)
        );

        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());
        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(saved.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.INVALID);
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.INVALID.name());
        assertThat(response.result().signatureValid()).isFalse();
    }

    @Test
    void submitPresentation_marksReplaySuspected_whenCoreReportsReplay() {
        String vpJwt = "vp.jwt.replay";
        mockSuccessfulSubmit(vpJwt, coreResponse(false, true));

        VpPresentationResponse response = service.submitPresentation(
                userDetails(),
                new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-001", vpJwt)
        );

        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());
        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(saved.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED);
        assertThat(response.result().replayDetected()).isTrue();
    }

    @Test
    void submitPresentation_throwsWhenNonceDoesNotMatch() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.getById(100L)).thenReturn(credential);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submitPresentation(
                        userDetails(),
                        new VpPresentationRequest("vp-req-001", 100L, "nonce-999", "challenge-001", "vp.jwt.value")
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_NONCE_INVALID);
        verify(coreRequestService, never()).createVpVerificationRequest(any(), any());
    }

    @Test
    void submitPresentation_throwsWhenChallengeDoesNotMatch() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.getById(100L)).thenReturn(credential);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submitPresentation(
                        userDetails(),
                        new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-999", "vp.jwt.value")
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_CHALLENGE_INVALID);
        verify(coreRequestService, never()).createVpVerificationRequest(any(), any());
    }

    @Test
    void submitPresentation_throwsWhenReplayCandidateExists() {
        String vpJwt = "vp.jwt.value";
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );

        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.getById(100L)).thenReturn(credential);
        when(vpVerificationRepository.existsReplayCandidate("nonce-001", TokenHashUtil.sha256(vpJwt))).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submitPresentation(
                        userDetails(),
                        new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-001", vpJwt)
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_PRESENTATION_REPLAY_SUSPECTED);
        verify(coreRequestService, never()).createVpVerificationRequest(any(), any());
    }

    @Test
    void submitPresentation_throwsWhenJsonLdVpFormatRequested() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submitPresentation(
                        userDetails(),
                        new VpPresentationRequest(
                                "vp-req-001",
                                100L,
                                "nonce-001",
                                "challenge-001",
                                "vp.jwt.value",
                                "kyvc-jsonld-vp-v1",
                                null,
                                null
                        )
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(coreRequestService, never()).createVpVerificationRequest(any(), any());
    }

    @Test
    void submitPresentation_throwsWhenPresentationIsMissing() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submitPresentation(
                        userDetails(),
                        new VpPresentationRequest(
                                "vp-req-001",
                                100L,
                                "nonce-001",
                                "challenge-001",
                                null,
                                "vp+jwt",
                                null,
                                null
                        )
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(coreRequestService, never()).createVpVerificationRequest(any(), any());
    }

    @Test
    void getPresentationResult_returnsPresentedStatus() {
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        vpVerification.markPresented(100L, TokenHashUtil.sha256("vp.jwt.value"), "core-request-id", LocalDateTime.now());

        when(vpVerificationRepository.getById(21L)).thenReturn(vpVerification);
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));

        VpPresentationResultResponse response = service.getPresentationResult(userDetails(), 21L);

        assertThat(response.presentationId()).isEqualTo(21L);
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.PRESENTED.name());
        assertThat(response.replaySuspected()).isFalse();
        assertThat(response.result()).isNull();
    }

    @Test
    void submitPresentation_rejectsCredentialNotEligible() {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.N.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );
        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.getById(100L)).thenReturn(credential);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submitPresentation(
                        userDetails(),
                        new VpPresentationRequest("vp-req-001", 100L, "nonce-001", "challenge-001", "vp.jwt.value")
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VP_CREDENTIAL_NOT_ELIGIBLE);
        verify(coreRequestService, never()).createVpVerificationRequest(any(), any());
    }

    private void mockSuccessfulSubmit(
            String hashSource, // Replay 해시 대상 원문
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        VpVerification vpVerification = createRequestedVpVerification(
                21L,
                10L,
                "vp-req-001",
                "nonce-001",
                "challenge-001",
                LocalDateTime.now().plusMinutes(30)
        );
        Credential credential = createCredential(
                100L,
                10L,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now().plusDays(1)
        );
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                21L,
                null
        );

        when(vpVerificationRepository.getByRequestId("vp-req-001")).thenReturn(vpVerification);
        when(credentialRepository.getById(100L)).thenReturn(credential);
        if (hashSource == null) {
            when(vpVerificationRepository.existsReplayCandidate(eq("nonce-001"), anyString())).thenReturn(false);
        } else {
            when(vpVerificationRepository.existsReplayCandidate("nonce-001", TokenHashUtil.sha256(hashSource))).thenReturn(false);
        }
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
        when(vpVerificationRepository.save(any(VpVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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

    private Credential createCredential(
            Long credentialId,
            Long corporateId,
            String walletSavedYn,
            KyvcEnums.CredentialStatus credentialStatus,
            LocalDateTime expiresAt
    ) {
        Credential credential = newInstance(Credential.class);
        ReflectionTestUtils.setField(credential, "credentialId", credentialId);
        ReflectionTestUtils.setField(credential, "corporateId", corporateId);
        ReflectionTestUtils.setField(credential, "kycId", 300L);
        ReflectionTestUtils.setField(credential, "credentialExternalId", "cred-ext-" + credentialId);
        ReflectionTestUtils.setField(credential, "credentialTypeCode", KyvcEnums.CredentialType.KYC_CREDENTIAL.name());
        ReflectionTestUtils.setField(credential, "issuerDid", "did:kyvc:issuer");
        ReflectionTestUtils.setField(credential, "credentialStatus", credentialStatus);
        ReflectionTestUtils.setField(credential, "vcHash", "vc-hash-" + credentialId);
        ReflectionTestUtils.setField(credential, "issuedAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(credential, "expiresAt", expiresAt);
        ReflectionTestUtils.setField(credential, "walletSavedYn", walletSavedYn);
        return credential;
    }

    private VpVerification createRequestedVpVerification(
            Long vpVerificationId,
            Long corporateId,
            String requestId,
            String nonce,
            String challenge,
            LocalDateTime expiresAt
    ) {
        VpVerification vpVerification = newInstance(VpVerification.class);
        ReflectionTestUtils.setField(vpVerification, "vpVerificationId", vpVerificationId);
        ReflectionTestUtils.setField(vpVerification, "corporateId", corporateId);
        ReflectionTestUtils.setField(vpVerification, "requestNonce", nonce);
        ReflectionTestUtils.setField(vpVerification, "purpose", "KYC 제출 증빙");
        ReflectionTestUtils.setField(vpVerification, "vpVerificationStatus", KyvcEnums.VpVerificationStatus.REQUESTED);
        ReflectionTestUtils.setField(vpVerification, "replaySuspectedYn", KyvcEnums.Yn.N.name());
        ReflectionTestUtils.setField(vpVerification, "requestedAt", LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(vpVerification, "expiresAt", expiresAt);
        ReflectionTestUtils.setField(vpVerification, "vpRequestId", requestId);
        ReflectionTestUtils.setField(vpVerification, "requesterName", "KYVC Verifier");
        ReflectionTestUtils.setField(vpVerification, "requiredClaimsJson", "[\"businessName\"]");
        ReflectionTestUtils.setField(vpVerification, "challenge", challenge);
        return vpVerification;
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
