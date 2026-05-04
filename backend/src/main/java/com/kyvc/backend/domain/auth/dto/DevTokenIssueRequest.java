package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

/**
 * Dev Token 발급 요청
 *
 * @param email 발급 대상 이메일
 */
@Schema(description = "Dev Token 발급 요청")
public record DevTokenIssueRequest(
        @Schema(description = "발급 대상 이메일", example = "dev-user@kyvc.local")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email // 발급 대상 이메일
) {

    /**
     * @return 발급 대상 이메일
     */
    public String email() {
        return email;
    }
}
