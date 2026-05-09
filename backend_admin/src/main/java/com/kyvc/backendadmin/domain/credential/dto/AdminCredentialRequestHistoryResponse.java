package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential 요청 이력 응답 DTO입니다.
 */
@Schema(description = "Credential 요청 이력 응답")
public record AdminCredentialRequestHistoryResponse(

        /** Credential 요청 이력 ID */
        @Schema(description = "Credential 요청 이력 ID", example = "1")
        Long credentialRequestId,

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** 요청 유형 코드 */
        @Schema(description = "요청 유형 코드", example = "VC_ISSUE")
        String requestTypeCode,

        /** 요청 상태 코드 */
        @Schema(description = "요청 상태 코드", example = "REQUESTED")
        String requestStatusCode,

        /** 사유 코드 */
        @Schema(description = "사유 코드")
        String reasonCode,

        /** 요청 사유 */
        @Schema(description = "요청 사유", example = "KYC 승인 완료에 따른 VC 발급")
        String reason,

        /** 요청자 유형 코드 */
        @Schema(description = "요청자 유형 코드", example = "ADMIN")
        String requestedByTypeCode,

        /** 요청자 ID */
        @Schema(description = "요청자 ID", example = "1")
        Long requestedById,

        /** 요청 시각 */
        @Schema(description = "요청 시각")
        LocalDateTime requestedAt,

        /** 완료 시각 */
        @Schema(description = "완료 시각")
        LocalDateTime completedAt,

        /** Core 요청 ID */
        @Schema(description = "Core 요청 ID")
        String coreRequestId
) {
}
