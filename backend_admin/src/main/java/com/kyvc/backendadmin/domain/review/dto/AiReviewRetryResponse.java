package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 재심사 요청 생성 응답 DTO입니다.
 */
@Schema(description = "AI 재심사 요청 생성 응답")
public record AiReviewRetryResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** 생성된 Core 요청 ID */
        @Schema(description = "생성된 Core 요청 ID", example = "AI_REVIEW-100-550e8400-e29b-41d4-a716-446655440000")
        String coreRequestId,

        /** Core 요청 유형 */
        @Schema(description = "Core 요청 유형", example = "AI_REVIEW")
        String requestType,

        /** Core 요청 상태 */
        @Schema(description = "Core 요청 상태", example = "QUEUED")
        String status
) {
}
