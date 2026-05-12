package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialOffer;
import com.kyvc.backend.domain.credential.dto.CredentialOfferCreateResponse;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialOfferServiceTest {

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
                new ObjectMapper().findAndRegisterModules(),
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
}
