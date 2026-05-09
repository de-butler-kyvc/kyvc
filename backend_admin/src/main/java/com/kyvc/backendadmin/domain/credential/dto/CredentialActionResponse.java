package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VC 재발급 또는 폐기 요청 처리 결과 응답입니다.
 */
@Schema(description = "VC 재발급 또는 폐기 요청 처리 결과")
public record CredentialActionResponse(
        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** 요청 액션 */
        @Schema(description = "요청 액션", example = "REISSUE")
        String action,

        /** 요청 접수 여부 */
        @Schema(description = "요청 접수 여부", example = "true")
        boolean requested,

        /** Backend 또는 Core 요청 ID */
        @Schema(description = "Backend 또는 Core 요청 ID", example = "VC_REISSUE-1-uuid")
        String requestId,

        /** 요청 처리 메시지 */
        @Schema(description = "요청 처리 메시지", example = "VC 재발급 요청이 접수되었습니다.")
        String message
) {
    /**
     * Backend 응답에 명시 데이터가 없을 때 사용할 기본 성공 응답을 생성합니다.
     *
     * @param credentialId Credential ID
     * @param action 요청 액션
     * @param message 처리 메시지
     * @return 요청 처리 결과
     */
    public static CredentialActionResponse accepted(Long credentialId, String action, String message) {
        return new CredentialActionResponse(credentialId, action, true, null, message);
    }
}
