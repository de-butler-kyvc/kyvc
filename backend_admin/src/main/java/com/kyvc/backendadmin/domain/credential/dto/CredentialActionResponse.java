package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VC 재발급 또는 폐기 요청 처리 결과 DTO
 */
@Schema(description = "VC 재발급 또는 폐기 요청 처리 결과")
public record CredentialActionResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID

        @Schema(description = "Credential 요청 이력 ID", example = "100")
        Long credentialRequestId, // Credential 요청 이력 ID

        @Schema(description = "요청 유형 코드", example = "REISSUE")
        String requestType, // 요청 유형 코드

        @Schema(description = "요청 상태 코드", example = "REQUESTED")
        String requestStatus, // 요청 상태 코드

        @Schema(description = "요청 시각")
        LocalDateTime requestedAt, // 요청 시각

        @Schema(description = "요청 처리 메시지", example = "VC 재발급 요청이 접수되었습니다.")
        String message // 요청 처리 메시지
) {
    /**
     * 요청 이력 ID 없는 성공 응답 생성
     *
     * @param credentialId Credential ID
     * @param requestType 요청 유형 코드
     * @param message 처리 메시지
     * @return 요청 처리 결과
     */
    public static CredentialActionResponse accepted(Long credentialId, String requestType, String message) {
        return new CredentialActionResponse(credentialId, null, requestType, "REQUESTED", null, message);
    }

    /**
     * 요청 이력 ID 포함 성공 응답 생성
     *
     * @param credentialId Credential ID
     * @param credentialRequestId Credential 요청 이력 ID
     * @param requestType 요청 유형 코드
     * @param requestStatus 요청 상태 코드
     * @param requestedAt 요청 시각
     * @param message 처리 메시지
     * @return 요청 처리 결과
     */
    public static CredentialActionResponse accepted(
            Long credentialId,
            Long credentialRequestId,
            String requestType,
            String requestStatus,
            LocalDateTime requestedAt,
            String message
    ) {
        return new CredentialActionResponse(credentialId, credentialRequestId, requestType, requestStatus, requestedAt, message);
    }
}
