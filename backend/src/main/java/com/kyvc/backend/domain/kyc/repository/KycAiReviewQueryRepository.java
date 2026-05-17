package com.kyvc.backend.domain.kyc.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * KYC AI 심사 결과 조회 Repository
 */
public interface KycAiReviewQueryRepository {

    /**
     * KYC AI 심사 결과 조회
     *
     * @param kycId KYC 신청 ID
     * @return KYC AI 심사 결과 조회 행
     */
    Optional<Row> findByKycId(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC AI 심사 결과 조회 행
     *
     * @param kycId KYC 신청 ID
     * @param corporateId 법인 ID
     * @param corporateUserId 법인 소유 사용자 ID
     * @param applicantUserId 신청 사용자 ID
     * @param applicationStatusCode KYC 신청 상태 코드
     * @param aiReviewStatusCode AI 심사 상태 코드
     * @param aiReviewResultCode AI 심사 결과 코드
     * @param aiConfidenceScore AI 신뢰도 점수
     * @param aiReviewSummary AI 심사 요약
     * @param aiReviewDetailJson AI 심사 상세 JSON
     * @param coreAiAssessmentJson Core AI assessment JSON
     * @param manualReviewReason 수기 심사 사유
     * @param aiReviewReasonCode AI 심사 사유 코드
     * @param approvedAt 승인 일시
     * @param rejectedAt 반려 일시
     * @param updatedAt 수정 일시
     */
    record Row(
            Long kycId,
            Long corporateId,
            Long corporateUserId,
            Long applicantUserId,
            String applicationStatusCode,
            String aiReviewStatusCode,
            String aiReviewResultCode,
            BigDecimal aiConfidenceScore,
            String aiReviewSummary,
            String aiReviewDetailJson,
            String coreAiAssessmentJson,
            String manualReviewReason,
            String aiReviewReasonCode,
            LocalDateTime approvedAt,
            LocalDateTime rejectedAt,
            LocalDateTime updatedAt
    ) {
    }
}
