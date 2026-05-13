package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * KYC 신청 상세 조회 응답 DTO입니다.
 */
@Schema(description = "KYC 신청 상세 조회 응답")
public record AdminKycApplicationDetailResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,

        /** 신청 사용자 ID */
        @Schema(description = "신청 사용자 ID", example = "1")
        Long applicantUserId,

        /** 법인 유형 코드 */
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode,

        /** 법인명 */
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,

        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNumber,

        /** 대표자명 */
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName,

        /** KYC 신청 상태 */
        @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
        String status,

        /** AI 심사 상태 */
        @Schema(description = "AI 심사 상태", example = "SUCCESS")
        String aiReviewStatus,

        /** AI 심사 결과 */
        @Schema(description = "AI 심사 결과", example = "PASS")
        String aiReviewResult,

        /** AI 신뢰도 점수 */
        @Schema(description = "AI 신뢰도 점수", example = "92.50")
        BigDecimal aiConfidenceScore,

        /** AI 심사 요약 */
        @Schema(description = "AI 심사 요약", example = "필수 서류와 법인 정보가 일치합니다.")
        String aiReviewSummary,

        /** AI 심사 사유 코드 */
        @Schema(description = "AI 심사 사유 코드", example = "LOW_AI_CONFIDENCE")
        String aiReviewReasonCode,

        /** 반려 사유 코드 */
        @Schema(description = "반려 사유 코드", example = "INVALID_DOCUMENT")
        String rejectReasonCode,

        /** 수동심사 사유 */
        @Schema(description = "수동심사 사유", example = "관리자 확인이 필요한 신청입니다.")
        String manualReviewReason,

        /** 반려 사유 */
        @Schema(description = "반려 사유", example = "제출 문서가 식별되지 않습니다.")
        String rejectReason,

        /** 제출 문서 수 */
        @Schema(description = "제출 문서 수", example = "3")
        Long documentCount,

        /** Credential 발급 상태 */
        @Schema(description = "Credential 발급 상태", example = "VALID")
        String credentialStatus,

        /** KYC 제출 시각 */
        @Schema(description = "KYC 제출 시각")
        LocalDateTime submittedAt,

        /** KYC 수정 시각 */
        @Schema(description = "KYC 수정 시각")
        LocalDateTime updatedAt,

        /** 수동 심사 상태 */
        @Schema(description = "수동 심사 상태", example = "MANUAL_REVIEW")
        String manualReviewStatus,

        /** 최신 Core 요청 상태 */
        @Schema(description = "최신 Core 요청 상태", example = "SUCCESS")
        String latestCoreRequestStatus,

        /** 최신 심사 이력 액션 유형 */
        @Schema(description = "최신 심사 이력 액션 유형", example = "APPROVE")
        String latestReviewActionType,

        /** 최신 심사 이력 의견 */
        @Schema(description = "최신 심사 이력 의견", example = "서류 검토 완료")
        String latestReviewComment,

        /** 최신 심사 이력 생성 시각 */
        @Schema(description = "최신 심사 이력 생성 시각")
        LocalDateTime latestReviewCreatedAt
) {
}
