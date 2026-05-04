package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 신청 응답
 *
 * @param kycId KYC 신청 ID
 * @param corporateId 법인 ID
 * @param applicantUserId 신청 사용자 ID
 * @param corporateTypeCode 법인 유형 코드
 * @param kycStatus KYC 상태
 * @param originalDocumentStoreOption 원본서류 저장 옵션
 * @param submittedAt 제출일시
 * @param createdAt 생성일시
 * @param updatedAt 수정일시
 */
@Schema(description = "KYC 신청 응답")
public record KycApplicationResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "신청 사용자 ID", example = "1")
        Long applicantUserId, // 신청 사용자 ID
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "KYC 상태", example = "DRAFT")
        String kycStatus, // KYC 상태
        @Schema(description = "원본서류 저장 옵션", example = "STORE")
        String originalDocumentStoreOption, // 원본서류 저장 옵션
        @Schema(description = "제출일시", example = "2026-05-04T12:30:00")
        LocalDateTime submittedAt, // 제출일시
        @Schema(description = "생성일시", example = "2026-05-04T12:30:00")
        LocalDateTime createdAt, // 생성일시
        @Schema(description = "수정일시", example = "2026-05-04T12:30:00")
        LocalDateTime updatedAt // 수정일시
) {
}
