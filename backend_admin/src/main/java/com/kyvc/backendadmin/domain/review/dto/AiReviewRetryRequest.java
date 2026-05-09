package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * AI 재심사 요청 DTO입니다.
 */
@Schema(description = "AI 재심사 요청")
public record AiReviewRetryRequest(

        /** AI 재심사 요청 사유 */
        @NotBlank(message = "reason은 필수입니다.")
        @Schema(description = "AI 재심사 요청 사유", example = "문서 불일치 재확인")
        String reason,

        /** 재심사 대상 문서 ID 목록 */
        @Schema(description = "재심사 대상 문서 ID 목록", example = "[1, 2]")
        List<Long> documentIds
) {
}
