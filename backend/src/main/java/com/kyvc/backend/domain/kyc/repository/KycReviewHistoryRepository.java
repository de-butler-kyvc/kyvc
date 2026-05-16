package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.global.util.KyvcEnums;

import java.time.LocalDateTime;

/**
 * KYC 심사 이력 Repository
 */
public interface KycReviewHistoryRepository {

    /**
     * KYC 상태 변경 이력 저장
     *
     * @param kycId KYC 신청 ID
     * @param actionType 심사 처리 유형
     * @param beforeStatus 변경 전 KYC 상태
     * @param afterStatus 변경 후 KYC 상태
     * @param comment 처리 의견
     * @param createdAt 생성 일시
     */
    void saveStatusChange(
            Long kycId, // KYC 신청 ID
            KyvcEnums.ReviewActionType actionType, // 심사 처리 유형
            KyvcEnums.KycStatus beforeStatus, // 변경 전 KYC 상태
            KyvcEnums.KycStatus afterStatus, // 변경 후 KYC 상태
            String comment, // 처리 의견
            LocalDateTime createdAt // 생성 일시
    );
}
