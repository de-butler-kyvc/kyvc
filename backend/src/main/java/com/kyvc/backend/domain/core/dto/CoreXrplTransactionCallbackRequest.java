package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core XRPL 트랜잭션 Callback 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param status Callback 처리 상태
 * @param txHash 트랜잭션 해시
 * @param ledgerIndex Ledger 인덱스
 * @param errorMessage 실패 메시지
 * @param confirmedAt 확정 시각
 */
@Schema(description = "Core XRPL 트랜잭션 Callback 요청")
public record CoreXrplTransactionCallbackRequest(
        @Schema(description = "Core 요청 ID", example = "42e42720-66ed-48f1-a4b2-f6ea3ff5594c")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Callback 처리 상태", example = "SUCCESS")
        String status, // Callback 처리 상태
        @Schema(description = "트랜잭션 해시", example = "4A5B6C7D8E9F")
        String txHash, // 트랜잭션 해시
        @Schema(description = "Ledger 인덱스", example = "9345454")
        String ledgerIndex, // Ledger 인덱스
        @Schema(description = "실패 메시지", example = "XRPL transaction failed")
        String errorMessage, // 실패 메시지
        @Schema(description = "확정 시각", example = "2026-05-06T17:00:00")
        LocalDateTime confirmedAt // 확정 시각
) {
}
