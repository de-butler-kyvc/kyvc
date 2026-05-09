package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 발급 요청 DTO입니다.
 */
@Schema(description = "관리자 VC 발급 요청")
public record AdminCredentialIssueRequest(

        /** 중요 작업 승인을 위해 발급받은 MFA 세션 토큰 */
        @NotBlank(message = "mfaToken은 필수입니다.")
        @Schema(description = "중요 작업 승인을 위해 발급받은 MFA 세션 토큰", example = "mfa_session_token")
        String mfaToken,

        /** VC 발급 요청 사유 또는 관리자 메모 */
        @Schema(description = "VC 발급 요청 사유 또는 관리자 메모", example = "KYC 승인 완료에 따른 VC 발급")
        String comment
) {
}
