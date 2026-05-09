package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential 상태 변경 이력 응답 DTO입니다.
 */
@Schema(description = "Credential 상태 변경 이력 응답")
public record AdminCredentialStatusHistoryResponse(

        /** 상태 변경 이력 ID */
        @Schema(description = "상태 변경 이력 ID", example = "1")
        Long historyId,

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** 변경 전 상태 코드 */
        @Schema(description = "변경 전 상태 코드")
        String beforeStatusCode,

        /** 변경 후 상태 코드 */
        @Schema(description = "변경 후 상태 코드", example = "ISSUING")
        String afterStatusCode,

        /** 사유 코드 */
        @Schema(description = "사유 코드")
        String reasonCode,

        /** 변경 사유 */
        @Schema(description = "변경 사유", example = "KYC 승인 완료에 따른 VC 발급")
        String reason,

        /** 변경자 유형 코드 */
        @Schema(description = "변경자 유형 코드", example = "ADMIN")
        String changedByTypeCode,

        /** 변경자 ID */
        @Schema(description = "변경자 ID", example = "1")
        Long changedById,

        /** 변경 시각 */
        @Schema(description = "변경 시각")
        LocalDateTime changedAt
) {
}
