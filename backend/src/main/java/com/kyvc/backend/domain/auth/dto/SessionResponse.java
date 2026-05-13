package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 세션 조회 응답
 *
 * @param authenticated 인증 여부
 * @param userId 사용자 ID
 * @param email 로그인 이메일
 * @param userName 사용자명
 * @param userType 사용자 유형
 * @param roles 권한 목록
 * @param corporateId 법인 ID
 * @param corporateRegistered 법인 등록 여부
 * @param corporateName 회원가입 온보딩 단계 입력 법인명
 * @param userStatus 사용자 상태
 */
@Schema(description = "세션 조회 응답")
public record SessionResponse(
        @Schema(description = "회원가입 온보딩 단계 입력 법인명", example = "주식회사 케이원")
        String corporateName, // 회원가입 온보딩 단계 입력 법인명
        @Schema(description = "사용자 상태", example = "ACTIVE")
        String userStatus, // 사용자 상태
        @Schema(description = "인증 여부", example = "true")
        boolean authenticated, // 인증 여부
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "로그인 이메일", example = "user@kyvc.local")
        String email, // 로그인 이메일
        @Schema(description = "사용자명", example = "홍길동")
        String userName, // 사용자명
        @Schema(description = "사용자 유형", example = "CORPORATE_USER")
        String userType, // 사용자 유형
        @Schema(description = "권한 목록", example = "[\"ROLE_CORPORATE_USER\"]")
        List<String> roles, // 권한 목록
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "법인 등록 여부", example = "true")
        boolean corporateRegistered // 법인 등록 여부
) {

    public SessionResponse {
        roles = List.copyOf(roles);
    }

    /**
     * @return 인증 여부
     */
    public boolean authenticated() {
        return authenticated;
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
     * @return 사용자명
     */
    public String userName() {
        return userName;
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

    /**
     * @return 법인 ID
     */
    public Long corporateId() {
        return corporateId;
    }

    /**
     * @return 법인 등록 여부
     */
    public boolean corporateRegistered() {
        return corporateRegistered;
    }
}
