package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.kyc.repository.KycReviewHistoryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycSubmissionStatusServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long KYC_ID = 20L;
    private static final Long CORPORATE_ID = 30L;
    private static final String CORPORATE_TYPE_CODE = "CORPORATION";

    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private KycReviewHistoryRepository kycReviewHistoryRepository;

    private KycSubmissionStatusService service;

    @BeforeEach
    void setUp() {
        service = new KycSubmissionStatusService(
                kycApplicationRepository,
                kycReviewHistoryRepository
        );
    }

    @Test
    void reserveSubmission_updatesDraftToSubmittedThenAiReviewing() {
        LocalDateTime submittedAt = LocalDateTime.now();
        KycApplication kycApplication = KycApplication.createDraft(CORPORATE_ID, USER_ID, CORPORATE_TYPE_CODE);
        ReflectionTestUtils.setField(kycApplication, "kycId", KYC_ID);

        when(kycApplicationRepository.updateStatusIfCurrentStatus(
                KYC_ID,
                USER_ID,
                KyvcEnums.KycStatus.DRAFT,
                KyvcEnums.KycStatus.SUBMITTED,
                submittedAt,
                submittedAt
        )).thenReturn(1);
        kycApplication.submit(submittedAt);
        when(kycApplicationRepository.findById(KYC_ID)).thenReturn(Optional.of(kycApplication));
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycApplication result = service.reserveSubmission(USER_ID, KYC_ID, submittedAt);

        assertThat(result.getKycStatus()).isEqualTo(KyvcEnums.KycStatus.AI_REVIEWING);
        assertThat(result.getAiReviewStatus()).isEqualTo(KyvcEnums.AiReviewStatus.QUEUED);
        verify(kycReviewHistoryRepository).saveStatusChange(
                eq(KYC_ID),
                eq(KyvcEnums.ReviewActionType.SUBMIT),
                eq(KyvcEnums.KycStatus.DRAFT),
                eq(KyvcEnums.KycStatus.SUBMITTED),
                eq("KYC 제출 완료"),
                eq(submittedAt)
        );
        verify(kycReviewHistoryRepository).saveStatusChange(
                eq(KYC_ID),
                eq(KyvcEnums.ReviewActionType.AI_START),
                eq(KyvcEnums.KycStatus.SUBMITTED),
                eq(KyvcEnums.KycStatus.AI_REVIEWING),
                eq("AI 심사 요청 시작"),
                eq(submittedAt)
        );
    }

    @Test
    void reserveSubmission_rejectsWhenDraftUpdateAlreadyTaken() {
        LocalDateTime submittedAt = LocalDateTime.now();
        when(kycApplicationRepository.updateStatusIfCurrentStatus(
                KYC_ID,
                USER_ID,
                KyvcEnums.KycStatus.DRAFT,
                KyvcEnums.KycStatus.SUBMITTED,
                submittedAt,
                submittedAt
        )).thenReturn(0);

        assertThatThrownBy(() -> service.reserveSubmission(USER_ID, KYC_ID, submittedAt))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.KYC_ALREADY_SUBMITTED);

        verify(kycApplicationRepository, never()).save(any(KycApplication.class));
        verify(kycReviewHistoryRepository, never()).saveStatusChange(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }
}
