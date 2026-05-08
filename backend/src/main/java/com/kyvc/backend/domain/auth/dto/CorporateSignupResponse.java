package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 사용자 회원가입 응답
 *
 * @param userId 사용자 ID
 * @param email 로그인 이메일
 * @param userName 사용자명
 * @param phone 사용자 연락처
 * @param userType 사용자 유형
 * @param userStatus 사용자 상태
 */
@Schema(description = "법인 사용자 회원가입 응답")
public record CorporateSignupResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "로그인 이메일", example = "user@kyvc.local")
        String email, // 로그인 이메일
        @Schema(description = "사용자명", example = "홍길동")
        String userName, // 사용자명
        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        String phone, // 사용자 연락처
        @Schema(description = "사용자 유형", example = "CORPORATE_USER")
        String userType, // 사용자 유형
        @Schema(description = "사용자 상태", example = "ACTIVE")
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
     * @return 사용자명
     */
    public String userName() {
        return userName;
    }

    /**
     * @return 사용자 연락처
     */
    public String phone() {
        return phone;
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
