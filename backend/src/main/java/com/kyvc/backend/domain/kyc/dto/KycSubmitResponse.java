package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 제출 응답
 *
 * @param kycId KYC 신청 ID
 * @param status KYC 상태
 * @param aiReviewStatus AI 심사 상태
 * @param reviewResultId AI 심사 결과 ID
 * @param nextActionCode 다음 행동 코드
 * @param submittedAt 제출 일시
 * @param submittable 제출 가능 여부
 * @param message 제출 결과 메시지
 */
@Schema(description = "KYC 제출 응답")
public record KycSubmitResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태", example = "MANUAL_REVIEW")
        String status, // KYC 상태
        @Schema(description = "AI 심사 상태", example = "LOW_CONFIDENCE")
        String aiReviewStatus, // AI 심사 상태
        @Schema(description = "AI 심사 결과 ID", example = "10")
        Long reviewResultId, // AI 심사 결과 ID
        @Schema(description = "다음 행동 코드", example = "WAIT_MANUAL_REVIEW")
        String nextActionCode, // 다음 행동 코드
        @Schema(description = "제출 일시", example = "2026-05-04T15:00:00")
        LocalDateTime submittedAt, // 제출 일시
        @Schema(description = "제출 가능 여부", example = "true")
        boolean submittable, // 제출 가능 여부
        @Schema(description = "제출 결과 메시지", example = "KYC 신청이 제출되었고 수동 심사로 전환되었습니다.")
        String message // 제출 결과 메시지
) {
}
