package com.kyvc.backend.domain.auth.dto;

/**
 * 토큰 재발급 응답
 *
 * @param refreshed 재발급 완료 여부
 */
public record TokenRefreshResponse(
        boolean refreshed // 재발급 완료 여부
) {

    /**
     * @return 재발급 완료 여부
     */
    public boolean refreshed() {
        return refreshed;
    }
}
