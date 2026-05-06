package com.kyvc.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 보완 제출 응답
 *
 * @param kycId KYC 요청 ID
 * @param supplementId 보완요청 ID
 * @param kycStatus KYC 상태
 * @param supplementStatus 보완요청 상태
 * @param submittedAt 제출 일시
 * @param message 결과 메시지
 */
@Schema(description = "보완 제출 응답")
public record SupplementSubmitResponse(
        @Schema(description = "KYC 요청 ID", example = "1")
        Long kycId, // KYC 요청 ID
        @Schema(description = "보완요청 ID", example = "1")
        Long supplementId, // 보완요청 ID
        @Schema(description = "KYC 상태", example = "SUBMITTED")
        String kycStatus, // KYC 상태
        @Schema(description = "보완요청 상태", example = "SUBMITTED")
        String supplementStatus, // 보완요청 상태
        @Schema(description = "제출 일시", example = "2026-05-05T11:00:00")
        LocalDateTime submittedAt, // 제출 일시
        @Schema(description = "결과 메시지", example = "보완 제출이 완료되었습니다.")
        String message // 결과 메시지
) {
}
