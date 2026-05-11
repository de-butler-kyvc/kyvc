package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Credential 상태 변경 이력 응답
 *
 * @param items 상태 변경 이력 목록
 */
@Schema(description = "Credential 상태 변경 이력 응답")
public record CredentialStatusHistoryResponse(
        @Schema(description = "상태 변경 이력 목록")
        List<Item> items // 상태 변경 이력 목록
) {

    public CredentialStatusHistoryResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Credential 상태 변경 이력 항목
     *
     * @param historyId 상태 이력 ID
     * @param credentialId Credential ID
     * @param fromStatusCode 변경 전 상태 코드
     * @param toStatusCode 변경 후 상태 코드
     * @param reason 변경 사유
     * @param changedAt 변경 일시
     */
    @Schema(description = "Credential 상태 변경 이력 항목")
    public record Item(
            @Schema(description = "상태 이력 ID", example = "1")
            Long historyId, // 상태 이력 ID
            @Schema(description = "Credential ID", example = "10")
            Long credentialId, // Credential ID
            @Schema(description = "변경 전 상태 코드", example = "ISSUING")
            String fromStatusCode, // 변경 전 상태 코드
            @Schema(description = "변경 후 상태 코드", example = "VALID")
            String toStatusCode, // 변경 후 상태 코드
            @Schema(description = "변경 사유", example = "VC 발급 완료")
            String reason, // 변경 사유
            @Schema(description = "변경 일시", example = "2026-05-11T10:00:00")
            LocalDateTime changedAt // 변경 일시
    ) {
    }
}
