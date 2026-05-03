package com.kyvc.backend.domain.auth.dto;

import java.util.List;

/**
 * 세션 조회 응답
 *
 * @param authenticated 인증 여부
 * @param userId 사용자 ID
 * @param email 로그인 이메일
 * @param userType 사용자 유형
 * @param roles 권한 목록
 * @param corporateId 법인 ID
 * @param corporateRegistered 법인 등록 여부
 */
public record SessionResponse(
        boolean authenticated, // 인증 여부
        Long userId, // 사용자 ID
        String email, // 로그인 이메일
        String userType, // 사용자 유형
        List<String> roles, // 권한 목록
        Long corporateId, // 법인 ID
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
