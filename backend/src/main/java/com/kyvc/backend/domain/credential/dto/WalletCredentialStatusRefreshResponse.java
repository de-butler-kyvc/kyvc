package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Wallet Credential 상태 갱신 응답
 *
 * @param credentialId Credential ID
 * @param credentialStatusCode Credential 상태 코드
 * @param xrplTxHash XRPL 트랜잭션 해시
 * @param refreshed 상태 갱신 수행 여부
 * @param refreshedAt 상태 갱신 응답 시각
 * @param message 처리 메시지
 */
@Schema(description = "Wallet Credential 상태 갱신 응답")
public record WalletCredentialStatusRefreshResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "Credential 상태 코드", example = "VALID")
        String credentialStatusCode, // Credential 상태 코드
        @Schema(description = "XRPL 트랜잭션 해시", example = "tx-hash")
        String xrplTxHash, // XRPL 트랜잭션 해시
        @Schema(description = "상태 갱신 수행 여부", example = "true")
        boolean refreshed, // 상태 갱신 수행 여부
        @Schema(description = "상태 갱신 응답 시각", example = "2026-05-07T23:59:59")
        LocalDateTime refreshedAt, // 상태 갱신 응답 시각
        @Schema(description = "처리 메시지", example = "Core 상태조회는 아직 연결하지 않았습니다. 현재 DB 상태를 반환합니다.")
        String message // 처리 메시지
) {
}

