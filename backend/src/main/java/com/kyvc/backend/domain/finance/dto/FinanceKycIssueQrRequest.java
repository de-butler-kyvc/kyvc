package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC QR 발급 요청
 *
 * @param expiresMinutes 만료 분
 */
@Schema(description = "금융사 방문 KYC QR 발급 요청")
public record FinanceKycIssueQrRequest(
        @Schema(description = "만료 분", example = "10")
        Integer expiresMinutes // 만료 분
) {
}
