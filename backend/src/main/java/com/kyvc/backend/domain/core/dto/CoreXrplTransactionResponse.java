package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core XRPL 트랜잭션 조회 응답
 *
 * @param txHash 트랜잭션 해시
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param checkedAt 조회 시각
 */
@Schema(description = "Core XRPL 트랜잭션 조회 응답")
public record CoreXrplTransactionResponse(
        @Schema(description = "트랜잭션 해시", example = "4A5B6C7D8E9F")
        String txHash, // 트랜잭션 해시
        @Schema(description = "처리 상태", example = "PENDING")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "XRPL transaction is pending in stub core.")
        String message, // 처리 메시지
        @Schema(description = "조회 시각", example = "2026-05-06T10:30:00")
        LocalDateTime checkedAt // 조회 시각
) {
}
