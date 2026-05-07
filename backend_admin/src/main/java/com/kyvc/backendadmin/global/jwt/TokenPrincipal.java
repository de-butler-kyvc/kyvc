package com.kyvc.backendadmin.global.jwt;

import java.util.List;

// JWT 발급에 필요한 공통 행위자 정보
/**
 * JWT 발급에 필요한 공통 행위자 정보입니다.
 *
 * <p>관리자와 사용자 등 토큰 발급 대상의 ID, 이메일, actorType, 권한 목록을
 * JwtTokenProvider에 전달하기 위해 사용합니다.</p>
 */
public record TokenPrincipal(
        // JWT subject에 들어갈 행위자 ID
        Long actorId,
        // 토큰 소유자의 이메일
        String email,
        // ADMIN, USER 등 토큰 소유자 유형
        String actorType,
        // ROLE_ 접두사를 포함하거나 포함하지 않은 권한 목록
        List<String> roles
) {
}
