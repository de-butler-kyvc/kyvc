package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * AI 심사 정책 사용 여부 변경 요청 DTO입니다.
 */
@Schema(description = "AI 심사 정책 사용 여부 변경 요청")
public record AiReviewPolicyEnabledUpdateRequest(

        /** 사용 여부 */
        @NotBlank(message = "enabledYn은 필수입니다.")
        @Schema(description = "사용 여부(Y/N)", example = "N")
        String enabledYn
) {
}
