package com.kyvc.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 보완 제출 요청
 *
 * @param submittedComment 제출 코멘트
 */
@Schema(description = "보완 제출 요청")
public record SupplementSubmitRequest(
        @Schema(description = "제출 코멘트", example = "요청하신 서류를 보완 제출했습니다.")
        String submittedComment // 제출 코멘트
) {
}
