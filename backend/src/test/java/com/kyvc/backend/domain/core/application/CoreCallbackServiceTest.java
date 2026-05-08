package com.kyvc.backend.domain.core.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreCallbackResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationCallbackRequest;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreCallbackServiceTest {

    @Mock
    private CoreRequestService coreRequestService;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private KycApplicationRepository kycApplicationRepository;

    @Mock
    private VpVerificationRepository vpVerificationRepository;

    @Mock
    private com.kyvc.backend.global.logging.LogEventLogger logEventLogger;

    @Captor
    private ArgumentCaptor<VpVerification> vpVerificationCaptor;

    @ParameterizedTest
    @ValueSource(strings = {"SUCCESS", "VALID"})
    void processVpVerificationCallback_marksValid(String callbackStatus) {
        LocalDateTime verifiedAt = LocalDateTime.now();
        CoreRequest coreRequest = createVpCoreRequest(21L);
        VpVerification vpVerification = createPresentedVpVerification(21L);
        CoreCallbackService service = createService();

        when(coreRequestService.getCoreRequest(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markCallbackSuccess(eq(coreRequest.getCoreRequestId()), anyString()))
                .thenAnswer(invocation -> {
                    coreRequest.markSuccess(invocation.getArgument(1));
                    return coreRequest;
                });
        when(vpVerificationRepository.getById(21L)).thenReturn(vpVerification);
        when(vpVerificationRepository.save(vpVerificationCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CoreCallbackResponse response = service.processVpVerificationCallback(
                coreRequest.getCoreRequestId(),
                new CoreVpVerificationCallbackRequest(
                        coreRequest.getCoreRequestId(),
                        callbackStatus,
                        "valid-summary",
                        false,
                        null,
                        verifiedAt
                )
        );

        assertThat(vpVerificationCaptor.getValue().getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID);
        assertThat(vpVerificationCaptor.getValue().getReplaySuspectedYn()).isEqualTo(KyvcEnums.Yn.N.name());
        assertThat(vpVerificationCaptor.getValue().getVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(response.status()).isEqualTo(KyvcEnums.CoreRequestStatus.SUCCESS.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "FAILED"})
    void processVpVerificationCallback_marksInvalid(String callbackStatus) {
        LocalDateTime verifiedAt = LocalDateTime.now();
        CoreRequest coreRequest = createVpCoreRequest(21L);
        VpVerification vpVerification = createPresentedVpVerification(21L);
        CoreCallbackService service = createService();

        when(coreRequestService.getCoreRequest(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markCallbackFailed(eq(coreRequest.getCoreRequestId()), anyString()))
                .thenAnswer(invocation -> {
                    coreRequest.markFailed(invocation.getArgument(1));
                    return coreRequest;
                });
        when(vpVerificationRepository.getById(21L)).thenReturn(vpVerification);
        when(vpVerificationRepository.save(vpVerificationCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CoreCallbackResponse response = service.processVpVerificationCallback(
                coreRequest.getCoreRequestId(),
                new CoreVpVerificationCallbackRequest(
                        coreRequest.getCoreRequestId(),
                        callbackStatus,
                        "invalid-summary",
                        false,
                        "failed-message",
                        verifiedAt
                )
        );

        assertThat(vpVerificationCaptor.getValue().getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.INVALID);
        assertThat(vpVerificationCaptor.getValue().getReplaySuspectedYn()).isEqualTo(KyvcEnums.Yn.N.name());
        assertThat(response.status()).isEqualTo(KyvcEnums.CoreRequestStatus.FAILED.name());
    }

    @Test
    void processVpVerificationCallback_marksReplaySuspectedWhenStatusMatches() {
        LocalDateTime verifiedAt = LocalDateTime.now();
        CoreRequest coreRequest = createVpCoreRequest(21L);
        VpVerification vpVerification = createPresentedVpVerification(21L);
        CoreCallbackService service = createService();

        when(coreRequestService.getCoreRequest(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markCallbackFailed(eq(coreRequest.getCoreRequestId()), anyString()))
                .thenAnswer(invocation -> {
                    coreRequest.markFailed(invocation.getArgument(1));
                    return coreRequest;
                });
        when(vpVerificationRepository.getById(21L)).thenReturn(vpVerification);
        when(vpVerificationRepository.save(vpVerificationCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CoreCallbackResponse response = service.processVpVerificationCallback(
                coreRequest.getCoreRequestId(),
                new CoreVpVerificationCallbackRequest(
                        coreRequest.getCoreRequestId(),
                        KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED.name(),
                        "replay-summary",
                        false,
                        null,
                        verifiedAt
                )
        );

        assertThat(vpVerificationCaptor.getValue().getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED);
        assertThat(vpVerificationCaptor.getValue().getReplaySuspectedYn()).isEqualTo(KyvcEnums.Yn.Y.name());
        assertThat(response.status()).isEqualTo(KyvcEnums.CoreRequestStatus.FAILED.name());
    }

    @Test
    void processVpVerificationCallback_marksReplaySuspectedWhenReplayFlagTrue() {
        LocalDateTime verifiedAt = LocalDateTime.now();
        CoreRequest coreRequest = createVpCoreRequest(21L);
        VpVerification vpVerification = createPresentedVpVerification(21L);
        CoreCallbackService service = createService();

        when(coreRequestService.getCoreRequest(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markCallbackFailed(eq(coreRequest.getCoreRequestId()), anyString()))
                .thenAnswer(invocation -> {
                    coreRequest.markFailed(invocation.getArgument(1));
                    return coreRequest;
                });
        when(vpVerificationRepository.getById(21L)).thenReturn(vpVerification);
        when(vpVerificationRepository.save(vpVerificationCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CoreCallbackResponse response = service.processVpVerificationCallback(
                coreRequest.getCoreRequestId(),
                new CoreVpVerificationCallbackRequest(
                        coreRequest.getCoreRequestId(),
                        "SUCCESS",
                        "replay-summary",
                        true,
                        null,
                        verifiedAt
                )
        );

        verify(coreRequestService, never()).markCallbackSuccess(eq(coreRequest.getCoreRequestId()), anyString());
        assertThat(vpVerificationCaptor.getValue().getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED);
        assertThat(vpVerificationCaptor.getValue().getReplaySuspectedYn()).isEqualTo(KyvcEnums.Yn.Y.name());
        assertThat(response.status()).isEqualTo(KyvcEnums.CoreRequestStatus.FAILED.name());
    }

    private CoreCallbackService createService() {
        return new CoreCallbackService(
                coreRequestService,
                credentialRepository,
                kycApplicationRepository,
                vpVerificationRepository,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    private CoreRequest createVpCoreRequest(Long vpVerificationId) {
        return CoreRequest.create(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                vpVerificationId,
                null
        );
    }

    private VpVerification createPresentedVpVerification(Long vpVerificationId) {
        VpVerification vpVerification = newInstance(VpVerification.class);
        ReflectionTestUtils.setField(vpVerification, "vpVerificationId", vpVerificationId);
        ReflectionTestUtils.setField(vpVerification, "corporateId", 10L);
        ReflectionTestUtils.setField(vpVerification, "requestNonce", "nonce-001");
        ReflectionTestUtils.setField(vpVerification, "purpose", "KYC 제출 증빙");
        ReflectionTestUtils.setField(vpVerification, "vpVerificationStatus", KyvcEnums.VpVerificationStatus.REQUESTED);
        ReflectionTestUtils.setField(vpVerification, "replaySuspectedYn", KyvcEnums.Yn.N.name());
        ReflectionTestUtils.setField(vpVerification, "requestedAt", LocalDateTime.now().minusMinutes(2));
        ReflectionTestUtils.setField(vpVerification, "expiresAt", LocalDateTime.now().plusMinutes(30));
        ReflectionTestUtils.setField(vpVerification, "vpRequestId", "vp-req-001");
        ReflectionTestUtils.setField(vpVerification, "challenge", "challenge-001");
        vpVerification.markPresented(100L, "vp-jwt-hash", "core-request-id", LocalDateTime.now().minusMinutes(1));
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
