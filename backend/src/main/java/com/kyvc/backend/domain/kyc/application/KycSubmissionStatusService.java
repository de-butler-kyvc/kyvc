package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.kyc.repository.KycReviewHistoryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// KYC 제출 상태 선반영 서비스
@Service
@RequiredArgsConstructor
public class KycSubmissionStatusService {

    private final KycApplicationRepository kycApplicationRepository;
    private final KycReviewHistoryRepository kycReviewHistoryRepository;

    // KYC 제출 상태 선점 및 AI 심사 시작
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KycApplication reserveSubmission(
            Long userId, // 사용자 ID
            Long kycId, // KYC 신청 ID
            LocalDateTime submittedAt // 제출 일시
    ) {
        int updateCount = kycApplicationRepository.updateStatusIfCurrentStatus(
                kycId,
                userId,
                KyvcEnums.KycStatus.DRAFT,
                KyvcEnums.KycStatus.SUBMITTED,
                submittedAt,
                submittedAt
        ); // 제출 선점 변경 건수
        if (updateCount != 1) {
            throw new ApiException(ErrorCode.KYC_ALREADY_SUBMITTED);
        }

        saveStatusHistory(
                kycId,
                KyvcEnums.ReviewActionType.SUBMIT,
                KyvcEnums.KycStatus.DRAFT,
                KyvcEnums.KycStatus.SUBMITTED,
                "KYC 제출 완료",
                submittedAt
        );

        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }

        KyvcEnums.KycStatus beforeAiReviewStatus = kycApplication.getKycStatus(); // AI 심사 시작 전 상태
        kycApplication.startAiReview(submittedAt);
        KycApplication savedApplication = kycApplicationRepository.save(kycApplication);
        saveStatusHistory(
                savedApplication.getKycId(),
                KyvcEnums.ReviewActionType.AI_START,
                beforeAiReviewStatus,
                savedApplication.getKycStatus(),
                "AI 심사 요청 시작",
                submittedAt
        );
        return savedApplication;
    }

    // KYC 상태 변경 이력 저장
    private void saveStatusHistory(
            Long kycId, // KYC 신청 ID
            KyvcEnums.ReviewActionType actionType, // 심사 처리 유형
            KyvcEnums.KycStatus beforeStatus, // 변경 전 KYC 상태
            KyvcEnums.KycStatus afterStatus, // 변경 후 KYC 상태
            String comment, // 처리 의견
            LocalDateTime createdAt // 생성 일시
    ) {
        kycReviewHistoryRepository.saveStatusChange(
                kycId,
                actionType,
                beforeStatus,
                afterStatus,
                comment,
                createdAt
        );
    }
}
