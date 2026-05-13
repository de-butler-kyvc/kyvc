package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Wallet Credential 목록 응답
 *
 * @param credentials Wallet Credential 요약 목록
 * @param totalCount Wallet Credential 총 건수
 */
@Schema(description = "Wallet Credential 목록 응답")
public record WalletCredentialListResponse(
        @Schema(description = "Wallet Credential 요약 목록")
        List<WalletCredentialSummaryResponse> credentials, // Wallet Credential 요약 목록
        @Schema(description = "Wallet Credential 총 건수", example = "1")
        int totalCount // Wallet Credential 총 건수
) {
}

