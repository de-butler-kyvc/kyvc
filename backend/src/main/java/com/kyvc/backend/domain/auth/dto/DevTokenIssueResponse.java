package com.kyvc.backend.domain.auth.dto;

import java.util.List;

/**
 * Dev Token 발급 응답
 *
 * @param userId 사용자 ID
 * @param email 사용자 이메일
 * @param userType 사용자 유형
 * @param userStatus 사용자 상태
 * @param created 신규 생성 여부
 * @param roles 권한 목록
 */
public record DevTokenIssueResponse(
        Long userId, // 사용자 ID
        String email, // 사용자 이메일
        String userType, // 사용자 유형
        String userStatus, // 사용자 상태
        boolean created, // 신규 생성 여부
        List<String> roles // 권한 목록
) {

    public DevTokenIssueResponse {
        roles = List.copyOf(roles);
    }

    /**
     * @return 사용자 ID
     */
    public Long userId() {
        return userId;
    }

    /**
     * @return 사용자 이메일
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

    /**
     * @return 신규 생성 여부
     */
    public boolean created() {
        return created;
    }

    /**
     * @return 권한 목록
     */
    public List<String> roles() {
        return roles;
    }
}
