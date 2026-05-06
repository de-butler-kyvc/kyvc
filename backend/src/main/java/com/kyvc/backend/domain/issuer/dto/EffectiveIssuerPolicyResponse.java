package com.kyvc.backend.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유효 Issuer 정책 응답
 *
 * @param policies 유효 Issuer 정책 목록
 * @param evaluatedAt 정책 평가 시각
 */
@Schema(description = "유효 Issuer 정책 응답")
public record EffectiveIssuerPolicyResponse(
        @Schema(description = "유효 Issuer 정책 목록")
        List<IssuerPolicyResponse> policies, // 유효 Issuer 정책 목록
        @Schema(description = "정책 평가 시각", example = "2026-05-05T11:00:00")
        LocalDateTime evaluatedAt // 정책 평가 시각
) {
}
