package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 관리자 MFA 인증번호 검증 요청 DTO
/**
 * 관리자 MFA 인증번호 검증 요청 DTO입니다.
 *
 * <p>challenge 식별자와 사용자가 입력한 인증번호를 받아 MFA 검증에 사용합니다.</p>
 */
@Schema(description = "관리자 MFA 인증번호 검증 요청")
public record AdminMfaVerifyRequest(
        @Schema(description = "MFA challenge 식별자")
        @NotBlank String challengeId, // MFA challenge 식별자
        @Schema(description = "6자리 인증번호", example = "123456")
        @NotBlank String verificationCode // 6자리 인증번호
) {
}
