package com.kyvc.backend.domain.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.auth.dto.WebVpLoginSubmitRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationVerifyResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebVpLoginServiceTest {

    @Mock
    private VpVerificationRepository vpVerificationRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoreAdapter coreAdapter;

    @Mock
    private AuthService authService;

    @Mock
    private TokenCookieUtil tokenCookieUtil;

    @Mock
    private LogEventLogger logEventLogger;

    private WebVpLoginService service;

    @BeforeEach
    void setUp() {
        service = new WebVpLoginService(
                vpVerificationRepository,
                credentialRepository,
                corporateRepository,
                userRepository,
                coreAdapter,
                authService,
                tokenCookieUtil,
                new ObjectMapper(),
                logEventLogger
        );
    }

    @Test
    void submit_allowsHashCredentialTypeWhenCredentialIsValidAndWalletSaved() {
        String requestId = "vp-login-req-001";
        String qrToken = "raw-qr-token";
        Map<String, Object> vp = Map.of(
                "format", "kyvc-sd-jwt-presentation-v1",
                "sdJwtKb", "masked-test-presentation"
        );
        VpVerification vpVerification = VpVerification.createWebVpLoginRequest(
                requestId,
                TokenHashUtil.sha256(qrToken),
                "browser-session-hash",
                "nonce-001",
                "challenge-001",
                "kyvc-corporate-web-login",
                "[]",
                LocalDateTime.now().plusMinutes(5),
                "{}"
        );
        ReflectionTestUtils.setField(vpVerification, "vpVerificationId", 100L);
        Credential credential = Credential.createIssuing(
                6L,
                10L,
                "credential-external-id",
                "4DF08F86334AB5C5CD4551A9685E27CB1063CF5E3102E30E5F123AFDE2904C8E",
                "did:xrpl:1:rIssuer",
                KyvcEnums.CredentialStatusPurpose.revocation.name(),
                "STANDARD",
                "KR",
                "did:xrpl:1:rHolder",
                "rHolder"
        );
        ReflectionTestUtils.setField(credential, "credentialId", 25L);
        credential.refreshStatus(KyvcEnums.CredentialStatus.VALID);
        credential.acceptToWallet("android-device-id", "did:xrpl:1:rHolder", "rHolder", LocalDateTime.now());
        credential.applyCredentialFormat("dc+sd-jwt");

        when(vpVerificationRepository.findByRequestId(requestId)).thenReturn(Optional.of(vpVerification));
        when(credentialRepository.findById(25L)).thenReturn(Optional.of(credential));
        when(vpVerificationRepository.save(any(VpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coreAdapter.verifyWebVpLoginPresentation(same(vp))).thenReturn(new CorePresentationVerifyResponse(
                true,
                null,
                null,
                List.of(),
                Map.of(),
                "Core 검증 성공"
        ));

        var response = service.submit(
                requestId,
                new WebVpLoginSubmitRequest(qrToken, 25L, vp, "android-device-id", null)
        );

        assertThat(response.status()).isEqualTo(KyvcEnums.VpVerificationStatus.VALID.name());
        assertThat(response.verified()).isTrue();
        assertThat(vpVerification.getCredentialId()).isEqualTo(25L);
        assertThat(vpVerification.getCorporateId()).isEqualTo(6L);
        verify(coreAdapter).verifyWebVpLoginPresentation(same(vp));
    }
}
