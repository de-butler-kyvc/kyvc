package com.kyvc.backend.domain.auth.dto;

/**
 * 법인 사용자 회원가입 응답
 *
 * @param userId 사용자 ID
 * @param email 로그인 이메일
 * @param userType 사용자 유형
 * @param userStatus 사용자 상태
 */
public record CorporateSignupResponse(
        Long userId, // 사용자 ID
        String email, // 로그인 이메일
        String userType, // 사용자 유형
        String userStatus // 사용자 상태
) {

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
     * @return 사용자 상태
     */
    public String userStatus() {
        return userStatus;
    }
}
