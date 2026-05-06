package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 대리인 정보 저장 요청
 *
 * @param name 대리인명
 * @param phoneNumber 대리인 연락처
 * @param email 대리인 이메일
 * @param authorityScope 대리인 권한 범위
 */
@Schema(description = "대리인 정보 저장 요청")
public record AgentRequest(
        @Schema(description = "대리인명", example = "김대리")
        @NotBlank(message = "대리인명은 필수입니다.")
        String name, // 대리인명
        @Schema(description = "대리인 연락처", example = "010-9876-5432")
        String phoneNumber, // 대리인 연락처
        @Schema(description = "대리인 이메일", example = "agent@kyvc.local")
        @Email(message = "대리인 이메일은 올바른 이메일 형식이어야 합니다.")
        String email, // 대리인 이메일
        @Schema(description = "대리인 권한 범위", example = "KYC 신청 대행")
        String authorityScope // 대리인 권한 범위
) {
}
