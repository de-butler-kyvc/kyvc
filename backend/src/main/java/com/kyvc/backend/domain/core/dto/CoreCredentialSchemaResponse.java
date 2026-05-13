package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core Credential 스키마 응답
 *
 * @param schemaId 스키마 ID
 * @param schemaName 스키마명
 * @param version 스키마 버전
 * @param available 스키마 가용 여부
 * @param message 처리 메시지
 */
@Schema(description = "Core Credential 스키마 응답")
public record CoreCredentialSchemaResponse(
        @Schema(description = "스키마 ID", example = "kyvc-credential-schema-v1")
        String schemaId, // 스키마 ID
        @Schema(description = "스키마명", example = "KYVC Credential Schema")
        String schemaName, // 스키마명
        @Schema(description = "스키마 버전", example = "1.0")
        String version, // 스키마 버전
        @Schema(description = "스키마 가용 여부", example = "true")
        boolean available, // 스키마 가용 여부
        @Schema(description = "처리 메시지", example = "Credential schema metadata returned by Core.")
        String message // 처리 메시지
) {
}
