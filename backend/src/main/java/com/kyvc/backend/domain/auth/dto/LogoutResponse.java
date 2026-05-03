package com.kyvc.backend.domain.auth.dto;

/**
 * 로그아웃 응답
 *
 * @param loggedOut 로그아웃 완료 여부
 */
public record LogoutResponse(
        boolean loggedOut // 로그아웃 완료 여부
) {

    /**
     * @return 로그아웃 완료 여부
     */
    public boolean loggedOut() {
        return loggedOut;
    }
}
