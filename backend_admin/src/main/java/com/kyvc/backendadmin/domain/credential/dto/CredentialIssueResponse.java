package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VC 발급 요청 응답 DTO입니다.
 */
@Schema(description = "VC 발급 요청 응답")
public record CredentialIssueResponse(

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** Core 요청 ID */
        @Schema(description = "Core 요청 ID", example = "VC_ISSUE-1-550e8400-e29b-41d4-a716-446655440000")
        String coreRequestId,

        /** Credential 유형 */
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType,

        /** Credential 상태 */
        @Schema(description = "Credential 상태", example = "ISSUING")
        String status
) {
}
