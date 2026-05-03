package com.kyvc.backend.domain.auth.dto;

import java.util.List;

/**
 * 로그인 응답
 *
 * @param userId 사용자 ID
 * @param email 로그인 이메일
 * @param userType 사용자 유형
 * @param roles 권한 목록
 */
public record LoginResponse(
        Long userId, // 사용자 ID
        String email, // 로그인 이메일
        String userType, // 사용자 유형
        List<String> roles // 권한 목록
) {

    public LoginResponse {
        roles = List.copyOf(roles);
    }

    /**
     * @return 사용자 ID
     */
    public Long userId() {
        return userId;
    }

    /**
     * @return 로그인 이메일
     */
    public String email() {
        return email;
    }

    /**
     * @return 사용자 유형
     */
    public String userType() {
        return userType;
    }

    /**
     * @return 권한 목록
     */
    public List<String> roles() {
        return roles;
    }
}
