package com.kyvc.backend.domain.verifier.application;

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
import com.kyvc.backend.domain.verifier.domain.Verifier;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationQueryRepository;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private VerifierVpService service;

    @BeforeEach
    void setUp() {
        service = new VerifierVpService(
                vpVerificationRepository,
                vpVerificationQueryRepository,
                credentialRepository,
                corporateRepository,
                verifierRepository,
                coreRequestService,
                coreAdapter,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    @Test
    void createFinanceVpRequest_createsRequestWithoutCoreCall() {
        Corporate corporate = createCorporate(10L, 1L);
        Credential credential = createCredential(100L, 10L);
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.findLatestByCorporateId(10L)).thenReturn(Optional.of(credential));
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceVpRequestCreateResponse response = service.createFinanceVpRequest(
                userDetails(),
                new FinanceVpRequestCreateRequest(
                        "ACCOUNT_OPENING",
                        List.of("corporateName", "businessRegistrationNo"),
                        600L
                )
        );

        verify(coreAdapter, never()).requestVpVerification(any(CoreVpVerificationRequest.class), any());
        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());

        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.REQUESTED.name());
        assertThat(response.requestId()).isEqualTo(saved.getVpRequestId());
        assertThat(response.qrPayload()).contains(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(saved.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.REQUESTED);
        assertThat(saved.getRequestTypeCode()).isEqualTo(KyvcEnums.VpRequestType.FINANCE_VERIFY);
        assertThat(saved.getFinanceInstitutionCode()).isEqualTo("FINANCE_USER_1");
    }

    @Test
    void createReAuthRequest_storesResultNotifyUrlWithoutCoreCall() {
        Corporate corporate = createCorporate(10L, 1L);
        Credential credential = createCredential(100L, 10L);
        Verifier verifier = createVerifier(3L, "user@test.com");
        when(verifierRepository.findLatestByContactEmail("user@test.com")).thenReturn(Optional.of(verifier));
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(credentialRepository.findLatestByCorporateId(10L)).thenReturn(Optional.of(credential));
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerifierReAuthRequestCreateResponse response = service.createReAuthRequest(
                userDetails(),
                new VerifierReAuthRequestCreateRequest(
                        10L,
                        "정기 재인증",
                        List.of("corporateName", "kycStatus"),
                        "https://verifier.example.com/kyvc/result"
                )
        );

        verify(coreAdapter, never()).requestVpVerification(any(CoreVpVerificationRequest.class), any());
        verifyNoInteractions(coreRequestService);
        verify(vpVerificationRepository).save(vpVerificationCaptor.capture());

        VpVerification saved = vpVerificationCaptor.getValue();
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.REQUESTED.name());
        assertThat(response.qrPayload()).contains(KyvcEnums.QrType.VP_REQUEST.name());
        assertThat(saved.getRequestTypeCode()).isEqualTo(KyvcEnums.VpRequestType.RE_AUTH);
        assertThat(saved.getReAuthYn()).isEqualTo(KyvcEnums.Yn.Y);
        assertThat(saved.getPermissionResultJson()).contains("resultNotifyUrl");
        assertThat(saved.getPermissionResultJson()).contains("https://verifier.example.com/kyvc/result");
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
        when(coreAdapter.requestVpVerification(any(CoreVpVerificationRequest.class), eq(vpJwt)))
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

        verify(coreAdapter).requestVpVerification(any(CoreVpVerificationRequest.class), eq(vpJwt));
        verify(coreRequestService).markRunning(coreRequest.getCoreRequestId());
        verify(coreRequestService).markSuccess(eq(coreRequest.getCoreRequestId()), any());
        verify(coreRequestService).updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), requestPayloadCaptor.capture());
        verify(vpVerificationRepository, org.mockito.Mockito.atLeastOnce()).save(vpVerificationCaptor.capture());

        VpVerification completed = vpVerificationCaptor.getAllValues().get(vpVerificationCaptor.getAllValues().size() - 1);
        assertThat(requestPayloadCaptor.getValue()).doesNotContain(vpJwt);
        assertThat(completed.getVpJwtHash()).isEqualTo(TokenHashUtil.sha256(vpJwt));
        assertThat(completed.getVpVerificationStatus()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID);
        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID.name());
        assertThat(response.result().signatureValid()).isTrue();
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
