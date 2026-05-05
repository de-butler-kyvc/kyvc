package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 관리자 MFA challenge 생성 요청 DTO
/**
 * 관리자 MFA challenge 생성 요청 DTO입니다.
 *
 * <p>MFA 채널과 목적을 받아 이메일 인증번호 발급에 사용합니다.</p>
 */
@Schema(description = "관리자 MFA challenge 생성 요청")
public record AdminMfaChallengeRequest(
        @Schema(description = "MFA 채널, EMAIL만 허용", example = "EMAIL")
        @NotBlank String channel, // MFA 채널, EMAIL만 허용
        @Schema(description = "MFA 목적", example = "LOGIN")
        @NotBlank String purpose // MFA 목적
) {
}
