package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 제출 응답
 *
 * @param kycId KYC 신청 ID
 * @param kycStatus KYC 상태
 * @param submittedAt 제출 일시
 * @param submittable 제출 가능 여부
 * @param message 제출 결과 메시지
 */
@Schema(description = "KYC 제출 응답")
public record KycSubmitResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태", example = "SUBMITTED")
        String kycStatus, // KYC 상태
        @Schema(description = "제출 일시", example = "2026-05-04T15:00:00")
        LocalDateTime submittedAt, // 제출 일시
        @Schema(description = "제출 가능 여부", example = "true")
        boolean submittable, // 제출 가능 여부
        @Schema(description = "제출 결과 메시지", example = "KYC 신청이 제출되었습니다.")
        String message // 제출 결과 메시지
) {
}
