package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.dto.WalletCredentialAcceptRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialAcceptResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialDetailResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialListResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialSummaryResponse;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.mobile.application.MobileDeviceService;
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

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileWalletServiceTest {

    private static final String QR_TOKEN = "qr-token-001";
    private static final String DEVICE_ID = "device-001";

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private MobileDeviceService mobileDeviceService;

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private CoreAdapter coreAdapter;

    @Mock
    private LogEventLogger logEventLogger;

    private MobileWalletService service;

    @BeforeEach
    void setUp() {
        service = new MobileWalletService(
                credentialRepository,
                mobileDeviceService,
                corporateRepository,
                coreAdapter,
                new CoreProperties(),
                logEventLogger
        );

        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(createCorporate(10L, 1L)));
    }

    @Test
    void acceptCredentialOffer_returnsNullCredentialPayload_whenAccepted() {
        Credential credential = createCredential(100L, false, LocalDateTime.now().plusMinutes(10));
        mockCredentialForAccept(credential);

        WalletCredentialAcceptResponse response = service.acceptCredentialOffer(
                userDetails(),
                100L,
                acceptRequest(true)
        );

        assertThat(response.walletSaved()).isTrue();
        assertThat(response.credentialPayload()).isNull();
        assertThat(response.message()).isEqualTo("Credential payload는 반환하지 않습니다. prepare/confirm API를 사용해 주세요.");
    }

    @Test
    void acceptCredentialOffer_doesNotReturnPayload_whenUserDeclines() {
        Credential credential = createCredential(100L, false, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));

        WalletCredentialAcceptResponse response = service.acceptCredentialOffer(
                userDetails(),
                100L,
                acceptRequest(false)
        );

        assertThat(response.walletSaved()).isFalse();
        assertThat(response.credentialPayload()).isNull();
        verify(credentialRepository, never()).save(any(Credential.class));
    }

    @Test
    void acceptCredentialOffer_throwsInvalidToken_whenQrTokenMismatch() {
        Credential credential = createCredential(100L, false, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.acceptCredentialOffer(
                        userDetails(),
                        100L,
                        new WalletCredentialAcceptRequest("wrong-token", DEVICE_ID, "did:xrpl:1:rHolder", "rHolder", true)
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_OFFER_INVALID_TOKEN);
    }

    @Test
    void acceptCredentialOffer_throwsDeviceNotRegistered_whenDeviceMissing() {
        Credential credential = createCredential(100L, false, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));
        when(mobileDeviceService.getActiveDeviceBinding(1L, DEVICE_ID))
                .thenThrow(new ApiException(ErrorCode.WALLET_DEVICE_NOT_REGISTERED));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.acceptCredentialOffer(userDetails(), 100L, acceptRequest(true))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WALLET_DEVICE_NOT_REGISTERED);
    }

    @Test
    void acceptCredentialOffer_throwsDeviceInactive_whenDeviceInactive() {
        Credential credential = createCredential(100L, false, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));
        when(mobileDeviceService.getActiveDeviceBinding(1L, DEVICE_ID))
                .thenThrow(new ApiException(ErrorCode.WALLET_DEVICE_INACTIVE));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.acceptCredentialOffer(userDetails(), 100L, acceptRequest(true))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WALLET_DEVICE_INACTIVE);
    }

    @Test
    void acceptCredentialOffer_throwsExpired_whenOfferExpired() {
        Credential credential = createCredential(100L, false, LocalDateTime.now().minusSeconds(1));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.acceptCredentialOffer(userDetails(), 100L, acceptRequest(true))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_OFFER_EXPIRED);
    }

    @Test
    void acceptCredentialOffer_throwsAlreadySaved_whenCredentialAlreadySaved() {
        Credential credential = createCredential(100L, true, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.acceptCredentialOffer(userDetails(), 100L, acceptRequest(true))
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WALLET_CREDENTIAL_ALREADY_SAVED);
    }

    @Test
    void getWalletCredentialDetail_returnsNullCredentialPayload() {
        Credential credential = createCredential(100L, true, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.getById(100L)).thenReturn(credential);

        WalletCredentialDetailResponse response = service.getWalletCredentialDetail(userDetails(), 100L);

        assertThat(response.credentialPayload()).isNull();
    }

    @Test
    void getWalletCredentialList_doesNotExposeCredentialPayload() {
        Credential credential = createCredential(100L, true, LocalDateTime.now().plusMinutes(10));
        when(credentialRepository.findWalletCredentialsByCorporateId(10L)).thenReturn(List.of(credential));

        WalletCredentialListResponse response = service.getWalletCredentials(userDetails());

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.credentials()).extracting(WalletCredentialSummaryResponse::credentialId).containsExactly(100L);
        assertThat(List.of(WalletCredentialSummaryResponse.class.getRecordComponents()))
                .extracting(component -> component.getName())
                .doesNotContain("credentialPayload");
    }

    private void mockCredentialForAccept(
            Credential credential // Wallet 저장 대상 Credential
    ) {
        when(credentialRepository.findById(100L)).thenReturn(Optional.of(credential));
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private WalletCredentialAcceptRequest acceptRequest(
            boolean accepted // 수락 여부
    ) {
        return new WalletCredentialAcceptRequest(
                QR_TOKEN,
                DEVICE_ID,
                "did:xrpl:1:rHolder",
                "rHolder",
                accepted
        );
    }

    private Credential createCredential(
            Long credentialId, // Credential ID
            boolean walletSaved, // Wallet 저장 여부
            LocalDateTime qrExpiresAt // QR 만료 일시
    ) {
        Credential credential = newInstance(Credential.class);
        LocalDateTime issuedAt = LocalDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(credential, "credentialId", credentialId);
        ReflectionTestUtils.setField(credential, "corporateId", 10L);
        ReflectionTestUtils.setField(credential, "kycId", 300L);
        ReflectionTestUtils.setField(credential, "credentialExternalId", "cred-ext-" + credentialId);
        ReflectionTestUtils.setField(credential, "credentialTypeCode", KyvcEnums.CredentialType.KYC_CREDENTIAL.name());
        ReflectionTestUtils.setField(credential, "issuerDid", "did:xrpl:1:rIssuer");
        ReflectionTestUtils.setField(credential, "credentialStatus", KyvcEnums.CredentialStatus.VALID);
        ReflectionTestUtils.setField(credential, "vcHash", "vc-hash-" + credentialId);
        ReflectionTestUtils.setField(credential, "xrplTxHash", "tx-hash-" + credentialId);
        ReflectionTestUtils.setField(credential, "qrToken", QR_TOKEN);
        ReflectionTestUtils.setField(credential, "qrExpiresAt", qrExpiresAt);
        ReflectionTestUtils.setField(credential, "issuedAt", issuedAt);
        ReflectionTestUtils.setField(credential, "expiresAt", LocalDateTime.now().plusYears(1));
        ReflectionTestUtils.setField(credential, "walletSavedYn", walletSaved ? KyvcEnums.Yn.Y.name() : KyvcEnums.Yn.N.name());
        ReflectionTestUtils.setField(credential, "walletSavedAt", walletSaved ? issuedAt.plusHours(1) : null);
        ReflectionTestUtils.setField(credential, "holderDid", "did:xrpl:1:rHolder");
        ReflectionTestUtils.setField(credential, "holderXrplAddress", "rHolder");
        ReflectionTestUtils.setField(credential, "credentialStatusId", "status-id-" + credentialId);
        ReflectionTestUtils.setField(credential, "credentialStatusPurposeCode", "REVOCATION");
        ReflectionTestUtils.setField(credential, "kycLevelCode", "BASIC");
        ReflectionTestUtils.setField(credential, "jurisdictionCode", "KR");
        credential.applyCredentialFormat("dc+sd-jwt");
        return credential;
    }

    private Corporate createCorporate(
            Long corporateId, // 법인 ID
            Long userId // 사용자 ID
    ) {
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

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(
                1L,
                "user@test.com",
                KyvcEnums.UserType.CORPORATE_USER.name(),
                List.of("ROLE_CORPORATE_USER"),
                true
        );
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
