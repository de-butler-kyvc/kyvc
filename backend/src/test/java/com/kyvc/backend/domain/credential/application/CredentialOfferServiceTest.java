package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialOffer;
import com.kyvc.backend.domain.credential.dto.CredentialOfferCreateResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialPrepareRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialPrepareResponse;
import com.kyvc.backend.domain.credential.repository.CredentialOfferRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.mobile.application.MobileDeviceService;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialOfferServiceTest {

    private static final String ACTUAL_ISSUER_ACCOUNT = "rpseLKeHEoLDWBnTJvRJgh1mSNz7vJVENc";
    private static final String ACTUAL_HOLDER_ACCOUNT = "rf7J73nMdQq3WRh8dPDDRmJtruXk15hnfd";
    private static final String ACTUAL_HOLDER_DID = "did:xrpl:1:" + ACTUAL_HOLDER_ACCOUNT;
    private static final String ACTUAL_CREDENTIAL_TYPE = "56435F5354415455535F56313A2EEE";
    private static final String ACTUAL_CREDENTIAL_STATUS_ID =
            "xrpl:credential:" + ACTUAL_ISSUER_ACCOUNT + ":" + ACTUAL_HOLDER_ACCOUNT + ":" + ACTUAL_CREDENTIAL_TYPE;

    @Mock
    private CredentialOfferRepository credentialOfferRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private KycApplicationRepository kycApplicationRepository;

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private MobileDeviceService mobileDeviceService;

    @Mock
    private CredentialClaimsAssembler credentialClaimsAssembler;

    @Mock
    private CredentialIssuerResolver credentialIssuerResolver;

    @Mock
    private CredentialIssuanceService credentialIssuanceService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private LogEventLogger logEventLogger;

    @Captor
    private ArgumentCaptor<CredentialOffer> offerCaptor;

    @Captor
    private ArgumentCaptor<String> holderKeyIdCaptor;

    private CredentialOfferService service;

    @BeforeEach
    void setUp() {
        service = new CredentialOfferService(
                credentialOfferRepository,
                credentialRepository,
                kycApplicationRepository,
                corporateRepository,
                mobileDeviceService,
                credentialClaimsAssembler,
                credentialIssuerResolver,
                credentialIssuanceService,
                auditLogService,
                logEventLogger
        );
    }

    @Test
    void createOffer_createsOfferWithoutCredentialPlaceholder() {
        Corporate corporate = createCorporate();
        KycApplication kycApplication = createApprovedKyc();
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(kycApplicationRepository.findById(10L)).thenReturn(Optional.of(kycApplication));
        when(credentialRepository.existsWalletSavedValidByKycId(10L)).thenReturn(false);
        when(credentialOfferRepository.findActiveOffersByKycId(10L)).thenReturn(List.of());
        when(credentialOfferRepository.save(any(CredentialOffer.class))).thenAnswer(invocation -> {
            CredentialOffer offer = invocation.getArgument(0);
            ReflectionTestUtils.setField(offer, "credentialOfferId", 100L);
            return offer;
        });

        CredentialOfferCreateResponse response = service.createOffer(1L, 10L);

        verify(credentialOfferRepository).save(offerCaptor.capture());
        verify(credentialRepository, never()).save(any(Credential.class));

        CredentialOffer savedOffer = offerCaptor.getValue();
        String rawQrToken = (String) response.qrPayload().get("qrToken");

        assertThat(response.offerId()).isEqualTo(100L);
        assertThat(response.offerStatus()).isEqualTo(KyvcEnums.CredentialOfferStatus.ACTIVE.name());
        assertThat(rawQrToken).isNotBlank();
        assertThat(savedOffer.getOfferTokenHash()).isEqualTo(TokenHashUtil.sha256(rawQrToken));
        assertThat(savedOffer.getOfferTokenHash()).isNotEqualTo(rawQrToken);
        assertThat(savedOffer.getCredentialId()).isNull();
    }

    @Test
    void createOffer_rejectsNotApprovedKyc() {
        Corporate corporate = createCorporate();
        KycApplication kycApplication = createDraftKyc();
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(kycApplicationRepository.findById(10L)).thenReturn(Optional.of(kycApplication));

        ApiException exception = assertThrows(ApiException.class, () -> service.createOffer(1L, 10L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_OFFER_CREATE_NOT_ALLOWED);
        verify(credentialOfferRepository, never()).save(any(CredentialOffer.class));
    }

    @Test
    void createOffer_rejectsWhenWalletSavedCredentialExists() {
        Corporate corporate = createCorporate();
        KycApplication kycApplication = createApprovedKyc();
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(kycApplicationRepository.findById(10L)).thenReturn(Optional.of(kycApplication));
        when(credentialRepository.existsWalletSavedValidByKycId(10L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.createOffer(1L, 10L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_ISSUANCE_ALREADY_COMPLETED);
        verify(credentialOfferRepository, never()).save(any(CredentialOffer.class));
    }

    @Test
    void prepareWalletCredential_usesHolderDidFallbackHolderKeyId() {
        mockPrepareCredential(ACTUAL_HOLDER_DID, null);

        service.prepareWalletCredential(
                1L,
                100L,
                new WalletCredentialPrepareRequest(
                        "qr-token-001",
                        "device-001",
                        ACTUAL_HOLDER_DID,
                        ACTUAL_HOLDER_ACCOUNT,
                        null,
                        true
                )
        );

        verify(credentialIssuanceService).issueKycCredentialForHolderPayload(
                any(KycApplication.class),
                eq(1L),
                eq(ACTUAL_HOLDER_DID),
                eq(ACTUAL_HOLDER_ACCOUNT),
                holderKeyIdCaptor.capture(),
                any(),
                any(ResolvedIssuer.class)
        );
        assertThat(holderKeyIdCaptor.getValue()).isEqualTo(ACTUAL_HOLDER_DID + "#holder-key-1");
    }

    @Test
    void prepareWalletCredential_usesRequestHolderKeyIdFirst() {
        mockPrepareCredential(ACTUAL_HOLDER_DID, ACTUAL_HOLDER_DID + "#custom-key");

        service.prepareWalletCredential(
                1L,
                100L,
                new WalletCredentialPrepareRequest(
                        "qr-token-001",
                        "device-001",
                        ACTUAL_HOLDER_DID,
                        ACTUAL_HOLDER_ACCOUNT,
                        ACTUAL_HOLDER_DID + "#custom-key",
                        true
                )
        );

        verify(credentialIssuanceService).issueKycCredentialForHolderPayload(
                any(KycApplication.class),
                eq(1L),
                eq(ACTUAL_HOLDER_DID),
                eq(ACTUAL_HOLDER_ACCOUNT),
                holderKeyIdCaptor.capture(),
                any(),
                any(ResolvedIssuer.class)
        );
        assertThat(holderKeyIdCaptor.getValue()).isEqualTo(ACTUAL_HOLDER_DID + "#custom-key");
    }

    @Test
    void prepareWalletCredential_usesCredentialStatusIdForMetadataIssuer() {
        mockPrepareCredential(ACTUAL_HOLDER_DID, null);

        WalletCredentialPrepareResponse response = service.prepareWalletCredential(
                1L,
                100L,
                new WalletCredentialPrepareRequest(
                        "qr-token-001",
                        "device-001",
                        ACTUAL_HOLDER_DID,
                        ACTUAL_HOLDER_ACCOUNT,
                        null,
                        true
                )
        );

        Map<String, Object> metadata = metadata(response.credentialPayload());
        assertThat(response.credentialPayload().get("format")).isEqualTo("dc+sd-jwt");
        assertThat(response.credentialPayload().get("sdJwt")).isEqualTo("header.payload.signature~disclosure-001~");
        assertThat(response.credentialPayload().get("credentialJwt")).isEqualTo("header.payload.signature~disclosure-001~");
        assertThat(response.credentialPayload().get("credential")).isNull();
        assertThat(response.credentialPayload().get("selectiveDisclosure")).isEqualTo(Map.of(
                "disclosablePaths",
                List.of("$.legalEntity.corporateName")
        ));
        assertThat(metadata.get("issuerAccount")).isEqualTo(ACTUAL_ISSUER_ACCOUNT);
        assertThat(metadata.get("issuerAccount")).isNotEqualTo("rIssuer");
        assertThat(metadata.get("issuerDid")).isEqualTo("did:xrpl:1:" + ACTUAL_ISSUER_ACCOUNT);
        assertThat(metadata.get("credentialType")).isEqualTo(ACTUAL_CREDENTIAL_TYPE);
        assertThat(metadata.get("holderXrplAddress")).isEqualTo(ACTUAL_HOLDER_ACCOUNT);
    }

    @Test
    void prepareWalletCredential_rejectsPreparedOfferPayloadReplay() {
        Corporate corporate = createCorporate();
        CredentialOffer offer = CredentialOffer.create(
                10L,
                20L,
                TokenHashUtil.sha256("qr-token-001"),
                LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(offer, "credentialOfferId", 100L);
        offer.bindPreparedCredential(200L, "device-001", ACTUAL_HOLDER_DID, ACTUAL_HOLDER_ACCOUNT);

        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(credentialOfferRepository.getById(100L)).thenReturn(offer);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.prepareWalletCredential(
                        1L,
                        100L,
                        new WalletCredentialPrepareRequest(
                                "qr-token-001",
                                "device-001",
                                ACTUAL_HOLDER_DID,
                                ACTUAL_HOLDER_ACCOUNT,
                                null,
                                true
                        )
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WALLET_CREDENTIAL_PAYLOAD_NOT_REPLAYABLE);
        verify(credentialIssuanceService, never()).issueKycCredentialForHolderPayload(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private Corporate createCorporate() {
        Corporate corporate = Corporate.create(
                1L,
                "KYVC Corp",
                "123-45-67890",
                "110111-1234567",
                "CORPORATION",
                LocalDate.of(2020, 1, 1),
                "02-0000-0000",
                "대표자",
                "010-0000-0000",
                "rep@test.com",
                "서울",
                null,
                "IT",
                KyvcEnums.CorporateStatus.ACTIVE
        );
        ReflectionTestUtils.setField(corporate, "corporateId", 20L);
        return corporate;
    }

    private KycApplication createApprovedKyc() {
        KycApplication kycApplication = createDraftKyc();
        kycApplication.approveForDevTest(java.time.LocalDateTime.now());
        return kycApplication;
    }

    private KycApplication createDraftKyc() {
        KycApplication kycApplication = KycApplication.createDraft(20L, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", 10L);
        return kycApplication;
    }

    private void mockPrepareCredential(
            String holderDid, // Holder DID
            String holderKeyId // Holder 키 ID
    ) {
        Corporate corporate = createCorporate();
        KycApplication kycApplication = createApprovedKyc();
        CredentialOffer offer = CredentialOffer.create(
                10L,
                20L,
                TokenHashUtil.sha256("qr-token-001"),
                LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(offer, "credentialOfferId", 100L);
        ResolvedIssuer issuer = new ResolvedIssuer(
                "rIssuer",
                "did:xrpl:1:rIssuer",
                "did:xrpl:1:rIssuer#issuer-key-1",
                "issuer-key-1",
                KyvcEnums.CredentialType.KYC_CREDENTIAL.name()
        );
        Credential credential = createValidCredential(holderDid, ACTUAL_HOLDER_ACCOUNT);

        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(credentialOfferRepository.getById(100L)).thenReturn(offer);
        when(kycApplicationRepository.findById(10L)).thenReturn(Optional.of(kycApplication));
        when(credentialIssuerResolver.resolveKycIssuer()).thenReturn(issuer);
        when(credentialClaimsAssembler.assemble(kycApplication)).thenReturn(Map.of("corporateName", "KYVC Corp"));
        when(credentialIssuanceService.issueKycCredentialForHolderPayload(
                any(KycApplication.class),
                eq(1L),
                eq(holderDid),
                eq(ACTUAL_HOLDER_ACCOUNT),
                eq(holderKeyId == null ? holderDid + "#holder-key-1" : holderKeyId),
                any(),
                eq(issuer)
        )).thenReturn(new CredentialIssuanceResult(
                credential,
                "dc+sd-jwt",
                "header.payload.signature~disclosure-001~",
                null,
                Map.of("disclosablePaths", List.of("$.legalEntity.corporateName"))
        ));
    }

    private Credential createValidCredential(
            String holderDid, // Holder DID
            String holderXrplAddress // Holder XRPL 주소
    ) {
        Credential credential = Credential.createIssuing(
                20L,
                10L,
                "credential-external-001",
                ACTUAL_CREDENTIAL_TYPE,
                "did:xrpl:1:" + ACTUAL_ISSUER_ACCOUNT,
                "REVOCATION",
                KyvcEnums.KycLevel.STANDARD.name(),
                KyvcEnums.Jurisdiction.KR.name(),
                holderDid,
                holderXrplAddress
        );
        ReflectionTestUtils.setField(credential, "credentialId", 200L);
        credential.applyIssuanceMetadata(
                "credential-external-001",
                ACTUAL_CREDENTIAL_TYPE,
                "did:xrpl:1:" + ACTUAL_ISSUER_ACCOUNT,
                KyvcEnums.CredentialStatus.VALID,
                "vc-hash-001",
                "tx-hash-001",
                ACTUAL_CREDENTIAL_STATUS_ID,
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1)
        );
        credential.applyCredentialFormat("dc+sd-jwt");
        return credential;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> metadata(
            Map<String, Object> credentialPayload // Credential payload 데이터
    ) {
        return (Map<String, Object>) credentialPayload.get("metadata");
    }
}
