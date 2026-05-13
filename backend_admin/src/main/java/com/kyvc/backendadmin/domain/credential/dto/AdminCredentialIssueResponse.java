package com.kyvc.backendadmin.domain.credential.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VC 발급 요청 응답 DTO입니다.
 */
@Schema(description = "관리자 VC 발급 요청 응답")
public record AdminCredentialIssueResponse(

        /** 생성된 Credential ID */
        @Schema(description = "생성된 Credential ID", example = "1")
        Long credentialId,

        /** Credential 발급 상태 코드 */
        @Schema(description = "Credential 발급 상태 코드", example = "ISSUING")
        String credentialStatusCode,

        /** Core 요청 생성 여부 */
        @Schema(description = "Core 요청 생성 여부", example = "true")
        boolean requested,

        /** Core 요청 ID */
        @Schema(hidden = true)
        @JsonIgnore
        String coreRequestId
) {
}
