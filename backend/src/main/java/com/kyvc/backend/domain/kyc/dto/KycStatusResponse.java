package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 진행상태 응답
 *
 * @param kycId KYC 신청 ID
 * @param kycStatus KYC 상태
 * @param corporateTypeCode 법인 유형 코드
 * @param originalDocumentStoreOption 원본서류 저장 옵션
 * @param submittedAt 제출일시
 */
@Schema(description = "KYC 진행상태 응답")
public record KycStatusResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태", example = "DRAFT")
        String kycStatus, // KYC 상태
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "원본서류 저장 옵션", example = "STORE")
        String originalDocumentStoreOption, // 원본서류 저장 옵션
        @Schema(description = "제출일시", example = "2026-05-04T12:30:00")
        LocalDateTime submittedAt // 제출일시
) {
}
