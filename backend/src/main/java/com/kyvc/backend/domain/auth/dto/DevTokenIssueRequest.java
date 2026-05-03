package com.kyvc.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;

/**
 * Dev Token 발급 요청
 *
 * @param email 발급 대상 이메일
 */
public record DevTokenIssueRequest(
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
