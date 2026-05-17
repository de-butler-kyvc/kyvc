package com.kyvc.backend.domain.review.application;

import com.kyvc.backend.domain.document.application.KycDocumentService;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.kyc.repository.KycReviewHistoryRepository;
import com.kyvc.backend.domain.review.domain.KycSupplement;
import com.kyvc.backend.domain.review.dto.SupplementSubmitRequest;
import com.kyvc.backend.domain.review.dto.SupplementSubmitResponse;
import com.kyvc.backend.domain.review.repository.SupplementDocumentRepository;
import com.kyvc.backend.domain.review.repository.SupplementRepository;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long KYC_ID = 10L;
    private static final Long CORPORATE_ID = 20L;
    private static final Long SUPPLEMENT_ID = 30L;

    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private KycDocumentService kycDocumentService;
    @Mock
    private SupplementRepository supplementRepository;
    @Mock
    private SupplementDocumentRepository supplementDocumentRepository;
    @Mock
    private KycReviewHistoryRepository kycReviewHistoryRepository;

    private SupplementService service;

    @BeforeEach
    void setUp() {
        service = new SupplementService(
                kycApplicationRepository,
                kycDocumentRepository,
                kycDocumentService,
                supplementRepository,
                supplementDocumentRepository,
                kycReviewHistoryRepository
        );
    }

    @Test
    void submitSupplement_changesKycStatusToSubmittedAndSavesHistory() {
        KycApplication kycApplication = needSupplementKycApplication();
        KycSupplement supplement = requestedSupplement();

        when(kycApplicationRepository.findById(KYC_ID)).thenReturn(Optional.of(kycApplication));
        when(supplementRepository.findByIdAndKycId(SUPPLEMENT_ID, KYC_ID)).thenReturn(Optional.of(supplement));
        when(kycDocumentRepository.existsByKycIdAndDocumentTypeCode(KYC_ID, "BUSINESS_REGISTRATION"))
                .thenReturn(true);
        when(supplementRepository.save(any(KycSupplement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplementSubmitResponse response = service.submitSupplement(
                USER_ID,
                KYC_ID,
                SUPPLEMENT_ID,
                new SupplementSubmitRequest("보완 제출")
        );

        assertThat(response.kycStatus()).isEqualTo(KyvcEnums.KycStatus.SUBMITTED.name());
        assertThat(response.supplementStatus()).isEqualTo(KyvcEnums.SupplementStatus.SUBMITTED.name());
        verify(kycReviewHistoryRepository).saveStatusChange(
                eq(KYC_ID),
                eq(KyvcEnums.ReviewActionType.SUPPLEMENT_SUBMIT),
                eq(KyvcEnums.KycStatus.NEED_SUPPLEMENT),
                eq(KyvcEnums.KycStatus.SUBMITTED),
                eq("보완 제출 완료"),
                any(LocalDateTime.class)
        );
    }

    private KycApplication needSupplementKycApplication() {
        KycApplication kycApplication = KycApplication.createDraft(CORPORATE_ID, USER_ID, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", KYC_ID);
        kycApplication.completeAiReviewAsNeedSupplement(
                new BigDecimal("0.50"),
                "보완 필요",
                "{}",
                "보완 요청"
        );
        return kycApplication;
    }

    private KycSupplement requestedSupplement() {
        KycSupplement supplement = BeanUtils.instantiateClass(KycSupplement.class);
        ReflectionTestUtils.setField(supplement, "supplementId", SUPPLEMENT_ID);
        ReflectionTestUtils.setField(supplement, "kycId", KYC_ID);
        ReflectionTestUtils.setField(supplement, "supplementStatus", KyvcEnums.SupplementStatus.REQUESTED);
        ReflectionTestUtils.setField(supplement, "requestedDocumentTypeCodes", "[\"BUSINESS_REGISTRATION\"]");
        return supplement;
    }
}
